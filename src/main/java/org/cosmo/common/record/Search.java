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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.OpenBitSet;
import org.cosmo.common.statistics.Clock;
import org.cosmo.common.util.Log;
import org.cosmo.common.util.New;

import ariba.util.core.Fmt;

public class Search
{
	FSDirectory _indexDir;
	IndexReader _indexReader;
	IndexSearcher _indexSearcher;
	Analyzer _analyzer;
	Meta _meta;


	protected Search (Meta meta)
	{
		_meta = meta;
		if (!_meta._hasIndexFields) {
			return;
		}
		init();
	}

	protected void init ()
	{
		try {
			_indexReader = IndexReader.open(_meta.index()._indexWriter, true);
			_analyzer =  new StandardAnalyzer(Index.Version);
			_indexSearcher = new IndexSearcher(_indexReader);
			verifyIndex();
		}
		catch (IOException e) {
			throw new RecordException(e);
		}
	}

	protected void verifyIndex ()
	  throws IOException
	{
		int count = _indexReader.maxDoc();
		int misAlign = 0;
		int badIndex  = 0;
		boolean hasDeletes = false;
		for (long i = 0; i < count; i++) {
				// check the alignment by get doc directly from indexReader and check field recordId matches
			Document doc = _indexReader.document((int)i);
			long docId =  Long.valueOf(doc.get(IndexDocument.RecordId));
				// if not aligned
			if (docId != i) {
				misAlign++;
					// delete search-able mis-aligned doc
				long id = recordId(IndexDocument.RecordId, String.valueOf(i));
				if (id > 0) {
					_meta.index()._indexWriter.deleteDocuments(new Term(IndexDocument.RecordId, String.valueOf(i)));
					New.prt("Delete index misaligned at: ", i, " pointing to :", docId);
					hasDeletes = true;
				}

					// mis-aligned yet not searchable - get counts
				if (id == -1) {
					badIndex++;
				}
			}
		}


		if (misAlign > 0 || badIndex > 0 ) {
			System.err.println(New.str("Index Corrupted. misaligned [", misAlign, "] corrupted [", badIndex, "]"));
			if (hasDeletes) {
				System.err.println(New.str("Commit misalgined deletions and optimize..."));
				_meta.index()._indexWriter.commit();
				_meta.index()._indexWriter.optimize();
				System.err.println(New.str("Done - exiting.."));
				System.exit(1);
			}
		}
		New.prt("Done Checking Index Alignment [" , _meta.name() ,"]  with count " , count, " ", Clock.timer().markAndCheckRunning()); // " and final size " , defn.getFieldCache().totalSize() / 1048576 , "MB");
	}



	public void refreshReader ()
	  throws IOException
	{
		refreshReader(false);
	}


	public void refreshReader (boolean forceNew)
	  throws IOException
	{
		// must be same order as Index.commit() otherwise deadlock!
		synchronized (_meta.search()._indexSearcher) {
			synchronized (_meta.search()._indexReader) {
				synchronized (_meta.index()._uncommitedIndexDocuments) {
					synchronized (_meta.index()._indexWriter) {

							// forceNew will open a new Reader instead of using reopen
							// forceNew is slower
						IndexReader freshedNewIndexReader = forceNew
						? IndexReader.open(_meta.index()._indexWriter, false)
						: _indexReader.reopen();

						if (freshedNewIndexReader != _indexReader) {
							  _indexSearcher.close();
							  _indexReader.close();
							  _indexReader = freshedNewIndexReader;
							  _indexSearcher = new IndexSearcher(_indexReader);
						}
					}
				}
			}
		}
	}



	public List<String> uniqueValuesForField (String fieldName)
	  throws IOException
	{
		TermEnum terms = reader().terms(new Term(fieldName, ""));
		List<String> list = new ArrayList();
		PrintWriter out = new PrintWriter(new FileOutputStream("c:/terms.txt"));
		try	{
		    while (fieldName.equals(terms.term().field())) { // XXX perhaps intern then do == ?
		    	Term term = terms.term();
		    	String text = term.text();
		    	out.println(text);
		    	list.add(term.text());
		        if (!terms.next())
		            break;
		    }
		    out.close();
		}
		finally {
		    terms.close();
		}
		return list;

	}

