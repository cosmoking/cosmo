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
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.cosmo.common.record.ArrayCache.ArrayCacheIterator;
import org.cosmo.common.statistics.Clock;
import org.cosmo.common.util.Log;

import ariba.util.core.Fmt;


/*
 * 	Cached version of Record Store. the longer it was last accessed (LRU) it will get purged.
 *  timestamped on every 6 secs, if accessed time is same then whichever get less hit gets purged.
 *
 *  when max count is reached (ie, if max is 10000, and loadfactor is 0.75 and count is 7500) then the purge() starts
 *
 *  for CachePurgerTask.java, anything beyond max is not stored, so when it reached 10000 it stops and will purge to 7500
 *  for GCCachePurgerTask.java, anything beyond max is put as GarbageCollectableRecord, so when it reached 10000,
 *  it stores 7500 regular, and rest of 2500 is GarbageCollectableRecord
 *
 *	for GCCachePurgerTask, each time it runs it will promote better records to HardReferenced CacheEntry,
 *	and more less used to GarbageCollectableRecord for GC
 *
 *  also it runs every 5th time
 *
 *  TODO create CacheInterface for Composite, Array, Map
 *
 *  TODO also CompositeCache has a limitation in which it create size in advance
 *
 *  TODO change   _lastAccessed to byte in minutes
 *
 *  2 type of backed cache,  MapCache and ArrayCache both are thread safe,  ArrayCache is about 25% faster but
 *  with a tradeoff of a constant sized array equal to total records.
 *
 *  TODO possible of composite backed cach of Map and Array
 *
 *  TODO provie an API for CachedRecordStore to allow force read from disk,
 *
 *  TODO should probably remove _loadFactor - or make it used only for CachePurgerTask and not GCCachePurgerTask
 *
 *  TODO should have an immutable Cache with best read time
 */

public class CachedRecordStore<T extends Record> extends RecordStore<T>
{
		// basically a shared common scheduler that periodically dispatches to purge cache
	private static final ScheduledThreadPoolExecutor CachePurger = new ScheduledThreadPoolExecutor(1);

		// cacheImpl,  MapCache vs ArrayCache
	private volatile CompositeCache<T> _cache;

		// purger, GCCachePurgerTask vs CachePurgerTask
	private CachePurgerTask<T> _cachePurger;


	private float _loadFactor = 0.85f;
	private int _wakeIntervalInSec = 6; // timestamp updated everyone 6 second, check also done in this interval
	private int _maxToLiveTimeInSec =  _wakeIntervalInSec * 3600; // 6 hours



	public CachedRecordStore (Meta meta)
	  throws IllegalAccessException, InstantiationException, IOException
	{
		super(meta);
		_cache = new CompositeCache(this);
		_cachePurger = meta._allowGCCache ? new GCCachePurgerTask(this) : new CachePurgerTask(this);
	}


	/*
	 *  Will attempt to read from Cache first, if toRecord is null, then a shared cached
	 *  record will be returned. Note, a shared record may be used by multiple threads;
	 *  changes made will be seen by all threads using that copy. Thread Safe.
	 */
	@Override
	public T read (long id, T toRecord, boolean fullRead)
	{
		T cachedRecord = readCached(id);
		if (cachedRecord == null) {
			cachedRecord = super.newInstance(id);
			super.read(id, cachedRecord, fullRead);
			_cachePurger.putToCache(id, cachedRecord);
		}
		_cachePurger.updateCacheStat((CachedTx)cachedRecord.tx());

		if (toRecord == null) {
			return cachedRecord;
		}
		else {
			try {
				toRecord.tx()._id = id;
				for (Defn defn : _meta._defns) {
					defn.field().set(toRecord, defn.field().get(cachedRecord));
				}
			}
			catch (IllegalArgumentException iae) {
				throw new Error(iae);
			}
			catch (IllegalAccessException iae2) {
				throw new Error(iae2);
			}
			return toRecord;
		}
	}

	@Override
	public T read (long id)
	{
		return read (id, null, true);
	}


	@Override
	public T read (long id, boolean fullRead)
	{
		return read (id, null, fullRead);
	}


