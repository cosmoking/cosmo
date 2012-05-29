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

import org.cosmo.common.net.Session;


/*
 * Session are not recycled but instead a new one is always returned.
 * The pool is used to make sure number of session is bounded and
 * that Sessions[] is available to be referenced out side of the pool
 *
 * For example, new thread claims a free slot in the pool (empty or expired),
 * later it can call Sessions[slot] to get the session.
 *
 */
public class SessionPool extends ObjectPool<Session>
{
	public static final int MaxSessionPoolSize = 8192;
	public static Session[] Sessions = new Session[MaxSessionPoolSize];

	public static final SessionPool Instance = new SessionPool(Session.class, Sessions);


	private SessionPool (Class<Session> clazz, Session[] objects)
	{
		super(clazz, objects);
	}

	@Override
	public Session newInstance (int slot, byte[] ip, int port)
	{
		return new Session(slot, ip, port);
	}

	@Override
	public Session recycleInstance (int slot, Session usedInstance, byte[] ip, int port)
	{
		return new Session(slot, ip, port);
	}

	@Override
	public boolean isReadyForRecycle (Session usedInstance)
	{
		return usedInstance.isExpired();
	}

	@Override
	public Session getInstanceButFullPool(ObjectPool<Session> pool)
	{
		throw new RuntimeException("No more Session, server busy");
	}
}

