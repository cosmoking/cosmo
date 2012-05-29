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

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.OpenBitSet;
import org.cosmo.common.statistics.Clock;
import org.cosmo.common.util.Log;

import ariba.util.core.Fmt;

import cern.colt.Sorting;
import cern.colt.list.LongArrayList;


	// Purpose of this class allows results to be intersected and unioned before final records are feteched
public class SearchResult
{

	public static final Map Cache = new ConcurrentSkipListMap();

	OpenBitSet _result;
	SearchContext _context;


	public SearchResult (DocIdSet result, SearchContext context)
	{
		_result = (OpenBitSet)result;
		_context = context;
	}


	public void intersect (SearchResult result)
	{
		if (result != null) {
			_result.and(result._result);
		}
	}

	public void union (SearchResult result)
	{
		if (result != null) {
			_result.or(result._result);
		}
	}

	public long[] records (boolean load)
	  throws Exception
	{
		if (Log.jlucene.getLevel() == java.util.logging.Level.FINE) {
			Log.jlucene.fine(Fmt.S("%s Start Fetching Records", Clock.timer().markAndCheckRunning()));
		}

	    DocIdSetIterator resultsIterator = _result.iterator();
	    Search search = _context._searchField._declaringMeta.search();


	    LongArrayList ids = new LongArrayList();

	    for (int docId = resultsIterator.nextDoc(); docId != DocIdSetIterator.NO_MORE_DOCS;) {
	    	ids.add((long)docId);
	    	docId = resultsIterator.nextDoc();
	    }

	    ids.trimToSize();
	    	// XXX REVMOE assertAndCorrectIds() ONCE BUG IS FIXED!!!!
	    //assertAndCorrectIds(search, ids);
	    //Sorting.quickSort(ids.elements(), 0, ids.size(), IdComparator);
	   
	    	throw new RuntimeException("fix below commenouted out line due to refactoring");
	    /*Sorting.mergeSort(ids.elements(), 0, ids.size(), RssContent.PubDateComparator.Instance);




		if (Log.jlucene.getLevel() == ariba.util.log.Log.DebugLevel) {
			Log.jlucene.debug("%s Done Fetching Records", Clock.timer().markAndCheckRunning());
		}
	    return ids.elements();
		*/
	}


		// XXX There is a problem in which  lucene doc id would get off sync with record Id - to be investigated
		// it should be sequentially aligned BUT for some reason it would not..
	public void assertAndCorrectIds (Search search, LongArrayList ids)
	  throws Exception
	{
	    IndexReader reader = search.reader();
	    for (int i = 0 ; i < ids.size(); i ++) {
	    	long docId = ids.get(i);
		    Document doc = reader.document((int)docId);
		    long docRecordId = Long.valueOf(doc.get("id"));
		    if (docRecordId != docId) {
		    	//System.out.println("docId:" + docId + " recordId: " + docRecordId);
		    	ids.set(i, docRecordId);
		    }
	    }
	}


	public long[] records ()
	  throws Exception
	{
		return records(true);
	}


	public long first ()
	  throws Exception
	{
		long[] records = records();
		if (records.length > 0) {
			return records[0];
		}
		return -1;
	}


}


/*
	public long[] recordIds ()
	  throws ParseException, IOException
	{

	    DocIdSetIterator resultsIterator = _result.iterator();
	    Search search = _context._searchField._declaringMeta.search();
	    IndexReader reader = search.reader();
	    System.out.println("Pre sorted ===");
	    for (int docId = resultsIterator.nextDoc(), i = 0; docId != DocIdSetIterator.NO_MORE_DOCS; i++) {
	    	Document doc =reader.document(docId);
	    	String stringId = doc.get("id");
	    	recordIds[i] = Long.valueOf(stringId);
	    	System.out.print (recordIds[i] + " ");
	    	docId = resultsIterator.nextDoc();
	    }
	    System.out.println("Pre sorted ===");
	    Arrays.sort(recordIds);
	    return recordIds;
	}

	public Set<Record> records (boolean load)
	  throws ParseException, IOException
	{
		long[] recordIds = recordIds();
	    TreeSet<Record> list = new TreeSet(RssContent.Comparator);
	    Search search = _context._searchField._declaringMeta.search();
	    System.out.println("Post sorted ===");
	    for (int i = 0; i < recordIds.length; i++) {
	    	System.out.print (recordIds[i] + " ");
	    	Record record = DefnRecord.setupRecord(recordIds[i], search._meta._clazz);
	    	if (load) {
	    		record.tx().load();
	    	}
	    	list.add(record);
	    }
	    System.out.println("Post sorted ===");
	    return list;
	}

}
*/