	public T readCached (long id)
	{
		return _cachePurger.getFromCache(id);
	}



		// allows CachedTx to read un-cached record
	public T superRead (long id, T record, boolean fullRead)
	{
		return super.read(id, record, fullRead);
	}


	@Override
	public Tx createTx (Record record)
	{
		return new CachedTx (record);
	}


	@Override
	protected T newInstance (long id)
	{
			// will read item in cache if not present and return as new instance. We could also return on in cache
			// like below .. but problems ..
		return read(id, null, true);

			// XXX when requested id is not in cache an empty instance is returned which is fine
			// but this creates a problem when none is loaded in cache and we got bunch of them
			// when eventually it's loaded all the previious one are still out there.
		//T cachedRecord = _cachePurger.getFromCache(id);
		//return cachedRecord == null ? super.newInstance(id) : cachedRecord;
	}


	public List<T> getTopHits (int count)
	{
		Iterator<T> cacheEntries = _cache.values();
		T[] list = (T[])new Record[_cache.size()];
		for (int i = 0; cacheEntries.hasNext(); i++) {
			list[i] = cacheEntries.next();
		}
		Arrays.sort(list, MostHitsComparator.Instance);

		List siteResult = new ArrayList();
		for (int i = 0; i < list.length; i++) {
			T cacheRecord = list[i];
			cacheRecord = _cachePurger.getFromCacheEntry(cacheRecord);
			if (cacheRecord != null) {
				siteResult.add(cacheRecord);
			}
			if (i >= count) {
				break;
			}
		}
		return siteResult;
	}



		// main purger task, also updates the "currentTimeInSec" timestamp used by
		// cacheRecord every wake interval.
	public static class CachePurgerTask<T extends Record> implements Runnable
	{
			// keeps an currentTimeOffSet in sec
		public static final long TimeOffSet = (System.currentTimeMillis() / 1000) * 1000;
		CachedRecordStore<T> _cachedRecordStore;
			// contains current time that gets updated every wake interval, read by each cache concurrent read()
		volatile short _currentTimeInSec;
			// contains the cache size, read by each cache concurrent read()
		volatile int _currentCacheSize;
		Clock _timer = Clock.create(Clock.Unit.Micro);


		public CachePurgerTask (CachedRecordStore cachedRecordStore)
		{
			_cachedRecordStore = cachedRecordStore;
			CachePurger.scheduleWithFixedDelay(this, 0, _cachedRecordStore._wakeIntervalInSec, TimeUnit.SECONDS);
		}

		public void putToCache (long id, T cachedRecord)
		{
				// stop putting into cache if exceeds limit, purge will start to kick in on
				// next run() since cacheSize has exceed Max Allowed
			if (_currentCacheSize < _cachedRecordStore._meta._maxAllowedCacheSize) {
				if (_cachedRecordStore._cache.put(id, cachedRecord)) {
					_currentCacheSize++;
				}
			}
		}

		public T getFromCache (long id)
		{
			return _cachedRecordStore._cache.get(id);
		}

		public T getFromCacheEntry (T entry)
		{
			return entry;
		}

			// basic version - force remove
		public int cleanMarkedCacheEntries (T[] cacheEntries, int sizeToDelete)
		{
			int deleted = 0;
			for (int i = 0;  i < sizeToDelete && i < cacheEntries.length; i++) {
				if (cacheEntries[i] == null) {
					continue;
				}
				_cachedRecordStore._cache.remove(cacheEntries[i].tx()._id);
				_currentCacheSize--;
				deleted++;
			}
			Log.jcache.fine(Fmt.S("Deleted [%s] entries", deleted));
			return _currentCacheSize;
		}


