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
package org.cosmo.common.net;

import java.io.IOException;

import org.cosmo.common.server.AppServer;
import org.cosmo.common.server.MainPageHandler;
import org.cosmo.common.server.ServerHandler;
import org.cosmo.common.server.WebResourceFiles;
import org.cosmo.common.service.SessionManager;
import org.cosmo.common.model.User;
import org.cosmo.common.statistics.Category;
import org.cosmo.common.util.Util;
import org.cosmo.common.xml.Node;

public class Session<T extends User>
{

	public static final long KeepLoginExpiration = 24 * 60 * 60 * 1000; // 24 hour
	public static final long SessionExpiration = 30 * 60 * 1000; // 30 min
	//public static final long SessionExpiration = 2 * 1000; // 30 min

	public static enum Status {
		Login,
		Logout;
	};

		// lazy parsing for now - , order matters as iPhone, iPad, Chrome all contains webkit
	public static enum BrowserType {
		iPhone ("iphone", WebResourceFiles.CacheBundle.Mobile),
		iPad ("ipad", WebResourceFiles.CacheBundle.Tablet),
		Chrome ("chrome", WebResourceFiles.CacheBundle.Desktop),
		Firefox ("firefox", WebResourceFiles.CacheBundle.Desktop),
		Safari ("safari", WebResourceFiles.CacheBundle.Desktop),
		MSIE ("msie", WebResourceFiles.CacheBundle.Desktop),
		Other ("other", WebResourceFiles.CacheBundle.Desktop);

		public final String _userAgentName;
		public final WebResourceFiles.CacheBundle _webResourceFilesCache;
		private BrowserType (String userAgentName, WebResourceFiles.CacheBundle webResourceFilesCache) {
			_userAgentName = userAgentName;
			_webResourceFilesCache = webResourceFilesCache;
		}
	}

		// main session identifiers
	public final int _id;
	public final byte[] _clientIP;
	public final int _clientPort;

		// session info
	public String _uniqueClientToken;  // dateStr(new Date(), true) + clientRandomToken() + "^serverRandomToken()^"
	public long _serverCreationTime;
	public volatile long _lastAccessedTime;
	public volatile Status _status;
	public BrowserType _browserType;
	public byte _browserVersion;
	public T _user;
	public int _serverHandler;
	public String _ajaxGetHandlerClass; // by default it's null - can be override
	public String _requestArgs;

    //public ContentControl _control;

    	// resource tokens
    public WebResourceFiles.Tokens _webResourceFilesTokens;


	public Session (int id, byte[] ip, int port)
	{
		_id = id;
		_clientIP = ip;
		_clientPort = port;
		_status = Status.Logout;
		_serverCreationTime = System.currentTimeMillis();
		_lastAccessedTime = _serverCreationTime;

		//_control = new ContentControl(this);
	}

	public String sessionId ()
	{
		return SessionManager.encodeSessionId(this);
	}

	public boolean isMobile ()
	{
		return _browserType._webResourceFilesCache == WebResourceFiles.CacheBundle.Mobile;
	}

	public boolean isTablet ()
	{
		return _browserType._webResourceFilesCache == WebResourceFiles.CacheBundle.Tablet;
	}

	public boolean isDesktop ()
	{
		return _browserType._webResourceFilesCache == WebResourceFiles.CacheBundle.Desktop;
	}


	public boolean isExpired ()
	{
		return  Status.Logout == _status || (System.currentTimeMillis() - _lastAccessedTime) > SessionExpiration;
	}

	public void terminate ()
	{
		_status = Status.Logout;
		_user = null;
		//_control = null;
	}

	public boolean handledByMainPage ()
	{
		return MainPageHandler.class.isAssignableFrom(AppServer.ServerPipelineFactory._serverHandler.ServerHandlers[_serverHandler].getClass());
	}


