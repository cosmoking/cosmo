/*******************************************************************************
 * Copyright 2012 Jack Wang
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.cosmo.common.record;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import org.cosmo.common.record.Defn.Header;
import org.cosmo.common.util.BitsUtil;
import org.cosmo.common.util.Bytes;
import org.cosmo.common.util.New;
import org.cosmo.common.util.Util;

/**
 *
 * TODO XXX
 *  - use reusable byte[] array buffer in readEntry,
 *  - read ahead buffer (ie, 4mb chunks)
 *  - IMPORTANT - duplicated records ARE NOT saved in the MasterLog - hence slave would not know, so does master when it restarts.
 *
 * RecordLog is used to "create" log entries to keep track of what records are being inserted/updated/deleted.
 *
 * From slave's point of view - recordLog entries are created each time any Record write operation (insert, update, or delete) is done
 * in the slave's jvm. Instead of writing into the actual RecordStore files it only creates RecordLog.Entry and writes in the Log.
 * It does not check for index or duplicates. The idea is slave only generates data and to be consumed and recreated in the master
 *
 * From master's point of view - like slave, recordLog entries are also created each Record write operation. The main difference is that
 * inside master vm. There are threads created for each slave to listen for slave's recordLog. These recordLogs are read into RecordLog Entry
 * and then being recreated (insert, updated, or delete) in master's RecordStore file system. In addition to that, each of this operation
 * also creates recordLog entries in master's recordLog file. The main different is that, the recordLog entry's "src" and "srcPos" refers
 * to the "slave". This allows a clear distinction the real source of the entry from master's log.
 *
 * Master's log also serves couple other purposes, it allows slave to subscribe to "operation events" that are happening in the master
 * this allows slaves's to get the latest updates from master. Such mechenism allows slave's "RecordStore.consumer" (ie cache) continue
 * to work.
 *
 * Another purpose of master's log is it allows master and slave to survive restarts - ie either master or slave dies it can start
 * and come back where it left off (fault tolrent). when master comes back online, it will scan the master log and finds where it left
 * off for each slave log (using entry's src, and srcpos).
 *
 * For slave the restart is different - the slave will always read from the "end" of master's log.
 *
 * A note about RecordStore's "consumer.sync()" - consumer in recordstore basically allows recordstore to create processed "cache"
 * on defn (column). it's sync when instance is started and then notified each time when there is an operation performed on the column.
 *
 * In slave, this is done when slave is started it sync's master recordstore file and then listens's to master log. However, it could
 * get out of sync when the time the slave is syncing - changes in master are also happening at same time!! and listening at end of  file
 * does not solve the problem either because it's changing too.
 *
 * Thus we allow "recordLog.lock" file to be created on either master of slave main directory. This file is checked each time
 * when an log entry is written. When present it halts that method. since logEntry insert is the last operation for recordStore.write()
 * it basically halts all write operation.  Using this mechenism we can basically make master stop write and bring up slave to sync
 * correctly. upon slave is done syncing, then remove the lock and resume master.
 *
 * recordLog files are also being rolled over upon reaching the limit - ie 64mb.
 *
 */

/*
 * TODO -
 * changed Index.MergeMaxDoc thingy - check regression! (allows per doc commit, but very slow, so right now we do batch index commit()
 * convert system.out to log category
 * convert RecordLog.Instance() to global rather by meta
 * optimize payload, ie checksum? header? combine  src,srcid,fileid to be Bit.Spec
 * revisit "fileLock" stuff - rightnow is manual, does pulling on every writeEntry(), use notify() when slave starts, and done when finishes
 * coniguration by params
 *
 * Need a way to recover from IOException due to network issue. ie Thread.run() place
 */



public class RecordLog implements Control.Haltable
{
		// for each batch it will consume at this rate
	private static final int MaxConsumeRatePerBatch = Util.getProperty(Integer.class, "RecordLogMaxConsumeRatePerBatch", Meta.Mode._isMaster ? 3000 : 10000);
		// the time it waits before process each batch
	private final int WaitTimeBetweenBatch = Meta.Mode._isMaster ? 3000 : 4000;
		// wait time when there is no record
	private final int WaitTimeWhenNoRecords = Meta.Mode._isMaster ? 2000 : 3000;
		// time it checks before the log is active
	private final int WaitTimeUntilLogIsActive = Meta.Mode._isMaster ? 5000 : 5000;

