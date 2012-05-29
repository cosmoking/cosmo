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
package org.cosmo.common.net;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import org.cosmo.common.util.Constants;
import org.cosmo.common.util.New;
import org.cosmo.common.util.URLDecoder;


public class StringTokens implements Iterator<String>, Iterable<String>
{

	public static final int NValue = Integer.MAX_VALUE;
	public static final char[] CommaSeparatorChar = new char[]{','};
	public static final char[] ColonSeparatorChar = new char[]{':'};
	public static final char[] SlahSeparatorChar = new char[]{'/'};
	public static final char[] SpaceSeparatorChar = new char[]{' '};
	public static final char[] NewlineSeparatorChar = new char[]{'\n'};
	public static final char[] PipeSeparatorChar = new char[]{'|'};
	public static final char[] QueryStringSeparatorChar = new char[] {'/','?','&'};
	public static final char[] NameValueSeparatorChar = new char[] {'=','&'};
	public static final char[] FunctionSeparatorChar = new char[] {'(',')',','};
	public static final char[] JsonSeparatorChar = new char[] {':',','};


	private char[] _tokenSeparatorChar;
	private String _path;
	private int _cursor;


	public static StringTokens on (Object o)
	{
		return on(o, CommaSeparatorChar);
	}

	public static StringTokens on (Object o, char[] tokenSeparatorChar)
	{
		if (o == null) {
			return null;
		}
		return new StringTokens(o.toString(), 0, tokenSeparatorChar);
	}


	/*
	public static StringTokens getInstance (String... args)
	{
		StringBuilder sb = new StringBuilder(args.length * 2);
		for (int i = 0; i < args.length; i++) {
			sb.append(args[i]).append(i == 0 ? "" : ',');
		}
		return getInstance(sb.toString());
	}
	*/



	private StringTokens (String path, int cursor, char[] c)
	{
	    _cursor = cursor;
	    _path = path;
	    _tokenSeparatorChar = c;
	}

	public Iterator<String> iterator ()
	{
		return this;
	}

	public boolean hasNext()
	{
		int currentCursor = _cursor;
		String currentPath = _path;
		boolean hasNext = next() != null;
		_cursor = currentCursor;
		_path = currentPath;
		return hasNext;
	}

		// null indicates end of it
	public String next ()
	{
		return next(_tokenSeparatorChar);
	}

	public String next (int counts) // times next is called()
	{
		String token = null;
		for (int j = 0; j < counts ; j++) {
			token = next();
		}
		return token;
	}

	public StringTokens append (String path)
	{
		_path = _path.length() == 0
			? path
			: New.str(_path, _tokenSeparatorChar[0], path);
		_cursor = 0;
		return this;
	}

	public StringTokens remove (String token)
	{
		StringTokens newTokens = StringTokens.on("");
		while (hasNext()) {
			String aToken = next();
			if (aToken == token || aToken.equals(token)) {
				continue;
			}
			else {
				newTokens.append(aToken);
			}
		}
		_path = newTokens._path;
		_cursor = newTokens._cursor;
		_tokenSeparatorChar = newTokens._tokenSeparatorChar;
		return this;
	}

	public StringTokens makeDistinct (boolean trim, boolean caseSenstive)
	{
		HashSet<String> setLowerCase = caseSenstive ? new HashSet() : null;
		StringBuffer sb = new StringBuffer(_path.length());
		while (hasNext()) {
			String token = trim ? next().trim() : next();
			if (caseSenstive && !setLowerCase.add(token.toLowerCase())) {
				continue;
			}

			if (sb.length() > 0) {
				sb.append(_tokenSeparatorChar[0]);
			}
			sb.append(token);
		}
		_path = sb.toString();
		_cursor = 0;
		return this;
	}


	@Deprecated
	public void remove ()
	{
		throw new UnsupportedOperationException("remove not supported");
	}


	public String next (char[] tokenSeparatorChar)
	{
		return parse(_cursor == 0 ? _path : remainingPath(), tokenSeparatorChar);
	}

	public StringTokens skip ()
	{
		next();
		return this;
	}

