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

import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ScheduledThreadPoolExecutor;


public class ScheduledUniqueTaskExecutor<T extends Runnable>
{

	private ScheduledThreadPoolExecutor _executor;
	private TreeMap _tasks;

    public ScheduledUniqueTaskExecutor(int corePoolSize)
    {
    	_executor = new ScheduledThreadPoolExecutor(corePoolSize);
    }

    public boolean submitTask (Object key, T task)
    {


    	if (_tasks.containsKey(key)) {
    		return false;
    	}
    	else {
	    	_tasks.put(key, task);
	    	_executor.submit(task);
	    	return true;
    	}
    }



}
