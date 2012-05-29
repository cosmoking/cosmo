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

import org.cosmo.common.util.Bytes;

public class BytesPool extends ObjectPool<Bytes>
{

	public static final BytesPool Instance = new BytesPool(Bytes.class, 4);

	private BytesPool (Class<Bytes> clazz, int size)
	{
		super(clazz, size);
	}

	@Override
	public Bytes newInstance ()
	{
		return new Bytes();
	}

	@Override
	public Bytes recycleInstance (Bytes usedInstance)
	{
		return usedInstance.reset();
	}

	@Override
	public boolean isReadyForRecycle (Bytes usedInstance)
	{
		return true; //return usedInstance.finished();
	}

	@Override
	public Bytes getInstanceButFullPool(ObjectPool<Bytes> pool)
	{
		return new Bytes();
	}
}
