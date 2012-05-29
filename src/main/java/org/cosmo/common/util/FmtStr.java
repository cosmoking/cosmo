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

import java.util.ArrayList;
import java.util.List;


import ariba.util.core.Fmt;

import org.cosmo.common.net.StringTokens;
import org.cosmo.common.statistics.Clock;

// Fast for repeat format - 50% faster than Fmt.S
public class FmtStr extends FmtBytes
{
	public static final char DefaultSeparatorChar = '^';
	public static final char[] DefaultSeparatorChars = new char[]{DefaultSeparatorChar};

	/*
	public enum Cases {
		NullValueUseNullString,
		NullValueUseEmptyString,
		NullValueThrowException,
		MissingValueUseNullString,
		MissingValueUseEmptyString,
		MissingValueThrowException
	}
	*/

    public FmtStr (int initializeSize)
    {
    	super(initializeSize);
    }

	public FmtStr (String str)
	{
		super(str.length() * 2);
		StringTokens tokens = StringTokens.on(str, DefaultSeparatorChars);
		for (String token : tokens) {
				// if full token == str means no variables hence don't add variable mark
			write(Util.UTF8(token), token == str ? false : true);
		}

			// if has variables
		if (_variablesMarkers != null) {
			_variablesMarkers.remove(_variablesMarkers.size() - 1);
		}
		makeExact();
	}


	@Override
	protected void handleEmptyByte (byte[] marker, Bytes dest)
	{
		if (Constants.NullByteMarker == marker) {
			dest.write(Constants.NullStringBytes);
			return;
		}
		if (Constants.EmptyByteMarker == marker) {
			return;
		}
		else {
			throw new RuntimeException("Invalid Marker");
		}
	}

	public String toString ()
	{
		return new String(_bytes, 0, _count);
	}

	public static void main (String[] args) throws Exception
	{
		/*
		FmtStr[] strings = new FmtStr[] {
				new FmtStr("^"),
				new FmtStr("^^"),
				new FmtStr("^^^"),
				new FmtStr("a"),
				new FmtStr(""),
				new FmtStr("^aaa^bbb^"),
				new FmtStr("aaa^bbb^ccc^"),
				new FmtStr("aaa^bbb^ccc^ddd")
		};

		String[][] vars = new String[][] {
			new String[] {null},
			new String[] {null, "(1)"},
			new String[] {null, "(1)", "(2)"},
			new String[] {null, "(1)", "(2)", "(3)"},
			new String[] {null, "(1)", "(2)", "(3)", "(4)"},
			new String[] {"(1)", "(2)", "(3)", "(4)"}
		};


		for (FmtStr f : strings) {
			for (String[] v : vars) {
				System.out.println(f.fmtStr(v));
			}
		}
		*/


		System.out.println(Clock.timer().markAndCheckRunning());
		FmtStr s = new FmtStr("111111111111111111^1111111111111111^11111111111111111111^111111111111111111111111");
		String s3 = "111111111111111111^1111111111111111^11111111111111111111^111111111111111111111111";
		String s2 = "111111111111111111%s1111111111111111%s11111111111111111111%s111111111111111111111111";
		String[] arg = new String[]{"abcde", "abcde","abcde"};
		//s.fmtStr("abcde", "abcde","abcde");
		for (int i = 0; i < 1000000; i++) {

			Fmt.S(s2, new String[]{"abcde", "abcde","abcde"});
			//FmtStr fm = new FmtStr(s3);
			//String ss = Util.string(fm.fmtBytes(arg)._bytes);

			//String ss = Util.string(s.fmtBytes(arg)._bytes);
			//s.fmtBytes(arg);

		}
		System.out.println(Clock.timer().markAndCheckRunning());


	}
}

