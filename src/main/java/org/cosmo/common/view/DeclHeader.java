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

import org.cosmo.common.server.WebResourceFiles;
import org.cosmo.common.template.Content;
import org.cosmo.common.net.Session;



public class DeclHeader extends BootStrapHTML
{



	public Object resourceDecl (Session session, Content content)
	  throws Exception
	{
		if (WebResourceFiles.UseCacheResourceDeclaration) {

			if (session._browserType._webResourceFilesCache.isEmptyToken(session._webResourceFilesTokens)) {
				return session._browserType._webResourceFilesCache.FullResourceBytes;
			}

			else if (session._browserType._webResourceFilesCache.isFullToken(session._webResourceFilesTokens)) {
				return session._browserType._webResourceFilesCache.CacheResourceBytes;
			}

			else {
				return session._browserType._webResourceFilesCache.validateAndGenerateResourceBytes(session._webResourceFilesTokens);
			}
		}
		else {
			return session._browserType._webResourceFilesCache.HTMLResourceBytes;
		}
	}


}









