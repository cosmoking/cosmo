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

import org.cosmo.common.net.StringTokens;

public class ArgList extends ArrayList<String>
{

	/* style:'300px',content:portletA! */
	public ArgList (String s, char entrySepChar, char entryPairChar, char quoteChar)
	{
		StringBuffer token = new StringBuffer();
		boolean inQuotes = false;
        for (int i = 0; i < s.length(); i++) {
        	char c = s.charAt(i);
        	if (c == quoteChar) {
        		inQuotes = !inQuotes;
        	}

        	if (!inQuotes) {
	        	if (c == entrySepChar) {
	        		add(token.toString());
	        		token = new StringBuffer();
	        		continue;
	        	}
	        	if (c == entryPairChar) {
	        		add(token.toString());
	    			token = new StringBuffer();
	    			continue;
	        	}
        	}
        	token.append(c);
        	if (i + 1 == s.length()) {
        		add(token.toString());
        	}
        }

	}

	public String toString ()
	{
		StringBuffer buf = new StringBuffer();
		for (String str : this) {
			buf.append(str).append("\n");
		}
		return buf.toString();
	}

	public static void main (String[] args) throws Exception
	{
		System.out.println(new ArgList("a:b,c:d", ':',',','`'));
		System.out.println(new ArgList("a:`b`,c:d", ':',',','`'));
		System.out.println(new ArgList("a:`b`,c:'d'", ':',',','`'));
		System.out.println(new ArgList("a:`b:`,c:`d,`", ':',',','`'));
	}

}