		public void run ()
		{
			try {
					// each wake syncs the current system time
				_currentTimeInSec = (short)((System.currentTimeMillis() - TimeOffSet) / 1000);
				_cachedRecordStore._cache.ensureCapacity();

				Log.jcache.fine(Fmt.S("[%s] cache [%s] vs allowed [%s]", _cachedRecordStore._meta._clazz.getSimpleName(), _currentCacheSize, _cachedRecordStore._meta._maxAllowedCacheSize));
					// if current cache equal or exceed max allowed size.
				if (_currentCacheSize >= _cachedRecordStore._meta._maxAllowedCacheSize) {
					purge();
				}
				//Test.useMemory();
			}

				// should not happen.. but in case..
			catch (Throwable e) {
				System.err.println("ERROR While purging cache:" + e.getMessage());
				e.printStackTrace();
				synchronized (_cachedRecordStore) {
					_cachedRecordStore._cache.clear();
					_cachedRecordStore._cache = new CompositeCache(_cachedRecordStore);
				}
				Log.jcache.fine("Error purging.. Reinit cache..");
			}
		}

		protected void purge ()
		{
			if (Log.jcache.getLevel() == java.util.logging.Level.FINE) {
				_timer.reset();
				Log.jcache.fine(Fmt.S("Running [%s,%s] for [%s] at cache size [%s]", getClass().getSimpleName(), _cachedRecordStore._cache.getClass().getSimpleName(), _cachedRecordStore._meta._clazz.getSimpleName(), _currentCacheSize));
				Log.jcache.fine(Fmt.S("%s PRE Snapshot: %s", _cachedRecordStore._cache.getClass().getSimpleName(), _cachedRecordStore._cache));
			}

			float averagelastAccessed = 0;
			float averageCachedHits = 0;
			int removedDueToOld = 0;
			int removedDueToGC = 0;

				// Scan phase - gather stat, sort by lastAccessed
				// cacheEntries may or may not reflect insertion and deletion after this point, remember multi-threaded access?
			Iterator<T> cacheEntries = _cachedRecordStore._cache.values();
			List<T> copyOfScannedEntries = new ArrayList();
			for (int i = 0; cacheEntries.hasNext(); i++) {

				T cacheRecord = cacheEntries.next();
				copyOfScannedEntries.add(cacheRecord);

					// resolves to the actual cache entry, as it may be regular record or gc'record
				cacheRecord = getFromCacheEntry(cacheRecord);

					// could be null if getFromCacheEntry returns null (ie, gc'record that got GC'ed), also remove
				if (cacheRecord == null) {
					cacheEntries.remove();
					removedDueToGC++;
					continue;
				}

					// get each entry's stat
				short lastAccessedInterval = (short) (_currentTimeInSec - ((CachedTx)cacheRecord.tx())._lastAccessed);
				short cacheHits = ((CachedTx)cacheRecord.tx())._cacheHits;

				if (Log.jcache.getLevel() == java.util.logging.Level.FINE) {
					// calculate average  stats
					averagelastAccessed = averagelastAccessed + ((float)lastAccessedInterval / (float)_currentCacheSize);
					averageCachedHits = averageCachedHits + ((float)cacheHits / (float)_currentCacheSize);
				}

					// here just remove entries that is just old...
				if (lastAccessedInterval > _cachedRecordStore._maxToLiveTimeInSec) {
					cacheEntries.remove();
					removedDueToOld++;
					continue;
				}

				//System.out.println(Fmt.S("id [%s] last accessed [%s]", cacheEntry.getValue().tx().id(), ((CachedTx)cacheEntry.getValue().tx())._lastAccessed));
			}

			if (Log.jcache.getLevel() == java.util.logging.Level.FINE) {
				Log.jcache.fine(Fmt.S("Avg hits [%s] Avg LastAccessed [%s sec]",averageCachedHits,averagelastAccessed));
				Log.jcache.fine(Fmt.S("Removed [%s] entries that are older than [%s sec]", removedDueToOld, _cachedRecordStore._maxToLiveTimeInSec));
				Log.jcache.fine(Fmt.S("Removed [%s] entries that are garbage collected", removedDueToGC));
			}


			int requiredCount = (int) (_cachedRecordStore._meta._maxAllowedCacheSize * _cachedRecordStore._loadFactor);
			int sizeToDelete = copyOfScannedEntries.size() - requiredCount;

				// clean phase,  sorted in lastAccessed order then perform clean phase
			T[] sortedByLastAccessed = (T[])copyOfScannedEntries.toArray((T[])new Record[copyOfScannedEntries.size()]);
			Arrays.sort(sortedByLastAccessed, LastAccessedComparator.Instance);
			_currentCacheSize = cleanMarkedCacheEntries(sortedByLastAccessed, sizeToDelete);


			if (Log.jcache.getLevel() == java.util.logging.Level.FINE) {
				Log.jcache.fine(Fmt.S("Ran Purger for [%s] with post cache size [%s]", _timer.markTime(),  _currentCacheSize));
			}
		}

