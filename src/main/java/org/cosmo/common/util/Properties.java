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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.naming.OperationNotSupportedException;


class Properties
{

	StringBuffer buf = new StringBuffer();

	public static final Field<String> BEGIN = new Field<String>() {

		@Override
		public void apply (String t, Properties properties) {
			_value = t;
			properties.buf.append(t);
		}
	};




	public static  final Field<Date> Date = new Field<Date>() {

		@Override
		public void apply (Date t, Properties properties) {
			_value = t;
			properties.buf.append(t);
		}
	};


	public static final HashMap<String, Field> Fields;
	static {
		Fields = new HashMap();
		try {
			java.lang.reflect.Field fields[] = Properties.class.getFields();
			for (java.lang.reflect.Field field : fields) {
				if (field.getType().equals(Field.class)) {
					Fields.put(field.getName().toUpperCase(), (Field)field.get(null));
				}
			}
		}
		catch (IllegalAccessException e) {
			throw new Error(e);
		}
	}


	public static Properties instance (InputStream input)
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




	public static Properties instance (Iterator<String> lines)
	{
		Properties iCal = new Properties();
		for (String line = lines.next(); line != null; lines.next()) {
			int colonPos = line.indexOf(":");
			String fieldName = line.substring(0, colonPos);
			fieldName = fieldName.trim();
			String value = line.substring(colonPos + 1, line.length());
			value = value.trim();
			Field field = Field.resolve(fieldName);
			apply(field, value, iCal);
		}
		return iCal;
	}

	public static Properties instance (Object... fieldValuePair)
	{
		Properties iCal = new Properties();
		for (int i = 0; i < fieldValuePair.length; i++) {
			if (fieldValuePair[i] instanceof Field) {
				apply((Field)fieldValuePair[i], fieldValuePair[i + 1], iCal);
			}
		}
		return iCal;
	}

	private static void apply (Field field, Object value, Properties properties)
	{
		field.apply(value instanceof String ? (String)value : value, properties);
	}


	static abstract class Field<T>  {

		public T _value;
		public Class<T> _clazz;

		private Field ()
		{
		}

		private Field (Class<T> clazz)
		{
			this();
			_clazz = clazz;
		}

		public Class<T> boundedType ()
		{
			if (_clazz == null) {
				ParameterizedType parameterizedType =  (ParameterizedType) getClass().getGenericSuperclass();
				Class<T> clazz = (Class<T>)parameterizedType.getActualTypeArguments()[0];
				_clazz = clazz;
			}
		    return _clazz;
		}


		public void apply (String s, Properties properties)
		{
			try {
				Constructor<T> c = boundedType().getConstructor(String.class);
				T t = c.newInstance(s);
				apply(t, properties);
			}
			catch (Exception e) {
				throw new IllegalArgumentException(e);
			}
		}


		public static Field resolve (String fieldName)
		{
			return Properties.Fields.get(fieldName.toUpperCase());

			/*
			try {
				java.lang.reflect.Field fields[] = ICal.class.getFields();
				for (java.lang.reflect.Field field : fields) {
					if (field.getName().toUpperCase().equals(fieldName.toUpperCase())) {
						Object o = field.get(null);
						return (Field)o;
					}
				}
			}
			catch (IllegalAccessException e) {
			}
			return null;
			*/
		}

		abstract public void apply (T t, Properties properties);
	}


	public static void main (String[] args) throws Exception
	{
		/*
		ICal properties = ICal.instance(
			Title, "abc",
			Date, "Sat, 12 Aug 1995 13:30:00 GMT");
		*/

		Properties properties = Properties.instance(new FileInputStream("c:/sample.properties"));


	}


}


/*
class G extends Field<String>
{

	public G (Class<String> clazz)
	{
		super(clazz);
	}

	public void accept (String t) {

	}

	public String getValue ()
	{
		return _value;
	}


}

*/