	private final long LogRollOverSize = 64 * 1024 * 1024;
	private final int LogFileSequenceDigits = 8;

	private final Control.Lock _lock;
	private final Meta.Mode _src;

	private FileChannel _io;
	private ByteBuffer _ioBuf;
	private File _file;
	private int _fileId;
	private Map<Meta.Mode, RecordLog.Journal> _masterLogJournal; // only used by master to keep track of slaves checkpoints
	private boolean[] _dirtyMetaIds; // track for metas that have entries being processed for create/update/delete

	public static final String LogFilePrefix = "recordLog_";

	final static RecordLog Instance;
	static {
		try {
			Instance = new RecordLog(Meta.Mode);
			Instance.initialize(true);
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
			throw new IllegalArgumentException(ioe);
		}
	}

	public static RecordLog instance (Meta meta)
	{
		return Instance;
	}


	private RecordLog (Meta.Mode src)
	  throws IOException
	{
		_lock = Control.haltableLock(this, RecordLog.class.getSimpleName());
		_src = src;
		_file = firstLogFile();
		_ioBuf = ByteBuffer.allocate(0);
		_dirtyMetaIds = new boolean[Meta.ClassMetaList.size()];
	}


	public RecordLog initialize (boolean write)
	  throws IOException
	{
		if (!write && !_file.exists()) {
			throw new FileNotFoundException(_file.getAbsolutePath());
		}
		_io = new RandomAccessFile(_file, write ? "rw" : "r").getChannel();
		String filename = _file.getAbsolutePath();
		_fileId = org.cosmo.common.util.Util.zeroPaddedIntStr(filename, filename.length() - LogFileSequenceDigits, LogFileSequenceDigits);
		if (write) {
			_io.position(_io.size());
		}
		return this;
	}


	public void rollOver (File newFile, boolean write)
	  throws IOException
	{
		_io.close();
		_file = newFile;
		_ioBuf = ByteBuffer.allocate(0);  // reset buffer
		initialize(write);
	}

	void rollOverAndArchive (File newFile, boolean write, File archiveDir)
	  throws IOException
	{
			// compose current File,and archive File before rollover
		File currentFile = _file;
		File archiveFile = new File(archiveDir, currentFile.getName());

		rollOver(newFile, write);

			// This can only be done after rollover because then FileChannel  of "currentFile"
			// is closed to allow rename.
		currentFile.renameTo(archiveFile);
	}


	private File firstLogFile ()
	{
		TreeSet<File> files = org.cosmo.common.util.Util.filesStartsWith(_src._logFile);
		return files.isEmpty() ? nextLogFile() : files.first();
	}


	private File nextLogFile ()
	{
		if (_file == null) {
			return file(0);
		}
		String curFile = _file.getAbsolutePath();
		int curFileId = org.cosmo.common.util.Util.zeroPaddedIntStr(curFile, curFile.length() - LogFileSequenceDigits, LogFileSequenceDigits);
		return file(++curFileId);
	}


	public File file (int fileId)
	{
		return new File (New.str(_src._logFilename, "_", org.cosmo.common.util.Util.zeroPaddedInt(fileId, LogFileSequenceDigits)));
	}

	public Entry newEntry (Operation operation, Meta meta, long recordId)
	{
		return new Entry(operation, meta, recordId);
	}

