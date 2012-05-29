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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cosmo.common.statistics.Clock;
import org.cosmo.common.xml.Node;
import org.cosmo.common.xml.XML;


public class GenericTest
{



	public static void main2 (String[] args)
	{

		byte[] bytes = new byte[] {(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF};
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		System.out.println(buffer.getLong());



		GenericTest t = new GenericTest();
		System.out.println (GenericTest.getBytes((short)0));
		System.out.println (GenericTest.getBytes((short)255));
		System.out.println (GenericTest.getBytes((short)3000));
		System.out.println (GenericTest.getBytes((short)32000));
		System.out.println (GenericTest.getBytes((short)62000));
	}

	public static void main4 (String[] args) throws IOException
	{
		FileOutputStream fall = new FileOutputStream(new File("d:/posts/file_all"));
		Clock clock = Clock.create(Clock.Unit.Hundredth);
		System.out.println(clock.markTime());
		for (int i = 0; i < 10; i++) {
			write(fall);
		}
		System.out.println(clock.markAndCheckRunning());
		fall.close();

		FileOutputStream[] files = new FileOutputStream[10];
		for (int i = 0; i < files.length; i++) {
			files[i] = new FileOutputStream(new File("d:/posts/file_" + i));
		}
		clock = clock = Clock.create(Clock.Unit.Hundredth);
		System.out.println(clock.markTime());
		for (int i = 0; i < files.length; i++) {
			write(files[i]);
		}
		System.out.println(clock.markAndCheckRunning());
		for (int i = 0; i < files.length; i++) {
			files[i].close();
		}
	}


	public static void write (FileOutputStream out)
	  throws IOException
	{
		byte[] bytes = new byte[8];
		Arrays.fill(bytes, (byte)0);
		for (int i = 0; i < 50000 ; i++) {
			out.write(bytes);
			out.flush();
		}
	}


	public static short makeShort(byte b1, byte b0) {
			return (short)((b1 << 8) | (b0 & 0xff));
	}

	public static short getBytes (short s)
	{
		byte b1 = (byte)(s >> 8);
		byte b2 = (byte)(s >> 0);

		short ss0 = makeShort(b1, b2);

		return ss0;
	}


	public static void main10 (String[] args) throws Exception
	{
		System.out.println("abc".hashCode());
		System.out.println("abc".hashCode());

	}

	public static void main11 (String[] args) throws Exception
	{
		FileInputStream in = new FileInputStream(args[0]);
		LineNumberReader reader = new LineNumberReader(new InputStreamReader(in));
		for (String s = reader.readLine(); s != null;)
		{
			try {
				if (s.indexOf("Subscribe to Feed") > 0) {
					Node node = new XML(new StringReader(s + "</a>") );
					System.out.println(node.value("@href"));
				}
			}
			catch (Exception e) {

			}
			s = reader.readLine();

		}
	}

	public static void main (String[] args ) throws Exception
	{
		for (long i = 0; i < 155; i++) {
			byte[] bytes = ByteBuffer.wrap(new byte[8]).putLong(i).array();
			System.out.println(ByteBuffer.wrap(bytes).getLong());
		}



	}

	public static void main140 (String[] args) throws Exception
	{
			System.out.println(org.cosmo.common.util.Util.dateFromToday(0));
			System.out.println(new java.util.Date(System.currentTimeMillis()));



			Calendar calendar = GregorianCalendar.getInstance();
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			java.util.Date date = calendar.getTime();
			System.out.println(date);

			long day = 24 * 60 * 60 * 1000;

			long howmanydays = (System.currentTimeMillis() - date.getTime() + day + 2343) / day;

			System.out.println(howmanydays);

			System.out.println(new java.util.Date(date.getTime() + day));

	}


	public static void main139 (String[] args) throws Exception
	{

		Pattern pattern = Pattern.compile("abc", Pattern.CASE_INSENSITIVE /*| Pattern.DOTALL | Pattern.MULTILINE*/);
		Matcher source = pattern.matcher("Abc abc what everer aBc");
		StringBuffer sb = new StringBuffer();
		while (source.find()) {
			String strMatch = source.toMatchResult().group();
			source.appendReplacement(sb, "-" + strMatch + "-");

		}
		System.out.println(sb);
	}



	public static void main9 (String[] args) throws Exception
	{
		RandomAccessFile f = new RandomAccessFile("d:/chunk.txt", "rw");
		int size = 32 * 1024 * 1024;
		f.setLength(size);
		ByteBuffer buffer = f.getChannel().map(MapMode.READ_WRITE, 0, size);


		byte[] bytes = new byte[1024];
		Arrays.fill(bytes, (byte)64);
		for (int i = 0; i < 1024 * 32 ; i++) {
			//buffer.put(bytes);
			f.write(bytes);
		}
		f.close();
	}

}

class TypeName <T>
{

}
