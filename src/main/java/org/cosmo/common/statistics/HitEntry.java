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

import java.util.Arrays;

import ariba.util.core.Fmt;

public interface HitEntry
{
	public Object[] keys ();
	public String[] labels ();
	public long startTime ();
	public long endTime ();
	public int hitsValue ();

		// startTime and endTime are abstract, use FullImpl for full data
	public static class Impl implements HitEntry
	{
		private int _hits;
		private Object[] _keys;
		private String[] _labels;

		public Impl (Key key, int hits)
		{
			this (key._keys, key.hasLabel() ? key.label()._labels : new String[0], hits);
		}

		public Impl (Object[] keys, String[] labels, int hits)
		{
			_keys = keys;
			_labels = labels;
			_hits = hits;
		}

		public Object[] keys ()
		{
			return _keys;
		}

		public String[] labels ()
		{
			return _labels;
		}


		public int hitsValue ()
		{
			return _hits;
		}

		public int hashCode ()
		{
			return Key.arrayHashCode(_keys);
		}

			// only compare keys excluding labels
		public boolean equals (Object o)
		{
			if (o == null) {
				return false;
			}
			if (o instanceof HitEntry) {
				return Key.arrayEquals(keys(), ((HitEntry)o).keys());
			}
			return false;
		}

		public String toString ()
		{
			return Fmt.S("Hits [%s] %s", String.valueOf(_hits), Arrays.deepToString(_keys));
		}


		public long startTime ()
		{
			return -1;
		}

		public long endTime ()
		{
			return -1;
		}
	}


	public static class FullImpl extends HitEntry.Impl implements Comparable<FullImpl>
	{
		long _startTime;
		long _endTime;

		public FullImpl (Object[] keys, String[] labels, int hits, long startTime, long endTime)
		{
			super(keys, labels, hits);
			_startTime = startTime;
			_endTime = endTime;

		}

		public FullImpl (Key key, int hits, long startTime, long endTime)
		{
			super (key, hits);
			_startTime = startTime;
			_endTime = endTime;
		}

		public long startTime ()
		{
			return _startTime;
		}

		public long endTime ()
		{
			return _endTime;
		}

		public int compareTo (FullImpl o)
		{
			if (o.endTime() == endTime()) {
				return 0;
			}
			return o.endTime() > endTime() ? 1 : -1;
		}
	}
}
