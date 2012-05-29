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

import org.cosmo.common.statistics.Clock;

import ariba.util.core.Fmt;


	// different than StringBuilder alikes interms of str concat:
	// - StringBuilder is an object that consists of char[] and int, here simply return String object.
	// - avoid array copying when size is to big. constant size
	// - smaller api call , ie append(a).append(b).append(c) ->  (a,b,c);
public class New {

	public static String fmt(String s, Object... src)
	{
		return Fmt.S(s, src);
	}


	public static char[] ch (Object... src)
	{
		int size = 0;
		for (int i = 0; i < src.length; i++) {

				// XXX treat null as "" ?
			if (src[i] == null) {
				src[i] = "";
			}

				// if not String - get toString
			if (!(src[i] instanceof String)) {
				src[i] = src[i].toString();
			}
				// array of strings, calling toString() returns 'this' so it's not costly
			size += src[i].toString().length();
		}
		char[] dest = new char[size];

		String s = null;
		for (int i = 0,destPos = 0; i < src.length; i++) {
				// at this point src[i] is already converted for non-string objs -
			s = src[i].toString();

			if (s.length() == 1) {
				dest[destPos] = s.charAt(0);
			}
			else {
				s.getChars(0, s.length(), dest, destPos);
				//System.arraycopy(src[i].toCharArray(), 0, dest, destPos, src[i].length());
			}
			destPos += s.length();
		}
		return dest;
	}


	public static char[] ch (String... src)
	{
		int size = 0;
		for (int i = 0; i < src.length; i++) {

				// XXX treat null as "" ?
			if (src[i] == null) {
				src[i] = "";
			}


				// array of strings, calling toString() returns 'this' so it's not costly
			size += src[i].length();
		}
		char[] dest = new char[size];

		String s = null;
		for (int i = 0,destPos = 0; i < src.length; i++) {
			s = src[i];
			if (s.length() == 1) {
				dest[destPos] = s.charAt(0);
			}
			else {
				s.getChars(0, s.length(), dest, destPos);
				//System.arraycopy(src[i].toCharArray(), 0, dest, destPos, src[i].length());
			}
			destPos += s.length();
		}
		return dest;
	}	
	
	
	public static String str(Object... src)
	{
		return new String(ch(src));
	}
	
	public static String str(String... src)
	{
		return new String(ch(src));
	}

	public static String prt (Object... src)
	{
		String s = str(src);
		System.out.println(s);
		return s;
	}

	public static void main (String[] args) throws Exception
	{
		String a = "xxxxxxx";
		String b = "asdlkfjdlksfjdslkf";
		String c = "dsfdsfdsfdsfsd";

		Clock.timer().markAndCheckRunning(System.out);
		for (int i = 0; i < 1000000; i++) {
		}
		Clock.timer().markAndCheckRunning(System.out);
		for (int i = 0; i < 1000000; i++) {
			String s = new StringBuffer().append(a).append(b).append(c).toString();
		}
		Clock.timer().markAndCheckRunning(System.out);
		for (int i = 0; i < 1000000; i++) {
			String s = a + b + c;
		}
		Clock.timer().markAndCheckRunning(System.out);
		for (int i = 0; i < 1000000; i++) {
			String s = New.str(a,b,c);
		}
		Clock.timer().markAndCheckRunning(System.out);



	}

}



