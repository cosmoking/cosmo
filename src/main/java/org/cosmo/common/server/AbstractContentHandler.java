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

import java.net.InetSocketAddress;

import org.cosmo.common.net.HttpResponse;
import org.cosmo.common.net.Session;
import org.cosmo.common.net.StringTokens;
import org.cosmo.common.service.SessionManager;
import org.cosmo.common.util.DeflatableBytes;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.stream.ChunkedStream;

	// handles all page that provides content. pretty much the next call after AbstractServerHandler
	// abstract some of the basic wiring as below:
abstract public class AbstractContentHandler extends ServerHandler
{
	public static int count;
	
	public static final DeflatableBytes AsyncResponsePayload = new DeflatableBytes(0);
	
	
	@Override
    public ChannelFuture handleRequest (ChannelHandlerContext ctx, MessageEvent e, StringTokens args, StringTokens bootStrapArgs)
      throws Exception
    {
	
    		// get session
		Session session = getSession(e, args, bootStrapArgs);
		
			// delegate to the actual handler for  request
		DeflatableBytes responsePayload = handleRequest(ctx, e, session, args, (HttpRequest)e.getMessage()).end();
		
			// write response - if async do thing as it will written later, otherwise write immediately
		return responsePayload == AsyncResponsePayload ? null : writeResponse(e, session, responsePayload);
    }
	
	
	public ChannelFuture writeResponse (MessageEvent e, Session session, DeflatableBytes payload)
	{
		HttpResponse response = session.isIE()
				? payload._compressed
					? HttpResponse.OK_GzipNoCache
					: HttpResponse.OK_PlainNoCache
				: payload._compressed
					? HttpResponse.OK_Deflate
					: HttpResponse.OK_Plain;


				Channel ch = e.getChannel();
				if (AppServer.TransferEncodingChunking) {
					ch.write(response.fmtBytes(payload.count()).toChannelBuffer());
					ChannelFuture writeFuture = ch.write(new ChunkedStream(payload.toInputStream()));
					 return writeFuture;
				}
				else {
					return ch.write(ChannelBuffers.wrappedBuffer(response.fmtBytes(payload.count()).bytes(), payload.makeExact().bytes()));
				}		
	}

	
		// subclass override
    public DeflatableBytes handleRequest (ChannelHandlerContext ctx, MessageEvent e, Session session, StringTokens args, HttpRequest request)
      throws Exception
    {
    	return handleRequest(session, args, request);
    }
	

    	// subclass override
    public DeflatableBytes handleRequest (Session session, StringTokens args, HttpRequest request)
      throws Exception
    {
    	throw new IllegalArgumentException("Override this implemented");
    }

}
