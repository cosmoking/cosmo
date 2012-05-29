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
package org.cosmo.common.statistics;

import java.util.Date;
import java.util.List;

abstract public class HitStore
{

	public abstract boolean create (Category category, Key key, Hits hits, Object context);

	public abstract boolean update (Category category, Key key, Hits hits, Object updateThisEntry, Object context);

	public abstract List<HitEntry> getHitEntries (Category category, Date from, Date to, Key key, boolean forUpdate, boolean full, Object context);

	public abstract int getNextUniqueCategoryID ();

	public abstract Object newContext();

	public abstract KeyResolver getKeyResolver(Category category);
}
