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

import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;


	// works with default NodeFormat and no comments
public class PrintIndex extends Print
{
	ArrayList _index;
	byte _offset;
	Node _node;

	public static final byte NID = 1;
	public static final byte NVALUE = 2;
	public static final byte NEND = 3;
	public static final byte AID = 4;

	public PrintIndex (Node root, Appendable buf)
	{
        super(DefaultNodeFormat, buf);
		_index = new ArrayList();
		_offset = 0;
		_node = root;
	}


    public Print appendType (int type, Object value) {

    	super.appendType(type, value);
    	String str = value.toString();
    	switch (type) {
    		case BeginNode:
    			_index.add(new Byte(NID));
    			_index.add(new Byte(_offset)); _offset = 0;
    			_index.add(new Short((short)str.length()));
    			break;

    		case EndNode:
    			_index.add(new Byte(NEND));
    			_offset = (byte)(_offset + str.length());
    	        break;

    		case Attribute:
    			_index.add(new Byte(AID));
    			_index.add(new Byte(_offset)); _offset = 0;
    			_index.add(new Short((short)str.length()));
    			break;

    		case AttributeValue:
    			_index.add(new Byte(_offset)); _offset = 0;
    			_index.add(new Short((short)str.length()));
    			break;

    		case NodeValue:
    			_index.add(new Byte(NVALUE));
    			_index.add(new Byte(_offset)); _offset = 0;
    			_index.add(new Short((short)value.toString().length()));
    			break;
    	}
    	return this;
    }

    public Print appendChar (char c)
    {
    	super.appendChar(c);
    	_offset++;
    	return this;
    }


	public static byte readType (RandomAccessFile index)
	  throws IOException
	{
		try {
			return index.readByte();
		}
		catch (EOFException e) {
			return -1;
		}
	}




	public static String readValue (RandomAccessFile index, RandomAccessFile xml, int cursor)
	  throws IOException
	{
		int length = index.readShort(); byte[] bytes = new byte[length + cursor];
		xml.read(bytes);
		String value = new String(bytes, cursor, length);
		return value;
	}


	public void dumpIndex (OutputStream index)
	  throws IOException
	{
		DataOutputStream indexOut = new DataOutputStream(index);
		try {
			for (Object i : _index) {
				if (i instanceof Short) {
					indexOut.writeShort(((Short)i).shortValue());
				}
				else if (i instanceof Byte) {
					indexOut.writeByte(((Byte)i).byteValue());
				}
				else {
					throw new RuntimeException("Invalid Type");
				}
			}
			indexOut.flush();
		}
		finally {
			indexOut.close();
		}
	}

	public static Node loadPartial (List<String> xpaths, RandomAccessFile index, RandomAccessFile content)
	  throws IOException
	{

			Node contentNode = new Node("root");
			String focusNode = "root";
			int cursor;

			Stack<String> stack = new Stack<String>();
			stack.push(focusNode);

			String xpath = null;

			int type = readType(index);
			while (type > 0) {
				if (type == NEND) {

					xpath = xpath.substring(0, xpath.length() - (focusNode.toString().length() + 1));
					focusNode = stack.pop();
					type = readType(index);
					continue;
				}

				// else
				cursor = index.readInt();
				String value = readValue(index, content, cursor);

				if (type == NID) {
					stack.push(focusNode);
					focusNode = value;

					xpath = xpath == null? value : xpath + "." + value;
					System.out.println(xpath);

				}
				else if (type == NVALUE) {
					if (xpaths.contains(xpath)) {
						contentNode.set(xpath, value);
					}
				}
				else if (type == AID) {
					cursor = index.readInt();

					String attributeXpath = xpath + ".@" + value;
					Object attributeValue = readValue(index, content, cursor);

					if (xpaths.contains(attributeXpath)) {
						contentNode.set(attributeXpath, attributeValue);
					}
					System.out.println(attributeXpath);

				}
				type = index.readInt();
			}
			return (Node)contentNode.value();
	}

/*
	public static Node loadPartial (RandomAccessFile index, RandomAccessFile content)
	  throws IOException
	{
			Node focusNode = new Node("root");
			Node root = focusNode;
			int cursor;

			Stack<Node> stack = new Stack<Node>();
			stack.push(focusNode);

			String xpath = null;

			int type = readType(index);
			while (type > 0) {
				if (type == NEND) {

					xpath = xpath.substring(0, xpath.length() - (focusNode.id().toString().length() + 1));
//					System.out.println(focusNode.id());
//					System.out.println(xpath);
					focusNode = stack.pop();
					type = readType(index);
					continue;
				}

				// else
				cursor = index.readInt();
				String value = readValue(index, content, cursor);

				if (type == NID) {
					stack.push(focusNode);
					focusNode = focusNode.add(value);

					xpath = xpath == null? value : xpath + "." + value;
					System.out.println(xpath);

				}
				else if (type == NVALUE) {
					focusNode.set(value);
				}
				else if (type == AID) {
					cursor = index.readInt();
					focusNode.addAttribute(value, readValue(index, content, cursor));
					System.out.println(xpath + "@" + value);

				}
				type = index.readInt();
			}
			return (Node)root.value();
	}
*/
	public static Node load (RandomAccessFile index, RandomAccessFile content)
	  throws IOException
	{
			Node focusNode = new Node("root");
			Node root = focusNode;
			int offset;


			Stack<Node> stack = new Stack<Node>();
			stack.push(focusNode);

			byte type = readType(index);
			while (type > 0) {
				if (type == NEND) {
					focusNode = stack.pop();
					type = readType(index);
					continue;
				}
				offset = index.readByte();
				String value = readValue(index, content, offset);
				if (type == NID) {
					stack.push(focusNode);
					focusNode = focusNode.addChild(value);
				}
				else if (type == NVALUE) {
					focusNode.set(value);
				}
				else if (type == AID) {
					offset = index.readByte();
					String attributeValue = readValue(index, content, offset);
					focusNode.addAttribute(value, attributeValue);
				}
				type = readType(index);
			}
			return (Node)root.value();
	}
}
