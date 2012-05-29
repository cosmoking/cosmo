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

import java.util.Date;

public class Query
{
	public static final String TO = " TO ".intern();
	public static final String AND = " AND ".intern();

	StringBuilder _str = new StringBuilder();
	Defn _defn;

		// on range
	public Query recordsRange (Defn defn, Object from, Object to)
	{
		_str.append(defn.getIndexFieldName()).append(":[").append(defn.fmt(from, Defn.FormatType.ForSearch)).append(TO).append(defn.fmt(to,Defn.FormatType.ForSearch)).append("]");
		_defn = defn;
		return this;
	}

	public Query and ()
	{
		_str.append(AND);
		return this;
	}

	public Query records (Defn defn, Object o)
	{
		_str.append(defn.getIndexFieldName()).append(":").append("\"").append(defn.fmt(o, Defn.FormatType.ForSearch)).append("\"");
		_defn = defn;
		return this;
	}



	public static Query on ()
	{
		return new Query();
	}

}
