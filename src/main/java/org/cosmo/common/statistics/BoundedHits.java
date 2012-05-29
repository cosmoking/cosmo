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

import java.util.Calendar;
import java.util.Date;
import ariba.util.core.Fmt;

abstract public class BoundedHits extends Hits
{
	public static enum Interval {ByMinute, ByHour, ByDay, ByMonth}

	int _endIntervalTime;

	public BoundedHits (int i, Calendar currentTimeMillis)
	{
		super(i, currentTimeMillis.getTimeInMillis());
	}

	public boolean reachedInterval (long currentTimeMillis)
	{
		return currentTimeMillis > endIntervalTime();
	}

	public long endIntervalTime ()
	{
		return getTimeInMillis(_endIntervalTime);
	}

	public long startIntervalTime ()
	{
		try {
			int timeUnit = getClass().getField("TimeUnit").getInt(null);
			return getTimeInMillis(_endIntervalTime - timeUnit + 1);
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Assert", e);
		}
	}

	public BoundedHits previous ()
	{
		return null;
	}

	public String toString ()
	{
		return Fmt.S(
			"%s %s Start Interval [%s] End Interval [%s]", getClass().getSimpleName(), super.toString(),
			new Date(startIntervalTime()), new Date(endIntervalTime()));
	}

	public static BoundedHits create (Interval type, Calendar currentTimeMillis)
	{
		if (type == Interval.ByMinute) {
			return new MinuteHits(0, currentTimeMillis);
		}
		if (type == Interval.ByHour) {
			return new HourlyHits(0, currentTimeMillis);
		}
		if (type == Interval.ByDay) {
			return new DailyHits(0, currentTimeMillis);
		}
		if (type == Interval.ByMonth) {
			return new MonthlyHits(0, currentTimeMillis);
		}
		return null;
	}


	public static BoundedHits link (BoundedHits hits, BoundedHits previousHits)
	{
		LinkedHits linkedHits = new LinkedHits(hits);
		linkedHits._previous = previousHits;
		return linkedHits;
	}

	BoundedHits ()
	{
	}
}

class MinuteHits extends BoundedHits
{
	public static final int TimeUnit = 60; // Minute = 60;

	public MinuteHits (int i, Calendar currentTime)
	{
		super(i, currentTime);
		currentTime.set(Calendar.SECOND, 60);
		currentTime.set(Calendar.MILLISECOND, 0);
		_endIntervalTime = getTimeInSec(currentTime.getTimeInMillis()) - 1;
	}
}


class HourlyHits extends BoundedHits
{
	public static final int TimeUnit = 60 * 60;  //Hour = 60 * 60;

	public HourlyHits (int i, Calendar currentTime)
	{
		super(i, currentTime);
		currentTime.set(Calendar.MINUTE, 60);
		currentTime.set(Calendar.SECOND, 0);
		currentTime.set(Calendar.MILLISECOND, 0);
		_endIntervalTime = getTimeInSec(currentTime.getTimeInMillis()) - 1;
	}
}


class DailyHits extends BoundedHits
{
	public static final int TimeUnit = 24 * HourlyHits.TimeUnit; //Day = 24 * HourlyHits.Hour;

	public DailyHits (int i, Calendar currentTime)
	{
		super(i, currentTime);
		currentTime.set(Calendar.HOUR_OF_DAY, 24);
		currentTime.set(Calendar.MINUTE, 0);
		currentTime.set(Calendar.SECOND, 0);
		currentTime.set(Calendar.MILLISECOND, 0);
		_endIntervalTime = getTimeInSec(currentTime.getTimeInMillis()) - 1;
	}
}


class MonthlyHits extends BoundedHits
{

	public MonthlyHits (int i, Calendar currentTime)
	{
		super(i, currentTime);
		currentTime.set(Calendar.MONTH, currentTime.get(Calendar.MONTH) + 1);
		currentTime.set(Calendar.DAY_OF_MONTH, 1);
		currentTime.set(Calendar.HOUR_OF_DAY, 0);
		currentTime.set(Calendar.MINUTE, 0);
		currentTime.set(Calendar.SECOND, 0);
		currentTime.set(Calendar.MILLISECOND, 0);
		_endIntervalTime = getTimeInSec(currentTime.getTimeInMillis()) - 1;
	}

	public long startIntervalTime ()
	{
		Calendar time = Calendar.getInstance();
		time.setTimeInMillis(getTimeInMillis(_endIntervalTime));
		time.set(Calendar.DAY_OF_MONTH, 1);
		time.set(Calendar.HOUR_OF_DAY, 0);
		time.set(Calendar.MINUTE, 0);
		time.set(Calendar.SECOND, 0);
		time.set(Calendar.MILLISECOND, 0);
		return time.getTimeInMillis();
	}
}

class LinkedHits extends BoundedHits
{
	BoundedHits _previous;
	int _startIntervalTime;

	public LinkedHits (BoundedHits hits)
	{
		_hits = hits._hits;
		_startTime = hits._startTime;
		_endTime = hits._endTime;
		_endIntervalTime = hits._endIntervalTime;
		_startIntervalTime = getTimeInSec(hits.startIntervalTime());
	}

	public long startIntervalTime ()
	{
		return getTimeInMillis(_startIntervalTime);
	}

	public BoundedHits previous ()
	{
		return _previous;
	}
}
