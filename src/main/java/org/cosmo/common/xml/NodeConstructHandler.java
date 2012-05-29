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

import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class NodeConstructHandler extends DefaultHandler
{

    protected Node focusNode;
    protected Node nameSpaces;
    protected StringBuilder chars;
    protected boolean isCDATA;
    protected Stack stack;
    protected DefaultHandler listener = new DefaultHandler();


    public NodeConstructHandler (Node root)
    {
        stack = new Stack();
        focusNode = root;
        nameSpaces = new Node();
    }


    public void startDocument ()
      throws SAXException
    {
        listener.startDocument();
    }



    public void endDocument ()
      throws SAXException
    {
    	listener.endDocument();
    }


    public void startPrefixMapping (String prefix, String uri)
      throws SAXException
    {
        nameSpaces.addChild(prefix, uri);
    }


    public void endPrefixMapping (String prefix)
      throws SAXException
    {
    // no op
    }


    public void startElement (String uri, String localName,
                  String qName, Attributes attributes)
      throws SAXException
    {
        try {
            Node previousNode = focusNode;
            focusNode = focusNode.addChild(localName);
            addNameSpacesToNode(focusNode, nameSpaces);
            nameSpaces = new Node();
            addAttributesToNode(focusNode, attributes);
            chars = new StringBuilder();

            stack.push(previousNode);
            listener.startElement(uri, localName, qName, attributes);
        }
        catch (Throwable e) {
            e.printStackTrace(System.out);
        }
    }


    public void endElement (String uri, String localName, String qName)
      throws SAXException
    {
        try {
            Object str = chars.toString().trim();
            if (!(Util.nullOrEmptyOrBlankString((String)str))) {
            	if (isCDATA) {
            		str = new XML.CDATA(str);
            		isCDATA = false;
            	}
        		focusNode.set(str);
                chars = new StringBuilder();
            }
            listener.endElement(uri, localName, qName);
            focusNode = (Node)stack.pop();

        }
        catch (Throwable e) {
        	e.printStackTrace(System.out);
        }
    }


    public void characters (char ch[], int start, int length)
      throws SAXException
    {
        chars.append(ch, start, length);
    }


    public void ignorableWhitespace (char ch[], int start, int length)
      throws SAXException
    {

    }

    public void processingInstruction (String target, String data)
      throws SAXException
    {
        super.processingInstruction(target, data);

    }

    public void notationDecl (String name, String publicId, String systemId)
      throws SAXException
    {
        super.notationDecl(name, publicId, systemId);
    }

    public void unparsedEntityDecl (String name,
            String publicId,
            String systemId,
            String notationName)
      throws SAXException
    {
        super.unparsedEntityDecl(name, publicId, systemId, notationName);
    }


    public void addNameSpacesToNode (Node node, Node namespaces)
    {
        for (Node namespaceEntry : namespaces.children()) {
            String prefix = "xmlns";
            if (!Util.nullOrEmptyOrBlankString((String)namespaceEntry.id())) {
                prefix = prefix + ":" + namespaceEntry.id();
            }
            node.addAttribute(prefix, namespaceEntry.value());
        }
    }


    public void addAttributesToNode (Node node, Attributes atts)
    {

        for (int i = 0; i < atts.getLength(); i++)
        {
            node.addAttribute(atts.getQName(i), (atts.getValue(i).trim()));
        }
    }

    public void fatalError (SAXParseException e)
      throws SAXException
    {
    	listener.fatalError(e);
    }
}