		public void updateCacheStat (CachedTx tx)
		{
			tx._cacheHits++;
			tx._lastAccessed = _currentTimeInSec;
		}
	}

	// A version of CachePurgerTask that can handle GarbageCollectableRecord
	public static class GCCachePurgerTask<T extends Record> extends CachePurgerTask<T>
	{
			// only runs 1 in every specified time compare to regular CachePurgerTask
		private static final int RunInMultipleOfWakeIntervalInSec = 5;


		public GCCachePurgerTask (CachedRecordStore cachedRecordStore)
		{
			super(cachedRecordStore);
		}

		@Override
		public void putToCache (long id, T cachedRecord)
		{
			if (_currentCacheSize < _cachedRecordStore._meta._maxAllowedCacheSize) {
				if (_cachedRecordStore._cache.put(id, cachedRecord)) {
					_currentCacheSize++;
				}
			}
			else {
				if (_cachedRecordStore._cache.put(id, (T)new GarbageCollectableRecord(cachedRecord))) {
					_currentCacheSize++;
				}
			}

		}

		@Override
		public T getFromCache (long id)
		{
			return getFromCacheEntry(_cachedRecordStore._cache.get(id));
		}

		@Override
		public T getFromCacheEntry (T cachedRecord)
		{
				// return the actual cache object
			if (cachedRecord == null || !(cachedRecord instanceof GarbageCollectableRecord)) {
				return cachedRecord;
			}
			else {
				return (T)((GarbageCollectableRecord)cachedRecord).get();
			}
		}

		@Override
		public int cleanMarkedCacheEntries (T[] cacheEntries, int sizeToDelete)
		{
			int finalGCRecordCount = 0;
			int finalRefRecordCount = 0;
			int gcConvertedCount = 0;
			int refConvertedCount = 0;
			int gcCountWhileClean = 0;

			int i = 0;
			for (; i < cacheEntries.length; i++) {
				T cacheEntry = cacheEntries[i];
				if (cacheEntry == null) {
					continue;
				}
				T record = getFromCacheEntry(cacheEntry);
				if (record != null) {
					if (i < sizeToDelete) {
							//downgrade to GC'able cache record
						if (!(cacheEntry instanceof GarbageCollectableRecord)) {
							_cachedRecordStore._cache.replace(record.tx()._id, (T)new GarbageCollectableRecord(record));
							gcConvertedCount++;
						}
						finalGCRecordCount++;

					}
					else {
							// upgrade to regular cache record
						if (cacheEntry instanceof GarbageCollectableRecord) {
							_cachedRecordStore._cache.replace(record.tx()._id, record);
							refConvertedCount++;
						}
						finalRefRecordCount++;
					}
				}
				else {
						// it will be cleaned by next run..
					gcCountWhileClean++;
					//_cachedRecordStore._cache.remove(cacheEntry.tx()._id);
				}
			}

			if (Log.jcache.getLevel() == java.util.logging.Level.FINE) {
				Log.jcache.fine(Fmt.S("%s POST Snapshot: [%s Ref] [%s SoftRef] [%s Ref Upgrade] [%s SoftRef Downgrade] %s",
					_cachedRecordStore._cache.getClass().getSimpleName(), finalRefRecordCount, finalGCRecordCount, refConvertedCount, gcConvertedCount, ""));
			}

			if (gcCountWhileClean > 0) {
				Log.jcache.fine(Fmt.S("[%s] Expected entry was GC at same time as clean phase.", gcCountWhileClean));
			}

			return cacheEntries.length;
		}

