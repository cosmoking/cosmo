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

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Field.Index;

import org.cosmo.common.record.Defn.FormatType;
import org.cosmo.common.record.DefnDate.Precision;
import org.cosmo.common.util.Constants;


	// TODO handle empty string
public class DefnStr <T extends Record> extends DefnBytes <T>
{
	public static final Class[] TypeClasses = new Class[] {String.class, CharSequence.class, char[].class};


	private boolean _trimToFit; // if true ByteOverException is thrown, else trim to fit
	public static final String Ellipses = " ..".intern();


	public DefnStr (Meta declaringMeta, int size, boolean trimToFit)
	{
		super(declaringMeta, size);
		_trimToFit = trimToFit;
	}


	public DefnStr index (boolean index)
	{
		super.index(index);
		return this;
	}

	public DefnStr index (boolean index, Index indexType)
	{
		super.index(index, indexType);
		return this;
	}

	public DefnStr lazyLoad (boolean lazyLoad)
	{
		super.lazyLoad(lazyLoad);
		return this;
	}

	public Class[] typeClasses ()
	{
		return TypeClasses;
	}

	@Override
	public Object readImpl (ByteBuffer dataIO, boolean directConvertFromBytes)
	  throws IOException
	{
		if (directConvertFromBytes) {
			return org.cosmo.common.util.Util.string(dataIO.array());
		}

		byte[] result = readBytes(dataIO, this, _variableSize);
		return result.length == 0 ? "" : org.cosmo.common.util.Util.string(result);
	}

	@Override
	public byte[] writeImpl (byte[] dst, Object src, long i)
	  throws IOException
	{
		byte[] srcBytes = org.cosmo.common.util.Util.UTF8(src.toString());
		try {
			writeBytes(dst, 1, this, _variableSize, srcBytes, srcBytes.length);
			return srcBytes;
		}
		catch (ByteOverFlow e) {
			if (!_trimToFit || _variableSize < 8) {
				throw e;
			}
			else {
					// here trim the source string  by the (amount of bytes over / 4)
				int sizeToTrim = (srcBytes.length - _variableSize) / 4;
					// pick either by 1 at a time, or bigger size
				sizeToTrim = sizeToTrim > 1 ? sizeToTrim : 1;

					// trim until it fits
				String s = src.toString();
				s = s.substring(0, s.length() - sizeToTrim - Ellipses.length()) + Ellipses;
				return writeImpl(dst, s, i);
			}
		}
	}


	@Override
	public Object parse (String s)
	{
		return s;
	}


	@Override
	public boolean ifTrueForFunction (Object rightValue, String function, Object leftValue)
	{
		if ("equals".equals(function)) {
			return rightValue == leftValue || rightValue.equals(leftValue);
		}
		if ("contains".equals(function)) {
			return ((String)rightValue).contains((String)leftValue);
		}
		throw new IllegalArgumentException(function);
	}



	public DefnStr formatter (Class<? extends DefnFormatter> formatter)
	{
		try {
			Constructor constructor = formatter.getConstructor(new Class[] {Defn.class});
			_formatter = (DefnFormatter)constructor.newInstance(new Object[] {this});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		return this;
	}




}
