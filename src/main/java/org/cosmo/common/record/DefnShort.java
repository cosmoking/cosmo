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
import java.nio.ByteBuffer;


public class DefnShort <T extends Record> extends Defn
{
	public static final int Size = 2 + Defn.HeaderByteSize;
	public static final Class[] TypeClasses = new Class[] {short.class, Short.class};

	public DefnShort (Meta declaringMeta)
	{
		super(declaringMeta);
	}


	public DefnShort index (boolean index)
	{
		super.index(index);
		return this;
	}

	public DefnShort lazyLoad (boolean lazyLoad)
	{
		super.lazyLoad(lazyLoad);
		return this;
	}



	public int size ()
	{
		return Size;
	}

	public Class[] typeClasses ()
	{
		return TypeClasses;
	}

	public DefnShort listener (Class listenerClass)
	{
		super.listener(listenerClass);
		return this;
	}



	public Object readImpl (ByteBuffer dataIO, boolean directConvertFromBytes)
	  throws IOException
	{
		return dataIO.getShort();
	}

	@Override
	public byte[] writeImpl (byte[] dst, Object src, long i)
	  throws IOException
	{
		int v = (Short)src;
    	dst[1] = (byte)(v >>>  8);
    	dst[2] = (byte)(v >>>  0);
		return dst;
	}

	public String fmt (Object o, FormatType type)
	{
		if (o == null) {
			throw new RecordException("Can not format NULL value");
		}
		return Util.int2sortableStr((Short)o);
	}

	public Object parse (String s)
	{
		return Short.parseShort(s);
	}

	public void increment (long id)
	  throws IOException
	{
		short s = ((Short)read(id)).shortValue();
		if (s >= Short.MAX_VALUE) {
			return;
		}

		System.out.println("fix this: defnshort.increment");
		update(id, (short)(s + 1));
	}

	@Override
	public boolean ifTrueForFunction (Object rightValue, String function, Object leftValue)
	{
		if ("equals".equals(function)) {
			return rightValue == leftValue || rightValue.equals(leftValue);
		}
		if ("greaterThan".equals(function)) {
			return ((Short)rightValue).shortValue() > ((Short)leftValue).shortValue();
		}
		if ("lessThan".equals(function)) {
			return ((Short)rightValue).shortValue() < ((Short)leftValue).shortValue();
		}
		throw new IllegalArgumentException(function);
	}
}
