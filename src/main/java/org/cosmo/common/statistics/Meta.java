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

import java.io.PrintStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

public class Meta
{

		// category meta
	public String _description;
	public String _name;

		// buffer meta
	public BufferFlusher.Store _storeType;
	public BoundedHits.Interval _boundedHitsInterval;
	public long _flushWindow;
	public int _flushHitCountLimit;
	public int _flushHitEntryCountLimit;

		// key meta
	public int _keySize;
	public int _labelSize;
	public int _labelMaxLength;

		// flush type
	public BufferFlusher.Flush _flushType;

		// external
	public External _external = new External();


	public Meta (String name)
	{
		this (name, "");
	}

	public Meta (String name, String description)
	{
		_name = name;
		_description = description;
	}

	public Meta copy ()
	{
		try {
			return (Meta)super.clone();
		}
		catch (CloneNotSupportedException e) {
			return null;
		}
	}

	public class External
	{
			// thread factory
		public ThreadFactory _threadFactory;

			// debug stream
		public PrintStream _optionalLogOutput;

			// threaded queue
		public LinkedBlockingQueue<Runnable> _queue;

			// thread pool
		public ThreadPoolExecutor _pool;
	}
}
