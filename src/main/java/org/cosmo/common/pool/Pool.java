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
package org.cosmo.common.pool;

import java.lang.reflect.Array;
import java.util.concurrent.atomic.AtomicInteger;

import org.cosmo.common.util.New;




public class Pool<T>
{
	AtomicInteger _nextAvailableObjectSlot = new AtomicInteger(0);
	Factory<T> _factory;
	T[] _objects;


	public Pool (Factory<T> factory, T[] objects)
	{
		_factory = factory;
		_objects = objects;
	}


	public Pool (Factory<T> factory, int maxObjectPoolSize)
	{
		this (factory, (T[])Array.newInstance(factory.getClassType(), maxObjectPoolSize));
	}


	public T getInstance (Object... args)
	{
			// couple things:  each thread will enter and get an unique slot number
			// if it's null, great, lazy init and create a new object and put in the Objects Array for subsequent retrievel
			// if has a value then test if it has expired, if it has, claim the slot and create a new Object and assign to slot
			// iterate this until the whole Objects array has been tried

		for (int i = 0, slot = -1; i < _objects.length; i++) {
				// each thread is guarantee to get an unique slot number with upper bound at MaxObjectPoolSize
				// ie operating at it's unique array slot
	        for (;;) {
	            int current = _nextAvailableObjectSlot.get();
	            int next = (current + 1) % _objects.length;
	            if (_nextAvailableObjectSlot.compareAndSet(current, next)) {
	              slot = current;
	              break;
	            }
	        }

			if (_objects[slot] == null) {
				return _objects[slot] = _factory.newInstance(slot, args);
			}
			else if (_factory.isReadyForRecycle(_objects[slot])) {
				return _objects[slot] = _factory.recycleInstance(_objects[slot], slot, args);
			}

		}
		return _factory.getInstanceButFullPool(this);
	}

	public T getInstance ()
	{
		return getInstance((Object[])null);
	}



	public static interface Factory<T>
	{

		public Class<T> getClassType();

		// create new
		public T newInstance(int slot, Object... args);

			// recycle used and used the returned
		public T recycleInstance (T usedInstance, int slot, Object... args);

			// check if recycle
		public boolean isReadyForRecycle (T usedInstance);

			// return new, throw exception, sleep, retry loop
		public T getInstanceButFullPool(Pool<T> pool);


		public static class Default<T> implements Factory<T>
		{
			Class<T> _clazz;

			public Class<T> getClassType ()
			{
				return _clazz;
			}

			public Default (Class<T> clazz)
			{
				_clazz = clazz;
			}


			public T newInstance(int slot, Object... args)
			{
				try {
					return (T)_clazz.newInstance();
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}


				// recycle used and used the returned
			public T recycleInstance (T usedInstance, int slot, Object... args)
			{
				return newInstance(slot, args);
			}



			// check if recycle
			public boolean isReadyForRecycle (T usedInstance)
			{
				return true;
			}


			// return new, throw exception, sleep, retry loop
			public T getInstanceButFullPool(Pool<T> pool)
			{
				System.err.print(New.str("Pool ", this.getClass().getSimpleName(), " with class ", _clazz.getSimpleName(), "is full retrying..."));
				return pool.getInstance();
			}
		}
	}
}











