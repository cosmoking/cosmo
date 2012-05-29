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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;

import org.cosmo.common.file.IncompleteWriteException;
import org.cosmo.common.util.Bytes;


public class DefnBlob <T extends Record> extends DefnBytes
{
	Class<? extends BlobStore> _blobStoreClass;
	BlobStore _blobStore;

	public DefnBlob (Meta declaringMeta)
	{
		super(declaringMeta, 0);
	}


	public DefnBlob blobStore (Class<? extends BlobStore> blobStore)
	{
		_blobStoreClass = blobStore;
		return this;
	}

	public int size ()
	{
		return DefnLong.Size;
	}

	@Override
	public void lazyFieldInit ()
	{
		super.lazyFieldInit();
		try {
			Constructor constructor = _blobStoreClass.getConstructor(new Class[] {Defn.class});
			_blobStore = (BlobStore)constructor.newInstance(new Object[] {this});
			_blobStore._channel.checkIncompleteWrite(new File(_blobStore._channel.filename()));
		}
		catch (IncompleteWriteException e) {
			System.err.println(e.getMessage());
		}

		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Bytes readBytesObject (ByteBuffer dataIO, Defn defn, int size)
	  throws IOException
	{
		long lobPos = dataIO.getLong();
		if (lobPos >= 0) {
			return _blobStore.read(lobPos);
		}
		return null;
	}


	@Override
	public byte[] readBytes (ByteBuffer dataIO, Defn defn, int size)
	  throws IOException
	{
		return readBytesObject(dataIO, defn, size).bytes();
	}

	public boolean hasAdditionalSizeInfo ()
	{
		return true;
	}


	public DefnBlob lazyLoad (boolean lazyLoad)
	{
		super.lazyLoad(lazyLoad);
		return this;
	}

/*
	public void writeImpl (ByteBuffer dataIO, Object value, long i)
	  throws IOException
	{
		byte[] blob = value instanceof BytesBuffer
			? (byte[])value
			: ((BytesBuffer)value)._bytes;

		int length = value instanceof BytesBuffer
			? ((byte[])value).length
			: ((BytesBuffer)value)._count;
	}
*/

	// first write the actual size info bytes followed by the actual string data, pass either byte[] or BytesBuffer
	@Override
	public void writeBytes (byte[] dst, int dstPos, Defn defn, int defnSize, byte[] src, int srcLen)
	  throws IOException
	{
		if (this._declaringMeta._mode._isMaster) {
			long lobpos = (src != null) ? _blobStore.store(src, 0, srcLen) : -1;
			dst[1] = (byte)(lobpos >>> 56);
			dst[2] = (byte)(lobpos >>> 48);
			dst[3] = (byte)(lobpos >>> 40);
			dst[4] = (byte)(lobpos >>> 32);
			dst[5] = (byte)(lobpos >>> 24);
			dst[6] = (byte)(lobpos >>> 16);
			dst[7] = (byte)(lobpos >>>  8);
			dst[8] = (byte)(lobpos >>>  0);
		}
	}
}
