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

import java.util.concurrent.atomic.AtomicBoolean;

public class Subscriber<T, M>
{
 	public AtomicBoolean _isActive;
 	 	
 	public Subscriber () {
 		_isActive = new AtomicBoolean(true);
 	}
 	
	
 	public void onMessage (T topic, M message) {
 		throw new IllegalStateException("NOT IMPLEMENTED");
 	}	
}