	public Object[] keepLoginInfo ()
	  throws IOException
	{
		return new Object[] {Util.getIpAddress(_clientIP), String.valueOf(System.currentTimeMillis() + KeepLoginExpiration), _user.getEmail(), org.cosmo.common.util.Util.generateToken(32)};
	}


	public Node turnOnKeepLogin (Object[] keepLoginInfo)
	  throws IOException
	{
		// XXX note any changes in key make sure bootstrap.jwl is updated too !
		Node keepLoginInfoNode = _user._profile.load("keepLogin");
		keepLoginInfoNode.set("userIp", keepLoginInfo[0]);
		keepLoginInfoNode.set("expirationTime", keepLoginInfo[1]);
		keepLoginInfoNode.set("userId", keepLoginInfo[2]);
		keepLoginInfoNode.set("token", keepLoginInfo[3]);

		_user._profile.save();
		return keepLoginInfoNode;
	}

	public Node turnOnKeepLogin ()
	  throws IOException
	{
		// XXX note any changes in key make sure bootstrap.jwl is updated too !
		Object[] keepLoginInfo = keepLoginInfo();
		return turnOnKeepLogin(keepLoginInfo);
	}



	public void turnOffKeepLogin ()
	  throws IOException
	{
		_user._profile.set("keepLogin", null);
		_user._profile.save();
	}

	public boolean attemptKeepLoginUser (String userId, String userIp, String token, String expirationTime, T user)
	{
		Node keepLoginInfo = user._profile.load("keepLogin");
		String userIdX = keepLoginInfo.stringValue("userId", "");
		String userIpX = keepLoginInfo.stringValue("userIp", "");
		String tokenX = keepLoginInfo.stringValue("token", "");
		String expirationTimeX = keepLoginInfo.stringValue("expirationTime", "");
		if (userIdX.equals(userId) &&
			userIpX.equals(userIp) &&
			tokenX.equals(token) &&
			expirationTimeX.equals(expirationTime)) {
			loginUser(user);
			return true;
		}
  		else {
  			return false;
  		}
	}

	public void loginUser (T user)
	{
		_user = user;
		_status = Session.Status.Login;
			// for now
		//_control.setFocusCategoryTab(Category.Tech);
		
		
		//_control.setFocusCategoryTab(Category.Personal);

			// XXX remove this
		//Object o = Category.Tech.folder()._folderItems;
		//Object copy = (record.XML)Category.Food.folder()._folderItems.copy();
		//_user._profile = (record.XML)copy;
	}

	/*
	public Node getCategoryFolder ()
	{
		Node rootFolder = _control.focusCategoryTab() == Category.Personal
			? _user._profile.get("Folder") //Category.Tech.folder()._folderItems.get("Folder")//
			: _control.focusCategoryTab().folder()._folderItems.get("Folder");
		return rootFolder;
	}
	*/

	public String currentDateFromClientToken ()
	{
		return _uniqueClientToken.substring(0, 10);
	}

	public void promptLogin ()
	  throws PromptLoginException
	{
		if (_user == null) {
			throw new PromptLoginException();
		}
	}

	public boolean isIE ()
	{
		return BrowserType.MSIE == _browserType;
	}



    public void setBrowserInfo (String userAgent)
    {
    	_browserType = BrowserType.Other;
    	_browserVersion = 1;

    	userAgent = userAgent.toLowerCase();
    	for (BrowserType type : BrowserType.values()) {
    		int start = userAgent.indexOf(type._userAgentName);
    		if (start >= 0) {
    			start = start + type._userAgentName.length() + 1;
    			try {
	    			_browserType = type;
    				int end = userAgent.indexOf(".", start);
	    			String version = userAgent.substring(start, end > 0 ? end : userAgent.length());
    				_browserVersion = (byte)Integer.parseInt(version);
    			}
    			catch (Exception e) {
    			}
    			return;
    		}
    	}
    }


	public static class PromptLoginException extends Exception
	{

	}
}



