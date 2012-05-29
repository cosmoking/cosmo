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
package org.cosmo.common.server;

import org.cosmo.common.net.Session;
import org.cosmo.common.net.StringTokens;
import org.cosmo.common.template.Page;
import org.cosmo.common.util.New;

// ie http://^serverHostURL()^/0/6/Admin
//    http://^serverHostURL()^/0/6/BrowserAddExternalSite?arg1=va1&arg2=val2
//    http://^serverHostURL()^/#0/6/ContentViewHTML?180567
/*
 * 	How Hash works:
 *
 * 	1. any of eventMeta function sets  window.location.hash = "0/6/ContentViewHTML?180567"
 *  2. this changes the url to  http://localhost/#0/6/ContentViewHTML?180567
 *  3. when invoked via browser. it will first hit BootStrapHTML, there if detect has is present
 *  	it again fowards to  http://localhost/0/6/ContentViewHTML?180567
 *  4. DirectPageHandler does the right thing
 *  5. each time a eventMeta function is called in base. it clears window.location.hash
 */
public class DirectPageHandler extends MainPageHandler
{

	@Override
	public Page getTemplate (Session session, StringTokens args)
	{
			// here args are really empty because it got rewrite in the BootStrap.jwl
			// the actual arg is in the session.requestArg

    	args = StringTokens.on(session._requestArgs, StringTokens.QueryStringSeparatorChar);

    	// get templateName and strips away from the requestArg
    	String templateName = args.next();
    	session._requestArgs = args.remainingPath();

    	return Page.byName(New.str("appui." + templateName));
	}

}