	public void writeEntry (Entry entry)
	  throws IOException
	{
		if (entry != null) {
			_lock.lock();
			try {
				byte[] entryBytes = entry._bytes.bytes();

				int size = entry._bytes.count() - 24; // size excluding the "size" header (-4) and MagicHeader (-4), src, id, checksum, total of  6 * 4 = 20 bytes
				int checksum = 13342; // whatever for now;

					// if this thread is currently one of "master's slave consumer thread"
					// then this entry must set the src and the file info to be slave's since this write
					// comes from consuming slave's recordLog, this allows later back tracking by master upon failover
				Consumer consumer = consumerThread();
				int src = consumer == null ? Meta.Mode.ordinal() : consumer._log._src.ordinal();
	            int fileId = consumer == null ? RecordLog.Instance._fileId : consumer._log._fileId;
	            										// this gives the actual file pos since file pos does not change when read into buffer
				long filePos = consumer == null ? RecordLog.Instance._io.position() : consumer._log._io.position() + consumer._log._ioBuf.position();


				BitsUtil.putInt(entryBytes, 4, src);
				BitsUtil.putInt(entryBytes, 8, fileId);
				BitsUtil.putInt(entryBytes, 12, (int)filePos);
				BitsUtil.putInt(entryBytes, 16, checksum);
				BitsUtil.putInt(entryBytes, 20, size);
				_io.write(ByteBuffer.wrap(entryBytes, 0, entry._bytes.count()));  // one shot

				if (RecordLog.Instance._io.size() > LogRollOverSize) {
					RecordLog.Instance.rollOver(RecordLog.Instance.nextLogFile(), true);
				}
			}
			finally {
				_lock.unlock();
			}
		}
	}

	@Override
	public Control.Lock halt ()
	{
		_lock.lock();
		return _lock;
	}


	private Consumer consumerThread ()
	{
		Thread t = Thread.currentThread();
		if (t instanceof Consumer) {
			return (Consumer)t;
		}
		return null;
	}





	private void readEntry (RecordLog.Journal journal, boolean skipProcessing, boolean notifyDefnListenerOnly)
	  throws Exception
	{
			// header
		int magicHeader = _ioBuf.getInt();
		if (magicHeader != Entry.MagicHeader) {
			throw new Exception("HEADER NOT MATCH");
		}

			// read src, srcPos, checksum, entrySize
		int srcId =_ioBuf.getInt();
		int fileId =_ioBuf.getInt();
		int filePos =_ioBuf.getInt();
		int checksum =_ioBuf.getInt();
		int entrySize =_ioBuf.getInt();
		int entryBoundary = _ioBuf.position() + entrySize;


			// update journal
		if (journal != null) {
			journal._src = Meta.Mode.Lookup[srcId];
			journal._fileId = fileId;
			journal._filePos = filePos;
		}

		if (skipProcessing) {
				// move ioBuf position accordingly for next read
			_ioBuf.position(entryBoundary);
			return;
		}


			// read op, meta, id
		Operation operation = Operation.Lookup[_ioBuf.get()];
		Meta meta = Meta.ClassMetaList.get(_ioBuf.getInt());
		long recordId = _ioBuf.getLong();

			// mark metas that have changes
		_dirtyMetaIds[meta._arrayId] = true;


			// XXX For SLAVE only notify listener on changes
		if (notifyDefnListenerOnly && operation != Operation.Notify) {
			notifyDefnListener(entryBoundary, meta, recordId, Meta.Mode.Lookup[srcId], operation);
			return;
		}


		if (Operation.Insert == operation) {

			Record record = (Record)meta._clazz.newInstance();
			while (_ioBuf.position() < entryBoundary) {
				Defn defn = (Defn)meta.defns().get(_ioBuf.getInt());
				Object value = bytesToValue(defn);
				defn.setValue(record, value);

			}

					// Note: we are skipping DuplicateRecordException,
					// complication is - this may need to propoage back to slave
					// also, master will have smaller logs compare to slave during (1 to 1 replication) test assertions.
			record.tx().insert(true, false);

			if (journal != null) {
				journal._record = record;
			}

			//System.out.println(New.str("Inserted [", meta._clazz.getSimpleName(), "] src [", Meta.Mode.Lookup[srcId].name(), "] srcPos [", filePos,"] logPos [", _ioBuf.position(), "] logLen [", _io.size(), "] file [", _file, "]"));
			return;
		}
		if (Operation.Update == operation) {
			while (_ioBuf.position() < entryBoundary) {
				Defn defn = (Defn)meta.defns().get(_ioBuf.getInt());
				Object value = bytesToValue(defn);
				defn.update(recordId, value);
				//System.out.println(New.str("Updated [", defn.getDeclaringFieldName(), "] src [", Meta.Mode.Lookup[srcId].name(), "] srcPos [", filePos,"] logPos [", _ioBuf.position(), "] logLen [", _io.size(), "] file [", _file, "]"));
			}
			return;
		}

		if (Operation.Delete == operation) {

			throw new java.lang.UnsupportedOperationException("XXX not implemented yet");
		}

		if (Operation.Notify == operation) {
			byte[] payload = new byte[_ioBuf.remaining()];
			_ioBuf.get(payload, 0, payload.length);

			New.prt("Received Notification from [",Meta.Mode.Lookup[srcId].name(), "] with data [",  new String(payload), "]");
			return;
		}

		throw new java.lang.UnsupportedOperationException("OP delate");
	}


