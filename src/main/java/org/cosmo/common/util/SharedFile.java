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
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;


public class SharedFile
{

	public static final int Entries = 32;
	public static final int ByteSize = Entries * 2;
    public static final char[] digits = {'0' , '1' , '2' , '3' , '4' , '5' , '6' , '7' , '8' , '9' };


	public void write (File file)
	  throws Exception
	{
		if (file.exists()) {
			file.delete();
		}

		RandomAccessFile raf = new RandomAccessFile(file, "rw");
		raf.setLength(ByteSize);
		FileChannel fc = raf.getChannel();
		MappedByteBuffer buf = fc.map(FileChannel.MapMode.READ_WRITE, 0, ByteSize);


		for (int value = 1 ; true ; value++) {
			for (int i = 0; i < Entries; i++) {
				Thread.sleep(250);
				buf.putChar(i * 2, digits[value % 10]);
				//buf.force();
				print(buf);
			}
		}
	}


	public void read (File file)
	  throws Exception
	{

		FileChannel fc = new RandomAccessFile(file, "r").getChannel();
		ByteBuffer buf = fc.map(FileChannel.MapMode.READ_ONLY, 0, ByteSize);

		for (int value = 1 ; true ; value++) {
			for (int i = 0; i < Entries; i++) {
				Thread.sleep(500);
				print(buf);
			}
		}
	}


	public void print (ByteBuffer buf)
	{
		// debug
		for (int i = 0; i < Entries; i++) {
			System.out.print(buf.getChar(i * 2));
		}
		System.out.println("");
	}


	public static void main (String[] args)
	  throws Exception
	{
		SharedFile sf = new SharedFile();
		File file = new File("x:/sharedfile");
		if (args.length > 0) {
			sf.write(file);
		}
		else {
			sf.read(file);
		}





	}
}
