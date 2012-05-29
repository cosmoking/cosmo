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
package org.cosmo.common.template;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.cosmo.common.net.StringTokens;



abstract public class Binding
{

	public Object applyValue (Page page, BindingSrc bindingSrc, Content container, Object context, Options options) throws Exception
	{
		Object result = getValue(page, bindingSrc, context, options);
		if (container != null) {
			container.pushBinding(result);
		}
		return result;
	}

	abstract public Object getValue (Page page, BindingSrc bindingSrc, Object context, Options options) throws Exception;


	abstract public String name ();

		// indicates in can be passed override by bindingBinding ie ^bind(sdafsd)^
	static interface AllowOverride
	{
	}

		// indicates it can be passed by calling templateBinding ie ^abc{arg1:value1,arg2:value2}^
	static abstract class ValueBinding extends Binding
	{
		private Map _annonations = Collections.EMPTY_MAP;
		protected String _name;

		public ValueBinding (String name)
		{
			_name = extractAnnonations(name);
		}


		public Map<String,String> getAnnonations()
		{
			return _annonations;
		}

		public String extractAnnonations (String name)
		{
			int idx = name.indexOf("@");
			if (idx > 0) {
				_annonations = new HashMap();
				String annonationsStr = name.substring(idx + 1, name.length());
				StringTokens st = StringTokens.on(annonationsStr, StringTokens.JsonSeparatorChar);
				while (st.hasNext()) {
					_annonations.put(st.next(), st.next());
				}
				return name.substring(0, idx);
			}
			return name;
		}
	}
}






