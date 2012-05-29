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

import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicLong;

import org.cosmo.common.util.New;

import ariba.util.core.Fmt;

// Operates that a timer clock for timing. everything returned is in Seconds

public class Clock
{

    private static final ThreadLocal<Clock> Timer = new ThreadLocal();

    public static Clock timer ()
    {
    		// this should be threadsafe as the get() uses currentThread() as key
    	if (Timer.get() == null) {
    		Timer.set(Clock.create(Clock.Unit.Micro).reset());
    	}
    	return Timer.get();
    }


	long _startTime;
	AtomicLong _markedTime;
	Unit _precision;
	public final Core _core;

	private Clock ()
	{
		_core = new Core(this);
	}

	public static Clock create (Unit precision)
	{
		Clock timer = new Clock();
		timer._precision = precision;
		timer._markedTime = new AtomicLong(0);
		timer.reset();
		return timer;
	}

	public String runningTime(Unit unit)
	{
		return unit.fmt(_core.runningTime());
	}

	public String runningTime()
	{
		return runningTime(_precision);
	}

	public String markTime (Unit unit)
	{
		return unit.fmt(_core.markTime());
	}

	public String markTime ()
	{
		return markTime(_precision);
	}

	public String markAndCheckRunning (Unit unit)
	{
		long currentTimeMillis = System.nanoTime();
		return New.str("[",unit.fmt(_core.runningTime(currentTimeMillis, unit)),"|",unit.fmt(_core.markTime(currentTimeMillis, unit)) ,"]");
	}

	public String markAndCheckRunning ()
	{
		return markAndCheckRunning (_precision);
	}

	public void markAndCheckRunning (PrintStream out)
	{
		out.println(markAndCheckRunning());
	}

	public String markedTime (Unit unit)
	{
		return unit.fmt(_core.markedTime());
	}

	public String markedTime ()
	{
		return markedTime(_precision);
	}

	public boolean runningTimeHasPassed (double time)
	{
		return _core.runningTime() > time;
	}

	public boolean markedTimeHasPassed (double time)
	{
		return _core.markedTime() > time;
	}

	public synchronized Clock reset ()
	{
		_startTime = System.nanoTime();
		_markedTime.set(_startTime);
		return this;
	}

	public static enum Unit {

		Sec (new DecimalFormat("0"), 1000000000, 1) ,
		Tenth (new DecimalFormat("0.0"), 100000000, 10) ,
		Hundredth (new DecimalFormat("0.00"), 10000000, 100),
		Milli (new DecimalFormat("0.000"), 1000000, 1000),
		Micro(new DecimalFormat("0.000000"), 1000, 1000000),
		Nano (new DecimalFormat("0.000000000"), 1, 1000000000);

	    private final DecimalFormat _format;
	    public final long _offset;
	    public final long _unitsInSecond;

	    Unit (DecimalFormat format, long offset, long unitsInSecond) {
			_format = format;
			_offset = offset;
			_unitsInSecond = unitsInSecond;
		}

		public synchronized String fmt (double number)
		{
			return New.str(_format.format(number),"s");
		}

		public long original (double number)
		{
			return (long)(number * _unitsInSecond);
		}
	}

	public static class Core
	{
		Clock _clock;

		public Core (Clock clock)
		{
			_clock = clock;
		}

		private double runningTime (long currentTimeMillis, Unit unit)
		{
			long runningTime = (currentTimeMillis - _clock._startTime) / unit._offset * unit._offset;
			return (double)runningTime / 1000000000.0;
		}

		public double runningTime ()
		{
			return runningTime(System.nanoTime(), _clock._precision);
		}

		public double markTime ()
		{
			return markTime(System.nanoTime(), _clock._precision);
		}

		private double markTime (long currentTimeMillis, Unit unit)
		{
			long lastMark = 0;
			do {
				lastMark = _clock._markedTime.get();
				if (lastMark >= currentTimeMillis) {
					return 0.0;
				}
			} while (!_clock._markedTime.compareAndSet(lastMark, currentTimeMillis));
			return ((currentTimeMillis - lastMark) / unit._offset * unit._offset) / 1000000000.0;
		}

		/*
		public double[] markAndCheckRunning ()
		{
			long currentTimeMillis = System.nanoTime();
			return new double[] {markTime(currentTimeMillis, _clock._precision), runningTime(currentTimeMillis, _clock._precision)};
		}
		*/

		public double markedTime ()
		{
			return (double)(_clock._markedTime.get() / 1000000000.0);
		}
	}
}
