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

import java.net.InetAddress;

import org.cosmo.common.net.Session;
import org.cosmo.common.server.AppServer;
import org.cosmo.common.template.Content;
import org.cosmo.common.util.New;
import org.cosmo.common.util.Util;

public class BootStrapHTML extends UIRegion
{
	public static final String Server;
	public static final String ServerHostURLPublic;
	public static final String ServerHostURLLocal;
	public static final String ServerHostAddress;


	static {
		try {
			InetAddress addr = InetAddress.getLocalHost();
			ServerHostAddress = addr.getHostAddress();
			Server = Util.getProperty(String.class, "Server", ServerHostAddress);
			
			ServerHostURLPublic = AppServer.Port == 80 ? Server : New.str(Server,":",AppServer.Port);
			ServerHostURLLocal =  New.str(addr.getHostAddress(),":", AppServer.Port);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

	}


	public String serverDomainName (Session session, Content cc)
	{
		return AppServer.Production ? "freshinterval.com" : serverHostURL(session, cc);

	}

	public String serverHostURL (Session session, Content cc)
	{
			// XXX this is used to route internal request to internal ip address
			// since external address can't be accessed internally for some routers
		if (requestIsInternal(session)) {
			return ServerHostURLLocal;
		}
		else {
			return ServerHostURLPublic;
		}
	}


	public String webResourceFileIds (Session session, Content cc)
	{
		return session._browserType._webResourceFilesCache.ArrayString;

	}

	private boolean requestIsInternal (Session session)
	{
		return session._clientIP[0] == (byte)192;
	}


	public String sessionId (Session session, Content cc)
	{
		return session.sessionId();
	}

	public String serverRandomToken (Session session, Content cc)
	{
		return org.cosmo.common.util.Util.generateToken(8);
	}


	public String handler (Session session, Content cc)
	{
		return String.valueOf(session._serverHandler);
	}

	public String uniqueClientToken (Session session, Content c)
	{
		return session._uniqueClientToken;
	}

	public String requestArgs (Session session, Content c)
	{
		return session._requestArgs == null ? "''" : "'&' + escape('" + session._requestArgs + "')";
	}
}
