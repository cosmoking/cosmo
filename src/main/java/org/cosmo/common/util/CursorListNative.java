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
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class CursorListNative
{
	public enum Type
	{
		First {
			public boolean testRange (CursorListNative list, boolean set) {
				int begin = 0;
				int end = list.size() > list._rangeSize ? list._rangeSize : list.size();
				return checkAndSetRange(list, begin, end, set);
			}
		},

		Next {
			public boolean testRange (CursorListNative list, boolean set) {
				int begin = list._endRange < list.size() ?  list._endRange : list._beginRange;
				int end = begin + list._rangeSize < list.size() ? begin + list._rangeSize : list.size();
				return checkAndSetRange(list, begin, end, set);
			}
		},
		Previous {
			public boolean testRange (CursorListNative list, boolean set) {
				int end = list._beginRange > 0 ? list._beginRange : list._endRange;
				int begin = end - list._rangeSize > 0 ? end - list._rangeSize : 0;
				return checkAndSetRange(list, begin, end, set);
			}
		},
		Last {
			public boolean testRange (CursorListNative list, boolean set) {
				int end = list.size();
				int begin = list.size() > list._rangeSize ? list.size() - list._rangeSize : 0;
				return checkAndSetRange(list, begin, end, set);
			}
		};


		abstract public boolean testRange (CursorListNative list, boolean set);


		private static boolean checkAndSetRange (CursorListNative list, int begin, int end, boolean set)
		{
			boolean result = end != list._endRange && begin != list._beginRange;
			if (set) {
				list._beginRange = begin;
				list._endRange = end;
			}
			return result;
		}

		public boolean testRange (CursorListNative list) {
			if (list == null || list.isEmpty()) {
				return false;
			}
			return testRange(list, false);
		}

		public boolean setRange (CursorListNative list) {
			if (list == null) {
				return false;
			}
			return testRange(list, true);
		}
	}

	public int _beginRange;
	public int _endRange;
	private int _rangeSize;
	private int _newRangeSize;

	private long[] _elements;


	public int size ()
	{
		return _elements.length;
	}

	public boolean isEmpty ()
	{
		return _elements.length == 0;
	}

	public void clear ()
	{
		_elements = null;
	}

	public CursorListNative (long[] elements, int rangeSize)
	{
		_elements = elements;
		_beginRange = 0;
		setRangeSize(rangeSize);
		_endRange = _rangeSize < size() ?  rangeSize : size();
	}

	public void setRangeSize (int rangeSize)
	{
		_rangeSize = rangeSize;
	}

	public boolean setRange (String type)
	{
		return setRange(Type.valueOf(type));
	}

	public boolean setRange (Type type)
	{
		return type.setRange(this);
	}

	public boolean testRange (String type)
	{
		return testRange(Type.valueOf(type));
	}

	public boolean testRange (Type type)
	{
		return type.testRange(this, false);
	}


	public void resizeRange (int size)
	{
			// if there is a previous we just rewind, resize, then come back
		if (testRange(Type.Previous)) {
			setRange(Type.Previous);
			_rangeSize = size;
			setRange(Type.Next);
		}
			// otherwise this is first page, simply resize and set to first
		else {
			_rangeSize = size;
			setRange(Type.First);
		}
	}


	public long[] getCurrentRange ()
	{
		long[] currentRange = new long[_endRange - _beginRange];
		System.arraycopy(_elements, _beginRange, currentRange, 0, currentRange.length);
		return currentRange;
	}


}
