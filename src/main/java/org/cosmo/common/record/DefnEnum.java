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
import java.lang.reflect.Method;
import java.nio.ByteBuffer;


public class DefnEnum <T extends Record> extends Defn <T>
{
	public static final int Size = 1 + Defn.HeaderByteSize;
	public static final Class[] TypeClasses = new Class[] {Enum.class};

	Enum[] _enumList;

	public DefnEnum (Meta declaringMeta, Object defaultValue)
	{
		super(declaringMeta, 0, defaultValue);
	}


	public DefnEnum index (boolean index)
	{
		super.index(index);
		return this;
	}

	public DefnEnum lazyLoad (boolean lazyLoad)
	{
		super.lazyLoad(lazyLoad);
		return this;
	}

	public Class[] typeClasses ()
	{
		return TypeClasses;
	}

	public int size ()
	{
		return Size;
	}

	@Override
	public void lazyFieldInit ()
	{
		super.lazyFieldInit();
		try {
			Class enumClass = field().getType();
			Method enumMethod = enumClass.getDeclaredMethod("values", new Class[]{});
			_enumList= (Enum[])enumMethod.invoke(enumClass, new Object[]{});

		}
		catch (Exception e) {
			throw new RuntimeException("Unable to resolve Enum class for variable " + field().getName(), e);
		}
	}


	public Object readImpl (ByteBuffer dataIO, boolean directConvertFromBytes)
	  throws IOException
	{
		int enumIdx = (int)dataIO.get();
		return _enumList[enumIdx];
	}

	@Override
	public byte[] writeImpl (byte[] dst, Object src, long i)
	  throws IOException
	{
		int enumIdx = ((Enum)src).ordinal();
		dst[1] = (byte)enumIdx;

			// XXX convert this to array lookup!
		return dst;
	}



}

