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

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.cosmo.common.util.DateTools;



public interface Record <T extends Record>
{
	abstract public Tx<T> tx();

	abstract public Meta<T> meta();

	public static final Boolean T = true;
	public static final Boolean F = false;

	public static class Static
	{
		public static long[] idsArray (int size)
		{
			long[] ids = new long[size];
			for (int i = 0; i < size; i++) {
				ids[i] = i;
			}
			return ids;
		}

		public static boolean hasSameId (Record r1, Record r2)
		{
			return r1 != null && r2 != null && r1.tx()._id == r2.tx()._id;
		}

		public static boolean equals (Record r1, Record r2)
		{
			if (r1.meta() == r2.meta()) {
				List<Defn> defns = r1.meta()._defns;
				for (Defn defn : defns) {
					Object value1  = defn.getValue(r1);
					Object value2  = defn.getValue(r2);
					if (value1 == null && value2 == null) {
						continue;
					}
					if (value1.equals(value2)) {
						continue;
					}
					return false;
				}
				return true;

			}
			return false;
		}
	}

}