		int counter = 0;
		@Override
		protected void purge ()
		{
				// don't need to run as aggressive as regular CachePurgerTask since
				// 1) got GCCacheEntry to hold, 2) more expensive to go through entire list
			if (++counter % RunInMultipleOfWakeIntervalInSec == 0) {
				counter = 0;
				super.purge();
			}
		}
	}

		// extend version of Tx that adds cache info to each record
	public static class CachedTx<T extends Record> extends Tx<T>
	{
		public static final CachedTx InvalidTx = new CachedTx(null);


			// concurrent updates..
		volatile short _cacheHits;
		volatile short _lastAccessed; // in seconds

		CachedTx (T record)
		{
			super(record);
		}

		public short getCacheHits ()
		{
			return _cacheHits;
		}

		public void incrementHit ()
		{
			_cacheHits++;
		}


		@Override
		protected void uncachedRead (long id, T toRecord)
		{
			((CachedRecordStore)_record.meta().store()).superRead(id, _record, true);
		}
	}


		// Sorts the most unwanted cache to the tops
	public static class LastAccessedComparator<T extends Record> implements Comparator<T>
	{
		public static final LastAccessedComparator Instance = new LastAccessedComparator();

		public int compare(T p1, T p2) {

			CachedTx o1 = (CachedTx)p1.tx();
			CachedTx o2 = (CachedTx)p2.tx();

				// could be InvalidTx for GCRecord
			if (o1 == CachedTx.InvalidTx) {
				return -1;
			}
			if (o2 == CachedTx.InvalidTx) {
				return 1;
			}

			if (o1._lastAccessed < o2._lastAccessed) {
				return -1;
			}
			else {
				if (o1._lastAccessed > o2._lastAccessed) {
					return 1;
				}
				else {
					if (o1._cacheHits < o2._cacheHits) {
						return -1;
					}
					else {
						if (o1._cacheHits > o2._cacheHits) {
							return 1;
						}
						else {
							if (o1.id() < o2.id()) {
								return -1;
							}
							else {
								if (o1.id() > o2.id()) {
									return 1;
								}
								else {
									return o1.hashCode() > o2.hashCode() ? 1 : -1;
								}
							}
						}
					}
				}
			}
		}
	}


	public static class MostHitsComparator<T extends Record> implements Comparator<T>
	{
		public static final MostHitsComparator Instance = new MostHitsComparator();

		public int compare(T p1, T p2) {

			CachedTx o1 = (CachedTx)p1.tx();
			CachedTx o2 = (CachedTx)p2.tx();

				// could be InvalidTx for GCRecord
			if (o1 == CachedTx.InvalidTx) {
				return -1;
			}
			if (o2 == CachedTx.InvalidTx) {
				return 1;
			}

			if (o1._cacheHits > o2._cacheHits) {
				return -1;
			}
			else {
				if (o1._cacheHits < o2._cacheHits) {
					return 1;
				}
				else {
					if (o1._lastAccessed > o2._lastAccessed) {
						return -1;
					}
					else {
						if (o1._lastAccessed < o2._lastAccessed) {
							return 1;
						}
						else {
							if (o1.id() > o2.id()) {
								return -1;
							}
							else {
								if (o1.id() < o2.id()) {
									return 1;
								}
								else {
									return o1.hashCode() < o2.hashCode() ? 1 : -1;
								}
							}
						}
					}
				}
			}
		}
	}
}

// an extend version of Record that allows actual "record" to be GC'ed
class GarbageCollectableRecord<T extends Record> extends WeakReference<T> implements Record
{

	public GarbageCollectableRecord(T record)
    {
    	super(record);
    }

	public Tx tx()
	{
		Record record = get();
		return record == null ? CachedRecordStore.CachedTx.InvalidTx :record.tx();
	}

	public Meta meta()
	{
		throw new RuntimeException("Not supported");
	}
}


	// backed by ConcurrentHashMap - slow but can hold arbitrary size and grow on demand
class MapCache<T>
{
	private volatile ConcurrentHashMap<Long, T> _cache;
	private CachedRecordStore _cacheStore;

