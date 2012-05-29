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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.cosmo.common.util.Constants;

public class Print extends Visitor
{
	public static NodeFormat DefaultNodeFormat = new NodeFormat();

	public static final int BeginNode = 0;
	public static final int EndNode = 1;
	public static final int Attribute = 2;
	public static final int AttributeValue = 3;
	public static final int NodeValue = 4;

	public static final int OpenBracket = 0;
	public static final int CloseBracket = 1;
	public static final int Equals = 2;
	public static final int Slash = 3;
	public static final int Quote = 4;
	public static final int Line = 5;
	public static final int Space = 6;

	public static final char[] Chars = new char[] {'<', '>', '=', '/', '"', '\n', ' '};

	static String[] entityReferences = new String[128];

	static {
		entityReferences['>'] =  "&gt;";
		entityReferences['<'] =  "&lt;";
		entityReferences['&'] =  "&amp;";
		entityReferences['\"'] = "&quot;";
		entityReferences['\''] = "&apos;";
	}


	Appendable _buf;
	NodeFormat _format;
	//NodeParentList _parentList;


	Print ()
	{
		_format = DefaultNodeFormat;
		//if (_NodeFormat.printCircularAsReference) {
		//	_parentList = new NodeParentList();
		//}
	}


	public Print (NodeFormat NodeFormat, Appendable buf)
	{
		this();
		_buf = buf;
		_format = NodeFormat;
	}

	public char chars (int i)
	{
		return Print.Chars[i];
	}

    public void beginNode (Node node, Iterator<Node> attributes, int depth)
	{
		//if (_NodeFormat.printCircularAsReference) {
		//	_parentList.addIfNotAnAncestor(node, _activeParentNode);
		//}
		appendIndent(depth).appendType(OpenBracket).appendType(BeginNode, getId(node)).appendAttribute(node, attributes, depth);

		if (node.value() == null) {
			appendType(Slash).appendType(EndNode).appendType(Line);

		} else {
			appendType(CloseBracket);
		}
	}

	public void beforeNextNode (Node node, int depth)
	{
		appendType(Line);
	}

	public void afterNextNode (Node node, int depth)
	{
		appendIndent(depth).appendType(OpenBracket).appendType(Slash).appendType(EndNode, getId(node)).appendType(CloseBracket).appendType(Line);
	}

	public void handleValue (Node node, int depth)
	{

		if (!_format.oneLinePrint) {
			appendType(Line).appendIndent(++depth);
		}

		Object value = node.value();
		if (!_format.cdataAsString  && value instanceof XML.CDATA) {
			value = ((XML.CDATA)value).toCDATAString();
		}
		else if (_format.replaceEntityReference) {
			value = replaceEntityReference(value);
		}


		appendType(NodeValue, value);
		if (!_format.oneLinePrint) {
			appendType(Line).appendIndent(--depth);
		}
		appendType(OpenBracket).appendType(Slash).appendType(EndNode, getId(node)).appendType(CloseBracket).appendType(Line);
	}

	public String getId (Node node)
	{
		// return node.unmaskPath(node._id.toString());
		return node._id.toString();
	}


	public Print appendString (CharSequence str)
	{
		try {
			_buf.append(str);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	public Print appendChar (char c)
	{
		try {
			_buf.append(c);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		return this;
	}


	// type is used by subclass which needs context about type
	public Print appendType (int type, Object value)
	{
		return appendString(value == null
			? ""
			: value instanceof CharSequence
				? (CharSequence) value
				: value.toString());
	}

	public Print appendType (int type)
	{
		return appendChar(chars(type));
	}

	public Print appendIndent (int indents)
	{
		if (indents < 0) {
			return this;
		}
		for (int i = 0; i  < indents; i++) {
			appendSpace(_format.indent.length());
		}
		return this;
	}

	public Print appendSpace (int space)
	{
		// XXX optimize!
		for (int i = 0; i < space; i++) {
			appendChar(chars(Space));
		}
		return this;
	}


	public void appendAttribute (Node node, Iterator<Node> attributes, int indent)
	{
		if (_format.indentAttributes && !_format.oneLinePrint) {

			for (int count = 0; attributes.hasNext(); count++) {
				Node attr = (Node)attributes.next();
				if (count > 0) {
					appendType(Line).appendSpace((indent + 1) * _format.indent.length() + node._id.toString().length() -1);
				}
				appendType(Space).
				appendType(Attribute, attr.id()).
				appendType(Equals).
				appendType(Quote).
				appendType(AttributeValue, _format.replaceEntityReference ? replaceEntityReference(attr.value()) :attr.value()).
				appendType(Quote);
			}
		}
		else {
			while (attributes.hasNext()) {
				Node attr = (Node)attributes.next();
				appendType(Space).
				appendType(Attribute, attr.id()).
				appendType(Equals).
				appendType(Quote).
				appendType(AttributeValue, _format.replaceEntityReference ? replaceEntityReference(attr.value()) :attr.value()).
				appendType(Quote);
			}
		}
	}



	public String toString ()
	{
		return _buf.toString();
	}


	public String replaceEntityReference (Object obj)
	{
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
				fsb.append(entityReferences[(byte)c]);
			}
			else {
				fsb.append(c);
			}
		}
		return fsb.toString();
	}
}

