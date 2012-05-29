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


public class SimpleLongArray
{

    public long[] _longs;
    public int _count;

    public SimpleLongArray ()
    {
    }

    public SimpleLongArray (int size)
    {
    	_longs = new long[size];
    }

    public void add (long v)
    {
    	_longs[_count++] = v;
    }

    public void addAll (long[] v)
    {
    	System.arraycopy(v, 0, _longs, _count, v.length);
    	_count += v.length;
    }

    public long[] atMost (int maxEntries)
    {
    	if (_count == 0) {
    		return org.cosmo.common.util.Constants.EmptyLongArray;
    	}
    	else if (_count > maxEntries) {
    		return Arrays.copyOf(_longs, maxEntries);
    	}
    	else {
    		return Arrays.copyOf(_longs, _count);
    	}
    }

}
