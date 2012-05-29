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


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.cosmo.common.xml.Node;
import org.cosmo.common.xml.Visitor;

import javax.naming.OperationNotSupportedException;

import ariba.util.core.Fmt;


public class ICal
{

	public static final ContentDefn PRODID = new StringContentDefn();
	public static final ContentDefn VERSION = new StringContentDefn();
	public static final ContentDefn METHOD = new StringContentDefn();
	public static final ContentDefn ATTENDEE = new StringContentDefn();
	public static final ContentDefn ORGANIZER = new StringContentDefn();
	public static final ContentDefn TRANSP = new StringContentDefn();
	public static final ContentDefn SEQUENCE = new StringContentDefn();
	public static final ContentDefn LOCATION = new StringContentDefn();
	public static final ContentDefn UID = new StringContentDefn();
	public static final ContentDefn DTSTAMP = new TimeContentDefn();
	public static final ContentDefn DTSTART = new TimeContentDefn();
	public static final ContentDefn DTEND = new TimeContentDefn();
	public static final ContentDefn DESCRIPTION = new StringContentDefn();
	public static final ContentDefn SUMMARY = new StringContentDefn();
	public static final ContentDefn PRIORITY = new StringContentDefn();
	public static final ContentDefn CLASS = new StringContentDefn();
	public static final ContentDefn TRIGGER = new StringContentDefn();
	public static final ContentDefn ACTION = new StringContentDefn();
	public static final ContentDefn RECURRENCE_ID = new StringContentDefn();
	public static final ComponentDefn BEGIN = new BeginComponentDefn();
	public static final ComponentDefn END = new EndComponentDefn();

	public static final HashMap<String, ContentDefn> ContentDefnMap;
	static {
		ContentDefnMap = new HashMap();
		try {
			java.lang.reflect.Field fields[] = ICal.class.getFields();
			for (java.lang.reflect.Field field : fields) {
				if (ContentDefn.class.isAssignableFrom(field.getType())) {
					ContentDefn theField = (ContentDefn)field.get(null);
					theField._name = field.getName();
					ContentDefnMap.put(field.getName().toUpperCase(), theField);
				}
			}
		}
		catch (IllegalAccessException e) {
			throw new Error(e);
		}
	}


	public static ICal instance (InputStream input)
	{
		LineNumberReader reader = new LineNumberReader(new InputStreamReader(input));
		Iterable<String> lines = new LineIterator(reader);
		return instance((Iterator<String>)lines);
	}


	private static class LineIterator implements Iterable<String>, Iterator<String>
	{
		private LineNumberReader _lineReader;
		private String _line = LineConsumed;
		private static final String LineConsumed = new String("");


		public LineIterator (LineNumberReader lineReader)
		{
			_lineReader = lineReader;
		}

		public Iterator<String> iterator()
		{
			return this;
		}


	    public boolean hasNext()
	    {
	    	if (_line == LineConsumed) {
	    		try {
	    			_line = _lineReader.readLine();
	    		}
	    		catch (IOException e) {
	    			throw new RuntimeException(e);
	    		}
	    	}
	    	return _line != null;
	    }

	    public String next()
	    {
	    	if (hasNext()) {
		    	String next = _line;
		    	_line = LineConsumed;
		    	return next;
	    	}
	    	return null;
	    }

	    public void remove()
	    {
	    	throw new RuntimeException("Not supported");
	    }
	}


	public static ICal instance (Iterator<String> lines)
	{
		ICal iCal = new ICal();

		ContentDefn field = null;
		for (String line = lines.next(); line != null; line = lines.next()) {
			if (line.startsWith(" ")) {
				field.appendStringValue(line.substring(1, line.length()), iCal); continue;
			}
			int colonPos = line.indexOf(":");
			if (colonPos < 0) {
				System.err.println("Skipping unmatched line: " + line); continue;
			}
			String fieldName = line.substring(0, colonPos);
			String value = line.substring(colonPos + 1, line.length());
			field = ContentDefn.resolve(fieldName);
			if (field == null) {
				System.err.println("Skipping unmatched token: " + line); continue;
			}
			if (value instanceof String) {
				field.setStringValue((String)value, iCal);
			}
			else {
				field.setValue(value, iCal);
			}

		}
		return iCal;
	}


	public static ICal instance (Object... fieldValuePair)
	{
		ICal iCal = new ICal();
		for (int i = 0; i < fieldValuePair.length; i++) {
			if (fieldValuePair[i] instanceof ContentDefn) {
				ContentDefn field = (ContentDefn)fieldValuePair[i];
				field.setValue(fieldValuePair[i + 1], iCal);
			}
		}
		return iCal;
	}


	Node _icalTree = new Node("iCal");
	Node _icalLeaf = _icalTree;
	Stack<Node> _stack = new Stack();


	public String toString ()
	{
		Printer print = new Printer(this);
		return print._buf.toString();
	}

	public void set (ICal.ContentDefn content, Object value)
	{
		new Setter(this, content, value);
	}


	public static class ContentValue<T> {
		ContentDefn _content;
		T _value;

		public ContentValue (ContentDefn<T> content, T value)
		{
			_content = content;
			_value = value;
		}

		public String toString ()
		{
			return _content.toString(this);
		}
	}

	public static class ContentDefn<T> {

		Class<T> _clazz;
		String _name;


		protected ContentDefn (Class<T> clazz)
		{
			_clazz = clazz;
		}


