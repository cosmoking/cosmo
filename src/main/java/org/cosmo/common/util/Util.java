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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import ariba.util.core.Assert;
import ariba.util.core.Fmt;

import com.sun.syndication.io.impl.Base64;

import de.l3s.boilerpipe.BoilerpipeExtractor;
import de.l3s.boilerpipe.extractors.CommonExtractors;
import de.l3s.boilerpipe.sax.HTMLHighlighter;

public class Util
{

	public static final String[] EntityReferences = new String[128];

	static {
		EntityReferences['>'] =  "&gt;";
		EntityReferences['<'] =  "&lt;";
		EntityReferences['&'] =  "&amp;";
		EntityReferences['\"'] = "&quot;";
		EntityReferences['\''] = "&apos;";
	}


	public static final SimpleDateFormat rfc822DateFormats[] = new SimpleDateFormat[] {
		new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z"),
		new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z"),
		new SimpleDateFormat("EEE, d MMM yy HH:mm:ss z"),
		new SimpleDateFormat("EEE, d MMM yyyy HH:mm z"),
		new SimpleDateFormat("EEE, d MMM yy HH:mm z"),
		new SimpleDateFormat("d MMM yyyy HH:mm:ss z"),
		new SimpleDateFormat("d MMM yy HH:mm:ss z"),
		new SimpleDateFormat("d MMM yyyy HH:mm z"),
		new SimpleDateFormat("d MMM yy HH:mm z"),

		new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss"), // without timezone
		new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss"),
		new SimpleDateFormat("EEE, d MMM yy HH:mm:ss"),
		new SimpleDateFormat("EEE, d MMM yyyy HH:mm"),
		new SimpleDateFormat("EEE, d MMM yy HH:mm"),
		new SimpleDateFormat("d MMM yyyy HH:mm:ss"),
		new SimpleDateFormat("d MMM yy HH:mm:ss"),
		new SimpleDateFormat("d MMM yyyy HH:mm"),
		new SimpleDateFormat("d MMM yy HH:mm"),

		new SimpleDateFormat("EEE, d MMM yy HH:mm:ssz") // newegg.com      Thu, 3 Sep 2009 11:23:10GMT
	};


	public final static TimeZone GMT = TimeZone.getTimeZone("GMT");
	//public static final long TimeOffSet =  System.currentTimeMillis() - Calendar.getInstance(GMT).getTimeInMillis();
	public static long CurrentSystemDate = Long.MAX_VALUE;

	public static final long DayInMillis = 24 * 60 * 60 * 1000;


