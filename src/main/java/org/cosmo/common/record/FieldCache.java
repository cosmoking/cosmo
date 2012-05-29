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
import java.lang.reflect.Field;
import java.nio.ByteBuffer;

import org.cosmo.common.util.Bits;

import cern.colt.function.LongComparator;

// NOT! thread safe, access and set is not - only when growing it's ok
public abstract class FieldCache extends Listener
{
	public static final float GrowFactor = 1.2F;
	public final Field ElementsArrayField;


	public FieldCache ()
	{
		try {
				// This allows as to do set and not deal with types
			ElementsArrayField = (Field)this.getClass().getField("_elements");
		}
		catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}

	abstract public void setValue (int i, Object value);
	abstract Object array ();
	abstract int length();
	abstract int unitSize();



	public byte getByte (int i)
	{
		throw new RuntimeException("subclass need to override!");
	}

	public short getShort (int i)
	{
		throw new RuntimeException("subclass need to override!");
	}

	public int getInt (int i)
	{
		throw new RuntimeException("subclass need to override!");
	}

	public long getLong (int i)
	{
		throw new RuntimeException("subclass need to override!");
	}

	public int totalSize ()
	{
		return length() * unitSize();
	}

	@Override
	public int sync (int maxCounts)
	  throws IOException
	{
		ByteBuffer buf =_defn.readFullRawBytes(maxCounts);
		int count = Math.min(_defn.readCount(), maxCounts);
		org.cosmo.common.util.Util.setObjectFieldValue(this, ElementsArrayField, Array.newInstance(ElementsArrayField.getType().getComponentType(), count));

			// for boolean, byte, char, short, int, long, float, and double.  the readImpl returns native type
		if (_defn.isFieldTypePrimitive()) {
			for (int i = 0; i < count; i++) {
				buf.get();
				setValue(i, _defn.readImpl(buf, false));
			}
		}
		else if (_defn.size() == DefnLong.Size){
			for (int i = 0; i < count; i++) {
				buf.get();
				setValue(i, buf.getLong());
			}
		}
		else {
			throw new RuntimeException("other types need to implmented" + _defn.getDeclaringFieldName());
		}

		return count;
	}

	@Override
	public void passThrough (Object value, long id)
	{
		ensureCapacity(id, array(), length());
		setValue((int)id, value);
	}

		// XXX need to double check on this
	private void ensureCapacity (long id, Object elements, int length)
	{
		if (id >= length) {
			synchronized (this) {
				if (id >= Array.getLength(elements)) {
					Object newArray = org.cosmo.common.util.Util.growArray(elements);
					org.cosmo.common.util.Util.setObjectFieldValue(this, ElementsArrayField, newArray);
				}
			}
		}
	}


	// Two Record Id are passed in and we compare based on fieldCache's short value
	public static abstract class Comparator implements LongComparator
	{
		public FieldCache _fieldCache;

		protected Comparator ()
		{
		}

		@Override
		abstract public int compare(long id1, long id2);

		public boolean equals(Object obj)
		{
			throw new RuntimeException("Not Supported");
		}
	}


	public abstract static class IntFieldCache extends FieldCache
	{
		public volatile int[] _elements;
		Object array () {return _elements;}
		int length() { return _elements.length;}
		int unitSize() { return 4;}


		@Override
		public int getInt (int i)
		{
			return _elements[i];
		}

		abstract public void setValue (int i, Object value);
	}

	public abstract static class ShortFieldCache extends FieldCache
	{
		public volatile short[] _elements;
		Object array () {return _elements;}
		int length() { return _elements.length;}
		int unitSize() { return 2;}

		@Override
		public short getShort (int i)
		{
			return _elements[i];
		}

		abstract public void setValue (int i, Object value);
	}

	public abstract static class ByteFieldCache extends FieldCache
	{
		public volatile byte[] _elements;
		Object array () {return _elements;}
		int length() { return _elements.length;}
		int unitSize() { return 1;}

		@Override
		public byte getByte (int i)
		{
			return _elements[i];
		}

		abstract public void setValue (int i, Object value);
	}

	public abstract static class LongBitsFieldCache extends FieldCache
	{
		public volatile Bits.Atomic[] _elements;
		Object array () {return _elements;}
		int length() { return _elements.length;}
		int unitSize() { return 1;}

		@Override
		public long getLong (int i)
		{
			return _elements[i].get();
		}

		abstract public void setValue (int i, Object value);
	}
}






/*
abstract class FieldCache2
{
	public static final float GrowFactor = 1.2F;
	Defn _defn;

	public FieldCache2 (Defn defn)
	{
		_defn = defn;
	}

	abstract void sync (int maxCounts) throws IOException;
	abstract Object array();
	abstract void setArray (Object array);
	abstract int length ();
	abstract void set (int id, long value);


		// not thread safe! rely on the record store write to be thread safe
	public void passThrough (long value, int id)
	{
		if (id >= length()) {
			synchronized (array()) {
				int length = length();
				if (id >= length) {
					Object newElements = Array.newInstance(_defn.primitiveType(), (int)(length * GrowFactor) + 1);
					System.arraycopy(array(), 0, newElements, 0, length());
					setArray(newElements);
				}
			}
		}
		set(id, value);
	}

	public int binarySearchSortedIds (long[] ids, Number key, boolean increasing)
	{
		int low = 0;
		int high = ids.length - 1;

		if (increasing) {
			while (low <= high) {
			    int mid = (low + high) >>> 1;
			    long midVal = getLong(ids[mid]);  //   ids[mid] gets the actual id, getLong then gets the value
			    if (midVal < key)
			    	low = mid + 1;
			    else if (midVal > key)
			    	high = mid - 1;
			    else
				    return mid; // key found
			}
			return -(low + 1);  // key not found.
		}
		else {
			while (low <= high) {
			    int mid = (low + high) >>> 1;
			    long midVal = getLong(ids[mid]);
			    if (midVal > key)
			    	low = mid + 1;
			    else if (midVal < key)
			    	high = mid - 1;
			    else
				    return mid; // key found
			}
			return -(low + 1);  // key not found.

		}
	}

	static class Long extends FieldCache2
	{
		volatile long[] _longElements;

		public Long (Defn defn)
		{
			super(defn);
		}

	}
}

*/