		public void setValue (T value, ICal ical)
		{
			ContentValue<T> contentValue = new ContentValue(this, value);
			ical._icalLeaf.add(_name, contentValue);
		}


		public void setStringValue (String value, ICal ical)
		{
			try {
				Constructor<T> c = _clazz.getConstructor(String.class);
				T typedValue = c.newInstance(value);
				setValue(typedValue, ical);
			}
			catch (Exception e) {
				throw new IllegalArgumentException(e);
			}
		}

		public void appendStringValue (String value, ICal ical)
		{
			Node node = (Node)ical._icalLeaf.value();
			ICal.ContentValue contentValue = (ICal.ContentValue)node.value();
			contentValue._value = contentValue._value + value;
		}


		public static ContentDefn resolve (String fieldName)
		{
			ContentDefn f = ICal.ContentDefnMap.get(fieldName.toUpperCase());
			return f == null ? ICal.ContentDefnMap.get(fieldName.toUpperCase().replace('-', '_')) : f;
		}

		public String toString (ContentValue<T> contentValue)
		{
			return contentValue._value.toString();
		}

	}


	public static class StringContentDefn extends ContentDefn<String>
	{
		StringContentDefn ()
		{
			super(String.class);
		}
	}

	public static class TimeContentDefn extends ContentDefn<Long>
	{
		public static final String TimeFormat = "yyyyMMdd'T'HHmmss'Z'";

		TimeContentDefn ()
		{
			super(Long.class);
		}


		@Override
		public void setStringValue (String value, ICal ical)
		{
			try {
				SimpleDateFormat f = new SimpleDateFormat(TimeFormat);
				Date date = f.parse(value);
				this.setValue(date.getTime(), ical);
			}
			catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}


		@Override
		public String toString (ContentValue<Long> contentValue)
		{
			SimpleDateFormat f = new SimpleDateFormat(TimeFormat);
			String date = f.format(new Date(contentValue._value));
			return date;
		}
	}


	public static class ComponentDefn extends ContentDefn<ComponentDefn.Names>
	{
		public static final Names VCALENDAR = new Names("VCALENDAR");
		public static final Names VEVENT = new Names("VEVENT");
		public static final Names VJOURNAL = new Names("VJOURNAL");
		public static final Names VTIMEZONE = new Names("VTIMEZONE");
		public static final Names VTODO = new Names("VTODO");


		public ComponentDefn ()
		{
			super(ComponentDefn.Names.class);
		}


		public static class Names {

			String _name;

			public Names (String name)
			{
				_name = name;
			}

			public String toString ()
			{
				return _name;
			}
		}
	}


	public static class BeginComponentDefn extends ComponentDefn
	{
		@Override
		public void setValue (ComponentDefn.Names component, ICal ical) {
			ical._icalLeaf = ical._icalLeaf.add(component.toString());
			ical._stack.push(ical._icalLeaf);
		}
	}


	public static class EndComponentDefn extends ComponentDefn
	{
		@Override
		public void setValue (ComponentDefn.Names component, ICal ical) {
			ical._icalLeaf = ical._stack.pop();
		}
	}



	public static void main (String[] args) throws Exception
	{

		ICal ical = ICal.instance(
			ICal.BEGIN, ICal.BEGIN.VCALENDAR,
			ICal.VERSION, "2.0",
			ICal.PRODID, "-//Ariba Inc//Discovery//EN",
			ICal.BEGIN, ICal.BEGIN.VEVENT,
			ICal.UID, "SomeID",
			ICal.ORGANIZER, "Jack Wang",
			ICal.DTSTART, new Date().getTime(),
			ICal.DTEND, new Date().getTime(),
			ICal.SUMMARY, "abc",
			ICal.DESCRIPTION, null
		);



		System.out.println(ical);

		ical.set(ICal.SUMMARY, "XXX");


		System.out.println(ical);

		/*
		ICal ical = ICal.instance(new FileInputStream("c:/sample.ical"));

		System.out.println(ical);
		*/

		//ICalPrint print = new ICalPrint();
		//ical.dataNode.child().visit(print);

	}

}


class Printer extends Visitor
{
	public StringBuffer _buf = new StringBuffer();


	public Printer (ICal ical)
	{
		ical._icalTree.child().visit(this);
	}


    public void beginNode (Node node, Iterator<Node> attributes, int depth) throws Exception
    {
    	if (node.child() != null) {
    		_buf.append("BEGIN:").append(node.id()).append('\n');
    	}
    	else {
    		ICal.ContentValue contentValue = (ICal.ContentValue)node.value();
    		if (contentValue._value != null) {
    			_buf.append(node.id()).append(":").append(contentValue.toString()).append('\n');
    		}
    	}
    }

    public void endNode (Node node, int depth) throws Exception
    {
    	if (node.child() != null) {
    		_buf.append("END:").append(node.id()).append('\n');
    	}
    }
}

class Setter extends Visitor
{
	ICal.ContentDefn _content;
	Object _value;

	public Setter (ICal ical, ICal.ContentDefn content, Object value)
	{
		_content = content;
		_value = value;
		ical._icalTree.visit(this);
	}


    public void beginNode (Node node, Iterator<Node> attributes, int depth) throws Exception
    {
    	if (node.id().equals(_content._name)) {
    		ICal.ContentValue contentValue = (ICal.ContentValue)node.value();
    		contentValue._value = _value;
    	}
    }
}


