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


public abstract class Listener
{

	public Defn _defn;
	public Listener _next;


	public Listener ()
	{
	}

	public void notify (Object value, long id)
	{
		passThrough(value, id);
		//New.prt("Notify [", getClass().getSimpleName(), "] for [", _defn.getDeclaringFieldName(), "] at id [", id , "] with value [", value, "]");
		if (_next != null) {
			_next.notify(value, id);
		}
	}

	public FieldCache fieldCache()
	{
		return (FieldCache)this;
	}


	public GroupByList groupByList()
	{
		return (GroupByList)this;
	}


	abstract public int sync (int maxCounts)
	  throws IOException;


	abstract public void passThrough (Object value, long id);

}