	public MapCache (CachedRecordStore cacheStore)
	{
		_cache = new ConcurrentHashMap(_cacheStore._meta._initialCacheSize, 0.85f, _cacheStore._meta._initialCacheSize);
		_cacheStore = cacheStore;
	}

	public boolean put (long id, T t)
	{
		_cache.put(id, t);
		return true;
	}

	public void replace (long id, T t)
	{
		_cache.replace(id, t);
	}

	public T get (long id)
	{
		return _cache.get(id);
	}

	public void remove (long id)
	{
		_cache.remove(id);
	}

	public int size ()
	{
		return _cache.size();
	}

	public Iterator<T> values ()
	{
		return _cache.values().iterator();
	}

	public void clear ()
	{
		_cache.clear();
	}

    public void ensureCapacity ()
    {
    	// no op for map
    }

	@Override
	public String toString ()
	{
		int gc = 0;
		int gcEmpty = 0;
		int ref = 0;
		int empty = 0;
		for (Iterator i = values(); i.hasNext();) {
			T t = (T)i.next();
			if (t == null) {
				empty++;
			}
			else {
				if (t instanceof GarbageCollectableRecord) {
					GarbageCollectableRecord gcr = (GarbageCollectableRecord)t;
					if (gcr.get() == null) {
						gcEmpty++;
					}
					else {
						gc++;
					}
				}
				else {
					ref++;
				}
			}
		}
		return Fmt.S("[%s Ref] [%s SoftRef] [%s SoftRef nil] [%s nil] %s", ref, gc, gcEmpty, empty, "");
	}


}

	// backed by Array with random access, fast but  cache container (array)
	// will keep growing which could have unused elements.
	// good if size of the entire record collection is known
	// also - when request element id is greater the cache length, it won't
	// immediate cache but will grow on the next "wake" interval
class ArrayCache<T>
{
	private volatile AtomicReferenceArray<T> _cache;
	private CachedRecordStore _cacheStore;
	private float _physicalSizeLimitBeforeGrow;
	private volatile long _highestRequestedSlot;

	private final float DefaultGrowFactor = 1.20f;
	private final float DefaultLoadFactor = 0.90f;


	public ArrayCache (CachedRecordStore cacheStore)
	{
		_cacheStore = cacheStore;
		_cache = new AtomicReferenceArray(Math.min(_cacheStore._meta._initialCacheSize, _cacheStore._meta._maxAllowedCacheSize));
		_physicalSizeLimitBeforeGrow = _cacheStore._meta._initialCacheSize * DefaultLoadFactor;
		_highestRequestedSlot = -1;
	}

	public boolean put (long id, T t)
	{
		if (!validRange(id)) return false;
		_cache.set((int)id, t);
		return true;
	}

	public void replace (long id, T t)
	{
		if (!validRange(id)) return;
		_cache.set((int)id, t);
	}


	public T get (long id)
	{
		if (!validRange(id)) return null;
		return _cache.get((int)id);
	}

	public void remove (long id)
	{
		if (!validRange(id)) return;
		_cache.set((int)id, null);
	}

		// any slot (id) request beyond physical size is ignored
		// it will be grow on next awake of the purger thread
	public boolean validRange (long id)
	{
		if (id > _highestRequestedSlot) {
			_highestRequestedSlot = id;
		}
		return id < _cache.length() && id >= 0;
	}

	public int size ()
	{
		int size = 0;
		for (int i = 0, cacheLength = _cache.length(); i < cacheLength; i++) {
			if (_cache.get(i) != null) size++;
		}
		return size;
	}

	public Iterator<T> values ()
	{
		return new ArrayCacheIterator(_cache);
	}

	public void clear ()
	{
		clearInternal(_cache);
	}

	private void clearInternal (AtomicReferenceArray<T> cache)
	{
		for (int i = 0, cacheLength = cache.length(); i < cacheLength; i++) {
			cache.set(i, null);
		}
	}

