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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;


public class BufferFlusher
{
	public static enum Flush {Sync, Async, Pool}
	public static enum Store {Insert, Update}

	HitBuffer _hitBuffer;
	Category _category;

	public void flush (Category category, HitBuffer hitBuffer)
	{
		_category = category;
		_hitBuffer = hitBuffer;
		flushToStore();
	}

	public void flushToStore ()
	{

		_hitBuffer.Read.lock();
		try {
			for (Map.Entry<Key, BoundedHits> entry : _hitBuffer._flushBuffer.entrySet()) {
				BoundedHits hits = entry.getValue();
				if (hits != null) {
					do {
						Object dbContext = _category._hitStore.newContext();
						if (_category._meta._storeType == Store.Insert) {
							_category._hitStore.create(_category, entry.getKey(), hits, dbContext);
						}
						if (_category._meta._storeType == Store.Update) {
							BoundedDates hitsInterval = BoundedDates.create(new Date(hits.startIntervalTime()), new Date(hits.endIntervalTime()));
							List<HitEntry> l = _category._hitStore.getHitEntries(_category, hitsInterval.beginTime(), hitsInterval.endTime(), entry.getKey(), true, false, dbContext);
							if (l == null || l.isEmpty()) {
								_category._hitStore.create(_category, entry.getKey(), hits, dbContext);
							}
							else {
								_category._hitStore.update(_category, entry.getKey(), hits, l.get(0), dbContext);
							}
						}
						hits = hits.previous();
					}
					while (hits != null);
				}
			}
		}
		catch (Exception e) {
			_category.logWarning(
				    "BufferFlusher.flushToStore()", Thread.currentThread().toString(), e);
		}
		finally {
			_hitBuffer._flushBuffer.clear();
			_hitBuffer._flushBuffer = _hitBuffer._buffer;
			_hitBuffer.Read.unlock();
		}
	}


	public static BufferFlusher create (Flush type)
	{
		if (Flush.Sync == type) {
			return new BufferFlusher();
		}
		if (Flush.Async == type) {
			return new AsyncBufferFlusher();
		}
		if (Flush.Pool == type) {
			return new PoolBufferFlusher();
		}
		return null;
	}

	public static class ThreadFactoryImpl implements ThreadFactory
	{
		public static final ThreadFactory Instance = new ThreadFactoryImpl();

		public Thread newThread(Runnable r)
		{
			return new Thread(r);
		}
	}
}


class PoolBufferFlusher extends BufferFlusher implements Runnable
{
	public void flush (Category category, HitBuffer hitBuffer)
	{
		_category = category;
		_hitBuffer = hitBuffer;
		_category._meta._external._pool.execute(this);
	}

	public void run ()
	{
		try {
			flushToStore();
		}
		finally {
			synchronized (_hitBuffer) {
				_hitBuffer.notifyAll();
			}
		}
	}
}


class AsyncBufferFlusher extends PoolBufferFlusher implements Runnable
{
	Thread _thread;

	public void flush (Category category, HitBuffer hitBuffer)
	{
		_category = category;
		_hitBuffer = hitBuffer;
		_thread = _category._meta._external._threadFactory == null
		    ? ThreadFactoryImpl.Instance.newThread(this)
		    : _category._meta._external._threadFactory.newThread(this);
		_thread.start();
	}
}



