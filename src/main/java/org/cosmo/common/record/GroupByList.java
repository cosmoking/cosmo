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

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.OpenBitSet;

import cern.colt.list.LongArrayList;

import ariba.util.core.Fmt;

import org.cosmo.common.record.Defn;
import org.cosmo.common.record.FieldCache;
import org.cosmo.common.util.Constants;
import org.cosmo.common.util.LongArrayIterator;
import org.cosmo.common.array.*;


/*
 * 1 - each grouplist (site) has different filter (ie some for 30 days, some for 14 days, or 14 days but no more than 5000)
    create some 5 or 6 preset policy filters

2 - instead of sorting when got all the ranges, but them in desired bucket.



4 - but view and bucket range is now in - hours / days / weeks, so that when sort it can be put in buckets


5 - no more view last - doens't make much sense


6  - 7 - normalize all time in hours - and when hours are equal sort by ratings !!!!! SOLUTION TO THE PROBLEM!




 */

public class GroupByList extends Listener implements OpenLongArray.Filter
{

	public OpenObjectArray<OpenLongArray> _groups;
	public GroupByList.Filter _filter;


	public GroupByList (GroupByList.Filter filter)
	{
		_groups = new OpenObjectArray<OpenLongArray>(OpenLongArray.class, null);
		_filter = filter;
	}

	@Override
	public int sync (int maxCount)
	  throws IOException
	{

		ByteBuffer buf = _defn.readFullRawBytes(maxCount);
		int count = buf.capacity() / _defn.size();

		int totalSynced = 0;

		for (int i = 0; i < count; i++) {
				// skip the header byte;
			buf.get();
				// get actual long value;
				// first is group, second is the elements
		    boolean added = retrieveGroup((int)buf.getLong()).addFastUnsafe(i,128);
		    if (added) {
		    	totalSynced++;
		    }
		}
		return totalSynced;

		/*
		for (int i = 0; i < _groups.array().elements().length; i++) {
			System.out.println(this.retrieveGroup(i).array().cursor());
		}
		*/

		/*
		OpenObjectArray.Elements<OpenLongArray> groupsArray = _groups.array();
		for (int i = 0; i < groupsArray.cursor(); i++) {
			OpenLongArray.Elements longArray = groupsArray.elements()[i].array();
			for (int j = 0; j < longArray.cursor(); j++) {
				long l = longArray.elements()[j];
				//System.out.println(Fmt.S("RssSite [%s] Content [%s]", i, l));
			}
		}
		*/

	}


	public long[] lookupAll ()
	{
		int range = _groups.array().elements().length;
		int totalSize = 0;
		for (int i = 0; i < range; i++) {
			totalSize += lookupArray(i).cursor();
		}

		try {
			long[] results = new long[totalSize];
			for (int i = 0, cursor = 0; i < range; i++) {
				OpenLongArray.Elements elements = lookupArray(i);
				System.arraycopy(elements.elements(), 0, results, cursor, elements.cursor());
				cursor += elements.cursor();
			}
			return results;
		}
		catch (IndexOutOfBoundsException e) {
			// XXX this should only happen when the size of one or more "list"
			// has changed since the totalSize is calculated - very rare but
			// possible - simply just retry.
			System.err.println("trying lookupAll again..");
			return lookupAll();
		}

	}

		// thread safe
	public long[] lookupAll (LongArrayIterator groupIds)
	{
		int totalSize = 0;
	    for (long groupId = groupIds.next(); groupId != LongArrayIterator.NoMoreElements;) {
			totalSize += lookupArray(groupId).cursor();
			groupId = groupIds.next();
		}

	    groupIds.reset();
		try {
			long[] results = new long[totalSize];
			int cursor = 0;
			for (long groupId = groupIds.next(); groupId != LongArrayIterator.NoMoreElements;) {
				OpenLongArray.Elements elements = lookupArray(groupId);
				System.arraycopy(elements.elements(), 0, results, cursor, elements.cursor());
				cursor += elements.cursor();
				groupId = groupIds.next();
			}
			return results;
		}
		catch (IndexOutOfBoundsException e) {
			// XXX this should only happen when the size of one or more "list"
			// has changed since the totalSize is calculated - very rare but
			// possible - simply just retry.
			System.err.println("trying lookupAll again..");
			return lookupAll(groupIds.reset());
		}
	}

	private OpenLongArray retrieveGroup (long groupId)
	{

		OpenObjectArray.Elements<OpenLongArray> array = _groups.array();

			// lazy init and fill all the groups
		if (groupId >= array.cursor()) {
			synchronized (_groups) {
				if (groupId >= array.cursor()) {
					while (groupId >= array.cursor()) {
						_groups.add(new OpenLongArray(this));
						array = _groups.array();
					}
				}
			}
		}
		return array.elements()[(int)groupId];
	}


	public long[] lookup (long groupId)
	{
		OpenLongArray.Elements elements = lookupArray(groupId);
		return Arrays.copyOf(elements.elements(), elements.cursor());
	}

	public OpenLongArray.Elements lookupArray (long groupId)
	{
		return retrieveGroup(groupId).array();
	}


		// called by defn on each write
	@Override
	public void passThrough (Object groupId, long recordId)
	{
		retrieveGroup(((Record)groupId).tx()._id).add(recordId);
	}


	/*
	 * (non-Javadoc)
	 * @see array.OpenLongArray.Filter#keepThis(long)
	 */
	public boolean keepThis (long recordId, int currentArraySize)
	{
		return _filter.keepThis(recordId, currentArraySize);
	}


	public static class Filter
	{
		public boolean keepThis (long recordId, int currentArraySize)
		{
			return true;
		}
	}
}


