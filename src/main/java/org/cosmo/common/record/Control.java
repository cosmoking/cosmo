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

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.cosmo.common.util.New;


public class Control
{
	private static final Map<Class, List<Haltable>> LockMap= new LinkedHashMap();
	static {
			// THIS IS THE ORDER IN WHICH HALT IS CALLED - BE SURE IN RIGHT ORDER OTHER WISE DEADLOCK!!
		LockMap.put(RecordStore.class, new ArrayList());
		LockMap.put(CachedRecordStore.class, new ArrayList());

		LockMap.put(Index.class, new ArrayList());
		LockMap.put(ReadOnlyIndex.class, new ArrayList());

		LockMap.put(RecordLog.class, new ArrayList());
	}

		// return the lock and keep in the list
	public synchronized static Lock haltableLock (Control.Haltable instance, String name)
	{
		List<Haltable> haltableList = LockMap.get(instance.getClass());
		if (haltableList == null) {
			throw new IllegalArgumentException(New.str("Invalid class ", instance.getClass()));
		}
		Lock lock = new LockImpl(name);
		haltableList.add(instance);
		return lock;
	}



	public static synchronized void prepareShutdown ()
	{
		for (Map.Entry<Class, List<Haltable>> entry: LockMap.entrySet()) {

			Class classType = entry.getKey();
			List<Haltable> haltableList = entry.getValue();

			if (!haltableList.isEmpty()) {
				New.prt("*** Starting shutdown process for class type [", classType.getSimpleName(), "]");
				for (Haltable haltable : haltableList) {
					Lock lock = haltable.halt();
					lock.lock();
					New.prt("****** Stopped [", lock.name(), "]");
				}
			}
		}
	}

	public static interface Lock extends java.util.concurrent.locks.Lock
	{
		public String name ();
	}

	public static class LockImpl extends java.util.concurrent.locks.ReentrantLock implements Control.Lock
	{
		public final String _name;

		public LockImpl (String name)
		{
			super();
			_name = name;
		}

		@Override
		public String name ()
		{
			return _name;
		}
	}


	final static File Lock;
	static {
		Lock = new File(Meta.Mode._dir, "recordLog.lock");
	}

	public static interface Haltable {

		public Control.Lock halt ();
	}


	/*
	private void haltIfLocked ()
	{
		while (Lock.exists()) {
			util.Util.sleep(LockReleaseRetryInternval);
			New.prt("RecordLog Locked");
		}
	}
	*/



}
