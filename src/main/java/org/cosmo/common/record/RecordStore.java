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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.cosmo.common.util.NetworkByteArray;

import ariba.util.core.SystemUtil;


/*
 *  XXX Read is not thread safe atm - make each thread to own it's copy of _readDataIO
 *
 *  read and write is synchronzied at the moment
 *
 *  TODO: change Lob to use FileChannel so that no synchronized is needed and no need
 *        to open 2 file handle.  FileChannel support pread(). positional read that
 *        is atomic and does not change the file pointer.
 *
 *  TODO: remove Tx from Record, instead an long typed id which would return a Tx
 *        use first say.. 8 bytes to encode the meta.
 */
public class RecordStore <T extends Record> implements Control.Haltable
{
	public final Object NotifyOnInsert = new Object();
	public final Object NotifyOnUpdate = new Object();


	Meta<? extends Record> _meta;
	private Control.Lock _writeLock;
	private NetworkByteArray.Setter _recordWrites;
	private RecordLog _recordLog;


	RecordStore (Meta meta)
	  throws IOException
	{
			// for slave we init log for writing
		_meta = meta;
		_writeLock = Control.haltableLock(this, _meta.name());
		_recordLog = RecordLog.instance(meta);

		/*
			NetworkByteArray.Handle handle = NetworkByteArray.create(new File(_meta._mode._dir, _meta._clazz.getSimpleName()+ ".slv"), 64);
			_recordWrites = handle.openForWrite();


		 */
	}

	public T read (long i)
	{
		return read(i, newInstance(i), true);
	}


	public T read (long i, boolean fullRead)
	{
		return read(i, newInstance(i), fullRead);
	}

	public T read (long id, T toRecord, boolean fullRead)
	{
		try {

			//if (id < 0) {  // id > count() is to expensive
 			//	return null;
			//}
			toRecord.tx()._id = id;
			toRecord.tx()._loaded = true;

			if (fullRead) {
				for (Defn defn : _meta._defns) {
					defn.read(id, toRecord);
				}
			}
			else {
				for (Defn defn : _meta._defns) {
					if (!defn._lazyLoad) {
						defn.read(id, toRecord);
					}
				}
			}
			//System.out.println(New.str("Read ", id, " ", _meta._clazz.getSimpleName(), " ", Clock.timer().markAndCheckRunning()));
		}
		catch (IOException e) {
			throw new RecordException(e);
		}
		catch (IllegalArgumentException iae) {
			throw new Error(iae);
		}
		catch (IllegalAccessException iae2) {
			throw new Error(iae2);
		}
		return toRecord;
	}


	public boolean exists (long i)
	{
		return this.count() > i;
	}


	public T read (String i)
	{
		return read(org.cosmo.common.util.Util.parseLong(i));
	}

	public List<T> readAll ()
	{
		return read(0, count());
	}

	public List<T> read (long offset, int length)
	{
		if (count() == 0) {
			return Collections.EMPTY_LIST;
		}
		if (offset < 0) {
			throw new IndexOutOfBoundsException("Offset can not be less than 0");
		}
		if (length < 0) {
			throw new IndexOutOfBoundsException("Lenght can not be less than 0");
		}

		Record[] records = new Record[length];
		ArrayList l = new ArrayList();
		for (int i = 0; i < length; i++) {
			//records[i] = read(offset + i);
			l.add(read(offset + i));
		}
		return l;
	}

	protected T newInstance (long id)
	{
		try {
			T record = (T)_meta._clazz.newInstance();
			record.tx()._id = id;
			record.tx()._loaded = false;
			return record;
		}
		catch (InstantiationException ie) {
			throw new Error(ie);
		}
		catch (IllegalAccessException iae) {
			throw new Error(iae);
		}
	}




	RecordLog.Entry newRecordLogEntry (boolean insert, long id)
	{
		return _recordLog != null
			? _recordLog.newEntry(insert ? RecordLog.Operation.Insert : RecordLog.Operation.Update, _meta, id)
			: null;
	}

	void writeRecordLogEntry (RecordLog.Entry entry)
	  throws IOException
	{
		if (_recordLog == null || entry == null) {
			return;
		}
		_recordLog.writeEntry(entry);
	}




		// NOTE! when autoCommit is true then index.commit() needs to be done outside of this scope.
		// Otherwise the document is persisted but not searchable.
		// record.write() and index.commit() must be done in serialized manner (synchronized) to
		// be thread safe.  for example, if doing batches of write inserts and then do index.commit() at the
		// end make sure the entire method is synchronized


	protected T write (T fromRecord, long id, boolean insert, boolean autoCommit)
	{
		_writeLock.lock();
		try {
			RecordLog.Entry logEntry = newRecordLogEntry(insert, id);
			if (_meta._mode._isMaster && (_meta._hasIndexFields || _meta.uniqueIdFactory() != null)) {
				IndexDocument idx = _meta.index().newDocument(fromRecord, id);
				for (Defn defn : _meta._defns) {
					defn.write(id, defn.getValue(fromRecord), idx, insert, logEntry);
				}
				fromRecord.tx()._id = id;
				idx.save(insert);
				if (autoCommit) {
					_meta.index().commit();
				}
			}
			else {
				for (Defn defn : _meta._defns) {
					defn.write(id, defn.getValue(fromRecord), null, insert, logEntry);
				}
				fromRecord.tx()._id = id;
			}
			writeRecordLogEntry(logEntry);
			return fromRecord;

		}
		catch (RecordException e) {
			throw e;
		}
		catch (Throwable e) {
			return handleWriteError(fromRecord, e, id);
		}
		finally {
			_writeLock.unlock();
		}
	}

	private T handleWriteError (T r, Throwable e, long id)
	{
		System.err.print(SystemUtil.stackTrace(e));
		try {
			_meta.index().commit(false);
		}
		catch (Throwable error) {
			error.printStackTrace();
		}

		try {
			_meta.performIndexAndRecordCountSanityCheck();
		}
		catch (Throwable error) {
			error.printStackTrace();
		}


		try {
			System.err.println("RecordStore [" + _meta.name() + "] write error for record " + String.valueOf(id));
			System.err.println("Record:\n" + r.tx().recordToXML(false, false));
		}
		catch (Throwable error) {
			error.printStackTrace();
		}

		System.exit(1);
		return null;
	}


	public T write (T record, boolean autoCommit)
	{
		_writeLock.lock();
		try {
				// why readCount instead of writeCount?
				// because only "Read" files  (FilePartition.java) are all loaded to give
				// the right count, "Write" files on the other hand
				// are lazy loaded hence would give wrong number
			long id = count();
			write(record, id, true, autoCommit);
			return record;
		}
		finally {
			_writeLock.unlock();
		}
	}


	public int count ()
	{
		return _meta._defns.get(0).readCount();
	}

	public long[] ids ()
	{
		long[] ids = new long[count()];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = i;
		}
		return ids;
	}


	public Tx createTx (Record record)
	{
		return new Tx (record);
	}

	public static <G extends Record>RecordStore Instance (Meta<G> meta)
	{
		return meta.store();
	}

	public void dump (T record)
	  throws IOException
	{
		String fileName = _meta.recordDir() +  File.separator + _meta._clazz.getSimpleName() + ".xml";
		FileOutputStream out = new FileOutputStream(fileName);
		try {
			out.write(org.cosmo.common.util.Util.UTF8(record.tx().toString()));
		}
		finally {
			out.close();
		}
	}

	@Override
	public Control.Lock halt ()
	{
		_writeLock.lock();
		return _writeLock;
	}
}
