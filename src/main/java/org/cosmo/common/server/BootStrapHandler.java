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
import org.cosmo.common.template.Content;
import org.cosmo.common.template.Page;
import org.cosmo.common.util.Bytes;
import org.cosmo.common.view.BootStrapHTML;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.stream.ChunkedStream;

/*
 *  Idea is client browser first hit "http://niunews.com"  it will land on BootStrapHandler.handleRequest().
 *  Here we create the initial Session and bound it with it's IP. After session is created we then
 * 	dynamic generate the BootStrap.jwl and return to the client browser. Upon loaded in will generate a "uniqueToken"
 *  plus all other information we want about the browser, isMobile,  latency, location,  dimension, time etc.
 *  and post back to us at "http://niwnews.com/^handler^/^sessionId^&clientToken_clientTime_dimeion^reqeuestArg^ if any.
 *  which will then be handled by the Handler.handleRequest(). Here handler is resolved by ^handler^. It's
 *  an index in ServerHandlers. There Session will then be validated against their IP.  After validated,
 *  we will create a unique serverToken, append together with clientToken and
 *  return site main page "Main.jwl" back to the client browser, their we embed the "uniqueClientToken".
 *
 *  When "Main.jwl" is loaded the niunews.js will use that token on each operation with the server
 *  in which ContentHandler.handleRequest() will check to make sure it's valid and return the bounded session.
 *
 *  This only protects that no other client browser can try to hijack someone else's session by trying
 *  some session key or uniqueToken, since we validate again uniqeToken and IP. however this does not
 *  protect from client lending the "clientUniqueToken" to someone within the same IP address or network
 *  packet snoofing..
 *
 *  http://niunews.com/sdflkjlksdfgarbage 	-> http://niews.com/0	because it's got invalid "s" as handler index
 *  http://niunews.com/9  					-> http://niews.com/0	if it does not map to a handler
 *  http://niunews.com/1/23434 				-> http://niews.com/0   if it carries invalid session id
 *  http://niunews.com/0/6/blahblah			-> http://niews.com/6   after the bootstrap it forward the request to DirectPageHandler
 *                                                                  via BootStrap.jwl form post. The arg will then be avialable
 *                                                                  in the session.requestArg, (ie. blahblah will be in the reqeustArg)
 */
public class BootStrapHandler extends ServerHandler
{

    public ChannelFuture handleRequest (ChannelHandlerContext ctx, MessageEvent e, StringTokens args, StringTokens bootStrapArgs)
      throws Exception
    {
			// Write the content.
		Page template = getBootStrapPage();
		InetSocketAddress incomingAddress = (InetSocketAddress)e.getChannel().getRemoteAddress();

			// allocate new session
		Session session = SessionManager.Instance.allocateNewSession(incomingAddress, args, (HttpRequest)e.getMessage());
		setServerHandler(session, args);


        	// render to BootStrapHTML back to client
		Content resultContainer = template.append(session);
		Bytes output = new Bytes();
		resultContainer.writeTo(output);

		Channel ch = e.getChannel();
		if (AppServer.TransferEncodingChunking) {
			ch.write(HttpResponse.OK_PlainNoCache.fmtBytes(output.count()).toChannelBuffer());
			return ch.write(new ChunkedStream(output.toInputStream()));
		}
		else {
			return ch.write(ChannelBuffers.wrappedBuffer(HttpResponse.OK_PlainNoCache.fmtBytes(output.count()).bytes(), output.makeExact().bytes()));
		}
    }
    
    
     public Page getBootStrapPage() {
 		return Page.byName(BootStrapHTML.class.getName());
     }
     

	// Should only be called by BootStrapHandler
	// based on the httpQueryString dertmine the "next" serverHandler for this session
	// i.e. 1)  http://freshinterval.com/ ->  would be posted by bootstrap handler to -> http://freshinterval.com/1
	//          obviously along with othe param captured by bootstrap so an actual would be something like
	//			http://freshinterval.com/1/0/2011110620DKOHaVF2Zq992FI9, where 0, is session followed by clientUniqueTOken
	//      2)  http://freshinterval.com/0/6/blaha -> would be posted by bootstrap handler to -> http://freshinterval.com/6/blah
	public void setServerHandler (Session session, StringTokens httpQueryString)
	{
		String serverHandlerId = httpQueryString.next();

			// if a specific serverId is specified, take it and capture request arg
		if (serverHandlerId != null && serverHandlerId.length() == 1) {
			session._serverHandler = (byte)serverHandlerId.charAt(0) - 48; // calculate actual int
			session._requestArgs = httpQueryString.remainingPath();
		}

			// if it's 0 (default) or somehow it's beyond the range
			// default to to 1 (mainPage)
	    if (session._serverHandler <= 0 || session._serverHandler > AppServer.ServerPipelineFactory._serverHandler.ServerHandlers.length) {
	    	session._serverHandler = 1;
	    }
	}




}