	private Object bytesToValue (Defn defn)
	  throws IOException
	{
		byte header = _ioBuf.get();
		Object value = null;
		if (header != Header.IsNull._asByte) {
			int defnValueSize = defn instanceof DefnBytes ?  _ioBuf.getInt() : defn.size() - Defn.HeaderByteSize ;
			byte[] valueBytes = new byte[defnValueSize];
			_ioBuf.get(valueBytes, 0, valueBytes.length);
			value = defn.readImpl(ByteBuffer.wrap(valueBytes), true);
		}
		return value;
	}

	private void notifyDefnListener (int bufBoundary, Meta meta, long id, Meta.Mode src, Operation operation)
	  throws IOException
	{
		while (_ioBuf.position() < bufBoundary) {
			Defn defn = (Defn)meta.defns().get(_ioBuf.getInt());
			Object value = bytesToValue(defn);
			defn.notifyListener(value, id, true);
		}

			// ONLY notify insert operation that originated from this src - see tx.insertSync()
		if (Meta.Mode == src && Operation.Insert == operation) {
			synchronized (meta.store().NotifyOnInsert) {
				//System.out.println(New.str("Notifying [", Thread.currentThread().toString(), "] [", meta.store().NotifyOnCreate, "]"));
				meta.store().NotifyOnInsert.notifyAll();
			}
		}
	}


	private long processBatch (int maxConsumeRatePerBatch, int waitTimeBetweenBatch)
	  throws Exception
	{
		long filePos = _io.position();
		long fileSize = _io.size();
		int readSize = (int)(fileSize - filePos);

			// ioBuf byte[] are cached to reduce  byte array creation for each batch.. unless
			// buffer is too small - the max is bounded too "logRollOverSize",
			// it also gets reset each time file is rolled over to avoid occasion big allocations
			//
			// Note. in order to cache it will read the entire log over network into the local memory
			// this means there will be long pauses each time a new file is read on slave threads since
			// it will try to buffered up the entire log file
			//
			// i.e. something like below from the console:
			// previous buffer: 0
			// current buffer: 67109532 required size : 67109532
			//

		if (_ioBuf.capacity() < readSize) {
			New.prt("previous buffer: ", _ioBuf.capacity());
			_ioBuf = ByteBuffer.wrap(new byte[readSize]);
		}
		New.prt("current buffer: ", _ioBuf.capacity(), " required size : ", readSize);
		_ioBuf.clear();
		_io.read(_ioBuf, filePos);
		_ioBuf.limit(readSize);
		_ioBuf.rewind();

			// for all the data in buffer
		while (_ioBuf.hasRemaining()) {

			int processedCount = 0;
			int beginPos = _ioBuf.position();

				// this is the critical section - as it's doing batch commit
			synchronized (Consumer.ProcessBatchMutex) {
				for (; processedCount < maxConsumeRatePerBatch && _ioBuf.hasRemaining(); processedCount++) {
					readEntry(null, false, Meta.Mode._isSlave);
				}

					// commit index if there has been records added for that given meta
					// currently done at batch level bcos lucene is too slow if we do this for every
					// record
				for (int i = 0; i < _dirtyMetaIds.length; i++) {
					if (_dirtyMetaIds[i]) {
						Meta.ClassMetaList.get(i).index().commit();
						_dirtyMetaIds[i] = false; // reset
					}
				}
			}
			Thread.currentThread().yield();
			New.prt("[", Meta.Mode.name(), "] consuming [", processedCount, "] from [", _src.name(), ":", _fileId, "] from [",  filePos + beginPos, "] to [", filePos + _ioBuf.position(), "] of size [", fileSize, "]");

				// break interval when there are records, for master allow slave not not starve each other, for slave, tunes consume rate
			org.cosmo.common.util.Util.sleep(waitTimeBetweenBatch);
		}
		_io.position(fileSize);
		return _io.position();
	}


