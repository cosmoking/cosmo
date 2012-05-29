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
import java.util.Iterator;
import java.util.List;

public class JSONPrint extends Print
{

    public static JSONFormat DefaultJSONFormat = new JSONFormat();

    public static final int BeginNode = 0;
    public static final int EndNode = 1;
    public static final int Attribute = 2;
    public static final int AttributeValue = 3;
    public static final int NodeValue = 4;

    public static final int OpenBracket = 0;
    public static final int CloseBracket = 1;
    public static final int Equals = 2;
    public static final int Colon = 3;
    public static final int Quote = 4;
    public static final int Line = 5;
    public static final int Space = 6;
    public static final int Comma = 7;
    public static final int OpenArrayBracket = 8;
    public static final int CloseArrayBracket = 9;

    public static final char[] Chars = new char[] {'{', '}', '=', ':', '"', '\n', ' ', ',', '[', ']'};

	public JSONPrint (Appendable buf)
	{
		this (DefaultJSONFormat, buf);
	}


    public JSONPrint (JSONFormat jsonFormat, Appendable buf)
    {
    	super();
        _buf = buf;
        _format = jsonFormat;
    }

    @Override
	public char chars (int i)
	{
		return JSONPrint.Chars[i];
	}


    @Override
    public void beginNode (Node node, Iterator<Node> attributes, int depth)
    {
    		// this has to be here bcos our data structure is  reversed ie  a,b,c,d  is stored as as  d -> c -> b -> a
    		// and we are inserting comma in reverse order
    	if (node instanceof Nodes && ((Nodes)node).getSibling() != null) {
        	appendType(Comma).appendType(Line);
        }

        appendIndent(depth).appendType(Quote).appendType(BeginNode, getId(node)).appendType(Quote).appendType(Colon);

      	if (node.value() == null) {
      		appendType(Quote).appendType(EndNode, "").appendType(Quote).appendType(Line);
      	}
    }

    public void beforeNextNode (Node node, int depth)
    {
        appendType(OpenBracket).appendType(Line);
    }

    public void afterNextNode (Node node, int depth)
    {
   		appendType(Line).appendIndent(depth).appendType(CloseBracket);
    }

    public void handleValue (Node node, int depth)
    {
	        Object value = node.value();
	        if (!_format.cdataAsString  && value instanceof XML.CDATA) {
	     	   value = ((XML.CDATA)value).toCDATAString();
	        }
	        else if (_format.replaceEntityReference) {
	            value = replaceEntityReference(value.toString());
	        }

	        if (value instanceof List) {
    			appendType(OpenArrayBracket);
    			List array = (List)value;
    			for (int i = 0, size = array.size(); i < size; i++) {
    				if (i > 0) appendType(Comma);
    					// the node value should be "visited" again if it's a node
    				appendType(Quote).appendType(NodeValue, array.get(i)).appendType(Quote);
    			}
    			appendType(CloseArrayBracket);
	        }
	        else {
	        	appendType(Quote).appendType(NodeValue, value.toString()).appendType(Quote);
	        }
    }



    public static void main (String[] args) throws Exception
    {
    	//XML xml =  XML.open(new File("c:/values.xml"));
    	//System.out.println(xml);

    	JSON json = new JSON();
    	json.add("a","b").add("c","d").add("e", new JSON().array("1","2", "3"));

        JSONPrint visitor = new JSONPrint(new JSONFormat(), new StringBuilder());
        json.visit(visitor, 0);
        System.out.println(visitor.toString());
    }
}
