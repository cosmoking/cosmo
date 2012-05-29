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
package org.cosmo.common.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import org.cosmo.common.record.Util;


public class NetworkByteArray
{

	Handle _handle;

	NetworkByteArray (Handle handle)
	{
		_handle = handle;
	}


	public static Handle create (File file, int size)
	  throws IOException
	{
		return new Handle(file, size);
	}

/*
	public static Setter openForWrite (Handle handle)
	  throws IOException
	{
		if (handle._file.exists()) {
			handle._file.delete();
		}
		RandomAccessFile raf = new RandomAccessFile(handle._file, "rw");
		raf.setLength(handle._size);
		FileChannel fc = raf.getChannel();
		handle._buf = fc.map(FileChannel.MapMode.READ_WRITE, 0, handle._size);
		return new Setter(handle);
	}


	public static Getter openForRead (Handle handle)
	  throws IOException
	{
		if (!handle._file.exists()) {
			throw new IllegalArgumentException ("File " +  handle._file + "Does not exists");
		}

		RandomAccessFile raf = new RandomAccessFile(handle._file, "r");
		FileChannel fc = raf.getChannel();
		handle._buf = fc.map(FileChannel.MapMode.READ_ONLY, 0, handle._size);
		return new Getter(handle);
	}
*/

	public static class Handle
	{
		final File _file;
		final int _size;
		MappedByteBuffer _buf;


		public Handle (File file, int size)
		{
			_file = file;
			_size = size;
		}

		public Setter openForWrite ()
		  throws IOException
		{
			if (_file.exists()) {
				_file.delete();
			}
			RandomAccessFile raf = new RandomAccessFile(_file, "rw");
			raf.setLength(_size);
			FileChannel fc = raf.getChannel();
			_buf = fc.map(FileChannel.MapMode.READ_WRITE, 0, _size);
			return new Setter(this);
		}


		public Getter openForRead ()
		  throws IOException
		{
			if (!_file.exists()) {
				throw new IllegalArgumentException ("File " +  _file + "Does not exists");
			}

			RandomAccessFile raf = new RandomAccessFile(_file, "r");
			FileChannel fc = raf.getChannel();
			_buf = fc.map(FileChannel.MapMode.READ_ONLY, 0, _size);
			return new Getter(this);
		}

		public synchronized void dump ()
		{

			byte[] dump = new byte[_size];
			_buf.position(0);
			_buf.get(dump);
			for (int i = 0; i < _size; i++) {
				System.out.print(dump[i]);
				System.out.print(" ");
			}
			System.out.println("");
		}
	}


	public static class Setter extends NetworkByteArray
	{

		Setter (Handle handle)
		{
			super(handle);
		}

		public void set (long src)
		{
			_handle._buf.position(0);
			_handle._buf.putLong(0, src);
		}

		public void set (byte[] src)
		{
			_handle._buf.position(0);
			_handle._buf.put(src, 0, 0);
		}

		public void set (int pos, byte[] src)
		{
			_handle._buf.position(pos);
			_handle._buf.put(src, 0, src.length);
		}

		public void set (int pos, byte[] src, int offset, int length)
		{
			_handle._buf.position(pos);
			_handle._buf.put(src, offset, length);
		}

		public void flush ()
		{
			_handle._buf.force();
		}
	}


	public static class Getter extends NetworkByteArray
	{
		Getter (Handle handle)
		{
			super(handle);
		}


		public long getLong ()
		{
			_handle._buf.position(0);
			return _handle._buf.getLong(0);
		}


		public void get (byte[] dst)
		{
			_handle._buf.position(0);
			_handle._buf.get(dst, 0, dst.length);
		}

		public void get (int pos, byte[] dst)
		{
			_handle._buf.position(pos);
			_handle._buf.get(dst, 0, dst.length);
		}

		public void get (int pos, byte[] dst, int offset, int length)
		{
			_handle._buf.position(pos);
			_handle._buf.get(dst, offset, length);
		}


	}


	public static final int Entries = 32;
	public static final int ByteSize = Entries * 2;
    public static final char[] digits = {'0' , '1' , '2' , '3' , '4' , '5' , '6' , '7' , '8' , '9', 'A', 'B','C','D','E','F' };
	public static void main (String[] args)
	  throws Exception
	{
		Handle handle = NetworkByteArray.create(new File("d:/rsssites/slave1/RssContent.slv"), ByteSize);
		System.out.println(Util.PID);

		if (args.length > 0) {
			Setter setter = handle.openForWrite();

			for (int value = 1 ; true ; value++) {
				for (int i = 0; i < Entries; i++) {
					Thread.sleep(250);
					//setter.set(i * 2, new byte[]{0, (byte)digits[value % 10]});
					//buf.force();
					//handle.dump();
					setter.set(i);
					System.out.println("setting " + i);
				}
			}
		}

		else {
			Getter getter = handle.openForRead();
			for (int value = 1 ; true ; value++) {
				for (int i = 0; i < Entries; i++) {
					Thread.sleep(700);
					//handle.dump();
					long longvalue = getter.getLong();
					System.out.println("getting " + longvalue);
				}
			}
		}

	}
}
