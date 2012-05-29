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

import java.io.File;
import java.io.IOException;

import org.cosmo.common.net.HttpResponse;

public class JSHandler extends AbstractResourceHandler
{
	public volatile long _requestCount;

	@Override
	public String resourceDir ()
	{
		return WebResourceFiles.Type.JS._relativeResourceDir;
	}

	@Override
	public ResourceFileCacheEntry getResourceFileCacheEntry (File file, HttpResponse response, boolean transferEncodingChunking)
	  throws IOException
	{
		return new MinifiedResourceFileCacheEntry(file, HttpResponse.OK_Gzip, AppServer.TransferEncodingChunking);
	}
}