	public List<String> uniqueValuesForField (Defn defn)
	  throws IOException
	{
		return uniqueValuesForField(defn.getDeclaringFieldName());
	}


	public IndexReader reader ()
	{
		return _indexReader;
	}


	public IndexSearcher searcher ()
	{
		return _indexSearcher;
	}


		// assumes record contains enough info to generate unique id field, check UniqueIDFactory impl of the class
	public Record getRecordIfExists (Record record)
	  throws IOException
	{
		String uid = record.tx().uid();
		long recordId = recordIdByUid(uid);
		if (recordId >= 0) {
			return record.meta().store().read(recordId);
		}
		return null;
	}

	public long recordIdByUid (String uid)
	  throws IOException
	{
		return recordId(Index.UniqueIdFieldName, uid);
	}

	public boolean ifUidExists (String uid)
	  throws IOException
	{
		return recordIdByUid(uid) >= 0;
	}

		// return single record id
	long recordId (String field, String value)
	  throws IOException
	{
		if (field == null || value == null) {
			throw new IllegalArgumentException(Fmt.S("Field [%s], Value [%s]", field, value));
		}
		Term term = new Term(field, value);
		Document doc = firstDoc(term);
	    if (doc != null) {
		    String id = doc.get(IndexDocument.RecordId);
		    return Long.valueOf(id);
	    }
	    return -1;
	}


		// return single lucene document
	Document firstDoc (Term term)
	  throws IOException
	{
		TermQuery query = new TermQuery(term);
		try {
			IndexSearcher searcher = searcher();
			TopDocs docs = searcher.search(query, 1);
			return docs.scoreDocs.length > 0 ? searcher.doc(docs.scoreDocs[0].doc) : null;
		}
			// This is possible if a index.commit has happened reader for this indexSearch has closed. just refresh and try again
		catch (AlreadyClosedException ace) {

			Log.jlucene.fine("XX Refreshing Reader due to AlreadyClosedException at firstDoc");
			refreshReader();
			return firstDoc(term);
		}
		catch (NullPointerException npe) {

			Log.jlucene.fine("XX Refreshing Reader due to NullPointerException at firstDoc");
			refreshReader();
			return firstDoc(term);
		}
	}



		// return single record by an index field
	public Record firstRecord (Defn defn, String value, boolean readCached)
	  throws IOException
	{
		long id = recordId(defn.getDeclaringFieldName(), value);
		if (id >= 0) {
				// read cached only works for CachedRecordStore - as it will returned cachd Record.
		  	Record record = defn._declaringMeta.store().newInstance(id);
    		return record.tx().load(readCached);
		}
		return null;
	}


	public OpenBitSet records (org.apache.lucene.search.Query query)
	  throws IOException
	{
		try {
			if (Log.jlucene.getLevel() == java.util.logging.Level.FINE) {
				Log.jlucene.fine(Fmt.S("%s Searching with query [%s]", Clock.timer().markAndCheckRunning(), query));
			}

			MyFilter filter = new MyFilter(query, this);
			DocIdSet results = filter.getDocIdSet(reader());

			if (Log.jlucene.getLevel() == java.util.logging.Level.FINE) {
				Log.jlucene.fine(Fmt.S("%s Done Searching", Clock.timer().markAndCheckRunning()));
			}

		    return (OpenBitSet)results;
		}
		// This is possible if a index.commit has happened reader for this indexSearch has closed. just refresh and try again
		catch (AlreadyClosedException ace) {
			Log.jlucene.fine("XX Refreshing Reader due to AlreadyClosedException at records");
			refreshReader();
			return records(query);
		}
		catch (NullPointerException npe) {
			Log.jlucene.fine("XX Refreshing Reader due to NullPointerException at records");
			refreshReader();
			return records(query);
		}
	}


