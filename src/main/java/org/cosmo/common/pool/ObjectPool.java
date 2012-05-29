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


abstract public class ObjectPool<T>
{
	AtomicInteger _nextAvailableObjectSlot = new AtomicInteger(0);
	T[] _objects;
	Class<T> _clazz;

	public ObjectPool (Class<T> clazz, T[] objects)
	{
		_clazz = clazz;
		_objects = objects;
	}

	public ObjectPool (Class<T> clazz, int maxObjectObjectPoolSize)
	{
		this (clazz, (T[])Array.newInstance(clazz, maxObjectObjectPoolSize));
	}

	abstract public boolean isReadyForRecycle (T usedInstance);
	abstract public T getInstanceButFullPool(ObjectPool<T> pool);


	// Default
	public T newInstance ()
	{
		throw new UnsupportedOperationException();
	}

	public T recycleInstance (T usedInstance)
	{
		throw new UnsupportedOperationException();
	}

	public T getInstance ()
	{
		for (int i = 0, slot = -1; i < _objects.length; i++) {

				// iterate until it finds an open slot in the pool, each thread is gurantee to get unique spot
	        for (;;) {
	            int current = _nextAvailableObjectSlot.get();
	            int next = (current + 1) % _objects.length;
	            if (_nextAvailableObjectSlot.compareAndSet(current, next)) {
	              slot = current;
	              break;
	            }
	        }
	        	// lazy init
			if (_objects[slot] == null)
				return _objects[slot] = newInstance();
			if (isReadyForRecycle(_objects[slot]))
				// instance is recycled and reassigned back to the same spot
				// it's likely different object is returned which would make
				// the existing object de-referenced from array and GC'ed
				// why? because some object is simply to expensive and hard to recycle
				// let each impl decide what they want to return
				return _objects[slot] = recycleInstance(_objects[slot]);
		}
			// couple thing can happen, return new, sleep, throw exception, call getInstance() again
		return getInstanceButFullPool(this);
	}


	// String, int
	public T newInstance (int slot, byte[] arg1, int arg2)
	{
		throw new UnsupportedOperationException();
	}

	public T recycleInstance (int slot, T usedInstance, byte[] arg1, int arg2)
	{
		throw new UnsupportedOperationException();
	}

	public T getInstance (byte[] arg1, int arg2)
	{
		for (int i = 0, slot = -1; i < _objects.length; i++) {
	        for (;;) {
	            int current = _nextAvailableObjectSlot.get();
	            int next = (current + 1) % _objects.length;
	            if (_nextAvailableObjectSlot.compareAndSet(current, next)) {
	              slot = current;
	              break;
	            }
	        }
			if (_objects[slot] == null)
				return _objects[slot] = newInstance(slot, arg1, arg2);
			if (isReadyForRecycle(_objects[slot]))
				return _objects[slot] = recycleInstance(slot, _objects[slot], arg1, arg2);
		}
		return getInstanceButFullPool(this);
	}





}

