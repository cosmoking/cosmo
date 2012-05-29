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
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;

import cern.colt.function.LongComparator;

import org.cosmo.common.record.Defn.FormatType;
import org.cosmo.common.util.BitsUtil;
import org.cosmo.common.util.New;


public class DefnLong <T extends Record> extends Defn
{
	public static final int Size = 8 + Defn.HeaderByteSize;
	public static final Class[] TypeClasses = new Class[] {long.class, Long.class};

	public GroupByList _groupByList;


	public DefnLong (Meta declaringMeta)
	{
		super(declaringMeta);
	}


	public DefnLong index (boolean index)
	{
		super.index(index);
		return this;
	}

	public DefnLong lazyLoad (boolean lazyLoad)
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


	public Object readImpl (ByteBuffer dataIO, boolean directConvertFromBytess)
	  throws IOException
	{
		return dataIO.getLong();
	}

	@Override
	public byte[] writeImpl (byte[] dst, Object src, long i)
	  throws IOException
	{
		long v = (Long)src;
    	dst[1] = (byte)(v >>> 56);
    	dst[2] = (byte)(v >>> 48);
    	dst[3] = (byte)(v >>> 40);
    	dst[4] = (byte)(v >>> 32);
    	dst[5] = (byte)(v >>> 24);
    	dst[6] = (byte)(v >>> 16);
    	dst[7] = (byte)(v >>>  8);
    	dst[8] = (byte)(v >>>  0);
		return dst;
	}


	public Object parse (String s)
	{
		return Long.parseLong(s);
	}




	@Override
	public boolean ifTrueForFunction (Object rightValue, String function, Object leftValue)
	{
		if ("equals".equals(function)) {
			return rightValue == leftValue || rightValue.equals(leftValue);
		}
		if ("greaterThan".equals(function)) {
			return ((Long)rightValue).longValue() > ((Long)leftValue).longValue();
		}
		if ("lessThan".equals(function)) {
			return ((Long)rightValue).longValue() < ((Long)leftValue).longValue();
		}
		throw new IllegalArgumentException(function);
	}

}
