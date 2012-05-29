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

import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;

public class Str implements CharSequence
{
	byte[] _bytes;

		// XXX to do - like String.intern() then we can use ==  for compare !;
	public static ConcurrentHashMap StrCache;

	private Str ()
	{
	}

	public static Str ingU (String str)
	{
		Str s = new Str();
		s._bytes = Util.UTF8(str);
		return s;
	}


	public static Str ing (String str)
	{
		Str s = new Str();
		s._bytes = new byte[str.length()];
		for (int i = 0; i < str.length(); i++) {
			s._bytes[i] = (byte)str.charAt(i);
		}
		return s;
	}

    public int length()
    {
    	return _bytes.length;
    }

    public char charAt(int index)
    {
    	return (char)_bytes[index];
    }

    public CharSequence subSequence(int start, int end)
    {
    		// this should be cached
    	Str s = new Str();
    	System.arraycopy(_bytes, start, s._bytes, 0, end - start);
    	return s;
    }

	public String toString ()
	{
		return new String(_bytes, Constants.UTF8);
	}

	public String text ()
	{
		return new String(_bytes, Constants.UTF8);
	}

	public int hashCode ()
	{
		if (_bytes.length == 0) {
			return -1;
		}
		int hashCode = 0;
		for (int i = 0; i < _bytes.length; i++) {
			hashCode += _bytes[i];
		}
		return hashCode;
	}

	public boolean equals (Object o)
	{
		if (this == o) {
		    return true;
		}

		if (o instanceof Str) {
			Str s = (Str)o;
			if (_bytes.length == s._bytes.length) {
				for (int i = 0; i < _bytes.length; i++) {
					if (_bytes[i] != s._bytes[i]) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	public static void main (String[] args) throws Exception
	{
		Str s =  Str.ing("ABCDSFSDFSDFDSF#@$&(*$&@#($\n\t&(@#!@#AASDSFSDFSD");
		Str s2 =  Str.ing("ABCDSFSDFSDFDSF#@$&(*$&@#($\n\t&(@#!@#AASDSFSDFSD");


		System.out.println(s.hashCode());
		System.out.println(s2.hashCode());
		System.out.println(s.equals(s2));
		System.out.println(s);
	}

}
