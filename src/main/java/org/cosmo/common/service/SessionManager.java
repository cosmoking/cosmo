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
package org.cosmo.common.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;

import org.cosmo.common.model.User;
import org.cosmo.common.net.Session;
import org.cosmo.common.net.StringTokens;
import org.cosmo.common.pool.SessionPool;
import org.cosmo.common.record.Meta;
import org.cosmo.common.server.WebResourceFiles;
import org.cosmo.common.statistics.Clock;
import org.cosmo.common.util.New;
import org.cosmo.common.util.Util;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.json.JSONException;
import org.json.JSONObject;


public class SessionManager
{

	public static final SessionManager Instance = new SessionManager();


	public Session allocateNewSession (InetSocketAddress incomingAddress, StringTokens httpQueryString, HttpRequest httpRequest)
	{
		byte[] ip = incomingAddress.getAddress().getAddress();
		int port = incomingAddress.getPort();

		Session session = SessionPool.Instance.getInstance(ip, port);
        session.setBrowserInfo(httpRequest.getHeader(HttpHeaders.Names.USER_AGENT));
		return session;
	}



	public Session retrieveSession (String sessionId, String clientToken, byte[] ipAddress, int port, StringTokens httpPostString)
	  throws IOException, JSONException
	{

			// verify id range
		int slot = decodeSessionId(sessionId);
		if (slot < 0) {
			throw new IOException(New.str("Bad Id from IP [", Util.getIpAddress(ipAddress), "]"));
		}


			// check if session is still there
		Session session = SessionPool.Sessions[slot];
		if (session == null) {
			throw new IOException(New.str("Invalid Id from IP [", Util.getIpAddress(ipAddress), "]"));
		}

			// check client ip
		if (session._clientIP == null || !Arrays.equals(session._clientIP, ipAddress)) {
			throw new IOException(New.str("Bad IP [", Util.getIpAddress(ipAddress), "]"));
		}

			// check client port - Actually client port varies between request
		//if (session._clientPort != port) {
		//	return null;
		//}

			// upon first time coming back from bootstrap - assign the clientToken, and time stamp
		if (session._uniqueClientToken == null) {
			setupBootstrap(session, clientToken, ipAddress, httpPostString);
		}
			// else check if token matches
		else if (!session._uniqueClientToken.equals(clientToken)) {
			throw new IOException(New.str("No Match from IP [", Util.getIpAddress(ipAddress), "]"));
		}

		session._lastAccessedTime = System.currentTimeMillis();



		return session;
	}



	public void setupBootstrap (Session session, String clientToken, byte[] ipAddress, StringTokens bootstrapParam)
	  throws IOException, JSONException
	{
			// dimension from bootstrap - only first time from bootstrap
		String height = bootstrapParam.next();
		String width = bootstrapParam.next();

		final int topBottomPadHeight = 64;
		final int entryHeight = 30;

			// for chrome - when concurrent sessions is open it reports wrong dimensions - hence max(5)
		//session._control._paginationModel._paginationSize =  Math.max(10, (int)((Integer.valueOf(height) - topBottomPadHeight) / entryHeight) - 1);
		//session._control._contentType = appui.Content.Mode.All;



			// get keepLogin from bootstrap - only first time from bootstrap
		String ipAddressStr = bootstrapParam.next();
		String time = bootstrapParam.next();
		String userId = bootstrapParam.next();
		String token = bootstrapParam.next();
		if (Util.getIpAddress(ipAddress).equals(ipAddressStr)) {
			if (time != null && Long.valueOf(time) > System.currentTimeMillis()) {
				User user = UserManager.Instance.userByEmail(userId);
				if (user != null) {
					if (!session.attemptKeepLoginUser(userId, ipAddressStr, token, time, user)) {
		  				System.out.println("KL: token different from serversid");
		  			}
				}
				else {
					System.out.println("KL: Invalid user " + userId);
				}
			}
			else {
				System.out.println("KL: Time expired");
			}
		}
		else {
			System.out.println("KL: IP don't match");
		}

			// parse the resource token and store in session - this would allow "DeclHeader" to decide if it needs
			// to bust the localstorage resource and append new ones OR use cached ones from localstorage
		session._webResourceFilesTokens = new WebResourceFiles.Tokens(bootstrapParam.next());

			// set client token
		session._uniqueClientToken = clientToken;

			// set optinos if any
		setOptions(bootstrapParam.next(), session);
	}


			// options are set via /0/6/OptionsHTML?{"UserAgentOverride":"ipad","clear":"true"} etc
	public void setOptions (String options, Session session)
	  throws JSONException
	{
		if (options == null || options.length() < 6) { // has to be a json string with right length
			return;
		}
		JSONObject values = new JSONObject(options);
		if (values.has("UserAgentOverride")) {
			String userAgentOverride = values.getString("UserAgentOverride");
			session.setBrowserInfo(userAgentOverride);
		}
	}





	/*
	 *
	 * 	figure out a way to efficient decode String to a set of conseuctite number
	 *  could use similiar way of Base64 to construce a char to int map
	 *  so that each char is then added to produce the final int
	 *  ie,  A = 1 B =2 C = 3 D = 4 then
	 *     "A" = 0;
	 *     "CAB" = 3 + 1 + 2 = 5; obvisly one would not do this as there is a letter repsent 6.. but this is the general idea
	 */


	public static String encodeSessionId (Session session)
	{
		return String.valueOf(session._id);
	}

	public static int decodeSessionId (String s)
	{
		return Util.parseInt(s);
	}


	public static void main (String[] args) throws Exception
	{
		Clock clock = Clock.create(Clock.Unit.Micro);
		int[] longs = new int[10000000];

		for (int i = 0; i < longs.length * 10; i++) {
		}
		System.out.println(clock.markAndCheckRunning());


		for (int i = 0; i < longs.length; i++) {
			longs[i] = i;
		}
		System.out.println(clock.markAndCheckRunning() + " assignment");

		for (int i = 0; i < longs.length; i++) {
			if (longs[i] == i);
		}
		System.out.println(clock.markAndCheckRunning() + " compare");

		for (int i = 0; i < longs.length; i++) {
			System.currentTimeMillis();
		}
		System.out.println(clock.markAndCheckRunning() + " systime");

		for (int i = 0; i < longs.length; i++) {
		}
		System.out.println(clock.markAndCheckRunning() + " noop");

	}
}


