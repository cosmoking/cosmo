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
package org.cosmo.common.record;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;

import org.cosmo.common.xml.Node;
import org.json.JSONException;
import org.json.JSONObject;


public class DefnRecord <T extends Record> extends Defn
{
	public static final Class[] TypeClasses = new Class[] {Record.class};


	public DefnRecord (Meta declaringMeta)
	{
		super(declaringMeta);
	}


	public DefnRecord index (boolean index)
	{
		super.index(index);
		return this;
	}

	@Override
	public DefnRecord lazyLoad (boolean lazyLoad)
	{
		super.lazyLoad(lazyLoad);
		return this;
	}

	public DefnRecord listener (Class listenerClass)
	{
		super.listener(listenerClass);
		return this;
	}

	public DefnRecord listener (Class listenerClass, Class paramClass)
	{
		super.listener(listenerClass, paramClass);
		return this;
	}


	public int size ()
	{
		return DefnLong.Size;
	}

	public Class[] typeClasses ()
	{
		return TypeClasses;
	}

	public Object readImpl (ByteBuffer dataIO, boolean directConvertFromBytes)
	  throws IOException
	{
			// read record
		long recordId = dataIO.getLong();
		Record record = Meta.Instance((Class<? extends Record>)field().getType()).store().newInstance(recordId);
		if (record instanceof IntrinsicRecord) {
			((IntrinsicRecord)record).setDefn(this);
		}
		return record;
	}


	@Override
	public byte[] writeImpl (byte[] dst, Object src, long i)
	  throws IOException
	{
		long v = ((Record)src).tx()._id;
    	dst[1] = (byte)(v >>> 56);
    	dst[2] = (byte)(v >>> 48);
    	dst[3] = (byte)(v >>> 40);
    	dst[4] = (byte)(v >>> 32);
    	dst[5] = (byte)(v >>> 24);
    	dst[6] = (byte)(v >>> 16);
    	dst[7] = (byte)(v >>>  8);
    	dst[8] = (byte)(v >>>  0);
		return dst;
	}


	@Override
	public Node fieldToXML (Record record)
	{
		try {
			Node field = new Node(field().getName());
			Record fieldRecord = (Record)field().get(record);
			field.set(fieldRecord == null ? null :fieldRecord.tx().recordToXML());
			return field;
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}

	}
	
	@Override
	public void fieldToJSON (Record record, JSONObject json, boolean skipNullFields, boolean load)
	  throws JSONException
	{
		Record value = (Record)getValue(record);		
		if (value == null && skipNullFields) {
			return;
		}		
		json.put(_declaringFieldName, value.tx().recordToJSON(skipNullFields, load));		
	}


	public String fmt (Object o, FormatType type)
	{
		if (o == null) {
			throw new RecordException("Can not format NULL value");
		}
		if (FormatType.ForIndex == type || FormatType.ForSearch == type) {
			if (o instanceof Record) {
				long id = ((Record)o).tx()._id;
				return String.valueOf(id);
			}
			return String.valueOf(o);
		}
		if (FormatType.ForDisplay == type) {
			return _formatter.fmt(o, type);
		}
		throw new RuntimeException ("Invalid type");
	}

	public DefnRecord formatter (Class<? extends DefnFormatter> formatter)
	{
		try {
			Constructor constructor = formatter.getConstructor(new Class[] {Defn.class});
			_formatter = (DefnFormatter)constructor.newInstance(new Object[] {this});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		return this;
	}


	@Override
	public Object parse (String s)
	{
		return org.cosmo.common.util.Util.parseLong(s);
	}

	@Override
	public boolean ifTrueForFunction (Object rightValue, String function, Object leftValue)
	{
		if ("equals".equals(function)) {
			return rightValue == leftValue || rightValue.equals(leftValue);
		}
		throw new IllegalArgumentException(function);
	}


	/*
	public static class FieldCacheImpl extends DefnLong.FieldCacheImpl
	{

		public FieldCacheImpl (Defn defn)
		{
			super(defn);
		}

		@Override
		void sync (int maxCounts)
		  throws IOException
		{
				// exact copy of Defn.readAll() except * below
			int count = Math.min(_defn.count(), maxCounts);
			byte[] buf = _defn.readFullRawBytes(maxCounts).array();
			Object elements = Array.newInstance(_defn.primitiveType(), count);
			ByteBuffer readDataIO = ByteBuffer.allocate(_defn.size());
			for (int i = 0, offset = 0, size = _defn.size(), c = count; i < c; i++, offset += size) {
				readDataIO.put(buf, offset, size);
				readDataIO.rewind();
				readDataIO.get(); // skip header byte
			    Array.set(elements, i, readDataIO.getLong()); // read id (long) instead of record (Object)
				readDataIO.rewind();
			}
			_elements = (long[])elements;
		}
	}
	*/
}
