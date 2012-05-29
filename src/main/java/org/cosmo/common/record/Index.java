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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.cosmo.common.util.New;
import org.cosmo.common.util.Util;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;

import org.cosmo.common.util.Log;

import ariba.util.core.Assert;
import ariba.util.core.Fmt;


public class Index implements Control.Haltable
{

	public static final String UniqueIdFieldName = "UniqueIdFieldName";
	public static final Version Version = org.apache.lucene.util.Version.LUCENE_33;


    Meta _meta;
	FSDirectory _indexDir;
	Analyzer _analyzer;
    IndexWriter _indexWriter;
    volatile ConcurrentHashMap<String, Record> _uncommitedIndexDocuments;
	private Control.Lock _lock;


	protected Index (Meta meta)
	{
		_meta = meta;
    	_lock = Control.haltableLock(this, _meta.name());
		if (!_meta._hasIndexFields) {
			return;
		}
    	init();
	}

	protected void init ()
	{

		try {
			_indexDir = FSDirectory.open(_meta.createFolderForClass(Index.class, "index", true));
			_analyzer = new StandardAnalyzer(Version);
			_uncommitedIndexDocuments = new ConcurrentHashMap();

			try {
				_indexWriter = new IndexWriter(_indexDir, _analyzer, false, IndexWriter.MaxFieldLength.UNLIMITED);
			}
			catch (FileNotFoundException e) {
				System.err.println("Index File Not found - creating...");
				_indexWriter = new IndexWriter(_indexDir, _analyzer, true, IndexWriter.MaxFieldLength.UNLIMITED);
			}
			catch (LockObtainFailedException e) {
				System.err.println("Lock File detected - retyring...");
				File file = new File (_meta.recordDir() + File.separator + "index" + File.separator + "write.lock");
				 if (!file.delete()) {
					 throw new RuntimeException("Lock File is being held by other process..." + file.getAbsolutePath());
				 }
				_indexWriter = new IndexWriter(_indexDir, _analyzer, false, IndexWriter.MaxFieldLength.UNLIMITED);
			}
				// DO NOT want auto flush - could cause deadlock if this is not used ,
				// 1/16 could have corrupt index if auto flush is enabled for 3.0.0
				// buffer are not flushed until it's committed. previously worried about saved doc not searchabel until committed
				// which is ok, the problem was about duplicate docs - but now internal hashmap of uncommitted solves the problem
				// if some weird search error come back here !
			_indexWriter.setMaxBufferedDocs(Integer.MAX_VALUE);
			_indexWriter.setRAMBufferSizeMB(IndexWriter.DISABLE_AUTO_FLUSH);
			_indexWriter.setMergeScheduler(new SerialMergeScheduler());
			_indexWriter.setMergePolicy(new LogByteSizeMergePolicy());


				// one time optimize upon vm start
			//_indexWriter.optimize(true);

		}
		catch (IOException e) {
			throw new RecordException(e);
		}
	}


	public void commit ()
	{
		commit(true, true);
	}

	public void commit (boolean sanityCheck)
	{
		commit(sanityCheck, true);
	}

		// unblockWhenDone is used so that index is "haltable"
	private void commit (boolean sanityCheck, boolean unblockWhenDone)
	{
		if (!_meta._hasIndexFields) {
			return;
		}
		_lock.lock();
		try {
			synchronized (_meta.search()._indexSearcher) {
				synchronized (_meta.search()._indexReader) {
					synchronized (_uncommitedIndexDocuments) {
						synchronized (_indexWriter) {
							try {
								_indexWriter.waitForMerges();
								_indexWriter.commit();
								_meta.search().refreshReader();
								if (sanityCheck) {
									verifyCommitDataIndexConsistency();
								}
							}
							catch (IOException ioe) {
								throw new RecordException(ioe);
							}
							finally {
								_uncommitedIndexDocuments = new ConcurrentHashMap();
							}
						}
					}
				}
			}
		}
		finally {
			if (unblockWhenDone) {
				_lock.unlock();
			}
		}
	}

	@Override
	public Control.Lock halt ()
	{
			// obtain lock so that no other commit can be called
		_lock.lock();
			// commit BUT do not release lock
		commit(true, false);
		return _lock;
	}

