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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public interface Constants
{
	public static final Charset UTF8 = Charset.forName("UTF8");
	public static final Charset ASCII = Charset.forName("ASCII");
	public static final Object[] EmptyObjectArray = new Object[]{};
	public static final long[] EmptyLongArray = new long[]{};
	public static final byte[] EmptyByteMarker = new byte[]{};
	public static final byte[] NullByteMarker = new byte[]{};
	public static final byte[] LeftSquareBytes = "[".getBytes(UTF8);
	public static final byte[] RightSquareBytes = "]".getBytes(UTF8);
	public static final byte[] CommaBytes = ",".getBytes(UTF8);
	public static final byte[] QuoteBytes = "\"".getBytes(UTF8);
	public static final byte[] NullStringBytes = "null".getBytes(UTF8);
	public static final byte[] NegativeOneLong = BitsUtil.fromLong((long)-1);

	public static final Class ObjectArrayClass = (new Object[]{}).getClass();
	public static final String EmptyString = "";
	public static final List EmptyList = Collections.EMPTY_LIST;

	
	
	public static final long Hours = (long)3600000;
	public static final long HalfHours = (long)1800000;

	public static final DateTimeFormatter  TimeFormat = DateTimeFormat.forPattern("EEE MM/dd/yy HH:mm a");
	public static final DateTimeFormatter DayFormat = DateTimeFormat.forPattern("EEE MM/dd");


    public static final String[] ZerosStr = new String[] {
    	"",
    	"0",
    	"00",
    	"000",
    	"0000",
    	"00000",
    	"000000",
    	"0000000",
    	"00000000",
    	"000000000",
    	"0000000000",
    	"00000000000"
    };

}

