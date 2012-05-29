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
package org.cosmo.common.view;

import static org.cosmo.common.net.Session.Status.Login;

import org.cosmo.common.net.Session;
import org.cosmo.common.template.BindingSrc;
import org.cosmo.common.template.Content;


public class UIRegion extends BindingSrc
{

	public static boolean DebugMode = true;

	public boolean isUserLoggedIn (Session session, Content content)
	{
		return  session._status == Login;
	}

	public Object browserClass (Session session, Content content)
	{
		return session._browserType._userAgentName;
	}

	public Object browserVersion (Session session, Content content)
	{
		return (int)session._browserVersion;
	}

	public Object browserInfo (Session session, Content content)
	{
		return session._browserType._userAgentName + " " + (int)session._browserVersion;
	}


	public boolean isDirectPage (Session session, Content content)
	{
		return !session.handledByMainPage();
	}

	public boolean isMobile (Session session, Content content)
	{
		return session.isMobile();
	}

	public String supportHover (Session session, Content content)
	{
		return session.isDesktop() ? "supportHover" : "";
	}

	public String debugMode (Session session, Content content)
	{
		return DebugMode ? "true" : "false";
	}
}