	public synchronized void recoverFromError (RecordLog log)
	{
		if (Meta.Mode._isMaster) {
			if (_masterLogJournal != null) {
				_masterLogJournal.clear(); // clear so that moveToLastProcessedPos() will reload masterLogJournal
			}
		}
		if (Meta.Mode._isSlave) {
			// basically should create a lock file on master and make it stop, slave restart, then release the lock
			// just like a complete slave restart basically. need to do this.
		}

	}


	/*
	 *  This replays master log and check what is the last "committed" entry id from the slave log. Typically scenario
	 *  is master died, and rebooted and this would scanned to last process pos.
	 *  Note - when master encounters "DuplicateRecordException" or other exception the entry is not written
	 *  the affect of this is that we'll reread the same record from slave which is ok in that
	 *  it's just hit DuplicateException again
	 */
	public synchronized void moveToLastProcessedPos (RecordLog log)
	  throws Exception
	{
		if (Meta.Mode._isMaster) {
				// iterate through master log ONCE and find out all the last read point for all slaves
				// this method gets called once by all slave threads once. but really only gets execute
				// once as it does the isEmpty() the lazy init check
			if  (_masterLogJournal == null) {
				_masterLogJournal = new HashMap();
				while (true) {
					_ioBuf = ByteBuffer.allocate((int)RecordLog.Instance._io.size());
					RecordLog.Instance._io.read(_ioBuf, 0);
					_ioBuf.rewind();
					while (_ioBuf.hasRemaining()) {
						RecordLog.Journal journal = new RecordLog.Journal();  // reuse object
						readEntry(journal, true, false);
						_masterLogJournal.put(journal._src, journal);
					}

					New.prt("Scanned RecordLog [", RecordLog.Instance._file, "] for last processed slave file position");
					File nextFile = RecordLog.Instance.nextLogFile();
					if (!nextFile.exists()) {
						break;
					}

					RecordLog.Instance.rollOver(nextFile, true);
				}
			}

				// roll-over the slave logs to it's last committed  position
			Journal journal = _masterLogJournal.get(log._src);
			if (journal != null) {
				File slaveLogFile = log.file(journal._fileId);
				try {
					New.prt("Rolling [", journal._src, "] to [", slaveLogFile, "]");
					log.rollOver(slaveLogFile, false);
				}
					// IF for weird reason that the file no longer exists at slaves
					// ie, restore from backups or deleted files from slaves
				catch (FileNotFoundException e) {

						// here the logic, is basically move to the last "slave" log
					New.prt("!!! File Not found for [", journal._src, "] to [", slaveLogFile, "]");
					slaveLogFile = log.firstLogFile();
					while (slaveLogFile.exists()) {
						log.rollOver(slaveLogFile, false);
						journal._filePos = slaveLogFile.length();
						New.prt("!!! Rolling from End [", journal._src, "] to [", slaveLogFile, "]");
						slaveLogFile = log.nextLogFile();
					}

				}
				log._io.position(journal._filePos);
			}
		}

		if (Meta.Mode._isSlave) {
				// log is master log instance as well
			while (true) {
				File nextFile = log.nextLogFile();
				if (!nextFile.exists()) {
					break;
				}
				log.rollOver(nextFile, false);
			}
			log._io.position(log._io.size());
		}

			// reset buffer so that we don't stretch to it's limit
		_ioBuf = ByteBuffer.allocate(0);

		New.prt("Last process entry for [", log._src.name(), "] with file [", log._file, "] at position [", log._io.position(), "]");
	}


	public static enum Operation {
		Insert(0),
		Update(1),
		Delete(2),
		Notify(3);