	public static String replaceEntityReference (Object obj)
	{
		if (obj == null) {
			return "";
		}
		CharSequence str = obj instanceof CharSequence ? (CharSequence)obj : obj.toString();

		StringBuilder fsb = new StringBuilder();
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (c == '>' ||
					c == '<' ||
					c == '&' ||
					c == '\'' ||
					c == '\"')
			{
				fsb.append(EntityReferences[(byte)c]);
			}
			else {
				fsb.append(c);
			}
		}
		return fsb.toString();
	}

	public static long currentSystemDate ()
	{
		if (CurrentSystemDate - System.currentTimeMillis() > 120000) {
			Calendar calendar = Calendar.getInstance(GMT);
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			CurrentSystemDate = calendar.getTimeInMillis();
		}
		return CurrentSystemDate;
	}

	public static <T extends Object>T getProperty (Class<T> clazz, String property)
	{
		return getProperty(clazz, property, null);
	}

	public static <T extends Object>T getProperty (Class<T> clazz, String property, T defaultValue)
	{
		// either cache those methods or uses this as a one time thingy
		try {

			String param = System.getProperty(property);
			if (param == null) {
				if (defaultValue == null) {
					throw new IllegalArgumentException("Property [" + property + "] not found");
				}
				System.out.println("[" + property + "]= Default[" + defaultValue + "]");
				return defaultValue;
			}
			else {
				System.out.println("[" + property + "]=[" + param + "]");
				if (Enum.class.isAssignableFrom(clazz)) {
					Method method = clazz.getMethod("valueOf", new Class[] {String.class});
					Object o = method.invoke(clazz, param);
					return (T)o;
				}
				else {
					Constructor<T> c = clazz.getConstructor(String.class);
					return c.newInstance(param);
				}
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	public static int daysFromToday (Date date)
	{
		return (int)((currentSystemDate() + DayInMillis - date.getTime()) / DayInMillis);
	}

	public static Date dateFromToday (int day)
	{
		Calendar calendar = Calendar.getInstance(GMT);
		calendar.setTimeInMillis(currentSystemDate() - (day * DayInMillis));
		return calendar.getTime();
	}


	public static Date parseRFC822Date (String value)
	{
		for (SimpleDateFormat formatter : rfc822DateFormats) {
			try {
				Date date = formatter.parse(value);
				return date;
			}
			catch (ParseException e) {
				continue;
			}
		}

			// progressively trim the space and retry till end of it
		int i = value.lastIndexOf(" ");
		if (i > 6) {
			value = value.substring(0, i);
			return parseRFC822Date(value);
		}
		return null;
	}

		// return MSB
    public static int highestOneBit(int i) {
        // HD, Figure 3-1
        i |= (i >>  1);
        i |= (i >>  2);
        i |= (i >>  4);
        i |= (i >>  8);
        i |= (i >> 16);
        return i - (i >> 1);
    }

	public static Date firstHourOfDay ()
	{
		DateTime d = new DateTime();
		return d.hourOfDay().setCopy(0).toDate();
	}

	public static java.util.Date parseRFC3339Date(String datestring)
	{
		return com.sun.syndication.io.impl.DateParser.parseW3CDateTime(datestring);
	}


		// XXX find better way to replace multipe patterns rather then iterate through one by one
	  public static String replaceKeyword (String src, List<String> keywords, String prefix, String suffix)
	  {
		  if (keywords == null || keywords.isEmpty()) {
			  return src;
		  }
		  for (String keyword : keywords) {
		      src = replaceKeyword(src, keyword, prefix, suffix);
		  }
		  return src;
	  }

	  public static String replaceKeyword (String src, String keyword, String prefix, String suffix)
	  {
		  Pattern pattern = Pattern.compile(keyword, Pattern.CASE_INSENSITIVE);
		  Matcher source = pattern.matcher(src);
		  StringBuffer sb = new StringBuffer();
		  while (source.find()) {
			  source.appendReplacement(sb, prefix + source.toMatchResult().group() + suffix);
		  }
		  source.appendTail(sb);
		  return sb.toString();
	  }


	  public static String strClean (String s)
	  {
		  return convertCP1252String(s);
	  }



	  public static String removeEntityReference (String s)
	  {
			return s.replaceAll("&amp;", "&")
			.replaceAll("&apos;", "'")
			.replaceAll("&gt;", ">")
			.replaceAll("&lt;", "<")
			.replaceAll("&quot;", "\"");
	  }


	  public static String convertCP1252String (String s)
	  {
		  StringBuffer b = new StringBuffer();
		  for (char c : s.toCharArray()) {
			  b.append(convertCP1252Char(c));
		  }
		  return b.toString();
	  }

	  public static char convertCP1252Char (char c)
	  {
		  int charCode = (int)c;
		  if (charCode >= 130 && charCode <= 159) {
			  return CP1252_to_UTF8[charCode - 130];
		  }
		  return c;
	  }

	  public static char[] CP1252_to_UTF8 =
		 new char[] {
		  '\u201A',
		  '\u0192',
		  '\u201E',
		  '\u2026',
		  '\u2020',
		  '\u2021',
		  '\u02C6',
		  '\u2030',
		  '\u0160',
		  '\u2039',
		  '\u0152',
		  '\u0000',
		  '\u017D',
		  '\u0000',
		  '\u0000',
		  '\u2018',
		  '\u2019',
		  '\u201C',
		  '\u201D',
		  '\u2022',
		  '\u2013',
		  '\u2014',
		  '\u02DC',
		  '\u2122',
		  '\u0161',
		  '\u203A',
		  '\u0153',
		  '\u0000',
		  '\u017E',
		  '\u0178',

	  };

/*
0x82	0x201A	#SINGLE LOW-9 QUOTATION MARK
0x83	0x0192	#LATIN SMALL LETTER F WITH HOOK
0x84	0x201E	#DOUBLE LOW-9 QUOTATION MARK
0x85	0x2026	#HORIZONTAL ELLIPSIS
0x86	0x2020	#DAGGER
0x87	0x2021	#DOUBLE DAGGER
0x88	0x02C6	#MODIFIER LETTER CIRCUMFLEX ACCENT
0x89	0x2030	#PER MILLE SIGN
0x8A	0x0160	#LATIN CAPITAL LETTER S WITH CARON
0x8B	0x2039	#SINGLE LEFT-POINTING ANGLE QUOTATION MARK
0x8C	0x0152	#LATIN CAPITAL LIGATURE OE
0x8D	      	#UNDEFINED
0x8E	0x017D	#LATIN CAPITAL LETTER Z WITH CARON
0x8F	      	#UNDEFINED
0x90	      	#UNDEFINED
0x91	0x2018	#LEFT SINGLE QUOTATION MARK
0x92	0x2019	#RIGHT SINGLE QUOTATION MARK
0x93	0x201C	#LEFT DOUBLE QUOTATION MARK
0x94	0x201D	#RIGHT DOUBLE QUOTATION MARK
0x95	0x2022	#BULLET
0x96	0x2013	#EN DASH
0x97	0x2014	#EM DASH
0x98	0x02DC	#SMALL TILDE
0x99	0x2122	#TRADE MARK SIGN
0x9A	0x0161	#LATIN SMALL LETTER S WITH CARON
0x9B	0x203A	#SINGLE RIGHT-POINTING ANGLE QUOTATION MARK
0x9C	0x0153	#LATIN SMALL LIGATURE OE
0x9D	      	#UNDEFINED
0x9E	0x017E	#LATIN SMALL LETTER Z WITH CARON
0x9F	0x0178	#LATIN CAPITAL LETTER Y WITH DIAERESIS




#
#    Name:     cp1252 to Unicode table
#    Unicode version: 2.0
#    Table version: 2.01
#    Table format:  Format A
#    Date:          04/15/98
#
#    Contact:       Shawn.Steele@microsoft.com
#
#    General notes: none
#
#    Format: Three tab-separated columns
#        Column #1 is the cp1252 code (in hex)
#        Column #2 is the Unicode (in hex as 0xXXXX)
#        Column #3 is the Unicode name (follows a comment sign, '#')
#
#    The entries are in cp1252 order
#
0x00	0x0000	#NULL
0x01	0x0001	#START OF HEADING
0x02	0x0002	#START OF TEXT
0x03	0x0003	#END OF TEXT
0x04	0x0004	#END OF TRANSMISSION
0x05	0x0005	#ENQUIRY
0x06	0x0006	#ACKNOWLEDGE
0x07	0x0007	#BELL
0x08	0x0008	#BACKSPACE
0x09	0x0009	#HORIZONTAL TABULATION
0x0A	0x000A	#LINE FEED
0x0B	0x000B	#VERTICAL TABULATION
0x0C	0x000C	#FORM FEED
0x0D	0x000D	#CARRIAGE RETURN
0x0E	0x000E	#SHIFT OUT
0x0F	0x000F	#SHIFT IN
0x10	0x0010	#DATA LINK ESCAPE
0x11	0x0011	#DEVICE CONTROL ONE
0x12	0x0012	#DEVICE CONTROL TWO
0x13	0x0013	#DEVICE CONTROL THREE
0x14	0x0014	#DEVICE CONTROL FOUR
0x15	0x0015	#NEGATIVE ACKNOWLEDGE
0x16	0x0016	#SYNCHRONOUS IDLE
0x17	0x0017	#END OF TRANSMISSION BLOCK
0x18	0x0018	#CANCEL
0x19	0x0019	#END OF MEDIUM
0x1A	0x001A	#SUBSTITUTE
0x1B	0x001B	#ESCAPE
0x1C	0x001C	#FILE SEPARATOR
0x1D	0x001D	#GROUP SEPARATOR
0x1E	0x001E	#RECORD SEPARATOR
0x1F	0x001F	#UNIT SEPARATOR
0x20	0x0020	#SPACE
0x21	0x0021	#EXCLAMATION MARK
0x22	0x0022	#QUOTATION MARK
0x23	0x0023	#NUMBER SIGN
0x24	0x0024	#DOLLAR SIGN
0x25	0x0025	#PERCENT SIGN
0x26	0x0026	#AMPERSAND
0x27	0x0027	#APOSTROPHE
0x28	0x0028	#LEFT PARENTHESIS
0x29	0x0029	#RIGHT PARENTHESIS
0x2A	0x002A	#ASTERISK
0x2B	0x002B	#PLUS SIGN
0x2C	0x002C	#COMMA
0x2D	0x002D	#HYPHEN-MINUS
0x2E	0x002E	#FULL STOP
0x2F	0x002F	#SOLIDUS
0x30	0x0030	#DIGIT ZERO
0x31	0x0031	#DIGIT ONE
0x32	0x0032	#DIGIT TWO
0x33	0x0033	#DIGIT THREE
0x34	0x0034	#DIGIT FOUR
0x35	0x0035	#DIGIT FIVE
0x36	0x0036	#DIGIT SIX
0x37	0x0037	#DIGIT SEVEN
0x38	0x0038	#DIGIT EIGHT
0x39	0x0039	#DIGIT NINE
0x3A	0x003A	#COLON
0x3B	0x003B	#SEMICOLON
0x3C	0x003C	#LESS-THAN SIGN
0x3D	0x003D	#EQUALS SIGN
0x3E	0x003E	#GREATER-THAN SIGN
0x3F	0x003F	#QUESTION MARK
0x40	0x0040	#COMMERCIAL AT
0x41	0x0041	#LATIN CAPITAL LETTER A
0x42	0x0042	#LATIN CAPITAL LETTER B
0x43	0x0043	#LATIN CAPITAL LETTER C
0x44	0x0044	#LATIN CAPITAL LETTER D
0x45	0x0045	#LATIN CAPITAL LETTER E
0x46	0x0046	#LATIN CAPITAL LETTER F
0x47	0x0047	#LATIN CAPITAL LETTER G
0x48	0x0048	#LATIN CAPITAL LETTER H
0x49	0x0049	#LATIN CAPITAL LETTER I
0x4A	0x004A	#LATIN CAPITAL LETTER J
0x4B	0x004B	#LATIN CAPITAL LETTER K
0x4C	0x004C	#LATIN CAPITAL LETTER L
0x4D	0x004D	#LATIN CAPITAL LETTER M
0x4E	0x004E	#LATIN CAPITAL LETTER N
0x4F	0x004F	#LATIN CAPITAL LETTER O
0x50	0x0050	#LATIN CAPITAL LETTER P
0x51	0x0051	#LATIN CAPITAL LETTER Q
0x52	0x0052	#LATIN CAPITAL LETTER R
0x53	0x0053	#LATIN CAPITAL LETTER S
0x54	0x0054	#LATIN CAPITAL LETTER T
0x55	0x0055	#LATIN CAPITAL LETTER U
0x56	0x0056	#LATIN CAPITAL LETTER V
0x57	0x0057	#LATIN CAPITAL LETTER W
0x58	0x0058	#LATIN CAPITAL LETTER X
0x59	0x0059	#LATIN CAPITAL LETTER Y
0x5A	0x005A	#LATIN CAPITAL LETTER Z
0x5B	0x005B	#LEFT SQUARE BRACKET
0x5C	0x005C	#REVERSE SOLIDUS
0x5D	0x005D	#RIGHT SQUARE BRACKET
0x5E	0x005E	#CIRCUMFLEX ACCENT
0x5F	0x005F	#LOW LINE
0x60	0x0060	#GRAVE ACCENT
0x61	0x0061	#LATIN SMALL LETTER A
0x62	0x0062	#LATIN SMALL LETTER B
0x63	0x0063	#LATIN SMALL LETTER C
0x64	0x0064	#LATIN SMALL LETTER D
0x65	0x0065	#LATIN SMALL LETTER E
0x66	0x0066	#LATIN SMALL LETTER F
0x67	0x0067	#LATIN SMALL LETTER G
0x68	0x0068	#LATIN SMALL LETTER H
0x69	0x0069	#LATIN SMALL LETTER I
0x6A	0x006A	#LATIN SMALL LETTER J
0x6B	0x006B	#LATIN SMALL LETTER K
0x6C	0x006C	#LATIN SMALL LETTER L
0x6D	0x006D	#LATIN SMALL LETTER M
0x6E	0x006E	#LATIN SMALL LETTER N
0x6F	0x006F	#LATIN SMALL LETTER O
0x70	0x0070	#LATIN SMALL LETTER P
0x71	0x0071	#LATIN SMALL LETTER Q
0x72	0x0072	#LATIN SMALL LETTER R
0x73	0x0073	#LATIN SMALL LETTER S
0x74	0x0074	#LATIN SMALL LETTER T
0x75	0x0075	#LATIN SMALL LETTER U
0x76	0x0076	#LATIN SMALL LETTER V
0x77	0x0077	#LATIN SMALL LETTER W
0x78	0x0078	#LATIN SMALL LETTER X
0x79	0x0079	#LATIN SMALL LETTER Y
0x7A	0x007A	#LATIN SMALL LETTER Z
0x7B	0x007B	#LEFT CURLY BRACKET
0x7C	0x007C	#VERTICAL LINE
0x7D	0x007D	#RIGHT CURLY BRACKET
0x7E	0x007E	#TILDE
0x7F	0x007F	#DELETE
0x80	0x20AC	#EURO SIGN
0x81	      	#UNDEFINED
0x82	0x201A	#SINGLE LOW-9 QUOTATION MARK
0x83	0x0192	#LATIN SMALL LETTER F WITH HOOK
0x84	0x201E	#DOUBLE LOW-9 QUOTATION MARK
0x85	0x2026	#HORIZONTAL ELLIPSIS
0x86	0x2020	#DAGGER
0x87	0x2021	#DOUBLE DAGGER
0x88	0x02C6	#MODIFIER LETTER CIRCUMFLEX ACCENT
0x89	0x2030	#PER MILLE SIGN
0x8A	0x0160	#LATIN CAPITAL LETTER S WITH CARON
0x8B	0x2039	#SINGLE LEFT-POINTING ANGLE QUOTATION MARK
0x8C	0x0152	#LATIN CAPITAL LIGATURE OE
0x8D	      	#UNDEFINED
0x8E	0x017D	#LATIN CAPITAL LETTER Z WITH CARON
0x8F	      	#UNDEFINED
0x90	      	#UNDEFINED
0x91	0x2018	#LEFT SINGLE QUOTATION MARK
0x92	0x2019	#RIGHT SINGLE QUOTATION MARK
0x93	0x201C	#LEFT DOUBLE QUOTATION MARK
0x94	0x201D	#RIGHT DOUBLE QUOTATION MARK
0x95	0x2022	#BULLET
0x96	0x2013	#EN DASH
0x97	0x2014	#EM DASH
0x98	0x02DC	#SMALL TILDE
0x99	0x2122	#TRADE MARK SIGN
0x9A	0x0161	#LATIN SMALL LETTER S WITH CARON
0x9B	0x203A	#SINGLE RIGHT-POINTING ANGLE QUOTATION MARK
0x9C	0x0153	#LATIN SMALL LIGATURE OE
0x9D	      	#UNDEFINED
0x9E	0x017E	#LATIN SMALL LETTER Z WITH CARON
0x9F	0x0178	#LATIN CAPITAL LETTER Y WITH DIAERESIS
0xA0	0x00A0	#NO-BREAK SPACE
0xA1	0x00A1	#INVERTED EXCLAMATION MARK
0xA2	0x00A2	#CENT SIGN
0xA3	0x00A3	#POUND SIGN
0xA4	0x00A4	#CURRENCY SIGN
0xA5	0x00A5	#YEN SIGN
0xA6	0x00A6	#BROKEN BAR
0xA7	0x00A7	#SECTION SIGN
0xA8	0x00A8	#DIAERESIS
0xA9	0x00A9	#COPYRIGHT SIGN
0xAA	0x00AA	#FEMININE ORDINAL INDICATOR
0xAB	0x00AB	#LEFT-POINTING DOUBLE ANGLE QUOTATION MARK
0xAC	0x00AC	#NOT SIGN
0xAD	0x00AD	#SOFT HYPHEN
0xAE	0x00AE	#REGISTERED SIGN
0xAF	0x00AF	#MACRON
0xB0	0x00B0	#DEGREE SIGN
0xB1	0x00B1	#PLUS-MINUS SIGN
0xB2	0x00B2	#SUPERSCRIPT TWO
0xB3	0x00B3	#SUPERSCRIPT THREE
0xB4	0x00B4	#ACUTE ACCENT
0xB5	0x00B5	#MICRO SIGN
0xB6	0x00B6	#PILCROW SIGN
0xB7	0x00B7	#MIDDLE DOT
0xB8	0x00B8	#CEDILLA
0xB9	0x00B9	#SUPERSCRIPT ONE
0xBA	0x00BA	#MASCULINE ORDINAL INDICATOR
0xBB	0x00BB	#RIGHT-POINTING DOUBLE ANGLE QUOTATION MARK
0xBC	0x00BC	#VULGAR FRACTION ONE QUARTER
0xBD	0x00BD	#VULGAR FRACTION ONE HALF
0xBE	0x00BE	#VULGAR FRACTION THREE QUARTERS
0xBF	0x00BF	#INVERTED QUESTION MARK
0xC0	0x00C0	#LATIN CAPITAL LETTER A WITH GRAVE
0xC1	0x00C1	#LATIN CAPITAL LETTER A WITH ACUTE
0xC2	0x00C2	#LATIN CAPITAL LETTER A WITH CIRCUMFLEX
0xC3	0x00C3	#LATIN CAPITAL LETTER A WITH TILDE
0xC4	0x00C4	#LATIN CAPITAL LETTER A WITH DIAERESIS
0xC5	0x00C5	#LATIN CAPITAL LETTER A WITH RING ABOVE
0xC6	0x00C6	#LATIN CAPITAL LETTER AE
0xC7	0x00C7	#LATIN CAPITAL LETTER C WITH CEDILLA
0xC8	0x00C8	#LATIN CAPITAL LETTER E WITH GRAVE
0xC9	0x00C9	#LATIN CAPITAL LETTER E WITH ACUTE
0xCA	0x00CA	#LATIN CAPITAL LETTER E WITH CIRCUMFLEX
0xCB	0x00CB	#LATIN CAPITAL LETTER E WITH DIAERESIS
0xCC	0x00CC	#LATIN CAPITAL LETTER I WITH GRAVE
0xCD	0x00CD	#LATIN CAPITAL LETTER I WITH ACUTE
0xCE	0x00CE	#LATIN CAPITAL LETTER I WITH CIRCUMFLEX
0xCF	0x00CF	#LATIN CAPITAL LETTER I WITH DIAERESIS
0xD0	0x00D0	#LATIN CAPITAL LETTER ETH
0xD1	0x00D1	#LATIN CAPITAL LETTER N WITH TILDE
0xD2	0x00D2	#LATIN CAPITAL LETTER O WITH GRAVE
0xD3	0x00D3	#LATIN CAPITAL LETTER O WITH ACUTE
0xD4	0x00D4	#LATIN CAPITAL LETTER O WITH CIRCUMFLEX
0xD5	0x00D5	#LATIN CAPITAL LETTER O WITH TILDE
0xD6	0x00D6	#LATIN CAPITAL LETTER O WITH DIAERESIS
0xD7	0x00D7	#MULTIPLICATION SIGN
0xD8	0x00D8	#LATIN CAPITAL LETTER O WITH STROKE
0xD9	0x00D9	#LATIN CAPITAL LETTER U WITH GRAVE
0xDA	0x00DA	#LATIN CAPITAL LETTER U WITH ACUTE
0xDB	0x00DB	#LATIN CAPITAL LETTER U WITH CIRCUMFLEX
0xDC	0x00DC	#LATIN CAPITAL LETTER U WITH DIAERESIS
0xDD	0x00DD	#LATIN CAPITAL LETTER Y WITH ACUTE
0xDE	0x00DE	#LATIN CAPITAL LETTER THORN
0xDF	0x00DF	#LATIN SMALL LETTER SHARP S
0xE0	0x00E0	#LATIN SMALL LETTER A WITH GRAVE
0xE1	0x00E1	#LATIN SMALL LETTER A WITH ACUTE
0xE2	0x00E2	#LATIN SMALL LETTER A WITH CIRCUMFLEX
0xE3	0x00E3	#LATIN SMALL LETTER A WITH TILDE
0xE4	0x00E4	#LATIN SMALL LETTER A WITH DIAERESIS
0xE5	0x00E5	#LATIN SMALL LETTER A WITH RING ABOVE
0xE6	0x00E6	#LATIN SMALL LETTER AE
0xE7	0x00E7	#LATIN SMALL LETTER C WITH CEDILLA
0xE8	0x00E8	#LATIN SMALL LETTER E WITH GRAVE
0xE9	0x00E9	#LATIN SMALL LETTER E WITH ACUTE
0xEA	0x00EA	#LATIN SMALL LETTER E WITH CIRCUMFLEX
0xEB	0x00EB	#LATIN SMALL LETTER E WITH DIAERESIS
0xEC	0x00EC	#LATIN SMALL LETTER I WITH GRAVE
0xED	0x00ED	#LATIN SMALL LETTER I WITH ACUTE
0xEE	0x00EE	#LATIN SMALL LETTER I WITH CIRCUMFLEX
0xEF	0x00EF	#LATIN SMALL LETTER I WITH DIAERESIS
0xF0	0x00F0	#LATIN SMALL LETTER ETH
0xF1	0x00F1	#LATIN SMALL LETTER N WITH TILDE
0xF2	0x00F2	#LATIN SMALL LETTER O WITH GRAVE
0xF3	0x00F3	#LATIN SMALL LETTER O WITH ACUTE
0xF4	0x00F4	#LATIN SMALL LETTER O WITH CIRCUMFLEX
0xF5	0x00F5	#LATIN SMALL LETTER O WITH TILDE
0xF6	0x00F6	#LATIN SMALL LETTER O WITH DIAERESIS
0xF7	0x00F7	#DIVISION SIGN
0xF8	0x00F8	#LATIN SMALL LETTER O WITH STROKE
0xF9	0x00F9	#LATIN SMALL LETTER U WITH GRAVE
0xFA	0x00FA	#LATIN SMALL LETTER U WITH ACUTE
0xFB	0x00FB	#LATIN SMALL LETTER U WITH CIRCUMFLEX
0xFC	0x00FC	#LATIN SMALL LETTER U WITH DIAERESIS
0xFD	0x00FD	#LATIN SMALL LETTER Y WITH ACUTE
0xFE	0x00FE	#LATIN SMALL LETTER THORN
0xFF	0x00FF	#LATIN SMALL LETTER Y WITH DIAERESIS

*/




	     /**
	     * This method ensures that the output String has only valid XML unicode characters as specified by the
	     * XML 1.0 standard. For reference, please see the
	     * standard. This method will return an empty String if the input is null or empty.
	     *
	     * @author Donoiu Cristian, GPL
	     * @param  The String whose non-valid characters we want to remove.
	     * @return The in String, stripped of non-valid characters.
	     */
	    public static String removeInvalidXMLCharacters(String s) {
	        StringBuilder out = new StringBuilder();                // Used to hold the output.
	    	int codePoint;                                          // Used to reference the current character.
	    	//String ss = "\ud801\udc00";                           // This is actualy one unicode character, represented by two code units!!!.
	    	//System.out.println(ss.codePointCount(0, ss.length()));// See: 1
			int i=0;
	    	while(i<s.length()) {
	    		//System.out.println("i=" + i);
	    		codePoint = s.codePointAt(i);                       // This is the unicode code of the character.
				if ((codePoint == 0x9) ||          				    // Consider testing larger ranges first to improve speed.
						(codePoint == 0xA) ||
						(codePoint == 0xD) ||
						((codePoint >= 0x20) && (codePoint <= 0xD7FF)) ||
						((codePoint >= 0xE000) && (codePoint <= 0xFFFD)) ||
						((codePoint >= 0x10000) && (codePoint <= 0x10FFFF))) {
					out.append(Character.toChars(codePoint));
				}
				i+= Character.charCount(codePoint);                 // Increment with the number of code units(java chars) needed to represent a Unicode char.
	    	}
	    	return out.toString();
	    }

	    /**
	     * Decodes html entities.
	     * @param s the <code>String</code> to decode
	     * @return the newly decoded <code>String</code>
 		 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
         * CDDL HEADER END
	     */
	 // http://www.w3.org/TR/REC-html40/sgml/entities.html
	  //http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references
	  //*org.apache.commons.lang.Class StringEscapeUtils


	    public static String htmlEntityDecode(String s) {
	    	return org.htmlparser.util.Translate.decode(s);

	    }

	    private static final Pattern RemoveTagsPattern =
	        Pattern.compile("<[^<]*?>", Pattern.MULTILINE);

	    public static String fullyConvertToPlainText (String text)
	    {
	        String converted = text.replaceAll("(<br/>)|(<br>)","\n");
	        converted = RemoveTagsPattern.matcher(converted).replaceAll("");
	        converted = converted.replaceAll(">","&gt;");
	        converted = converted.replaceAll("<", "&lt;");
	        return converted;
	    }

	    public static String htmlEntityDecodeSingle(String s) {

		int i = 0, j = 0, pos = 0;
		StringBuffer sb = new StringBuffer();
		while ((i = s.indexOf("&#", pos)) != -1 && (j = s.indexOf(';', i)) != -1) {
		    int n = -1;
		    for (i += 2; i < j; ++i) {
			char c = s.charAt(i);
			if ('0' <= c && c <= '9')
			    n = (n == -1 ? 0 : n * 10) + c - '0';
			else
			    break;
		    }
		    if (i != j)	n = -1;	    // malformed entity - abort
		    if (n != -1) {
			sb.append((char)n);
			i = j + 1;	    // skip ';'
		    }
		    else {
			for (int k = pos; k < i; ++k)
			    sb.append(s.charAt(k));
		    }
		    pos = i;
		}
		if (sb.length() == 0)
		    return s;
		else
		    sb.append(s.substring(pos, s.length()));
		return sb.toString();

	    }


	    /** File Util **/
	    private static File TmpDir =
	        new File(System.getProperties().getProperty("java.io.tmpdir"));

	    public static boolean DumpInTmpDir = false;



	    public static File newFile (String fileName)
	      throws IOException
	    {
	        return newFile(new File (fileName));
	    }


	    	// will always return the same file and backup old ones with timestamp
	    public static File newFile (File file)
	      throws IOException
	    {
	        String fileName = file.getAbsolutePath();
	        if (file.exists()) {
	            Date fileDate = new Date(file.lastModified());
	            SimpleDateFormat format = new SimpleDateFormat("MM.dd_hh.mm.ss");
	            String dateStr = format.format(fileDate);
	            int dot = fileName.lastIndexOf('.');
	            String backupFileName = (dot > 0)
	                ? fileName.substring(0, dot) + "." + dateStr + fileName.substring(dot,
	                    fileName.length())
	                : fileName + "." + dateStr;

	            File backupFile = (DumpInTmpDir)
	                ? new File(TmpDir, new File(backupFileName).getName())
	                : new File(backupFileName);

	                // if the backup file exists then recursive call this with
	                // new modified date, ie another backup file name
	            if (backupFile.exists()) {
	                file.setLastModified(file.lastModified() + 1000);
	                return newFile(fileName);
	            }
	            else {
	            	// rename the current file to backupfile
	            	boolean successRename = file.renameTo(backupFile);
	                for (int i=0; !successRename && i < 10; i++) {
                		try {
                			Thread.sleep(500);
                		}
                		catch (InterruptedException e) {
                		}
	                	successRename = file.renameTo(backupFile);
	                }
	                if (!successRename) {
	                	throw new IOException(Fmt.S("File [%s] already exists. Fail to back up to [%s]. check for process holding this file handle.",
	                		file.getName(), backupFile));
	                }
	            }
	        }
	        if (file.getParentFile() != null && !file.getParentFile().exists()) {
	            file.getParentFile().mkdirs();
	        }
	        return new File(fileName);
	    }

	    public static String fileName (String s)
	    {
	    	try {
	    		return URLEncoder.encode(s, Constants.UTF8).replace('*', '_');
	    	}
	    	catch (Exception e) {
	    		throw new RuntimeException(e);
	    	}
	    }


	    public static byte[] writeToFile (File file, byte[] bytes)
	      throws IOException
	    {
	    	FileOutputStream writer = null;
	    	try {
	    		writer = new FileOutputStream(file);
	    		writer.write(bytes);
	    		return bytes;
	    	}
	    	finally {
	    		if (writer != null) {
	    			try {
	    				writer.close();
	    			}
	    			catch (Exception e) {
	    				e.printStackTrace();
	    			}
	    		}
	    	}
	    }


	    	// backup  file to .bak extension if exist and returns  [file, backup file]
	    	// return  file if there isn't a file to begin with
	    	// if the backup.bak exists already. it will deleted it

	    public static File[] backupFile (File file)
	      throws IOException
	    {
	    	if (file.exists()) {
		    	String backupFileName = backupFileName(file);
	            File backupFile = new File(backupFileName);

            		// somehow the backup file exists, delete it
	            if (backupFile.exists()) {
	            	deleteFile(backupFile);
	            }
	            renameFile(file, backupFile);


		        if (file.getParentFile() != null && !file.getParentFile().exists()) {
		            file.getParentFile().mkdirs();
		        }
		        return new File[] {file, backupFile};
	        }
	    	return new File[]{file};
	    }


	    	// returns a file with .bak extension ie,  a.txt -> a.bak.txt
	    public static String backupFileName (File file)
	    {
	        String fileName = file.getAbsolutePath();

            int dot = fileName.lastIndexOf('.');
            String backupFileName = (dot > 0)
                ? fileName.substring(0, dot) + ".bak" +  fileName.substring(dot,
                    fileName.length())
                : fileName + ".bak";
            return backupFileName;
	    }


	    /*
	    System.out.println(Util.addSuffixToFile(new File("c:/abcd/abc"), "1"));
		System.out.println(Util.addSuffixToFile(new File("c:/efg/abcd.txt"), "1"));
		System.out.println(Util.addSuffixToFile(new File("c:/efg/abcd.txtx"), "1"));
		System.out.println(Util.addSuffixToFile(new File("c:/efg.ef/abcd.txtx"), "1"));
		System.out.println(Util.addSuffixToFile(new File("c:/efg.ef/abcd.tx"), "1"));
		System.out.println(Util.addSuffixToFile(new File("c:/efg.ef/abcd.txt"), "1"));

		c:\abcd\abc1
		c:\efg\abcd1.txt
		c:\efg\abcd1.txtx
		c:\efg.ef\abcd1.txtx
		c:\efg.ef\abcd1.tx
		c:\efg.ef\abcd1.txt


	     *
	     */

	    public static File addSuffixToFile (String fileName, Object suffix)
	    {
            int dot = fileName.lastIndexOf('.');
            String newFileName = (dot > 0 && fileName.length() - 5 <= dot)
                ? New.str(fileName.substring(0, dot),suffix,fileName.substring(dot, fileName.length()))
                : fileName + suffix;
            return new File(newFileName);
	    }

	    public static File addSuffixToFile (File file, Object suffix)
	    {
	        return addSuffixToFile(file.getAbsolutePath(), suffix);
	    }


	    public static TreeSet<File> filesStartsWith (File file)
	    {
	    	File dir = file.getParentFile();
	    	TreeSet<File> files = new TreeSet();
	    	if (dir.exists()) {
		    	for (File aFile : dir.listFiles()) {
		    		if (aFile.isFile() && aFile.getAbsolutePath().startsWith(file.getAbsolutePath())) {
		    			files.add(aFile);
		    		}
		    	}
	    	}
	    	return files;
	    }

	    public static void renameFile (File from, File to)
	      throws IOException
	    {
        	// rename the current file to backupfile
        	boolean successRename = from.renameTo(to);
            for (int i=0; !successRename && i < 10; i++) {
      		try {
      			Thread.sleep(500);
      		}
      		catch (InterruptedException e) {
      		}
            	successRename = from.renameTo(to);
            }
            if (!successRename) {
            	throw new IOException(Fmt.S("File [%s] already exists. Fail to back up to [%s]. check for process holding this file handle.",
            			from.getName(), to));
            }

	    }


	    public static void deleteFile (File file)
	      throws IOException
	    {
	    	if (file.exists()) {
	        	boolean successDelete = file.delete();
	            for (int i=0; !successDelete && i < 10; i++) {
	      		try {
	      			Thread.sleep(500);
	      		}
	      		catch (InterruptedException e) {
	      		}
	      			successDelete = file.delete();
	            }
	            if (!successDelete) {
	            	throw new IOException(Fmt.S("Unable to delete file [%s]. check for process holding this file handle.",
	            		file.getName()));
	            }
	    	}
	    }


	    public static void fixMe (String control, Object... args)
	    {
	    	StringBuilder builder = new StringBuilder();
	    	builder.append("XXX FixMe: ").append(Fmt.S(control, args));
	    	System.out.println(builder.toString());
	    }

	    public static String s (Object... args)
	    {
	    	StringBuilder builder = new StringBuilder();
	    	for (Object o : args) {
	    		builder.append(o);
	    	}
	    	return builder.toString();
	    }

	    public String JSON (Object... args)
	    {
	    	try {
		    	JSONObject json = new JSONObject();
		    	for (int i = 0; i < args.length; i = i + 2) {
		    		json.append(args[i].toString(), args[i+1]);
		    	}
		    	return json.toString();
	    	}
	    	catch (JSONException e) {
	    		throw new IllegalArgumentException(e);
	    	}
	    }

	    public static String generateToken (int n) {
	        char[] pw = new char[n];
	        int c  = 'A';
	        int  r1 = 0;
	        for (int i=0; i < n; i++)
	        {
	          r1 = (int)(Math.random() * 3);
	          switch(r1) {
	            case 0: c = '0' +  (int)(Math.random() * 10); break;
	            case 1: c = 'a' +  (int)(Math.random() * 26); break;
	            case 2: c = 'A' +  (int)(Math.random() * 26); break;
	          }
	          pw[i] = (char)c;
	        }
	        return new String(pw);
	      }




     public static void trimDirToSize (File dir, int size)
     {
    	 final java.io.FileFilter filter = new java.io.FileFilter () {

    		 public boolean accept(File f)
    		 {
    			 return f != null && f.exists() && !f.isDirectory();
    		 }
    	 };

    	 final java.util.Comparator comp = new java.util.Comparator<File> () {

    		 public int compare(File f1, File f2)
    		 {
   				 return f1.lastModified() < f2.lastModified()
   				 	? 1
   				 	: f1.lastModified() == f2.lastModified()
   				 		? 1
   				 		: -1;
    		 }

    		 public boolean equals(File obj) {
    			 return obj.equals(this);
    		 }
    	 };


    	if (dir == null) {
    		throw new IllegalArgumentException("param [dir] can not be null");
    	}

    	if (!dir.exists()) {
    		throw new IllegalArgumentException(dir + " do not exist");
    	}

    	if (!dir.isDirectory()) {
    		throw new IllegalArgumentException(dir + " is not a directory");
    	}

    	File[] files = dir.listFiles(filter);
    	Arrays.sort(files, comp);

    	for (int i = size; i < files.length; i++) {
    		for (int c = 0; c < 10 && files[i].exists() && !files[i].delete(); c++) {
    			try {
    				Thread.sleep(1000);
    			}
    			catch (Exception e) {
    			}
    		}
    		if (files[i].exists()) {
    			Assert.that(false, "Unable to delete file [" + files[i] + "]");
    		}
    	}
     }



   	public static String hostName (URL url)
   	{
		String hostname = url.getHost();
			// engadget, www.engadget.com, abc.abc.engdgate.com, engadget.com => engadget
		int i = hostname.lastIndexOf('.');
		hostname = hostname.substring(0, i > 0 ? i :  hostname.length());
		i = hostname.lastIndexOf('.');
		hostname = hostname.substring(i > 0 ? i + 1 : 0, hostname.length());
		return hostname;

   	}



    public static int parseInt (String s)
    {
		 // about 3 times faster than java.lang but don't deal with "-" and check for digits

		int result = 0;
		int base = 1;
		for (int i = s.length() - 1; i > -1 ; i--) {
			result += (s.charAt(i) - 48) * base;
			base *= 10;
		}
		return result;
    }


    public static long parseLong (String s)
    {
		 // about 3 times faster than java.lang but don't deal with "-" or check for digits


		long result = 0;
		long base = 1;
		for (int i = s.length() - 1; i > -1 ; i--) {
			result += (s.charAt(i) - 48) * base;
			base *= 10;
		}
		return result;
    }

    public String toString (long v)
    {
    	/*
    	if (v == 0) {
    		return "-
    	    }
    	    else {
    	        if (t < 0) {
    	            rv[i++] = '-';
    	            t = std::abs(t);
    	        }
    	        for (T b ; t ; t = b) {
    	            b = t/10;
    	            T c = t%10;
    	            rv[i++] = static_cast<char>('0' + c);
    	        }
    	    }
    	*/
    	return null;

    }



    public static byte[] jsonQuoteBytes (Object o)
    {
    	return Util.UTF8(JSONObject.quote(o.toString()));
    }

    public static String quote (Object o)
    {
    	return New.str("'", o.toString(), "'");
    }


	public static byte[] bytes (Object o)
	{
		if (o == null) {
			return Constants.NullByteMarker;
		}

		return o instanceof byte[]
		    ? (byte[])o
		    : Util.UTF8(o);
	}

	public static byte[] bytes (CharSequence str)
	{
		if (str instanceof ByteSequence) {
			return ((ByteSequence)str).bytes();
		}
		else {
			return UTF8(str);
		}
	}

	public static byte[] ASCII (String str)
	{
		return str.getBytes(Constants.UTF8);
	}


	public static byte[] UTF8 (Object o)
	{
		return o == null
			? Constants.NullByteMarker
			: UTF8(o.toString());
	}

	public static byte[] UTF8 (CharSequence str)
	{

		if (str.length() == 0) {
			return Constants.EmptyByteMarker;
		}

		int strlen = str.length();
		int utflen = 0;
		int c, count = 0;

		/* use charAt instead of copying String to char array */
		for (int i = 0; i < strlen; i++) {
			c = str.charAt(i);
			if ((c >= 0x0001) && (c <= 0x007F)) {
				utflen++;
			} else if (c > 0x07FF) {
				utflen += 3;
			} else {
				utflen += 2;
			}
		}

		byte[] bytearr = new byte[utflen];


		int i=0;
		for (i=0; i<strlen; i++) {
			c = str.charAt(i);
			if (!((c >= 0x0001) && (c <= 0x007F))) break;
			bytearr[count++] = (byte) c;
		}

		for (;i < strlen; i++){
			c = str.charAt(i);
			if ((c >= 0x0001) && (c <= 0x007F)) {
				bytearr[count++] = (byte) c;

			} else if (c > 0x07FF) {
				bytearr[count++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
				bytearr[count++] = (byte) (0x80 | ((c >>  6) & 0x3F));
				bytearr[count++] = (byte) (0x80 | ((c >>  0) & 0x3F));
			} else {
				bytearr[count++] = (byte) (0xC0 | ((c >>  6) & 0x1F));
				bytearr[count++] = (byte) (0x80 | ((c >>  0) & 0x3F));
			}
		}
		return bytearr;
	}

	public static String string (Bytes bytes)
	{
		return string(bytes.bytes(), bytes._count);
	}
	
	public static String string (byte[] bytearr)
	{
		return string(bytearr, bytearr.length);
	}
	
	
	public static String string (byte[] bytearr, int utflen)
	{
	       char[] chararr = new char[utflen];


	       int c, char2, char3;
	       int count = 0;
	       int chararr_count=0;


	        while (count < utflen) {
	            c = (int) bytearr[count] & 0xff;
	            if (c > 127) break;
	            count++;
	            chararr[chararr_count++]=(char)c;
	        }

	        while (count < utflen) {
	            c = (int) bytearr[count] & 0xff;
	            switch (c >> 4) {
	                case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
	                    /* 0xxxxxxx*/
	                    count++;
	                    chararr[chararr_count++]=(char)c;
	                    break;
	                case 12: case 13:
	                    /* 110x xxxx   10xx xxxx*/
	                    count += 2;
	                    if (count > utflen)
	                        throw new RuntimeException(
	                            "malformed input: partial character at end");
	                    char2 = (int) bytearr[count-1];
	                    if ((char2 & 0xC0) != 0x80)
	                        throw new RuntimeException(
	                            "malformed input around byte " + count);
	                    chararr[chararr_count++]=(char)(((c & 0x1F) << 6) |
	                                                    (char2 & 0x3F));
	                    break;
	                case 14:
	                    /* 1110 xxxx  10xx xxxx  10xx xxxx */
	                    count += 3;
	                    if (count > utflen)
	                        throw new RuntimeException(
	                            "malformed input: partial character at end");
	                    char2 = (int) bytearr[count-2];
	                    char3 = (int) bytearr[count-1];
	                    if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
	                        throw new RuntimeException(
	                            "malformed input around byte " + (count-1));
	                    chararr[chararr_count++]=(char)(((c     & 0x0F) << 12) |
	                                                    ((char2 & 0x3F) << 6)  |
	                                                    ((char3 & 0x3F) << 0));
	                    break;
	                default:
	                    /* 10xx xxxx,  1111 xxxx */
	                    throw new RuntimeException(
	                        "malformed input around byte " + count);
	            }
	        }
	        // The number of chars produced may be less than utflen
	        return new String(chararr, 0, chararr_count);
	}

	public static String getIpAddress(byte[] rawBytes) {
	    int i = 4;
	    String ipAddress = "";
	    for (byte raw : rawBytes) {
	        ipAddress += (raw & 0xFF);
	        if (--i > 0) {
	            ipAddress += ".";
	        }
	    }
	    return ipAddress;
	}

	public static short[] longToShorts (long value)
	{
		short[] shorts = new short[4];
        shorts[0] = (short)(value >>> 48);
        shorts[1] = (short)(value >>> 32);
        shorts[2] = (short)(value >>> 16);
        shorts[3] = (short)(value >>> 0);
        return shorts;
	}

    public static long shortsToLong(short[] shorts)
    {
        return (((long)shorts[0] << 48) +
                ((long)(shorts[1] & 65535) << 32) +
                      ((shorts[2] & 65535) << 16) +
                      ((shorts[3] & 65535) <<  0));
    }

	public static short[] longToShorts2 (long value)
	{
		short[] shorts = new short[4];
		ByteBuffer b = ByteBuffer.allocate(8);
		b.putLong(value);

		b.rewind();
		shorts[0] = b.getShort();
		shorts[1] = b.getShort();
		shorts[2] = b.getShort();
		shorts[3] = b.getShort();
		return shorts;

	}

    public static long shortsToLong2(short[] shorts)
    {
    	ByteBuffer b = ByteBuffer.allocate(8);
    	b.putShort(shorts[0]);
    	b.putShort(shorts[1]);
    	b.putShort(shorts[2]);
    	b.putShort(shorts[3]);
    	b.rewind();
    	return b.getLong();
    }


    public static boolean isBlank (CharSequence s)
    {
    	return s == null || s.length() == 0;
    }
    
    public static String zeroPaddedInt (int num)
    {
    	return zeroPaddedInt(num, 10);
    }

    public static String zeroPaddedInt (int num, int digits)
    {
    	String s = Integer.toString(num, 10);
    	int numberOfZeros = digits - s.length();
    	return Constants.ZerosStr[numberOfZeros] + s;
    }

    public static int zeroPaddedIntStr (String numStr, int offset, int digits)
    {
    	numStr = numStr.substring(offset, offset + digits);
		return Integer.parseInt(numStr);
    }


		public static final String[] NormalizedDates = new String []{
			"NA",
			"01", "01",
			"03", "03",
			"05", "05",
			"07", "07",
			"09", "09",
			"11", "11",
			"13", "13",
			"15", "11",
			"17", "17",
			"19", "19",
			"21", "21",
			"23", "23",
			"25", "25",
			"27", "27",
			"29", "29",
			"31"
	};

	/*20111101=>20111101
	20111102=>20111101
	20111103=>20111103
	20111104=>20111103
	20111105=>20111105
	20111106=>20111105
	20111107=>20111107
	20111108=>20111107
	20111109=>20111109
	20111110=>20111109
	20111111=>20111111
	20111112=>20111111
	20111113=>20111113
	20111130=>20111129
	20111131=>20111131
	*/
	public static String normalizeEveryTwoDayToOneDay (String dateInYYYYMMDD)
	{
		char d1 = dateInYYYYMMDD.charAt(6);
		char d2 = dateInYYYYMMDD.charAt(7);
		int finalDate = ((d1 - 48) * 10) + (d2 - 48);
		return dateInYYYYMMDD.substring(0, 6) + NormalizedDates[finalDate];
	}

    /*
    public static int currentTimeIn (long unitInMillis)
    {
    	return (int)(millisTrimTo(System.currentTimeMillis(), unitInMillis) / unitInMillis);
    }

    public static int millisToHours (long millis)
    {
    	 return (int)(millisTrimTo(millis, Constants.Hours) / Constants.Hours);
    }
    */

    public static long millisTrimTo (long millis, long unitInMillis)
    {
	   	 long remains =  millis % unitInMillis;
		 return millis - remains;
    }

    public static Object growArray (Object array)
    {
    	int length = Array.getLength(array);
		Class type = array.getClass().getComponentType();
		Object newElements = Array.newInstance(type, (int)(length * 1.2) + 1);
		System.arraycopy(array, 0, newElements, 0, length);
		return newElements;
    }

    public static void setObjectFieldValue (Object object, Field field, Object value)
    {
		try {
			field.set(object, value);
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
    }

    public static void sleep (int interval)
    {
    	try {
    		Thread.currentThread().sleep(interval);
    	}
    	catch (InterruptedException i) {
    		i.printStackTrace();
    	}
    }

		// ie , 1,12,14,19 returns 6
	public static long averageDelta (Object array)
	{
		long[] nums = new long[Array.getLength(array)];

		if (nums.length < 2) {
			return 0;
		}

		for (int i = 0; i < nums.length; i++) {
			nums[i] = ((Number)Array.get(array, i)).longValue();
		}

		Arrays.sort(nums);
		long totalDelta = 0;
		for (int i = nums.length - 1; i > 0 ; i--) {
			totalDelta += nums[i] - nums[i - 1];
		}
		return (long)((long)totalDelta / (long)(nums.length - 1));
	}



	public static CharSequence cssImgBase64DataURI (Bytes cssBytes, File imgDir, String beginToken, String endToken)
	  throws IOException
	{
		final String dataURIToken = "'data:image/png;base64,";

		StringBuffer buf = new StringBuffer();
		String css = cssBytes.toString();

		int end = 0;
		int begin = css.indexOf(beginToken);
		while (begin > 0) {
				// replicate css to buf
			buf.append(css.substring(end, begin));

				// parse out "img location"
			end = css.indexOf(endToken, begin + 1);
			String relativeImg = css.substring(begin + beginToken.length(), end);

				// load and base64 encode
			Bytes imgBytes = Bytes.load(new File(imgDir, relativeImg));
			String imgBase64 = Util.string(Base64.encode(imgBytes._bytes));

				// append base64 output to buf
			buf.append(dataURIToken);
			buf.append(imgBase64);

			begin = css.indexOf(beginToken, end);
		}

			// replicate rest of css to buf
		buf.append(css.substring(end, css.length()));
		return buf;
	}


	public static void main7 (String[] args) throws Exception
	{
		File file = new File("D:/aribaweb-5.ORC3/aribaweb-5.0RC3/project/jackapp/resource", "/css/img.css");
		//cssImgBase64DataURI(file, new File("D:/aribaweb-5.ORC3/aribaweb-5.0RC3/project/jackapp/resource/img"));
	}

	public static void main9 (String[] args) throws Exception
	{
		java.security.Security.setProperty("networkaddress.cache.ttl" , "-1");
		java.security.Security.setProperty("networkaddress.cache.negative.ttl" , "30");

		//System.out.println (System.setProperty("networkaddress.cache.ttl", "140"));
        System.out.println(sun.net.InetAddressCachePolicy.get());


		long hour = 36;
		long hour36 = hour * 60 * 60 * 1000;

		long currentTime = System.currentTimeMillis();
		for (int i = 0 ; i < 200; i++) {
			long time = currentTime +  (i * (60 * 60 * 1000));
			System.out.println(i + " - " + time / hour36);
		}



	}

	public static void main (String[] args) throws Exception
	{



		URL url = new URL(
				"http://www.nownews.com/2011/12/15/504-2766908.htm"
		);


		// choose from a set of useful BoilerpipeExtractors...
		final BoilerpipeExtractor extractor = CommonExtractors.ARTICLE_EXTRACTOR;

		final HTMLHighlighter hh = HTMLHighlighter.newExtractingInstance();

		PrintWriter out = new PrintWriter("d:/highlighted.html", "UTF-8");
		out.println(hh.process(url, extractor));
		out.close();


	}

		// XXX conver this to be done during build phase, also convert to use build.run() to execute this
	public static Bytes resourceMinify (File file)
	  throws IOException
	{
			// creates a temp file by appending .min   // NOTE -Xss32M IS the maxsize allowed for dataURI, increase accordingly
		File minifiedFile = new File (file.getAbsolutePath() + ".min");
		String cp = System.getProperty("java.class.path");
		ProcessBuilder pb = new ProcessBuilder(
			"java", "-Xss32M", "-Xmx256M", "-classpath",
			cp, "com.yahoo.platform.yui.compressor.YUICompressor",
			"-o", minifiedFile.getAbsolutePath(),
			"--charset", "UTF8",
			file.getAbsolutePath());
		pb.directory(new File("."));
		Process p = pb.start();
        writeProcessOutput(p);
		System.out.println("minifiying: " + file);
		return Bytes.load(minifiedFile);

	}


			// throws an error if there is any output from error stream
	private static void writeProcessOutput(Process process) throws IOException{
	      InputStreamReader tempReader = new InputStreamReader(
	          new BufferedInputStream(process.getErrorStream()));
	      BufferedReader reader = new BufferedReader(tempReader);
	      StringBuffer error = new StringBuffer();
	      while (true){
	          String line = reader.readLine();
	          if (line == null)
	              break;
	          error.append(line);
	      }
	      if (error.length() > 0) {
	      	throw new IOException(error.toString());
	      }
	}


	public static void main5 (String[] args) throws Exception
	{
		System.out.println(Util.addSuffixToFile(new File("c:/abcd/abc"), "1"));
		System.out.println(Util.addSuffixToFile(new File("c:/efg/abcd.txt"), "1"));
		System.out.println(Util.addSuffixToFile(new File("c:/efg/abcd.txtx"), "1"));
		System.out.println(Util.addSuffixToFile(new File("c:/efg.ef/abcd.txtx"), "1"));
		System.out.println(Util.addSuffixToFile(new File("c:/efg.ef/abcd.tx"), "1"));
		System.out.println(Util.addSuffixToFile(new File("c:/efg.ef/abcd.txt"), "1"));

	}


    public static void main4 (String[] args) throws Exception
    {
    	long[] longs = new long[]{154, 12, 14, 19};

    	System.out.println(averageDelta(longs));
    }


    public static void main3 (String[] args) throws Exception
    {
    	short[] shorts = new short[]{2000, 10000, 300, 2000};
    	long l = shortsToLong2(shorts);
    	short[] back = longToShorts2(l);
    	System.out.println(l);
    	for (int i = 0; i < back.length; i++) {
    		System.out.println(back[i]);
    	}

    }

     public static void main2 (String[] args) throws Exception
     {
    	 byte[] bytes = Util.UTF8(";sdf;'sdf;'df;'ABCDEFG");
    	 System.out.println(new String(bytes, Constants.UTF8));

    	 /*
    	 URL url = new URL("http://localhost");
    	 System.out.println(hostName(url));
    	 url = new URL("http://www.engadget.com");
    	 System.out.println(hostName(url));
    	 url = new URL("http://abc.abc.engadget.com");
    	 System.out.println(hostName(url));
    	 url = new URL("http://engadget.com");
    	 System.out.println(hostName(url));
    	 */



    	 //Util.trimDirToSize(new File("c:/test"), 3);

     }
}





class MacAddress {

    public static void main(String[] args) {
        try {
            //InetAddress address = InetAddress.getLocalHost();
            InetAddress address = InetAddress.getByName("192.168.46.53");

            /*
             * Get NetworkInterface for the current host and then read the
             * hardware address.
             */
            NetworkInterface ni = NetworkInterface.getByInetAddress(address);
            if (ni != null) {
                byte[] mac = ni.getHardwareAddress();
                if (mac != null) {
                    /*
                     * Extract each array of mac address and convert it to hexa with the
                     * following format 08-00-27-DC-4A-9E.
                     */
                    for (int i = 0; i < mac.length; i++) {
                        System.out.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : "");
                    }
                } else {
                    System.out.println("Address doesn't exist or is not accessible.");
                }
            } else {
                System.out.println("Network Interface for the specified address is not found.");
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
}



