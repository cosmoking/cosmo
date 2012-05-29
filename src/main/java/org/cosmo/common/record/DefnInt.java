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


public class DefnInt <T extends Record> extends Defn
{
	public static final int Size = 4 + Defn.HeaderByteSize;
	public static final Class[] TypeClasses = new Class[] {int.class, Integer.class};


	public DefnInt (Meta declaringMeta)
	{
		super(declaringMeta);
	}


	public DefnInt index (boolean index)
	{
		super.index(index);
		return this;
	}

	public DefnInt lazyLoad (boolean lazyLoad)
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

	public Object readImpl (ByteBuffer dataIO, boolean directConvertFromBytes)
	  throws IOException
	{
		return dataIO.getInt();
	}

	@Override
	public byte[] writeImpl (byte[] dst, Object src, long i)
	  throws IOException
	{
		int v = (Integer)src;
    	dst[1] = (byte)(v >>> 24);
    	dst[2] = (byte)(v >>> 16);
    	dst[3] = (byte)(v >>>  8);
    	dst[4] = (byte)(v >>>  0);
		return dst;
	}

	public String fmt (Object o, FormatType type)
	{
		if (o == null) {
			throw new RecordException("Can not format NULL value");
		}
		return Util.int2sortableStr((Integer)o);
	}

	public Object parse (String s)
	{
		return Integer.parseInt(s);
	}


	@Override
	public boolean ifTrueForFunction (Object rightValue, String function, Object leftValue)
	{
		if ("equals".equals(function)) {
			return rightValue == leftValue || rightValue.equals(leftValue);
		}
		if ("greaterThan".equals(function)) {
			return ((Integer)rightValue).intValue() > ((Integer)leftValue).intValue();
		}
		if ("lessThan".equals(function)) {
			return ((Integer)rightValue).intValue() < ((Integer)leftValue).intValue();
		}
		throw new IllegalArgumentException(function);
	}



}
