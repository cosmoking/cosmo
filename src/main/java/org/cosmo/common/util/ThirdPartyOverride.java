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

import java.util.List;

public class ThirdPartyOverride {

		// convert this to use annontation !
	public static Pair<Class, String>[] Classes = new Pair[]{
		new Pair(com.sun.syndication.io.impl.Atom10Parser.class, "overriding parseTextConstructToString()"),
		new Pair(org.json.JSONArray.class, "scopye change for var myArrayList"),
		new Pair(de.l3s.boilerpipe.sax.HTMLHighlighter.class, "override startElement and endElement"),
		new Pair(org.jboss.netty.handler.ssl.SslHandler.class, "override pollicy handling for flash"),
	};


	public static void verify (boolean verbose)
		throws Exception
	{
		for (Pair<Class, String> entry : Classes) {
			entry._t1.getMethod("thirdPartyOverride", null).invoke(null, null);
			if (verbose) {
				System.out.println(New.str("Class ", entry._t1, " - ", entry._t2, " checked."));
			}
		}
	}
}