		// this gets call every _wakeIntervalInSec second by purge thread
		// idea is to grow the physical array size to fit.
		// _highestRequestSlot is being record on each cache rw operation
		// if it goes beyond the limit we increase by DefaultGrowFactor of the physical array size
		// and copy over to new ones
    public void ensureCapacity ()
    {
    		// read from volatile var and cast to int
    	int highestRequestedSlot = (int)_highestRequestedSlot;

    	if (highestRequestedSlot > _physicalSizeLimitBeforeGrow) {

    			// pick biggest base and times grow factor
    	    int newCapacity = highestRequestedSlot > _cache.length()
    	    	? (int)(highestRequestedSlot * DefaultGrowFactor)
    	    	: (int)(_cache.length() * DefaultGrowFactor);

    	    AtomicReferenceArray newCache = new AtomicReferenceArray(newCapacity);

    	    for (int i = 0, cacheLength = _cache.length(); i < cacheLength; i++) {
    			newCache.set(i, _cache.get(i));
    		}

       		Log.jcache.fine(Fmt.S("Adjust [%s] ArrayCache from [%s] to [%s], Highest Request Slot [%s], SizeLimitBeforeGrow [%s], new SizeLimitBeforeGrow [%s]",
       				_cacheStore._meta._clazz.getSimpleName(), _cache.length(), newCapacity, highestRequestedSlot,_physicalSizeLimitBeforeGrow, newCapacity * DefaultLoadFactor));

    	    _physicalSizeLimitBeforeGrow = newCapacity * DefaultLoadFactor;
    	    AtomicReferenceArray oldCache = _cache;
    		_cache = newCache; // assignment on volatile array is thread safe
    		clearInternal(oldCache); // just be safe
    	}
    }

	@Override
	public String toString ()
	{
		int gc = 0;
		int gcEmpty = 0;
		int ref = 0;
		int empty = 0;
		for (int i = 0, cacheLength = _cache.length(); i < cacheLength; i++) {
			T t = (T)_cache.get(i);
			if (t == null) {
				empty++;
			}
			else {
				if (t instanceof GarbageCollectableRecord) {
					GarbageCollectableRecord gcr = (GarbageCollectableRecord)t;
					if (gcr.get() == null) {
						gcEmpty++;
					}
					else {
						gc++;
					}
				}
				else {
					ref++;
				}
			}
		}
		return Fmt.S("[%s Ref] [%s SoftRef] [%s SoftRef nil] [%s nil] %s", ref, gc, gcEmpty, empty, "");
	}


	public static class ArrayCacheIterator<T> implements Iterator
	{
		AtomicReferenceArray<T> _ac;
		int _cursor;
		T _entry;

		public ArrayCacheIterator (AtomicReferenceArray ac)
		{
			_ac = ac;
		}


	    public boolean hasNext()
	    {
	    	while (	_cursor < _ac.length()) {
	    		_entry = _ac.get(_cursor++);
	    		if (_entry != null) {
	    			return true;
	    		}
	    	}
	    	return false;
	    }

	    public T next()
	    {
	    	return _entry;
	    }

	    public void remove()
	    {
	    	_ac.set(_cursor - 1, null);
	    }
	}
}

	// near random access if maxAllowedCachedSize is equal to the entire collection size
	// any record that can not fit in the array is pushed to map. fast and best memory usage
class CompositeCache<T extends Record>
{
	private volatile AtomicReferenceArray<T> _L1Cache;
	private volatile ConcurrentHashMap<Long, T> _L2Cache;
	private CachedRecordStore _cacheStore;

	public CompositeCache (CachedRecordStore cacheStore)
	{
		_cacheStore = cacheStore;
		_L1Cache = new AtomicReferenceArray(Math.min(_cacheStore._meta._initialCacheSize, _cacheStore._meta._maxAllowedCacheSize));
		_L2Cache = new ConcurrentHashMap(64, 0.85f, 64);
	}


	public int arrayHashId (long id)
	{
		return (int)(id % _L1Cache.length());
	}

	public boolean put (long id, T entry)
	{
		int hashId = arrayHashId(id);
		T t = _L1Cache.get(hashId);
		if (t == null) {
			_L1Cache.set(hashId, entry);
		}
		else {
			_L2Cache.put(id, entry);
		}
		return true;
	}

