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


public class DefnByte <T extends Record> extends Defn <T>
{
	public static final int Size = 1 + Defn.HeaderByteSize;
	public static final Class[] TypeClasses = new Class[] {byte.class, Byte.class};

	public static final byte[][] BytesArray;
	static {
		BytesArray = new byte[128][];
		for (int i = 0; i < 128; i++) {
			BytesArray[i] = new byte[]{(byte)i};
		}
	}


	public DefnByte (Meta meta)
	{
		super(meta);
	}


	public int size ()
	{
		return Size;
	}

	public DefnByte index (boolean index)
	{
		super.index(index);
		return this;
	}

	public DefnByte lazyLoad (boolean lazyLoad)
	{
		super.lazyLoad(lazyLoad);
		return this;
	}


	public DefnByte listener (Class<? extends Listener> listenerClass)
	{
		super.listener(listenerClass);
		return this;
	}



	public Class[] typeClasses ()
	{
		return TypeClasses;
	}

	public Object readImpl (ByteBuffer dataIO, boolean directConvertFromBytes)
	  throws IOException
	{
		return dataIO.get();
	}

	@Override
	public byte[] writeImpl (byte[] dst, Object src, long i)
	  throws IOException
	{
		int byteValue = (Byte)src;
		dst[1] = (byte)byteValue;
		return dst;
	}




	public Object parse (String s)
	{
		return Byte.parseByte(s);
	}
}