		public static final Operation[] Lookup = new Operation[] {Insert, Update, Delete, Notify};

		final int _id;

		Operation (int id) {
			_id = id;
		}
	}

	public static class Entry
	{
		public static final int MagicHeader = 20101010;
		public static int Checksum = 123213; // XXX fix me

		private Bytes _bytes;

		Entry (Operation operation, Meta meta, long id)
		{
				// XXX we can really cache this - remember to add checksum, as it could have bad data,  optimze header later
			_bytes = new Bytes();
			_bytes.writeInt(MagicHeader);  // tells if this is a valid entry
			_bytes.writeInt(0); // reserve for src
			_bytes.writeInt(0); // reserve for scrPartition
			_bytes.writeInt(0); // reserve for srcPos
			_bytes.writeInt(0); // reserve for checksum
			_bytes.writeInt(0); // reserve for  "entry size" when it is going to be written finally
			_bytes.write(operation._id);
			_bytes.writeInt(meta._arrayId);
			_bytes.writeLong(id);
		}

		public void addData (byte[] data)
		{
			_bytes.write(data);
		}

		public void addDefn (Defn defn, byte header, byte[] value)
		{
				// write defn id
			_bytes.writeInt(defn._arrayId);

			if (Header.IsNull._asByte == header) {
				_bytes.write(Header.IsNull._asByte);
			}
			else {
					// DefnBytes type would not have header in value[0]
				if (defn instanceof DefnBytes) {
					_bytes.write(header);
					_bytes.writeInt(value.length);
					_bytes.write(value, 0, value.length);
				}
				else {
						// primitive type has header and value both in the value
					_bytes.write(value);
				}
			}
		}
	}

	// for now
	public static class Consumer extends Thread
	{

		private static final Object ProcessBatchMutex = new Object();

		private final RecordLog _log;
		private volatile boolean _enable;

		public Consumer (Meta.Mode src)
		  throws IOException
		{
			_log = new RecordLog(src);
		}


		@Override
		public void run ()
		{
			while (awaitLogAvailable()) {
				org.cosmo.common.util.Util.sleep(_log.WaitTimeUntilLogIsActive);
			}

			while (true) {
				try {
					RecordLog.Instance.moveToLastProcessedPos(_log);
					long logPos = _log._io.position();
					File nextLog = _log.nextLogFile();

					while (true) {
							// keep pulling to the very end of the file for new entries,
							// this repeats until file reaches end and being rolled over
						while (_enable && logPos < _log._io.size()) {
							logPos = _log.processBatch(_log.MaxConsumeRatePerBatch, _log.WaitTimeBetweenBatch);
						}

							// file is rolled over switch, else sleep and continue the loop for pulling
						if (nextLog.exists()) {
							_log.rollOver(nextLog, false);
							logPos = 0;
							nextLog = _log.nextLogFile();
						}
						else {
							org.cosmo.common.util.Util.sleep(_log.WaitTimeWhenNoRecords);
							//New.prt("[", _log._src.name(),"] thread is idle");
						}
					}
				}
				catch (Exception e) {
					org.cosmo.common.util.Util.sleep(3000);
					e.printStackTrace();
					RecordLog.Instance.recoverFromError(_log);
				}
			}
		}


		private boolean awaitLogAvailable ()
		{
			try {
				_log.initialize(false);
				New.prt(Meta.Mode.name(), " listen to [", _log._src.name(), "] at dir [", _log._src._dirStr, "] file [", _log._file, "]");
				return false;
			}
			catch (IOException e){
				return true;
			}
			catch (Exception e) {
				e.printStackTrace();
				return true;
			}
		}
	}

	public static class Journal
	{
		public Meta.Mode _src;
		public int _fileId;
		public long _filePos;
		public Record _record;

		public String toString ()
		{
			return New.str("[", _src.name(), "] at srcPart [" + _fileId + "] srcPos [", _filePos, "]");
		}

	}




