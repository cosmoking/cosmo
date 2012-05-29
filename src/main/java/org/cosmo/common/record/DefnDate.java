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
import java.nio.ByteBuffer;
import java.util.Date;

import org.cosmo.common.util.SearchRange;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;


public class DefnDate <T extends Record> extends Defn
{

	public static final Class[] TypeClasses = new Class[] {Date.class, DateTime.class, long.class};


		// used for index / search / display
	public static enum Precision {
			yyyy ("yyyy"),
			yyyyMM("yyyyMM"),
			yyyyMMdd("yyyyMMdd"),
			yyyyMMddHH("yyyyMMddHH"),
			yyyyMMddHHmm("yyyyMMddHHmm"),
			yyyyMMddHHmmss("yyyyMMddHHmmss"),
			yyyyMMddHHmmssSSS("yyyyMMddHHmmssSSS");

		DateTimeFormatter _formatterUTC;
		DateTimeFormatter _formatterLocal;
		String _format;

		Precision (String format)
		{
			_format = format;
			_formatterUTC = DateTimeFormat.forPattern(_format).withZone(DateTimeZone.UTC);
			_formatterLocal = DateTimeFormat.forPattern(_format);
		}

		public String format (long date)
		{
				// XXX: defaults to UTC - probably should introduce another flag to allow local time
			//System.out.println(_formatterUTC.print(date));
			return _formatterUTC.print(date);
		}
	}

		// used to parse the data for read and write
	public static enum FormatTime {

		LONG () {
			public Object getTypeValue (long time)
			{
				return time;
			}

			public long extractTypeValue (Object time) {
				return (Long)time;
			}
		},

		DATE () {
			public Object getTypeValue (long time)
			{
				return new Date(time);
			}
			public long extractTypeValue (Object time) {
				return ((Date)time).getTime();
			}

		},

		DATETIME () {
			public Object getTypeValue (long time)
			{
				return new DateTime(time);
			}
			public long extractTypeValue (Object time) {
				return ((DateTime)time).getMillis();
			}

		};

		abstract public Object getTypeValue (long time);
		abstract public long extractTypeValue (Object time);


	}

	private Precision _precision;
	private FormatTime _formatTime;


	public DefnDate (Meta declaringMeta)
	{
		super(declaringMeta);
	}

	public DefnDate index (boolean index)
	{
		super.index(index);
		return this;
	}


	public DefnDate lazyLoad (boolean lazyLoad)
	{
		super.lazyLoad(lazyLoad);
		return this;
	}


	public DefnDate listener (Class listenerClass)
	{
		super.listener(listenerClass);
		return this;
	}

	public DefnDate precision (Precision precision)
	{
		_precision = precision;
		return this;
	}

	public boolean isValueFixedSize ()
	{
		return true;
	}


	@Override
	public void lazyFieldInit ()
	{
		super.lazyFieldInit();
		_formatTime = FormatTime.valueOf(field().getType().getSimpleName().toUpperCase());
	}


	public int size ()
	{
		return DefnLong.Size;
	}

	public Class[] typeClasses ()
	{
		return TypeClasses;
	}

	@Override
	public Object readImpl (ByteBuffer dataIO, boolean directConvertFromBytes)
	  throws IOException
	{
		long timeInMillis = dataIO.getLong();
		  // -1 consider as a null as it was initially wrote intentionally
		if (timeInMillis == -1) {
			return null;
		}
		return _formatTime.getTypeValue(timeInMillis);
	}

	@Override
	public byte[] writeImpl (byte[] dst, Object src, long i)
	  throws IOException
	{
		long v = _formatTime.extractTypeValue(src);
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

	public String fmt (Object o, FormatType type)
	{
		if (o == null) {
			throw new RecordException("Can not format NULL value");
		}
		if (FormatType.ForIndex == type || FormatType.ForSearch == type) {
			if (o instanceof Date) {
				return _precision.format(((Date)o).getTime());
			}
			if (o instanceof Long) {
				return _precision.format((Long)o);
			}
			if (o instanceof DateTime) {
				return _precision.format(((DateTime)o).getMillis());
			}
			if (o instanceof String) {
				return o.toString();
			}
		}
		if (FormatType.ForDisplay == type) {
			if (o instanceof Date) {
				return o.toString();
			}
			if (o instanceof Long) {
				return new Date((Long)o).toString();
			}
			if (o instanceof DateTime) {
				return o.toString();
			}
			if (o instanceof SearchRange) {
				return ((SearchRange)o)._from.toString() + " TO " + ((SearchRange)o)._to.toString();
			}
			throw new RuntimeException("Invalid date object for display");
		}
		throw new RuntimeException("Invalid Dispaly Type");
	}



}
