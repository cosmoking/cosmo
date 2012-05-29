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

import org.cosmo.common.net.StringTokens;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;

public class IMGHandler extends AbstractResourceHandler
{
	public volatile long _requestCount;

	@Override
	public String resourceDir ()
	{
		return WebResourceFiles.Type.IMG._relativeResourceDir;
	}

	@Override
	public ChannelFuture handleRequest (ChannelHandlerContext ctx, MessageEvent e, StringTokens args, StringTokens bootStrapArgs)
	  throws Exception
	{
		return super.handleRequest(ctx, e, args, bootStrapArgs);
	}
}