	private static ArrayList<RecordLog.Consumer> LogConsumers;
	public synchronized static void toggleLogConsumers (boolean enable)
	  throws Exception
	{
		if (LogConsumers == null) {
				// ie, master would start slaves consumers, and slave would only listen to master consumer
			LogConsumers = new ArrayList();
			for (Meta.Mode src : Meta.Mode.values()) {
				if (Meta.Mode._isMaster && src._isSlave || Meta.Mode._isSlave && src._isMaster) {
					RecordLog.Consumer consumer = new RecordLog.Consumer(src);
					LogConsumers.add(consumer);

				}
			}
		}

		for (RecordLog.Consumer consumer : LogConsumers) {
			consumer._enable = enable;
			consumer.start();
		}
	}

	// XXX need some more refinements before use this since it addes some noise in the log
	public static void notifyLogIsReady ()
	  throws IOException
	{
		/*
			// notify ready
		RecordLog.Entry notification = RecordLog.Instance.newEntry(RecordLog.Op.Notify, Meta.ClassMetaList.get(0), -1); // refine this, too overloaded
		notification.addData("I am ready".getBytes());
		RecordLog.Instance.writeEntry(notification);
		*/
	}

	public static void mainOther (String[] args) throws Exception
	{

		ByteBuffer buf = ByteBuffer.allocateDirect(1024*1024*500);

		Thread.sleep(Integer.MAX_VALUE);
	}

	public static void mainOri (String[] args) throws Exception
	{

		/*
        User.Meta.store();
        RssSite.Meta.store();
        RssContent.Meta.store();
        PublicFolder.Meta.store();
        AddSiteLog.Meta.store();
        DownloadLog.Meta.store();
        */

        	// master will have smaller logs if slv logs contains duplicates
		RecordLog logSlv = new RecordLog(Meta.Mode.slave1);
		RecordLog logMst = new RecordLog(Meta.Mode.master);
		RecordLog.Journal resultContainerSlv = new RecordLog.Journal();
		RecordLog.Journal resultContainerMst = new RecordLog.Journal();
		for (int i = 0; true; i++) {
			logSlv.readEntry(resultContainerSlv, false, false);
			logMst.readEntry(resultContainerMst, false, false);
			Record slvEntry = resultContainerSlv._record;
			Record mstEntry = resultContainerMst._record;
			System.out.println(Record.Static.equals(slvEntry, mstEntry));
		}

	}


	//  java record.RecordLog$RenameLogFiles -DLogRootDir=D:/rsssites -DRenameTo=slave1
	//  would rename files under d:/rsssites/recordLog_master_00000000  to d:/rsssites/recordLog_slave1_00000000
	public static class RenameLogFiles {
		public static void main (String[] args) throws Exception
		{
			String dir = Util.getProperty(String.class, "LogRootDir");
			String renameTo = Util.getProperty(String.class, "RenameTo");
			File file = new File(dir);
			if (!file.exists()) {
				throw new IllegalArgumentException (dir + " not exist");
			}
			File[] files = file.listFiles();
			for (File aFile: files) {
				if (aFile.isFile() && aFile.getName().startsWith(RecordLog.LogFilePrefix)) {
					String fileName = aFile.getName();
					int begin = RecordLog.LogFilePrefix.length() + 1;
					int end = fileName.indexOf("_", begin);
					String newFileName = aFile.getParent() + File.separator + RecordLog.LogFilePrefix + renameTo + fileName.substring(end, fileName.length());
					if (!aFile.renameTo(new File(newFileName))) {
						throw new RuntimeException("Unable to rename:" + aFile + " : " + new File(newFileName));
					}
					else {
						System.out.println("Rename:" + aFile + " : " + new File(newFileName));
					}
				}
			}

		}
	}

