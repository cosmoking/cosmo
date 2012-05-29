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


public class DefnBoolean <T extends Record> extends Defn <T>
{
	public static final int Size = 1 + Defn.HeaderByteSize;
	public static final Class[] TypeClasses = new Class[] {boolean.class, Boolean.class};

	public static final byte TrueByte = (byte)1;
	public static final byte FalseByte = (byte)0;
	public static final byte[] TrueBytes = new byte[]{TrueByte};
	public static final byte[] FalseBytes = new byte[]{FalseByte};


	public DefnBoolean (Meta meta)
	{
		super(meta);
	}


	public int size ()
	{
		return Size;
	}

	public DefnBoolean index (boolean index)
	{
		super.index(index);
		return this;
	}

	public DefnBoolean lazyLoad (boolean lazyLoad)
	{
		super.lazyLoad(lazyLoad);
		return this;
	}


	public Class[] typeClasses ()
	{
		return TypeClasses;
	}

	public Object readImpl (ByteBuffer dataIO, boolean directConvertFromBytes)
	  throws IOException
	{
		return dataIO.get() == TrueByte;
	}

	@Override
	public byte[] writeImpl (byte[] dst, Object src, long i)
	  throws IOException
	{
		boolean isTrue = (Boolean)src;
		dst[1] = isTrue ? TrueByte : FalseByte;
		return dst;
	}



	public Object parse (String s)
	{
		return Boolean.valueOf(s);
	}
}