	public void replace (long id, T entry)
	{
		int hashId = arrayHashId(id);
		T t = _L1Cache.get(hashId);
		if (t != null && t.tx()._id == id) {
			_L1Cache.set(hashId, entry);
		}
		else {
			_L2Cache.replace(id, entry);
		}
	}

	public T get (long id)
	{
		int hashId = arrayHashId(id);
		T t = _L1Cache.get(hashId);
		if (t != null && t.tx()._id == id) {
			return t;
		}
		return _L2Cache.get(id);
	}

	public void remove (long id)
	{
		int hashId = arrayHashId(id);
		T t = _L1Cache.get(hashId);
		if (t != null && t.tx()._id == id) {
			_L1Cache.set(hashId, null);
		}
		else {
			_L2Cache.remove(id, t);
		}
	}

	public Iterator<T> values ()
	{
		return new CompositeCacheIterator(this);
	}

	public int size ()
	{
		int size = 0;
		for (int i = 0, cacheLength = _L1Cache.length(); i < cacheLength; i++) {
			if (_L1Cache.get(i) != null) size++;
		}
		return size + _L2Cache.size();
	}

	public void clear ()
	{
		for (int i = 0, cacheLength = _L1Cache.length(); i < cacheLength; i++) {
			_L1Cache.set(i, null);
		}
		_L2Cache.clear();
	}

    public void ensureCapacity ()
    {
    	// no-op
    }

	@Override
	public String toString ()
	{
		int gc = 0;
		int gcEmpty = 0;
		int ref = 0;
		int empty = 0;
		for (int i = 0, cacheLength = _L1Cache.length(); i < cacheLength; i++) {
			T t = (T)_L1Cache.get(i);
			if (t == null) {
				empty++;
			}
			else {
				if (t instanceof GarbageCollectableRecord) {
					GarbageCollectableRecord gcr = (GarbageCollectableRecord)t;
					if (gcr.get() == null) {
						gcEmpty++;
					}
					else {
						gc++;
					}
				}
				else {
					ref++;
				}
			}
		}
		for (Iterator i = _L2Cache.values().iterator(); i.hasNext();) {
			T t = (T)i.next();
			if (t == null) {
				empty++;
			}
			else {
				if (t instanceof GarbageCollectableRecord) {
					GarbageCollectableRecord gcr = (GarbageCollectableRecord)t;
					if (gcr.get() == null) {
						gcEmpty++;
					}
					else {
						gc++;
					}
				}
				else {
					ref++;
				}
			}
		}
		return Fmt.S("[%s Ref] [%s SoftRef] [%s SoftRef nil] [%s nil] %s", ref, gc, gcEmpty, empty, "");
	}


	public static class CompositeCacheIterator<T extends Record> implements Iterator<T>
	{
		ArrayCacheIterator<T> _L1CacheIterator;
		Iterator<T> _L2CacheIterator;


		public CompositeCacheIterator (CompositeCache<T> cc)
		{
			_L1CacheIterator = new ArrayCacheIterator(cc._L1Cache);
			_L2CacheIterator = cc._L2Cache.values().iterator();
		}

	    public boolean hasNext()
	    {
	    	while (_L1CacheIterator != null && _L1CacheIterator.hasNext()) {
	    		return true;
	    	}
	    	_L1CacheIterator = null;
	    	return _L2CacheIterator.hasNext();
	    }

	    public T next()
	    {
	    	return _L1CacheIterator != null ?  _L1CacheIterator.next() : _L2CacheIterator.next();
	    }

	    public void remove()
	    {
	    	if (_L1CacheIterator != null) {
	    		_L1CacheIterator.remove();
	    	}
	    	else {
	    		_L2CacheIterator.remove();
	    	}
	    }
	}
}


/*
class Test
{
	private static List list = new Vector();

	public static void useMemory ()
	{
		list.add(new byte[1024 * 1024 * 1]);
		System.out.println("XXX adding");
	}
}
*/

