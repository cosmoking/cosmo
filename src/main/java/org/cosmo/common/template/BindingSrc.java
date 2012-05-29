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
import java.util.Map;

import org.cosmo.common.util.Util;
import org.json.JSONObject;

public class BindingSrc
{

	public static final String BindingMarker = "!BM!";
	public static final byte[] BindingMarkerBytes = Util.UTF8(BindingMarker);
	public static final byte[] BindingMarkerBytesQuoted = Util.UTF8(JSONObject.quote(BindingMarker));

	PageBinding _caller;


	// TODO XXXif create methods in BindinSrc to allow returning cache one (need to reset fields though), otherwise return new instance can also pool,
	public static <H extends BindingSrc>H instance (Class<H> bindingSrcClass)
	{
		try {
			//System.out.println("NEW");
			return bindingSrcClass.newInstance();
		}
		catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}


	public Object applyValue (Binding binding, Page page, Content container, Object context, Options options)
	  throws Exception
	{
		if (_caller != null) {
			// check if the caller has the binding defined and passed down, if does delegate to caller's binding
			if (_caller._callBindings != null && binding instanceof Binding.ValueBinding) {
				if (_caller._callBindings.containsKey(binding.name())) {
					return _caller._callerBindingSrc.applyValue(_caller._callBindings.get(binding.name()), _caller._callerPage, container, context, options);
				}
			}
		}

			// check if there is bindbinding override - if there is one call it
		if (page._bindBindings != null && binding instanceof Binding.AllowOverride) {
			BindBinding bindbinding = page.getBindBinding(binding.name());
			if (bindbinding != null) {
				return bindbinding.applyValue(page, this, container, context, options);
			}
		}
			// otherwise delgate to the actual binding
		return binding.applyValue(page, this, container, context, options);
	}


	public Map<String, Binding> callBindings ()
	{
		return _caller == null ? Collections.EMPTY_MAP : _caller._callBindings;
	}
}




