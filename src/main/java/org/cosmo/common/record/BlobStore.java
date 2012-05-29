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
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;

import org.cosmo.common.util.ByteSequence;
import org.cosmo.common.util.Bytes;
import org.cosmo.common.util.Log;
import org.cosmo.common.util.New;
import org.json.JSONObject;

/*
 *	Stores write once blob in the file system.
 */
public class BlobStore extends LobStore
{
	public static final int MaxSizeAllowed = 1024 * 1024 * 16;
	public static final int InMemoryLobSize = 1024;

	public static final String BlobExceedMessage = "Blob exceed max size ";

	public BlobStore (Defn defn)
	{
		super(defn);
	}

	@Override
	public String getExtension () {
    	return ".blob";
    }

    	// not thread safe - consumer must manage their own
	public long store (byte[] writeBytes, int offset, int length)
	  throws IOException
	{
		if (writeBytes.length > MaxSizeAllowed) {
			throw new IllegalArgumentException(New.str("Can not allow blob size greater than ", MaxSizeAllowed, " : ", writeBytes.length));
		}
		return _channel.writeSizedEntry(writeBytes, offset, length);
	}

		// Thread Safe?, uses FileChannel Positional Read
	public Bytes read (long lobPos)
	  throws IOException
	{
			// If blob size is too big, XXX figure out a lazy load strat letter
		int size = _channel.readSize(lobPos);
		if (size > InMemoryLobSize) {
			return new LazyBytes(lobPos, size, this);
		}
		else {
			ByteBuffer bytes = _channel.readSizedEntry(size, lobPos);
			return new Bytes(bytes.array());
		}
	}

		// XXX caution - call anyother method on the Bytes will have problem
		// fix later
	public static class LazyBytes extends Bytes implements ByteSequence
	{
		private long _pos;
		private LobStore _lobStore;
		private volatile Reference<byte[]> _lobReference;

		public LazyBytes (long pos, int size, LobStore store)
		{
			_bytes = null;
			_count = size;
			_pos = pos;
			_lobStore = store;
		}

		@Override
		public byte[] bytes ()
		{


				// lazy fetch data and store in Soft Reference, even if both thread comes at same time
				// at worst read 2 times
			if (_lobReference == null) {

				try {
					if (_count> MaxSizeAllowed) {
							// XXX Need a stragegty to self recover from corrupted file or Large blob
							// adding quote to message so for JSON encoded string this message also works
						String msg = JSONObject.quote(New.str(BlobExceedMessage , _count, " for file ", _lobStore._channel.filename(), " at pos ", _pos));
						System.err.println(msg);
						return org.cosmo.common.util.Util.bytes(msg);
					}

					byte[] bytes = _lobStore._channel.readSizedEntry(_count, _pos).array();
					_lobReference = new SoftReference(bytes);
					//System.out.println("XXX Lazy get weak referenced string... ");

					return bytes;
				}
				catch (IOException e) {
					throw new RuntimeException (e);
				}
			}

				// IF GC claimed refetch..
			byte[] bytes = _lobReference.get();
			if (bytes == null) {
				_lobReference = null;
				Log.jcache.fine("Byte cache flushed due to GC - refetch bytes.");
				return bytes();
			}
			else {
				Log.jcache.fine("Bytes cache hit!");
				return bytes;
			}
		}
	}
}