	// java record.RecordLog$MergLogFiles -DPreview=true D:\rsssites\recordLog1 D:\rsssites\recordLog2
	// renames files sequentially for each of specifed dir
	/*
	 *[Preview]=[false]
	From [D:\rsssites\recordLog1\recordLog_master_00000000] To [D:\rsssites\recordLog1\recordLog_master_00000000]
	From [D:\rsssites\recordLog1\recordLog_master_00000001] To [D:\rsssites\recordLog1\recordLog_master_00000001]
	From [D:\rsssites\recordLog1\recordLog_master_00000002] To [D:\rsssites\recordLog1\recordLog_master_00000002]
	From [D:\rsssites\recordLog1\recordLog_master_00000003] To [D:\rsssites\recordLog1\recordLog_master_00000003]
	From [D:\rsssites\recordLog1\recordLog_master_00440003] To [D:\rsssites\recordLog1\recordLog_master_00000004]
	From [D:\rsssites\recordLog2\recordLog_master_00000004] To [D:\rsssites\recordLog2\recordLog_master_00000005]
	From [D:\rsssites\recordLog2\recordLog_master_00000005] To [D:\rsssites\recordLog2\recordLog_master_00000006]
	From [D:\rsssites\recordLog2\recordLog_master_00000006] To [D:\rsssites\recordLog2\recordLog_master_00000007]
	From [D:\rsssites\recordLog2\recordLog_master_00000007] To [D:\rsssites\recordLog2\recordLog_master_00000008]
	 */

	public static class MergeLogFiles {

		public static void main (String[] args) throws Exception
		{
			boolean preview = Util.getProperty(Boolean.class, "Preview", true);

			int LogID = 0;
			for (String arg : args) {
				File dir = new File(arg);
				if (!dir.exists()) {
					throw new IllegalArgumentException("dir not exist " + dir);
				}
				TreeSet<String> sortedName = new TreeSet();
				for (File aFile : dir.listFiles()) {
					if (!aFile.getName().startsWith(RecordLog.LogFilePrefix )) {
						System.out.println("Skipping [" + aFile.getAbsolutePath());
						continue;
					}
					String fileName = aFile.getAbsolutePath();
					sortedName.add(fileName);
				}

				for (Iterator<String> files = sortedName.iterator(); files.hasNext(); ) {

					String fileName = files.next();
					int number = Integer.valueOf(fileName.substring(fileName.length() - 8, fileName.length()));

					//String newFileName = fileName.substring(0, fileName.length() - 8) + org.cosmo.common.util.Util.zeroPaddedInt(number + 1, 8) + "_" + LogID++;
					String newFileName = fileName.substring(0, fileName.length() - 8) + org.cosmo.common.util.Util.zeroPaddedInt(LogID++, 8);

					File currentFile = new File(fileName);
					File renameToFile = new File(newFileName);

					if (!preview) {
						currentFile.renameTo(renameToFile);
					}
					System.out.println("From [" + currentFile.getAbsolutePath() + "] To [" + renameToFile.getAbsolutePath() + "]");

				}
			}
		}
	}


	//  java record.RecordLog$ArchiveLogFiles -DMode=master
	//  would move files under d:/rsssites/recordLog_master_00000000 to d:/rsssites/recordLog_/recordLog_master_00000000
	//  up to n - 1 files.  ie if there are 1000 only 1 to 999 will be moved

	public static class ArchiveLogFiles {
		public static void main (String[] args) throws Exception
		{

			File archiveDir = new File(Meta.Mode._dir, RecordLog.LogFilePrefix);
			if (!archiveDir.exists()) {
				if (!archiveDir.mkdirs()) {
					throw new Exception("unable to create archive dir : " + archiveDir);
				}
			}
			while (true) {
				File nextFile = RecordLog.Instance.nextLogFile();
				if (!nextFile.exists()) {
					break;
				}
				RecordLog.Instance.rollOverAndArchive(nextFile, false, archiveDir);
			}
/*
			Meta.Mode mode = Meta.Mode.master;


			System.setProperty("Mode", mode.name());

			File archiveDir = new File(mode._dir, RecordLog.LogFilePrefix);
			if (!archiveDir.exists()) {
				if (!archiveDir.mkdirs()) {
					throw new Exception("unable to create archive dir : " + archiveDir);
				}
			}
			RecordLog log = new RecordLog(mode.master);
			File curFile = log._file;
			File nextFile = log.nextLogFile();
			while (true) {
				if (!nextFile.exists()) {
					break;
				}

				File renameFile = new File(archiveDir, curFile.getName());
				curFile.renameTo(renameFile);
				curFile = nextFile;
				nextFile = log.nextLogFile();
			}
*/
		}
	}


}
