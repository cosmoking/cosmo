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
package org.cosmo.common.message;

import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class PubSubCenter<T, M, S extends Subscriber<T, M>>
{
	
	static final int MaxSubscriberSize = 10000; // for now
	static final int NewSubscribersPoolSize = 8;
	
	ConcurrentHashMap<T, AtomicReferenceArray<S>> _subscribersMap = new ConcurrentHashMap();
	Stack<AtomicReferenceArray<S>> _newSubscribersPool = new Stack();
		
	public PubSubCenter ()
	{
		for (int i = 0; i < NewSubscribersPoolSize; i++) {
			_newSubscribersPool.add(new AtomicReferenceArray(MaxSubscriberSize));
		}
	}
	
	
	public void publish (T topic, M message)
	{
        System.out.println(message);	
		AtomicReferenceArray<S> subscribers = _subscribersMap.get(topic);
		if (subscribers == null) {
			return;
		}
		beforePublish(topic, message);
		for (int i = 0; i < MaxSubscriberSize; i++) {
			S subscriber = subscribers.get(i);
			if (subscriber != null) {
				if (subscriber._isActive.get()) {
					subscriber.onMessage(topic, message);
				}
			}
			else {
				return;
			}
		}		
	}
	
	
	protected void beforePublish (T topic, M message)
	{
		
	}
	

	public boolean subscribe (T topic, S newSubscriber)
	{
		if (newSubscriber == null) {
			throw new IllegalArgumentException("subscriber can not be null");
		}
			// lazy init subscriber list
		AtomicReferenceArray<S> newSubscribers = _newSubscribersPool.pop();
		AtomicReferenceArray<S> previousSubscribers = _subscribersMap.putIfAbsent(topic, newSubscribers);
		_newSubscribersPool.push(previousSubscribers == null ? new AtomicReferenceArray(MaxSubscriberSize) : newSubscribers);
		AtomicReferenceArray<S> subscribers = previousSubscribers == null ? newSubscribers: previousSubscribers;
		
		beforeSubscribe(previousSubscribers == null, topic, newSubscriber);
		
		for (int i = 0; i < MaxSubscriberSize; i++) {
			S aSubscriber = subscribers.get(i);
				// if subscriber already subscribed in this channel
			if (aSubscriber != null) {
				if (aSubscriber.equals(newSubscriber)) {
					return false;
				}
			}
				// find first non empty spot and subscribe
			else if (aSubscriber == null) {
				if (subscribers.compareAndSet(i, null, newSubscriber)) {
					return true;
				}
			}
				//  detect an inactive user, take it' spot
			else if (!aSubscriber._isActive.get()) {
				if (subscribers.compareAndSet(i, aSubscriber, newSubscriber)) {
					return true;
				}
			}
		}
		throw new IllegalStateException("Can not accept any more subscribers");
	}
	
	
	protected void beforeSubscribe (boolean newTopic, T topic, S newSubscriber)
	{
		
	}
	

	public void unsubscribe (T topic, S existingSubscriber)
	{
		if (existingSubscriber == null) {
			throw new IllegalArgumentException("subscriber can not be null");
		}		
		AtomicReferenceArray<S> subscribers = _subscribersMap.get(topic);
		for (int i = 0; i <  MaxSubscriberSize; i++) {
			if (subscribers.compareAndSet(i, existingSubscriber, null)) {
				return;
			}
		}
	}	
}
 

 
