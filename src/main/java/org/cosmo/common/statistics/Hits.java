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

import java.util.Date;

import ariba.util.core.Fmt;

public class Hits
{
	static long OffSet = (System.currentTimeMillis() / 1000) * 1000;

	int _hits;
	int _startTime;
	int _endTime;

	public Hits (int i, long currentTimeMillis)
	{
		i = i >= 0 ? i : 0;
		_hits = i;
		_startTime = _endTime = getTimeInSec(currentTimeMillis);
	}

	protected void incrementBy (int i, long currentTimestamp)
	{
		i = i >= 0 ? i : 0;
		_hits = _hits + i;
		_endTime = getTimeInSec(currentTimestamp);
	}

	public int hits ()
	{
		return _hits;
	}

	public long startTime ()
	{
		return getTimeInMillis(_startTime);
	}

	public long endTime ()
	{
		return getTimeInMillis(_endTime);
	}

	public String toString ()
	{
		return Fmt.S("Hits [%s] Start [%s] End [%s]",
		    String.valueOf(_hits), new Date(startTime()), new Date(endTime()));
	}

	int getTimeInSec (long timeMillis)
	{
		return (int)((timeMillis - OffSet) / 1000);
	}

	long getTimeInMillis (int timeSec)
	{
		return ((long)timeSec * 1000) + OffSet;
	}

	Hits ()
	{
	}
}


