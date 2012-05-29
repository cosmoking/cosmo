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
package org.cosmo.common.statistics;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class BoundedDates
{
	static final SimpleDateFormat Format1 =
		new SimpleDateFormat("MMM yyyy");
	static final SimpleDateFormat Format2 =
		new SimpleDateFormat("MMM dd yyyy");


	private Date _beginTime;
	private Date _endTime;

	public static BoundedDates create (Date begin, Date end)
	{
		if (begin == null || end == null || begin.getTime() > end.getTime()) {
			return null;
		}
		BoundedDates instance = new BoundedDates();
		instance._beginTime = begin;
		instance._endTime = end;
		return instance;
	}

	public Date beginTime ()
	{
		return _beginTime;
	}

	public Date endTime ()
	{
		return _endTime;
	}

	public String getMonthString ()
	{
		return Format1.format(_beginTime);
	}


	public String getMonthString (Locale locale)
	{
		SimpleDateFormat localizedFormat = new SimpleDateFormat(Format1.toPattern(), locale);
		return localizedFormat.format(_beginTime);
	}


	public String getBoundedDateString ()
	{
		return Format2.format(_beginTime) + "  -  " + Format2.format(_endTime);
	}


	public String getBoundedDateString (Locale locale)
	{
		SimpleDateFormat localizedFormat = new SimpleDateFormat(Format2.toPattern(), locale);
		return localizedFormat.format(_beginTime) + " - " + localizedFormat.format(_endTime);
	}


	public boolean contains (Date date)
	{
		return contains(date.getTime());
	}

	public boolean contains (long time)
	{
		return time >= _beginTime.getTime() && time <= _endTime.getTime();
	}

	public String toString ()
	{
		return _beginTime.toString() + " - " + _endTime.toString();
	}

	public boolean equals (Object o)
	{
		if (o == null) {
			return false;
		}
		if (o instanceof BoundedDates) {
			BoundedDates compare = (BoundedDates)o;
			return _beginTime.equals(compare._beginTime) && _endTime.equals(compare._endTime);
		}
		return false;
	}


	public static Date getPastMonthBy (int i)
	{
		Calendar calendar = GregorianCalendar.getInstance();
		int currentMonth = calendar.get(Calendar.MONTH);
		calendar.set(Calendar.MONTH, currentMonth - i);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		Date date = calendar.getTime();
		return date;
	}

	public static Date getPastDayBy (int i)
	{
		Calendar calendar = GregorianCalendar.getInstance();
		int currentDay = calendar.get(Calendar.HOUR_OF_DAY);
		calendar.set(Calendar.HOUR_OF_DAY, currentDay - i);
		calendar.set(Calendar.HOUR_OF_DAY, 1);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		Date date = calendar.getTime();
		return date;
	}


	public static BoundedDates fromMonthBackAt (int i)
	{
		BoundedDates instance = new BoundedDates();
		instance._beginTime = getPastMonthBy(i);
		instance._endTime = getPastMonthBy(i - 1);
		instance._endTime = new Date (instance._endTime.getTime() - 1);
		return instance;
	}


}
