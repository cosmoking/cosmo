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
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import ariba.util.core.ListUtil;

public class HitBuffer
{
	Category _category;
	volatile ConcurrentHashMap<Key, BoundedHits> _buffer, _flushBuffer;
	BufferFlusher _bufferFlusher;
	long _lastFlushTimestamp;
	int _flushHitCount;
	final ReentrantLock Read = new ReentrantLock();
	final ReentrantLock Write = new ReentrantLock();


	public HitBuffer (Category category)
	{
		_category = category;
		_lastFlushTimestamp = System.currentTimeMillis();
		_buffer = new ConcurrentHashMap<Key, BoundedHits>();
		_flushBuffer = _buffer;
		_bufferFlusher = BufferFlusher.create(category._meta._flushType);
	}

	public void addNewHit (Category category, Key key)
	{
		addNewHits(category, key, 1);
	}

	public void addNewHits (Category category, Key key, int i)
	{
		Calendar currentTimestamp = GregorianCalendar.getInstance();
		long currentTimestampInMillis = currentTimestamp.getTimeInMillis();
		Write.lock();
		try {
			BoundedHits hits = _buffer.get(key);
			if (hits == null) {
				hits = BoundedHits.create(_category._meta._boundedHitsInterval, currentTimestamp);
				_buffer.put(key, hits);
			}
			else {
				if (hits.reachedInterval(currentTimestampInMillis)) {
					BoundedHits nextBoundedHits = BoundedHits.create(_category._meta._boundedHitsInterval, currentTimestamp);
					hits = BoundedHits.link(nextBoundedHits, hits);
					_buffer.put(key, hits);
				}
			}
			hits.incrementBy(i, currentTimestampInMillis);
			_flushHitCount++;
			flushChange(category, false, currentTimestampInMillis);
		}
		finally {
			Write.unlock();
		}
	}


	public List<HitEntry> getHitEntries (Date from, Date to, Key selectKey, boolean exact, boolean full)
	{
		List<HitEntry> list = ListUtil.list();
		if (exact) {
			BoundedHits hits = _flushBuffer.get(selectKey);
			if (hits != null) {
				do {
					if ((from == null || hits.startTime() >= from.getTime()) &&
						(to == null || hits.endTime() <= to.getTime())) {
						list.add(full
							? new HitEntry.FullImpl(selectKey, hits.hits(), hits.startTime(), hits.endTime())
							: new HitEntry.Impl(selectKey, hits.hits()));
					}
					hits = hits.previous();
				}
				while (hits != null);
			}
		}
		else {
				// match "startWith" wildcard. TODO sort keys for faster single key get
			for (Map.Entry<Key, BoundedHits> hitEntry : _flushBuffer.entrySet()) {
				if (hitEntry.getKey().contains(selectKey)) {
					BoundedHits hits = hitEntry.getValue();
					if (hits != null) {
						do {
							if ((from == null || hits.startTime() >= from.getTime()) &&
									(to == null || hits.endTime() <= to.getTime())) {
									list.add(full
										? new HitEntry.FullImpl(hitEntry.getKey(), hits.hits(), hits.startTime(), hits.endTime())
										: new HitEntry.Impl(hitEntry.getKey(), hits.hits()));
							}
							hits = hits.previous();
						}
						while (hits != null);
					}
				}
			}
		}
		return list;
	}


	boolean flushChange (Category category, boolean force, long currentTimestamp)
	{
		Write.lock();
		try {
			if (_flushBuffer == _buffer && _flushBuffer.size() > 0 &&
				 ( _flushBuffer.size() > _category._meta._flushHitEntryCountLimit ||
				  currentTimestamp - _lastFlushTimestamp > _category._meta._flushWindow ||
				  _flushHitCount >= _category._meta._flushHitCountLimit ||
				  force)) {
				_lastFlushTimestamp = currentTimestamp;
				_flushHitCount = 0;
				_buffer = new ConcurrentHashMap<Key, BoundedHits>();
				_bufferFlusher.flush(category, this);
				return true;
			}
			return false;
		}
		finally {
			Write.unlock();
		}
	}
}


