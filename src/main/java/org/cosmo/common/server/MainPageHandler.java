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
import org.cosmo.common.template.Content;
import org.cosmo.common.template.Page;
import org.cosmo.common.util.DeflatableBytes;
import org.jboss.netty.handler.codec.http.HttpRequest;


public class MainPageHandler extends AbstractContentHandler
{

	@Override
    public DeflatableBytes handleRequest (Session session, StringTokens args, HttpRequest request)
      throws Exception
    {
		Page template = getTemplate(session, args);
		Content content = new Content();
		template.append(session, content);
		DeflatableBytes output = new DeflatableBytes();
		content.writeTo(output);
		return output;
    }


	public Page getTemplate (Session session, StringTokens args)
	{

		if (Session.BrowserType.iPhone == session._browserType) {
			return Page.byName("appui.MobileMainHTML");
		}

		if (Session.BrowserType.iPad == session._browserType) {
			return Page.byName("appui.TabletMainHTML");
		}

		return Page.byName("appui.MainHTML");
	}

}

