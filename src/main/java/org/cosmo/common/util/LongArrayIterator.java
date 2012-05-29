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

import java.io.IOException;

import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.OpenBitSet;
import org.apache.lucene.util.OpenBitSetIterator;


	// abstracts long[] to be iterator objects based on Array or BitSet
abstract public class LongArrayIterator
{
	public static final int NoMoreElements = DocIdSetIterator.NO_MORE_DOCS;
	abstract public int size ();
	abstract public long next ();
	abstract public LongArrayIterator reset ();



	public static class Array extends LongArrayIterator
	{

		private long[] _longArray;
		private int _cursor;

		public Array (long[] longArray)
		{
			_longArray = longArray;
		}

		public int size ()
		{
			return _longArray.length;
		}

		public long next ()
		{
			return _cursor < _longArray.length ? _longArray[_cursor++] : NoMoreElements;
		}

		public LongArrayIterator reset ()
		{
			_cursor = 0;
			return this;
		}

	}


	public static class BitSet extends LongArrayIterator
	{
		private OpenBitSet _longArray;
		private DocIdSetIterator _iterator;

		public BitSet (OpenBitSet longArray)
		{
			_longArray = longArray;
			_iterator = longArray.iterator();
		}

		public int size ()
		{
			return (int)_longArray.cardinality();
		}

		public long next ()
		{
			try {
				return _iterator.nextDoc();
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public LongArrayIterator reset ()
		{
			_iterator = _longArray.iterator();
			return this;
		}


	}

}


