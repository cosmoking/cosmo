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
package org.cosmo.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.cosmo.common.net.Session;


public class Pagination
{


	public enum Type
	{
		First {
			public boolean testPage (Pagination pagination, boolean set) {
				int begin = 0;
				int end = pagination.size() > pagination._perPageSize ? pagination._perPageSize : pagination.size();
				//list._currentPage = 1;
				return checkAndSetPage(pagination, begin, end, 1, set);
			}
		},

		Next {
			public boolean testPage (Pagination pagination, boolean set) {
				int begin = pagination._endCursor < pagination.size() ?  pagination._endCursor : pagination._beginCursor;
				int end = begin + pagination._perPageSize < pagination.size() ? begin + pagination._perPageSize : pagination.size();
				//list._currentPage++;
				return checkAndSetPage(pagination, begin, end, pagination._currentPage + 1, set);
			}
		},
		Previous {
			public boolean testPage (Pagination pagination, boolean set) {
				int end = pagination._beginCursor > 0 ? pagination._beginCursor : pagination._endCursor;
				int begin = end - pagination._perPageSize > 0 ? end - pagination._perPageSize : 0;
				//list._currentPage--;
				return checkAndSetPage(pagination, begin, end, pagination._currentPage - 1, set);
			}
		},
		Last {
			public boolean testPage (Pagination pagination, boolean set) {
				int end = pagination.size();
				int begin = pagination.size() > pagination._perPageSize ? pagination.size() - pagination._perPageSize : 0;
				//list._currentPage = (int)Math.ceil(list.size() / list._pageSize);
				float page = (float)pagination.size() / (float)pagination._perPageSize ;
				return checkAndSetPage(pagination, begin, end, (int)Math.ceil(page), set);
			}
		};


		abstract public boolean testPage (Pagination pagination, boolean set);


		private static boolean checkAndSetPage (Pagination pagination, int begin, int end, int currentPage, boolean set)
		{
			boolean result = end != pagination._endCursor && begin != pagination._beginCursor;
			if (set) {
				pagination._beginCursor = begin;
				pagination._endCursor = end;
				pagination._currentPage = currentPage;
			}
			return result;
		}

		public boolean testPage (Pagination list) {
			if (list == null || list.size() == 0) {
				return false;
			}
			return testPage(list, false);
		}

		public boolean setPage (Pagination list) {
			if (list == null) {
				return false;
			}
			return testPage(list, true);
		}
	}

		// The range to display from the current page, ie, at page 4 with range 3 ... [1 2 3] 4 [5 6 7]
	public static final int DisplayRange = 3;

	public static final Pagination EmptyList = new Pagination(Constants.EmptyLongArray, 0);

		// The range that is buffered from the current page,
		// at page 10 with double of display range  [1 2 3 4 5 6 7 8 9]  10  [11 12 13 14 15 16 17 18 19]
		// this means the buffer won't fault until it goes beyond this bound
		// or at page with 1 [2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19]
	public static final int BufferRange = DisplayRange * 3;

		// The size of the buffer
		//  [1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19]   19 * 25(size) * 8(byte) = 3.8kb  ~ 475 elements
	public static final int BufferPages = (BufferRange * 2) + 1;
	public int _beginCursor;
	public int _endCursor;
	private int _perPageSize;
	private int _elementsSize;
	public int _currentPage;

		// just buffer not entire element set - minimal memory possible - see above
	private long[] _elementsBuffer;
	private int _elementsBufferOffset;


		// Elements's raw are not cached but referenced. it's up to Elements impl how to return the raw results
	public Pagination (long[] data, int pageSize)
	{
		_currentPage = 1;
		_perPageSize = pageSize;
		_elementsBuffer = new long[BufferPages * _perPageSize > data.length ? data.length : BufferPages * _perPageSize];
		System.arraycopy(data, 0, _elementsBuffer, 0, _elementsBuffer.length);
		_elementsBufferOffset = 0;
		_beginCursor = 0;
		_endCursor = _perPageSize < size() ?  pageSize : size();
		_elementsSize = data.length;
		setPage(Pagination.Type.First);
	}

	public int size ()
	{
		return _elementsSize;
	}

	public void clear ()
	{
		_elementsBuffer = null;
	}