	public String remainingPath ()
	{
		return _path == null
			? null
			: _cursor+1 <= _path.length()
				? _path.substring(_cursor+1, _path.length())
				: null;
	}

    public String path ()
    {
    	return _path;
    }

    @Override
    public String toString ()
    {
    	return _path;
    }

    public static StringTokens copy (StringTokens instance)
    {
    	return instance == null ? StringTokens.on(""): instance.copy();
    }
    
    public StringTokens copy ()
    {
    	return new StringTokens(_path,_cursor, _tokenSeparatorChar);
    }

	private String parse (String s, char[] tokenSeparatorChar)
	{
		if (s == null) {
			return null;
		}

		if (s.length() == 0) {
			_path = null;
			return Constants.EmptyString;
		}

			// parse it
        for (int i = 0; i < s.length(); i++) {
        	char c = s.charAt(i);
        	for (int j = 0 ; j < tokenSeparatorChar.length; j++) {
                if (c == tokenSeparatorChar[j]) {
               		_path = s;
                   	_cursor = i;
                   	_tokenSeparatorChar = tokenSeparatorChar;

                   		// when first char is separator
                   	if (i == 0) {
                   		if (_path.length() > 0) {
                   			_path = _path.substring(1, _path.length());
                   			return Constants.EmptyString;
                   		}
               			return null;
                	}
                   	return _path.length() == 0 ? null : _path.substring(0, _cursor);
                }
        	}
        }
        	// no tokenChar found
       	_path = s;
       	_cursor = s.length();
       	_tokenSeparatorChar = tokenSeparatorChar;
		return _path;
	}


	public boolean reachedEnd ()
	{
		return _cursor == _path.length();
	}


		// from js the utf8 is encoded  by  escape(escape(parm) + escape(parm))
		// the AbstracServerHandler unescape the first one (ie URLDecoder)
		// this unscape the 2nd one
	public void utf8Decode ()
	  throws UnsupportedEncodingException
	{
			// replaces %uxxxx unicode to utf8
		StringBuilder sb = new StringBuilder(_path.length());
		char[] chars = _path.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			if (chars[i] == '%' && i + 5 < chars.length && chars[i+1] == 'u') {


						// User Character.codePointAt(char[] a, int index) or toChars
				String unicode = new String(new char[]{chars[i+2], chars[i+3], chars[i+4], chars[i+5]});
				sb.append((char)Integer.parseInt(unicode, 16));
				i = i + 5;
			}
			else {
				sb.append(chars[i]);
			}
		}

		//_path = _path.replace("%u", "\\u");  // replace the js escaped
		_path = sb.toString();
		_path = URLDecoder.decode(_path, Constants.UTF8);  // decode URLEncoded

	}


    public static void main1 (String[] args) throws Exception
    {
    	String[] strs = new String[] {
    		"",		// ""
    		"=",	// "",""
    		"==",	// "","",""
    		"=a=",	// "",a,""
    		"==a",	// "","",a
    		"a==",	// a,"",""
    		"a=b=c",// a,b,c
    		"=a=b=" // "",a,b,""
    	};

    	for (String s: strs) {
	    	StringTokens tokens = StringTokens.on(s, new char[] {'='});
	    	String token = tokens.next();
	    	for (int i = 0; token != null; i++) {
    			if (i > 0) {
    				System.out.print(",");
    			}
    			System.out.print(token.equals("") ? "\"\"" : token);
   			 	token=tokens.next();
	    	}
	    	System.out.println("<pass>");
    	}
    }

    public static void main (String[] args) throws Exception
    {
    	StringTokens st = StringTokens.on("abc,efg,ABC,EFG");
    	st = st.makeDistinct(true, false);
    	while (st.hasNext()) {
    		System.out.println(st.next());
    	}

    	st = StringTokens.on("abc,efg,ABC,EFG");
    	st.append("xyz,XYZ,xxx");
    	st = st.makeDistinct(true, false);
    	while (st.hasNext()) {
    		System.out.println(st.next());
    	}

    }
}

