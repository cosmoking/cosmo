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

import static org.jboss.netty.handler.codec.http.HttpResponseStatus.EXPECTATION_FAILED;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.NOT_ACCEPTABLE;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.cosmo.common.net.HttpError;
import org.cosmo.common.net.Session;
import org.cosmo.common.net.StringTokens;
import org.cosmo.common.service.SessionManager;
import org.cosmo.common.statistics.Clock;
import org.cosmo.common.util.Bytes;
import org.cosmo.common.util.Constants;
import org.cosmo.common.util.New;
import org.cosmo.common.util.URLDecoder;
import org.cosmo.common.util.Util;
import org.cosmo.common.view.BootStrapHTML;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;


// The mother of all the handler class.

public class ServerHandler extends SimpleChannelUpstreamHandler {

	
	/*
	 *
	 * 	figure out a way to efficient decode String to a set of conseuctite number
	 *  could use similiar way of Base64 to construce a char to int map
	 *  so that each char is then added to produce the final int
	 *  ie,  A = 1 B =2 C = 3 D = 4 then
	 *     "A" = 0;
	 *     "CAB" = 3 + 1 + 2 = 5; obvisly one would not do this as there is a letter repsent 6.. but this is the general idea
	 */

	/*
	public static final ServerHandler[] ServerHandlers = new ServerHandler[8];
	static {
		ServerHandlers[0] = new BootStrapHandler();
		ServerHandlers[1] = new MainPageHandler();
		ServerHandlers[2] = new IMGHandler();
		ServerHandlers[3] = new CSSHandler();
		ServerHandlers[4] = new JSHandler();
		ServerHandlers[5] = new AjaxGetHandler();
		ServerHandlers[6] = new DirectPageHandler();
		ServerHandlers[7] = new FileUploadHandler();
	}

	public static final String  ServerHostURLPublicReferer = "http://" + BootStrapHTML.ServerHostURLPublic + "/";
	public static final String  ServerHostURLLocalReferer = "http://" + BootStrapHTML.ServerHostURLLocal + "/";

		//just a convenience to override userAgent , ie constant set to iPHone mode or etc
	public static final String UserAgentOverride;
	static {
		UserAgentOverride = Util.getProperty(String.class, "UserAgentOverride", "");
	}
	*/
		
		// Static set of ServerHandlers
	public static ServerHandler[] ServerHandlers = new ServerHandler [] {
		new BootStrapHandler(),
		new MainPageHandler(),
		new IMGHandler(),
		new CSSHandler(),
		new JSHandler(),
		new AjaxGetHandler(),
		new DirectPageHandler(),
		new FileUploadHandler()				
	} ;
	
		// constants on referrer URLs
	public static final String  ServerHostURLPublicReferer = (AppServer.SSL ? "https://" : "http://")  + BootStrapHTML.ServerHostURLPublic + "/";
	public static final String  ServerHostURLLocalReferer =  (AppServer.SSL ? "https://" : "http://") + BootStrapHTML.ServerHostURLLocal + "/";
	public static final String WebSocketProtocol = AppServer.SSL ? "wss://" : "ws://";

		//just a convenience to override userAgent , ie constant set to iPHone mode or etc
	public static final String UserAgentOverride = Util.getProperty(String.class, "UserAgentOverride", "");



	
	
