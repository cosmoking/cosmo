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

import java.util.zip.Adler32;
import java.util.zip.Checksum;

import org.cosmo.common.util.Bytes;
import org.cosmo.common.util.Util;

public class ChecksumPool<T extends Checksum> extends ObjectPool<T>
{

	public static final ChecksumPool<Checksum> Instance = new ChecksumPool(ChecksumImpl.class, 4);

	private ChecksumPool (Class<T> clazz, int size)
	{
		super(clazz, size);
	}

	@Override
	public T newInstance ()
	{
		return (T)new ChecksumImpl();
	}

	@Override
	public T recycleInstance (T usedInstance)
	{
		((ChecksumImpl)usedInstance).init();
		return usedInstance;
	}

	@Override
	public boolean isReadyForRecycle (T usedInstance)
	{
		return ((ChecksumImpl)usedInstance)._finished;
	}

	@Override
	public T getInstanceButFullPool(ObjectPool<T> pool)
	{
		return (T)new ChecksumImpl();
	}

	public long generateChecksum (byte[] b, int off, int len)
	{
		Checksum instance = Instance.getInstance();
		instance.update(b, off, len);
		long checksum = instance.getValue();
		instance.reset();
		return checksum;
	}

	public long generateChecksum (CharSequence data)
	{
		byte[] bytes = Util.bytes(data);
		return generateChecksum(bytes, 0, bytes.length);
	}

	public long generateChecksum (Bytes bytes)
	{
		return generateChecksum(bytes.bytes(), 0, bytes.count());
	}
}

class ChecksumImpl extends Adler32
{
	public volatile boolean _finished;


	public ChecksumImpl ()
	{
		super();
		init();
	}

	public void reset ()
	{
		super.reset();
		_finished = true;
	}

	public void init ()
	{
		_finished = false;
	}
}


