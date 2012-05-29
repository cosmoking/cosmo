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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.cosmo.common.array.OpenLongArray.Elements;
import org.cosmo.common.array.OpenLongArray.Filter;

import org.cosmo.common.util.Constants;
import cern.colt.list.LongArrayList;
import cern.colt.list.ObjectArrayList;

/*
	- A growOnly long array with elements directly accessible
	- non-block read access , block for add
	- filters on resize and add - i.e. used as cache to either remove outdated or least used entries
	- it's very important to note that Elements must be accessed in the order of cursor then elements
*/
public class OpenObjectArray<T>
{
	private static final int BufferSize = 32;
	private static final int MaxBufferSize = 1024;
	private static final Object[] BufferInitArray = new Object[MaxBufferSize];
	private static final Object BufferInitValue = null;
	static {
		Arrays.fill(BufferInitArray, BufferInitValue);
	}

		// this list allows fast thread safe read and swap without synchronization
	volatile Elements<T> _array;
	Filter _filter;
	Class<T> _clazz;

	public OpenObjectArray (Class<T> clazz, Filter filter)
	{
		_clazz = clazz;
		_array = new Elements<T>(clazz);
		_filter = filter;
	}

	public OpenObjectArray (Class<T> clazz, Filter filter, Object[] items)
	{
		this(clazz, filter);
		_array = createFilteredElements(filter, items, OpenObjectArray.BufferSize);
	}



		// array cursor and elements can change unexpectly but in the order of "cursor" then "elements"
		// so always use cursor to determine size of the elements
	public Elements array ()
	{
		return _array;
	}

	// add is synchronized. get is not and thread safe
	public boolean add (T item)
	{
		synchronized (_array) {
			return addFastUnsafe(item, OpenObjectArray.BufferSize);
		}
	}


	public boolean addFastUnsafe (T item, int bufferSize)
	{
		if (_filter != null && !_filter.keepThis(item)) {
			return false;
		}

		if (_array._cursor.get() >= _array._elements.length) {
			if (_filter == null) {
				_array = new Elements<T>(_clazz, _array._elements, bufferSize);
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


	private Elements createFilteredElements (Filter filter, Object[] items, int bufferSize)
	{
		ObjectArrayList tempList = new ObjectArrayList(items.length);
		for (int i = 0; i < items.length; i++) {
			if (filter.keepThis(items[i])) {
				tempList.add(items[i]);
			}
		}
		return new Elements<T>(_clazz, tempList.elements(), bufferSize);
	}


	public static class Elements<T>
	{
		private T[] _elements;
		private AtomicInteger _cursor;

		public Elements (Class<T> clazz)
		{
			this(clazz, Constants.EmptyObjectArray);
		}

		public Elements (Class<T> clazz, Object[] initialData)
		{
			this(clazz, initialData, BufferSize);
		}

		public Elements (Class<T> clazz, Object[] initialData, int bufferSize)
		{
			if (bufferSize > MaxBufferSize) {
				throw new IllegalArgumentException("buffer can not exceed " + MaxBufferSize);
			}

			_cursor = new AtomicInteger(initialData.length);
			_elements = (T[])Array.newInstance(clazz ,_cursor.get() + bufferSize);
			System.arraycopy(initialData, 0, _elements, 0, initialData.length);
			System.arraycopy(BufferInitArray, 0, _elements, initialData.length, bufferSize);
		}


		public T[] elements ()
		{
			return _elements;
		}

		public int cursor ()
		{
			return _cursor.get();
		}
	}

	static interface Filter
	{
		public boolean keepThis (Object element);
	}
}