	private void verifyCommitDataIndexConsistency ()
	{
		try {
			boolean hasError = false;
			for (Record record : _uncommitedIndexDocuments.values()) {

					// compare index id vs record id - the aligment is important as it allows getting the record by docid
				long recordId = record.tx().id();
				Document doc = _meta.search()._indexReader.document((int)recordId);
				String docId = doc.get(IndexDocument.RecordId);
				if (!docId.equals(String.valueOf(recordId))) {  // through testing - although getting document directly is faster but sometime yields wrong document
					String msg = New.str("Index ID not match - docid [", docId, "] vs recordId[", recordId,"] for class [", record.getClass().getSimpleName(), "] try again..");
					//System.err.println(msg);
					Exception e = new Exception(msg);
					e.printStackTrace();
						// compare again BUT this time use search document - just testing - at this point even match it's error and needs exit
					docId = String.valueOf(_meta.search().recordId(IndexDocument.RecordId, String.valueOf(recordId)));
					if (!docId.equals(String.valueOf(recordId))) {
						msg = New.str("Index ID STILL not match - docid [", docId, "] vs recordId[", recordId,"] for class [", record.getClass().getSimpleName(), "]");
						System.err.println(msg);
						hasError = true;
					}
				}
			}

			if (hasError) {
				// if data and index are consistent will exit the system!
				// and upon restart meta.performIndexVSRecordSanityCheck()
				// will attemp to recover itself
				System.err.println("Exiting due to Data and Index is inconistent - restart to recorrect");
				System.exit(1);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public boolean havePendingChanges ()
	{
		return _uncommitedIndexDocuments != null && _uncommitedIndexDocuments.size() > 0;
	}


	public IndexDocument newDocument (long id)
	{
		return new IndexDocument(this, id);
	}

		// if record requires uniqueness, it's validated then indexed
	public IndexDocument newDocument (Record record, long id)
	{
		if (record.meta().uniqueIdFactory() == null) {
			return newDocument(id);
		}
		else {
			String	uid = record.tx().uid();
			if (uid == null || uid.length() == 0) {
				throw new RecordException("Unique Id can not be null or empty");
			}
				// check from uncommitedDocs - between first and second put, first would be null, second will be non-null
				// effectively signifies that a doc is pending
			if (_uncommitedIndexDocuments.get(uid) != null) {
				if (Log.jlucene.getLevel() == java.util.logging.Level.FINE) {
					org.cosmo.common.util.Log.jlucene.fine(Fmt.S("Duplicated Record [%s] detected with id [%s] in uncommited records", record.getClass().getName(), uid));
				}
				throw new DuplicateRecordException(record);
			}

				// check from committed docs

			try {
				long recordId = record.meta().search().recordIdByUid(uid);
				if (recordId >= 0) {
					if (Log.jlucene.getLevel() == java.util.logging.Level.FINE) {
						org.cosmo.common.util.Log.jlucene.fine(Fmt.S("Duplicated Record detected with id [%s]", uid));
					}
					throw new DuplicateRecordException(record);
				}
			}
			catch (IOException ioe) {
				throw new RecordException(New.str("Uanble to resolve: ", uid, " due to error ", ioe.getMessage()));
			}


				// valid
			IndexDocument idx = new IndexDocument(this, id);
			idx.addField(UniqueIdFieldName, uid, Field.Index.NOT_ANALYZED);
			_uncommitedIndexDocuments.putIfAbsent(uid, record);
			return idx;
		}
	}


	public void close ()
	  throws IOException
	{
		_indexWriter.close(true);
	}




	public static void main (String[] args)
	{
		byte[] a = Converter.toBytes(100);
		byte[] b = Converter.toBytes(500);
		byte[] c = Converter.toBytes(10000);
		byte[] d = Converter.toBytes(9999999);
		byte[] e = Converter.toBytes(-500);
		String aStr = new String(a);
		String bStr = new String(b);
		String cStr = new String(c);
		String dStr = new String(d);
		String eStr = new String(e);

		System.out.println("a > b " + aStr.compareTo(bStr));
		System.out.println("b > c " + bStr.compareTo(cStr));
		System.out.println("c > d " + cStr.compareTo(dStr));
		System.out.println("d > e " + dStr.compareTo(aStr));
		System.out.println("e > a " + eStr.compareTo(aStr));





	}
}

class ReadOnlyIndex extends Index
{
	protected ReadOnlyIndex (Meta meta)
	{
		super(meta);
	}

	@Override
	protected void init ()
	{
		try {
			_indexDir = FSDirectory.open(_meta.createFolderForClass(Index.class, "index", true));
		}
		catch (IOException e) {
			throw new RecordException(e);
		}
	}

		// commit does not refresh

	@Override
	public void commit ()
	{
		// noop
	}
}

class Converter
{

		// not thread safe
	public static byte[] toBytes (long value)
	{
		 return ByteBuffer.wrap(new byte[8]).putLong(value).array();
	}

		// not thread safe
	public static long toLong (byte[] bytes)
	{
		return ByteBuffer.wrap(bytes).getLong();
	}
}


