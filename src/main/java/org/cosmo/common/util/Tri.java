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

public class Tri<T1, T2, T3>
{
	public T1 _t1;
	public T2 _t2;
	public T3 _t3;

	public Tri ()
	{
	}

	public Tri (T1 t1, T2 t2, T3 t3)
	{
		_t1 = t1;
		_t2 = t2;
		_t3 = t3;
	}
}
