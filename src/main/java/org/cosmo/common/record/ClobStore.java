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

import org.cosmo.common.util.ByteSequence;
import org.cosmo.common.util.Bytes;
import org.cosmo.common.util.New;

/*
 *	Stores write once clob in the file system.
 *
 *  TODO: change Lob to use FileChannel so that no synchronized is needed and no need
 *        to open 2 file handle.  FileChannel support pread(). positional read that
 *        is atomic and does not change the file pointer.
 *
 *  TODO: a strategy using compressed content for caching
 */
public class ClobStore extends LobStore
{

	public static final int MaxSizeAllowed = 1024 * 1024 * 24;
	public static final int InMemoryLobSize = 3072;

	public ClobStore (Defn defn)
	{
		super(defn);
	}

	@Override
	public String getExtension () {
    	return ".clob";
    }


    	// Not Thread safe - consumer must manage their own
	public long store (CharSequence data)
	  throws IOException
	{
		byte[] bytes = org.cosmo.common.util.Util.UTF8(data);
		return store (bytes);
	}


	public long store (byte[] utf8RawBytes)
	  throws IOException
	{
		if (utf8RawBytes.length > MaxSizeAllowed) {
			throw new IllegalArgumentException(New.str("Can not allow clob size greater than ", MaxSizeAllowed, " : ", utf8RawBytes.length));
		}
		return _channel.writeSizedEntry(utf8RawBytes, 0, utf8RawBytes.length);
	}



		// Thread Safe?, uses FileChannel Positional Read
	public CharSequence read (long lobPos)
	  throws IOException
	{
			// If clob size is too big, use lazyStringReference
		int size = _channel.readSize(lobPos);
		if (size > InMemoryLobSize) {
			return new LazyStringReference(lobPos, size, this);
		}
		else {
			ByteBuffer bytes = _channel.readSizedEntry(size, lobPos);
			return new LazyStringBytes(bytes.array());
		}
	}


		// String bytes is lazy retrieved, and save as SoftReference for cache purpose
		// XXX optimize this to use "most used-> soft reference", "use once -> weak reference"
	public static class LazyStringReference extends BlobStore.LazyBytes implements CharSequence, ByteSequence
	{

		public LazyStringReference (long pos, int size, LobStore store)
		{
			super(pos, size, store);
		}

		@Override
		public char charAt(int index) {
			throw new RuntimeException("not supported");
		}

		@Override
		public int length() {
			throw new RuntimeException("not supported");
		}

		@Override
		public CharSequence subSequence(int start, int end) {
			throw new RuntimeException("not supported");
		}

			// at time of this writing - bytes value is used more often than string value hence the
			// cached raw value is in bytes. This is because it's used more as bytes to write to IO than
			// as a string value. it's possible we can have a param to decide to store as string for other purpose
		@Override
		public String toString ()
		{
			return org.cosmo.common.util.Util.string(bytes());
		}
	}


	public static class LazyStringBytes extends Bytes implements CharSequence, ByteSequence
	{

		public LazyStringBytes (byte[] bytes)
		{
			super(bytes);
		}

		@Override
		public char charAt(int index) {
			throw new RuntimeException("not supported");
		}

		@Override
		public int length() {
			throw new RuntimeException("not supported");
		}

		@Override
		public CharSequence subSequence(int start, int end) {
			throw new RuntimeException("not supported");
		}

		@Override
		public String toString ()
		{
			return org.cosmo.common.util.Util.string(bytes());
		}
	}
}