	public Session getSession (MessageEvent e, StringTokens args, StringTokens bootStrapArgs)
	  throws Exception
	{
		String sessionId = args.next();
		InetSocketAddress incomingAddress = (InetSocketAddress)e.getChannel().getRemoteAddress();
		byte[] ip =  incomingAddress.getAddress().getAddress();
		int port = incomingAddress.getPort();

		// 1. get session
		Session session = SessionManager.Instance.retrieveSession(sessionId, args.next(), ip, port, bootStrapArgs);		
		return session;
	}	
	

	
	

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {

    	ChannelFuture writeFuture = null;
    	try {
	        Clock.timer().markTime();

        		// get request query string
			InetSocketAddress incomingAddress = (InetSocketAddress)e.getChannel().getRemoteAddress();
	        HttpRequest request = (HttpRequest)e.getMessage();
	        String uri = request.getUri();

	        	// just a convenience to override userAgent , ie constant set to iPHone mode or etc
	        if (UserAgentOverride.length() > 0) {
	        	request.setHeader(HttpHeaders.Names.USER_AGENT, UserAgentOverride);
	        }

	        	// NOTE: this really only deals with "Simple" browser on referer, as System can and will use "forged" referrer to by pass this
	        	// if referer is null and it's A) not front door or B) not direct page -> 4XX
	        /*
	        String referer = request.getHeader("Referer");
	        if (referer == null) {
	        	if (!(uri.length() == 1 && uri.charAt(0) == '/') && !(uri.length() > 3 && uri.startsWith("/0/6/"))) {
	        		throw new HttpError(PRECONDITION_FAILED , uri);
	        	}
	        }
        		// if referer not null and referer not match
	        else {
	        	if (!(referer.startsWith(ServerHostURLPublicReferer) || referer.startsWith(ServerHostURLLocalReferer))) {
	        		throw new HttpError(PRECONDITION_FAILED , uri);
	        	}
	        }
	        */

		       	// This step only performs from bootstrap redirect, it extract all the request args, both request url and bootstrap arg
		    	// Does not apply for AjaxRequest or Resource Request. should only come from bootstrap page
	        StringTokens bootStrapArgs = null;
	        if (uri.length() == 1 && uri.charAt(0) == '/' && request.getMethod() == HttpMethod.POST && !request.isChunked()) {
	            ChannelBuffer content = request.getContent();
	            if (content != null && content.capacity() > 0) {
	            	String rawPostData = URLDecoder.decode(content.toString("UTF-8"), Constants.UTF8);

	            		// get queryString arg from bootstrap and append to uri  (ie) Bootstrap.jwl:handShakeToken()
	            	StringTokens st = StringTokens.on(rawPostData, StringTokens.NameValueSeparatorChar);
	            	st.next();
	            	String argsStr = st.next();
	            	uri += argsStr;

	            		// get misc args from bootsrap (ie) Bootstrap.jwl:clientInfo()
	            	st.next();
	            	String bootStrapArgsStr = st.next();
	            	bootStrapArgs = StringTokens.on(bootStrapArgsStr, StringTokens.QueryStringSeparatorChar);
	            }
	        }
	        else {
	        		// else not bootstrap request, decode query string
	            uri = URLDecoder.decode(uri, Constants.UTF8);
	        }

	        	// find the handler based on the request arg
	        int handlerIdx = uri.length() == 1 ? 0 : (byte)uri.charAt(1) - 48;
	        if (handlerIdx > ServerHandlers.length) {
	        	throw new HttpError(NOT_ACCEPTABLE, uri);
	        }

	        	// create args StringsToken
	        System.out.print(uri);
	        StringTokens args = StringTokens.on(uri.substring(1, uri.length()), StringTokens.QueryStringSeparatorChar).skip();

	        	// handle request
	        writeFuture = ServerHandlers[handlerIdx].handleRequest(ctx, e, args, bootStrapArgs);



	        	// 6. onward handle the response loop
	        	// when writeFuture is null it implies there is already an error and sendError is called within the handle
	            // request. TODO this behavior is not implicit
	       // if (writeFuture == null) {
	       // 	return;
	       // }

	        /* Decide whether to close the connection or not.
	    	 	TODO: Keep-Alive just for initial page load - there after we should close it as soon as it's finished
	    	 	for now just close it

	        	boolean close = true;
	            HttpHeaders.Values.CLOSE.equalsIgnoreCase(request.getHeader(HttpHeaders.Names.CONNECTION)) ||
	            request.getProtocolVersion().equals(HttpVersion.HTTP_1_0) &&
	            !HttpHeaders.Values.KEEP_ALIVE.equalsIgnoreCase(request.getHeader(HttpHeaders.Names.CONNECTION));
		        if (close) {
		            writeFuture.addListener(ChannelFutureListener.CLOSE);
		        }
	        */
    	}
    	catch (Exception ex) {
    		// breakpoint here;
    		throw ex;
    	}
    	
    	finally {
	        System.out.println(Clock.timer().markAndCheckRunning());
    		disconnect(ctx, writeFuture);
    	}

    }



    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
    {
   	
    	ChannelFuture writeFuture = null;
    	try {
    		HttpResponseStatus status = INTERNAL_SERVER_ERROR;
    		String message = null;
    		Throwable error = e.getCause();

    	    if (error instanceof TooLongFrameException) {
    	        status = EXPECTATION_FAILED;
    	        message = error.getMessage();
    	    }
    	    else if (error instanceof HttpError){
    	    	status = ((HttpError)error)._httpStatus;
    	    	message = (((HttpError)error)._message);
    	    }
    	    else {
    	    	message = error.getMessage();
    	    }
    	    
    	    message = New.str("[", status.getCode(), ":", e.getClass().getSimpleName(), ":",  message == null ? "<null>" : message, ":" + error.getMessage(), ":" + e.toString() + "]");
    	   

    	    System.err.println(message);
	        if (error != null) {
	        	if (!(error instanceof HttpError)) {
	        		if (!(error instanceof IOException)) {
	        			error.printStackTrace();
	        		}
	        	}
	        }

	        if (ctx.getChannel().isConnected() && ctx.getChannel().isWritable()) {
	        	// XXX in most error cases, writing error response don't succeed.  skip it for now
		        //HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
		        //response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
		        //response.setContent(new Bytes(Util.UTF8(message.toString())).toChannelBuffer());
		        //writeFuture  = ctx.getChannel().write(response);
	        }
    	}
    	catch (Throwable fatal) {
    		System.err.println("Fatal error while handling exception.");
    		fatal.printStackTrace();
    	}
    	finally {
    		disconnect(ctx, writeFuture);
    	}
    }

    public void disconnect (ChannelHandlerContext ctx, ChannelFuture writeFuture)
    {
    	try {
	    	if (writeFuture != null) {
	    		if (ctx.getChannel().isConnected()) {
	    			writeFuture.addListener(ChannelFutureListener.CLOSE);
	    		}
	    	}
	    	else if (ctx.getChannel().isConnected()) {
	    		ctx.getChannel().disconnect();
	    	}
    	}
    	catch (Throwable fatal) {
    		System.err.println("Error while closing channel.");
    	}
    }


    public ChannelFuture handleRequest (ChannelHandlerContext ctx, MessageEvent e, StringTokens args, StringTokens bootStrapArgs)
      throws Exception
    {
    	throw new RuntimeException("Not Supported");
    }


}

