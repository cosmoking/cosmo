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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class CursorList2 extends ArrayList
{
	public static enum Type
	{
		First,
		Next,
		Previous,
		Last
	}

	public int _beginRange;
	public int _endRange;
	private int _rangeSize;


	public CursorList2 (Iterator list, int rangeSize)
	{
		this(iteratorToList(list), rangeSize);
	}


	public CursorList2 (List list, int rangeSize)
	{
		super(list);
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
		switch (type) {
			case First: return hasFirstRange(true);
			case Next : return hasNextRange(true);
			case Previous : return hasPreviousRange(true);
			case Last : return hasLastRange(true);
			default: return false;
		}
	}

	public boolean hasLastRange (boolean set)
	{
		int end = size();
		int begin = size() > _rangeSize ? size() - _rangeSize : 0;
		return checkAndSetRange(begin, end, set);
	}

	public boolean hasLastRange ()
	{
		return hasLastRange(false);
	}

	public boolean hasFirstRange (boolean set)
	{
		int begin = 0;
		int end = size() > _rangeSize ? _rangeSize : size();
		return checkAndSetRange(begin, end, set);
	}

	public boolean hasFirstRange ()
	{
		return hasFirstRange(false);
	}


	public boolean hasNextRange (boolean set)
	{
		int begin = _endRange < size() ?  _endRange : _beginRange;
		int end = begin + _rangeSize < size() ? begin + _rangeSize : size();
		return checkAndSetRange(begin, end, set);
	}

	public boolean hasNextRange ()
	{
		return hasNextRange(false);
	}

	public boolean hasPreviousRange (boolean set)
	{
		int end = _beginRange > 0 ? _beginRange : _endRange;
		int begin = end - _rangeSize > 0 ? end - _rangeSize : 0;
		return checkAndSetRange(begin, end, set);
	}

	public boolean hasPreviousRange ()
	{
		return hasPreviousRange(false);
	}


	private boolean checkAndSetRange (int begin, int end, boolean set)
	{
		boolean result = end != _endRange && begin != _beginRange;
		if (set) {
			_beginRange = begin;
			_endRange = end;
		}
		return result;
	}



	public List getCurrentRange ()
	{
		List l = subList(_beginRange, _endRange);
		return l;
	}

/*
	public List getLastRange ()
	{
		_endRange = size();
		_beginRange = size() > _rangeSize ? size() - _rangeSize : 0;
		List l = subList(_beginRange, _endRange);
		return l;
	}

	public List getFirstRange ()
	{
		_beginRange = 0;
		_endRange = size() > _rangeSize ? _rangeSize : size();
		List l = subList(_beginRange, _endRange);
		return l;
	}

	public List getNextRange ()
	{
		int begin = _endRange < size() ?  _endRange : _beginRange;
		int end = begin + _rangeSize < size() ? begin + _rangeSize : size();
		_beginRange = begin;
		_endRange = end;
		List l = subList(begin, end);
		return l;
	}

	public List getPreviousRange ()
	{
		int end = _beginRange > 0 ? _beginRange : _endRange;
		int begin = end - _rangeSize > 0 ? end - _rangeSize : 0;
		_beginRange = begin;
		_endRange = end;
		List l = subList(begin, end);
		return l;
	}
*/
	public static List iteratorToList (Iterator list)
	{
		ArrayList l = new ArrayList();
		while (list.hasNext()) {
			l.add(list.next());
		}
		return l;
	}


	public static void main (String[] args)
	  throws Exception
	{


	}

}
