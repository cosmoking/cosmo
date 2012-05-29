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
package org.cosmo.common.array;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.cosmo.common.util.Constants;
import cern.colt.list.LongArrayList;


/*
	- A growOnly long array with elements directly accessible
	- non-block read access , block for add
	- filters on resize and add - i.e. used as cache to either remove outdated or least used entries
	- it's very important to note that Elements must be accessed in the order of cursor then elements
*/
public class OpenLongArray
{
	private static final int BufferSize = 32;
	private static final int MaxBufferSize = 1024;
	private static final long[] BufferInitArray = new long[MaxBufferSize];
	private static final long BufferInitValue = Long.MIN_VALUE;
	static {
		Arrays.fill(BufferInitArray, BufferInitValue);
	}

		// this allows fast thread safe read and swap without synchronization
	volatile Elements _array;
	Filter _filter;

	public OpenLongArray (Filter filter)
	{
		_array = new Elements();
		_filter = filter;
	}

	public OpenLongArray (Filter filter, long[] items)
	{
		this(filter);
		_array = createFilteredElements(filter, items, OpenLongArray.BufferSize);
	}


	// array cursor and elements can change unexpectly but in the order of "cursor" then "elements"
	// so always use cursor to determine size of the elements
	public Elements array ()
	{
		return _array;
	}


		// add is synchronized. get is not and thread safe
	public boolean add (long item)
	{
		synchronized (_array) {
			return addFastUnsafe(item, OpenLongArray.BufferSize);
		}
	}


	public boolean addFastUnsafe (long item, int bufferSize)
	{
		if (_filter != null && !_filter.keepThis(item, _array._cursor.get())) {
			return false;
		}

		if (_array._cursor.get() >= _array._elements.length) {
			if (_filter == null) {
				_array = new Elements(_array._elements, bufferSize);
			}
			else {
				_array = createFilteredElements(_filter,_array._elements, bufferSize);
			}
		}

		// set value before increment cursor - do not change the order
		_array._elements[_array._cursor.get()] = item;
		_array._cursor.incrementAndGet();
		return true;
	}



	private Elements createFilteredElements (Filter filter, long[] items, int bufferSize)
	{
		LongArrayList tempList = new LongArrayList(items.length);
		for (int i = 0; i < items.length; i++) {
			if (filter.keepThis(items[i], items.length)) {
				tempList.add(items[i]);
			}
		}
		return new Elements(tempList.elements(), bufferSize);
	}


	public static class Elements
	{
		private long[] _elements;
		private AtomicInteger _cursor;

		public Elements ()
		{
			this(Constants.EmptyLongArray);
		}

		public Elements (long[] initialData)
		{
			this(initialData, BufferSize);
		}

		public Elements (long[] initialData, int bufferSize)
		{
			if (bufferSize > MaxBufferSize) {
				throw new IllegalArgumentException("buffer can not exceed " + MaxBufferSize);
			}
			_cursor = new AtomicInteger(initialData.length);
			_elements = new long[_cursor.get() + bufferSize];
			System.arraycopy(initialData, 0, _elements, 0, initialData.length);
			System.arraycopy(BufferInitArray, 0, _elements, initialData.length, bufferSize);
		}

		public long[] elements ()
		{
			return _elements;
		}

		public int cursor ()
		{
			return _cursor.get();
		}
	}

	public static interface Filter
	{
		public boolean keepThis (long element, int currentArraySize);
	}
}