		// queryStr is analyzed . ie "A B" becoms "A" or "B"
	public OpenBitSet records (Defn defn, String queryStr)
	  throws IOException, ParseException
	{
		ensureDefnIsDeclaredIndex(defn);

		org.apache.lucene.search.Query query = new QueryParser(Index.Version, defn.getIndexFieldName(), _analyzer).parse(queryStr);
		//org.apache.lucene.search.Query query = new QueryParser(defn.getIndexFieldName(), _analyzer).parse(queryStr);


		try {
			if (Log.jlucene.getLevel() == java.util.logging.Level.FINE) {
				Log.jlucene.fine(Fmt.S("%s Searching [%s] with query [%s]", Clock.timer().markAndCheckRunning(), defn.getDeclaringFieldName(), query));
			}

			MyFilter filter = new MyFilter(query, this);
			DocIdSet results = filter.getDocIdSet(reader());

			if (Log.jlucene.getLevel() == java.util.logging.Level.FINE) {
				Log.jlucene.fine(Fmt.S("%s Done Searching", Clock.timer().markAndCheckRunning()));
			}

		    return (OpenBitSet)results;
		}
		// This is possible if a index.commit has happened reader for this indexSearch has closed. just refresh and try again
		catch (AlreadyClosedException ace) {
			Log.jlucene.fine("XX Refreshing Reader due to AlreadyClosedException at records");
			refreshReader();
			return records(defn, queryStr);
		}
		catch (NullPointerException npe) {
			Log.jlucene.fine("XX Refreshing Reader due to NullPointerException at records");
			refreshReader();
			return records(defn, queryStr);
		}

	}

	public OpenBitSet records (Query query)
	  throws ParseException, IOException
	{
		return records(query._defn, query._str.toString());
	}



	public static void ensureDefnIsDeclaredIndex (Defn defn)
	{
		if (!defn._isIndexField) {
			throw new RecordException(defn.getDeclaringFieldName() + "is not declared as an index field");
		}
	}

	public static class MyFilter extends QueryWrapperFilter
	{
		private org.apache.lucene.search.Query query;
		private Search _search;
		private OpenBitSet bits;

		public MyFilter(org.apache.lucene.search.Query query, Search search) {
			super(query);
			this.query = query;
			this._search = search;
		}

		// @override
		public DocIdSet getDocIdSet(final IndexReader reader)
		  throws IOException
		{

			bits = new OpenBitSet(reader.maxDoc());

		    _search.searcher().search(query, new Collector() {

		    	private int base = 0;

		        public void setScorer(Scorer scorer) throws IOException {
		          // score is not needed by this collector
		        }
		        public final void collect(int doc) {
		          bits.set(doc + base);  // set bit for hit
		        }
		        public void setNextReader(IndexReader reader, int docBase) {
		          base = docBase;
		        }
		        public boolean acceptsDocsOutOfOrder() {
		          return true;
		        }
		      });
		   return bits;
		}

	}

}

class ReadOnlySearch extends Search
{
	protected ReadOnlySearch (Meta meta)
	{
		super(meta);
	}

	@Override
	protected void init ()
	{
		try {
				// reader opens the directory instead of the writer like "search"
			_indexReader = IndexReader.open(_meta.index()._indexDir, true);
			_analyzer =  new StandardAnalyzer(Index.Version);
			_indexSearcher = new IndexSearcher(_indexReader);
		}
		catch (IOException e) {
			throw new RecordException(e);
		}
	}


	@Override
	public void refreshReader ()
	  throws IOException
	{
		// must be same order as Index.commit() otherwise deadlock!
		synchronized (_meta.search()._indexSearcher) {
			synchronized (_meta.search()._indexReader) {
				IndexReader freshedNewIndexReader = IndexReader.open(_meta.index()._indexDir, true);
				if (freshedNewIndexReader != _indexReader) {
					_indexSearcher.close();
					_indexReader.close();
					_indexReader = freshedNewIndexReader;
					_indexSearcher = new IndexSearcher(_indexReader);
				}
			}
		}
	}
}



