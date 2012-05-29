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

import org.cosmo.common.net.StringTokens;

public class Options
{

	boolean _trim = false;
	boolean _escapeQuote = false;
	boolean _jsonEncode = false;
	String _bindingSrc = null;
	String _pageName = null;

	public Options (StringTokens args)
	{
		_pageName = args.next();

		while (args.hasNext()) {
			StringTokens arg = StringTokens.on(args.next(), StringTokens.ColonSeparatorChar);
			String paramName = arg.next();

			if ("trim".equalsIgnoreCase(paramName)) {
				_trim = Boolean.parseBoolean(arg.next());
			}

			if ("escapeQuote".equalsIgnoreCase(paramName)) {
				_escapeQuote = Boolean.parseBoolean(arg.next());
			}

			if ("jsonEncode".equalsIgnoreCase(paramName)) {
				_jsonEncode = Boolean.parseBoolean(arg.next());
			}

			if ("bindingSrc".equalsIgnoreCase(paramName)) {
				_bindingSrc = arg.next().trim();
			}
		}
	}

}