		// basically calls elements.data(context) to get raw - this allows adhoc get on the actual elements
		// without storing all the elements, both "search" and "latest" can rerun the query to generate the results
	public long[] getCurrentPage (DataProvider elements, Object dataSource)
	  throws Exception
	{
			// create currentPage list
		long[] currentPage = new long[_endCursor - _beginCursor];

		if (currentPage.length > _elementsBuffer.length) {
			throw new RuntimeException("Range can not be bigger than buffer");
		}

			// test is requested range is within buffer
		if (_beginCursor >= _elementsBufferOffset &&_endCursor <= _elementsBufferOffset + _elementsBuffer.length) {
			System.arraycopy(_elementsBuffer, _beginCursor - _elementsBufferOffset, currentPage, 0, currentPage.length);
			return currentPage;
		}

		else {
				// get the same result again
			long[] results = elements.fetchData(dataSource);

				// calculate both ends of window by BufferRange and PageSize
			int begin = _beginCursor - (BufferRange * _perPageSize);
			begin = begin <= 0 ? 0 : begin;
			int end = _endCursor + (BufferRange * _perPageSize);
			end = end > results.length ? results.length : end;

				// extend to full length of buffer if it's at beginning or end of the page
			if (begin == 0) {
				end = begin + _elementsBuffer.length;
			}
			else if (end == results.length) {
				begin = end - _elementsBuffer.length;
			}


				// copy result into buffer
			System.arraycopy(results, begin, _elementsBuffer, 0, _elementsBuffer.length);
			_elementsBufferOffset = begin;

				// recall same method again to return currentPage
			return getCurrentPage(elements, dataSource);
		}
	}


	public void setPageSize (int pageSize)
	{
		_perPageSize = pageSize;
	}

	public boolean setPage (String type)
	{
		return setPage(Type.valueOf(type));
	}

	public boolean setPage (Type type)
	{
		return type.setPage(this);
	}

	public boolean setPage (int pageIdx)
	{
		if (pageIdx == Integer.MAX_VALUE) {
			Type.Last.testPage(this, true);
			return true;
		}

		if (pageIdx <= 1) {
			Type.First.testPage(this, true);
			return true;
		}

		if (pageIdx > _currentPage) {
			for (int i = 0, size = pageIdx - _currentPage; i < size; i++) {
				Type.Next.testPage(this, true);
			}
			return true;
		}

		if (pageIdx < _currentPage) {
			for (int i = 0, size = _currentPage - pageIdx; i < size; i++) {
				Type.Previous.testPage(this, true);
			}
			return true;
		}

		return false;
	}

	public boolean testPage (String type)
	{
		return testPage(Type.valueOf(type));
	}

	public boolean testPage (Type type)
	{
		return type.testPage(this, false);
	}


		//
	public int pagesAvailableFromLeft ()
	{
		int beginCursorOri = _beginCursor;
		int endCursorOri = _endCursor;
		int currentPageOri =_currentPage;

		int pagesAvailableFromLeft = DisplayRange;
		for (int i = 0; i < DisplayRange; i++) {
			if (!Type.Previous.testPage(this, true)) {
				pagesAvailableFromLeft = i;
				break;
			}
		}

		_beginCursor = beginCursorOri;
		_endCursor = endCursorOri;
		_currentPage = currentPageOri;
		return pagesAvailableFromLeft;

	}

	public int pagesAvailableFromRight ()
	{
		int beginCursorOri = _beginCursor;
		int endCursorOri = _endCursor;
		int currentPageOri =_currentPage;

		int pagesAvailableFromRight = DisplayRange;
		for (int i = 0; i < DisplayRange; i++) {
			if (!Type.Next.testPage(this, true)) {
				pagesAvailableFromRight = i;
				break;
			}
		}

		_beginCursor = beginCursorOri;
		_endCursor = endCursorOri;
		_currentPage = currentPageOri;
		return pagesAvailableFromRight;
	}


	public void resizePage (int size)
	{
			// if there is a previous we just rewind, resize, then come back
		if (testPage(Type.Previous)) {
			setPage(Type.Previous);
			_perPageSize = size;
			setPage(Type.Next);
		}
			// otherwise this is first page, simply resize and set to first
		else {
			_perPageSize = size;
			setPage(Type.First);
		}
	}


	public static interface DataProvider<T>
	{
		public long[] fetchData (T context) throws Exception;
	}






}
