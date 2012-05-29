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

import org.cosmo.common.util.Bytes;
import org.cosmo.common.util.Constants;
import org.cosmo.common.util.New;


public class DefnBytes <T extends Record> extends Defn <T>
{

	public static final Class[] TypeClasses = new Class[] {byte[].class, Bytes.class};
	//public static final byte[] Pad = new byte[32768]; // this is a prett large pad .. anyways..



	public DefnBytes (Meta declaringMeta, int variableSize)
	{
		super(declaringMeta, variableSize, null);
	}

		// addition 1 or 2 bytes are allocated to determine the actual "size" of the String from the allocated size
	public int size ()
	{
		return (_variableSize < 256 ? _variableSize + 1 : _variableSize + 2) + Defn.HeaderByteSize;
	}

	public Class[] typeClasses ()
	{
			// use of Bytes allows lazy load - memory reclaimable, efficient
		return TypeClasses;
	}

	public Object readImpl (ByteBuffer dataIO, boolean directConvertFromBytes)
	  throws IOException
	{
		if (directConvertFromBytes) {
			return _field.getType() == Bytes.class
				? new Bytes(dataIO.array())
				: dataIO.array();
		}

		return _field.getType() == Bytes.class
			? readBytesObject(dataIO, this, _variableSize)
			: readBytes(dataIO, this, _variableSize);
	}


		// allow overrite by DefnBlob
	public Bytes readBytesObject (ByteBuffer dataIO, Defn defn, int size)
	  throws IOException
	{
		return new Bytes(readBytes(dataIO, this, _variableSize));
	}


	public byte[] readBytes (ByteBuffer dataIO, Defn defn, int size)
	  throws IOException
	{
		int actualSize = 0;
		  // first 1 or 2 bytes reserved for size
		if (size < 256) {
			actualSize = (int)(dataIO.get() & 0xFF);
		}
		else if (size < 32768) {
			actualSize = (int)(dataIO.getShort());
		}
		else {
			throw new IOException("Does not support size beyond 32768");
		}
		byte[] value = new byte[actualSize];
		dataIO.get(value);
		// skip remaining pads if any, also moves cursor
		dataIO.get(new byte[size - actualSize]);
		return value;
	}

	public byte[] writeImpl (byte[] dst, Object src, long i)
	  throws IOException
	{
		byte[] bytes = null;
		if (src == null) {
			bytes =  Constants.NullByteMarker;
		}
		else if (src instanceof Bytes) {
			bytes = ((Bytes)src).bytes();
		}
		else {
			bytes = (byte[])src;
		}

			// dstOffset starts 1 bcos 0 is reserved for header
		writeBytes(dst, 1, this, _variableSize, bytes, bytes.length);
		return bytes;
	}

		// first write the actual size info bytes followed by the actual string data, pass either byte[] or BytesBuffer
	public void writeBytes (byte[] dst, int dstPos, Defn defn, int defnSize, byte[] src, int srcLen)
	  throws IOException
	{
		if (srcLen > defnSize) {
			String msg = New.str("Defn:[", defn.getDeclaringFieldName(), "] Expect Size:[", defnSize, "] vs Actual Size:[", srcLen, "] with value:[", new String(src));
			throw new ByteOverFlow(msg, defnSize, srcLen - defnSize);
		}

		int length =  srcLen;

			// first 1 or 2 bytes reserved for size
		if (defnSize < 256) {
			dst[dstPos] = (byte)length;
		}
		else if (defnSize < 32768) {
			dst[dstPos] = (byte)(length >>>  8);
			dst[++dstPos] = (byte)(length >>>  0);
		}
		else {
			throw new IOException ("Does not support size beyond 32768");

		}
			// write data
		System.arraycopy(src, 0, dst, dstPos + 1, length);

		// write pad
		//System.arraycopy(Pad, 0, dataIO, bytes.length, size - length);

	}

	public DefnBytes lazyLoad (boolean lazyLoad)
	{
		super.lazyLoad(lazyLoad);
		return this;
	}


	public static class ByteOverFlow extends IOException
	{
		int _expectSize;
		int _sizeToTrim;

		public ByteOverFlow (String s, int expectSize, int sizeToTrim)
		{
			super(s);
			_expectSize = expectSize;
			_sizeToTrim = sizeToTrim;
		}

	}
}
