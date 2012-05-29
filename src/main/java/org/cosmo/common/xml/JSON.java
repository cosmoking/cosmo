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
package org.cosmo.common.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*

  http://www.xml.com/pub/a/2006/05/31/converting-between-xml-and-json.html
A single structured XML element might come in seven flavors:

	   1. an empty element
	   2. an element with pure text content
	   3. an empty element with attributes
	   4. an element with pure text content and attributes
	   5. an element containing elements with different names
	   6. an element containing elements with identical names
	   7. an element containing elements and contiguous text

	The following table shows the corresponding conversion patterns between XML and JSON.
	Pattern 	XML 	JSON 	Access
	1 	<e/> 	"e": null 	o.e
	2 	<e>text</e> 	"e": "text" 	o.e
	3 	<e name="value" /> 	"e":{"@name": "value"} 	o.e["@name"]
	4 	<e name="value">text</e> 	"e": { "@name": "value", "#text": "text" } 	o.e["@name"] o.e["#text"]
	5 	<e> <a>text</a> <b>text</b> </e> 	"e": { "a": "text", "b": "text" } 	o.e.a o.e.b
	6 	<e> <a>text</a> <a>text</a> </e> 	"e": { "a": ["text", "text"] } 	o.e.a[0] o.e.a[1]
	7 	<e> text <a>text</a> </e> 	"e": { "#text": "text", "a": "text" } 	o.e["#text"] o.e.a
*/

public class JSON extends Node
{

	public static final Object Anonymous = new Object();


	public JSON ()
	{
		super(Anonymous);
	}

	public JSON (Object id)
	{
		super(id);
	}

	public static List array (Object... arrayValues)
	{
		return Arrays.asList(arrayValues);
	}


	public JSON setArray (List array)
	{
		_value = array;
		return this;
	}

	public JSON setArray (Object... arrayValues)
	{
		_value = Arrays.asList(arrayValues);
		return this;
	}


	public boolean isArray ()
	{
		return _value != null && _value instanceof List;
	}

	@Override
    public String toString ()
    {
        return toString(JSONPrint.DefaultJSONFormat);
    }



	public static void main (String[] args) throws Exception
	{
		JSON json = new JSON("car");
    	json.set("a.d","b");
    	json.set("c.e","d");
    	json.set("e.g", JSON.array("1","2","3"));

		//JSON json = new JSON().array("key","value1", "value2", "value3");


        System.out.println(json);

        json.setArray("1", "2", "3");

        System.out.println(json);
	}
}
