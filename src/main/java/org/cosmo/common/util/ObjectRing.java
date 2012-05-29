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
package org.cosmo.common.util;

import java.lang.reflect.Array;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;


public class ObjectRing<T>
{
	private AtomicInteger _cursor;
	private AtomicReferenceArray<T> _objects;	
	private int _size;
	
	public ObjectRing (Class<T> objectClass, int size)
	{
		_objects = new AtomicReferenceArray(size);
		_cursor = new AtomicInteger(0);
	}
	
	
	public void insert (T object)
	{
		//_objects[_cursor.next()] = object;
		/*
		 while(true) {
			int currentCursor = _cursor.get() ;
			int newCursor = (currentCursor + 1) % _size;
			T currentObject = _objects.get(currentCursor);
			if ()
					
					_cursor.compareAndSet(currentCursor, newCursor)) {
				_objects[newCursor] = object;
				break;
			}
		 }
		 */
		
	}
	
}
