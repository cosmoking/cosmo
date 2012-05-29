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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.SystemUtil;


public class Category
{
	Meta _meta;
	HitBuffer _hitBuffer;
	HitStore _hitStore;
	int _id;

	public static Category create (Meta meta, HitStore hitStore)
	{
		if (meta == null || hitStore == null) {
			return null;
		}
		Category category = new Category();
		category._meta = meta;
		category._id = hitStore.getNextUniqueCategoryID();
		category._hitStore = hitStore;
		category._hitBuffer = new HitBuffer(category);
		return category;
	}

	public boolean increment (Object... keys)
	{
		return increment(Label.Empty, keys);
	}

	public boolean increment (Label label, Object... keys)
	{
		return increment(1, label, keys);
	}

	public boolean increment (int i, Object... keys)
	{
		return increment(i, Label.Empty, keys);
	}

	public boolean increment (int i, Label label, Object... keys)
	{
		try {
			Key key = _hitStore.getKeyResolver(this).getKey(label, keys);
			if (key == Key.Invalid) {
				return false;
			}
			_hitBuffer.addNewHits(this, key, i);
			return true;
		}
		catch (Exception e) {
			logWarning("increment()", Arrays.deepToString(keys), e);
			return false;
		}
	}

	public Key keyFor (Object... keys)
	{
		return _hitStore.getKeyResolver(this).getKey(Label.Empty, keys);
	}


	public void flush ()
	{
		long timeStamp = System.currentTimeMillis();
		_hitBuffer.flushChange(this, true, timeStamp);
	}


	public void flushUntilFinish ()
	{
		long timeStamp = System.currentTimeMillis();
		synchronized (_hitBuffer.Write) {
			synchronized (_hitBuffer) {
				try {
					while (!_hitBuffer._buffer.isEmpty() || !_hitBuffer._flushBuffer.isEmpty()) {
						flush();
						if (!_hitBuffer._buffer.isEmpty() || !_hitBuffer._flushBuffer.isEmpty()) {
							_hitBuffer.wait(3000);
						}
					}
				}
				catch (InterruptedException e) {
					this.logWarning("flushUntilFinish", Thread.currentThread(), e);
				}
			}
		}
	}

	public List<HitEntry> getHitEntries (Date startTime, Date endTime, Object... keys)
	{
		return getHitEntries(startTime, endTime, false, false, keys);
	}


	public List<HitEntry> getHitEntries (Date startTime, Date endTime, boolean exact, boolean full, Object... keys)
	{
		try {
			Key key = _hitStore.getKeyResolver(this).getKey(keys);
			if (key == Key.Invalid) {
				return ListUtil.list();
			}
			List<HitEntry> hitEntries = new ArrayList();
			_hitBuffer.Read.lock();
			try {
				List<HitEntry> bufferEntries = _hitBuffer.getHitEntries(
						startTime, endTime, key, exact, full);
				hitEntries.addAll(bufferEntries);
				List<HitEntry> storeEntries = _hitStore.getHitEntries (
						this, startTime, endTime, key, false, full, _hitStore.newContext());
				hitEntries.addAll(storeEntries);
			}
			finally {
				_hitBuffer.Read.unlock();
			}
			return hitEntries;
		}
		catch (Exception e) {
			logWarning("getHitEntries()", Arrays.deepToString(keys), e);
			return ListUtil.list();
		}
	}


	public int getHits (Date from, Date to, Object... keys)
	{
		List <HitEntry> hitEntries = getHitEntries(from, to, keys);
		int hits = 0;
		if (hitEntries == null || hitEntries.isEmpty()) {
			return hits;
		}
		for (HitEntry hitEntry : hitEntries) {
			hits = hits + hitEntry.hitsValue();
		}
		return hits;
	}

	public List<HitEntry> getHitEntries (Object... keys)
	{
		return getHitEntries(null, null, keys);
	}

	public int getHits (Object... keys)
	{
		return getHits(null, null, keys);
	}


	public List<HitEntry> getDistinctHitEntries (Comparator<HitEntry> sortOrder /* null allowed */,
													Date startTime,
													Date endTime,
													Object... keys)
	{
		HashMap<Key, HitEntry> entries = new HashMap();
		for (HitEntry entry : getHitEntries(startTime, endTime, false, true, keys)) {
			Key key = new Key(entry.keys());
			HitEntry storedEntry = entries.get(key);
			if (storedEntry == null) {
				entries.put(key, entry);
			}
			else {
				HitEntry.FullImpl newEntry = new HitEntry.FullImpl(
					entry.keys(),
					Arrays.deepToString(storedEntry.labels()).length() > Arrays.deepToString(entry.labels()).length() ? storedEntry.labels() : entry.labels(),
					storedEntry.hitsValue() + entry.hitsValue(),
					storedEntry.startTime() < entry.startTime() ? storedEntry.startTime() :  entry.startTime(),
					storedEntry.endTime() > entry.endTime() ? storedEntry.endTime() : entry.endTime());
				entries.put(key, newEntry);
			}
		}

		HitEntry[] sorted  = entries.values().toArray(new HitEntry[0]);
		if (sortOrder == null) {
			Arrays.sort(sorted); // default sorts by endTime
		}
		else {
			Arrays.sort(sorted, sortOrder);
		}
		return Arrays.asList(sorted);
	}


	public int id ()
	{
		return _id;
	}

	public Meta meta ()
	{
		return _meta.copy();
	}

	public String toString ()
	{
		return Fmt.S("Category [%s] Description [%s]", _meta._name, _meta._description);
	}

	public void logWarning (String method, Object context, Exception e)
	{
		String msg = Fmt.S(
			"Statistic Category [%s] warning on [%s] with context [%s]:\n%s",
			_meta._name, method, context, SystemUtil.stackTrace(e));
		if (_meta._external._optionalLogOutput != null) {
			_meta._external._optionalLogOutput.println(msg);
		}
	}
}
