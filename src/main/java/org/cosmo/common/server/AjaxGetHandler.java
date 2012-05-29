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

import static org.cosmo.common.net.Session.Status.Login;
import static org.cosmo.common.net.Session.Status.Logout;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


import org.apache.commons.lang.StringUtils;
import org.cosmo.common.model.User;
import org.cosmo.common.net.FormValidator;
import org.cosmo.common.net.Script;
import org.cosmo.common.net.Session;
import org.cosmo.common.net.StringTokens;
import org.cosmo.common.record.Meta;
import org.cosmo.common.service.SessionManager;
import org.cosmo.common.service.UserManager;
import org.cosmo.common.util.DeflatableBytes;
import org.cosmo.common.util.JSONList;
import org.cosmo.common.util.New;
import org.cosmo.common.util.Util;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cern.colt.list.LongArrayList;



public class AjaxGetHandler extends AbstractContentHandler
{

	private static final HashMap<String, AjaxGetHandler> AjaxGetHandlers = new HashMap();


		// get the handler delegate then return response
	@Override
    public DeflatableBytes handleRequest (ChannelHandlerContext ctx, MessageEvent e, Session session, StringTokens args, HttpRequest request)
      throws Exception
    {
		String handler = args.next();
		AjaxGetHandler ajaxGetHandler = get(handler, session);
		AjaxResponse ajaxResponse = ajaxGetHandler.newResponse(ctx, e, session, args, request);
		try {
			ajaxGetHandler.appendToResponse(ctx, e, ajaxResponse, session, args, request);		
			return ajaxResponse.responsePayload();
		}
		catch (Session.PromptLoginException promptLoginException) {
	    	return handleRequest(session, StringTokens.on("promptLogin"), request);
		}
    }

    	// resolve handler
    public AjaxGetHandler get (String handler, Session session)
      throws Exception
    {    	
    		// unsafe lazy init for now
		AjaxGetHandler ajaxGetHandler = AjaxGetHandlers.get(ajaxGetHandlerClassName(handler, session, true));
		if (ajaxGetHandler == null) {
			Class handlerClazz = Class.forName(ajaxGetHandlerClassName(handler, session, false));
			ajaxGetHandler = (AjaxGetHandler)handlerClazz.newInstance();
			AjaxGetHandlers.put(ajaxGetHandlerClassName(handler, session, true), ajaxGetHandler);
		}

		return ajaxGetHandler;
    }

    private String ajaxGetHandlerClassName (String handler, Session session, boolean simpleName)
    {
		return session._ajaxGetHandlerClass != null
			? session._ajaxGetHandlerClass + handler
			: simpleName
				? handler
			    : New.str(this.getClass().getName(), "$" ,handler);
    }


		// subclass to override to generate actual response content
	public void appendToResponse (ChannelHandlerContext ctx, MessageEvent e, AjaxResponse ajaxResponse, Session session, StringTokens args, HttpRequest request)
		throws Exception
	{
		appendToResponse(ajaxResponse, session, args, request);
	}    
    
    
    	// subclass to override to generate actual response content
    public void appendToResponse (AjaxResponse ajaxResponse, Session session, StringTokens args, HttpRequest request)
    	throws Exception
    {
    	throw new IllegalArgumentException("Not implemented");
    }

    	// subclass is allowed to generate it's own response
    	// in some case it return cached ones (ie, QueryAhead result)
    public AjaxResponse newResponse (ChannelHandlerContext ctx, MessageEvent e, Session session, StringTokens args, HttpRequest request)
    	throws Exception
    {
    	return new AjaxResponse.Impl(); // recycle and bound to the session
    }


	
  
	

    
    /* subclass handlers */  
    public static class promptLogin extends AjaxGetHandler
    {

        public void appendToResponse (AjaxResponse ajaxResponse, Session session, StringTokens args, HttpRequest request)
          throws Exception
        {
    		ajaxResponse.generateContentFor(Script.ApplyPromptLogin);
        }	
    }




    public static class loginClicked extends AjaxGetHandler 
    {
    	
        public void appendToResponse (AjaxResponse ajaxResponse, Session session, StringTokens args, HttpRequest request)
          throws Exception
        {
    		FormValidator validator = new FormValidator();
    		args.utf8Decode();
      		JSONObject json = new JSONObject(args.remainingPath());		
    		verifyLogin(json, validator, session);

    		ajaxResponse.generateContentFor(validator._validationResult, "loginDialog");	
    		
    		if (validator.noErrors()) {
    			setupKeepLogin (this, ajaxResponse, session, Boolean.valueOf(json.getString("loginDialog_keepLogin")));
    		}
    		
    			// return validation results, empty implies pass 
    		//ajaxResponse.generateContentFor(Util.quote(Category.Personal.name()), Script.ApplyCallback.apply("ActionHighlight.highlightCategoryFolderByName"));		
        }
        
    	
    	public void verifyLogin (JSONObject json, FormValidator validate, Session session)
    	  throws JSONException, IOException
    	{
    		try {
    	  		String email = json.getString("loginDialog_email");
    	  		validate.notBlank(email).withMessage("Please enter your account email.").onElement("#loginDialog_email").check();
    	  		
    	  		String password = json.getString("loginDialog_password");
    	  		validate.notBlank(password).withMessage("Please enter your account password.").onElement("#loginDialog_password").check();
    	  		
    			User user = UserManager.Instance.userByEmail(email);
    			validate.notNull(user).withMessage("Please enter valid Email and password.").onElement("#loginDialog div.tips").check();
    			validate.isTrue(user.matchingPassword(password)).withMessage("Please enter valid email and Password.").onElement("#loginDialog div.tips").check();
    				
    			session.loginUser(user);
    		}
    		catch (FormValidator.BreakOnError e) { 
    		}		
    	}
    	
        public static void setupKeepLogin (AjaxGetHandler handler, AjaxResponse ajaxResponse, Session session, boolean keepLogin)
          throws Exception
        {

    			// setup keep loginz
    		if (keepLogin) {
    			Object[] keepLoginInfo = session.keepLoginInfo();
    			session.turnOnKeepLogin(keepLoginInfo);
    	        ajaxResponse.generateContentFor(new JSONList(keepLoginInfo), Script.ApplyCallback.apply("setupKeepLogin"));
    		}
    		else {
    			session.turnOffKeepLogin();
    		}
      }	
    }


    public static class logoutClicked extends loginClicked 
    { 
    	
        public void appendToResponse (AjaxResponse ajaxResponse, Session session, StringTokens args, HttpRequest request)
          throws Exception
        {
    		//session.turnOffKeepLogin(); don't need this because at client side we already invalidated the keepLogin
        	session.terminate();    		
        }    
    }


    public static class createAccountClicked extends AjaxGetHandler
    {
        public void appendToResponse (AjaxResponse ajaxResponse, Session session, StringTokens args, HttpRequest request)
          throws Exception
        {
    		session._status = Logout;
    		FormValidator validator = new FormValidator();
    		createUser(args, validator, session);

  		  // return validation results, empty implies pass
    		ajaxResponse.generateContentFor(validator._validationResult, "createAccountDialog");		
    		
    			// login
    		if (validator.noErrors()) {
    			loginClicked.setupKeepLogin(this, ajaxResponse, session, true);
    		}
    		
        }	
    	
        public User newUser (String userName, String email, String password)
          throws Exception
        {
        	return new User(userName, email, password);
        }

        public synchronized void createUser (StringTokens args, FormValidator validate, Session session)
          throws UnsupportedEncodingException, IOException, JSONException
    	{
        	try {
        		args.utf8Decode();
          		JSONObject json = new JSONObject(args.remainingPath());		
    			String email = json.getString("createAccountDialog_email"); 		
    	  		validate.notBlank(email).withMessage("Please enter your email.").onElement("#createAccountDialog_email").check();
    	
    	  		/*
    	    	String cemail = args.next();
    	  		validate.notBlank(cemail).withMessage("Please enter the same email above.").onElement("#createAccountDialog_cemail").check();    	
    	  		validate.isTrue(email.equals(cemail)).withMessage("Please enter the same email above.").onElement("#createAccountDialog_cemail").check();
    	    	*/  
    	    	  		
    	    	String password = json.getString("createAccountDialog_password");
    	  		validate.notBlank(password).withMessage("Please enter a password.").onElement("#createAccountDialog_password").check();    	
    	    	
    	  		/*
    	    	String cpassword = args.next();
    	    	validate.notBlank(password).withMessage("Please enter the same password above.").onElement("#createAccountDialog_cpassword").check();
    	    	validate.isTrue(password.equals(cpassword)).withMessage("Please enter the same password above.").onElement("#createAccountDialog_cpassword").check();
    	    	*/

    	    	String userName = json.getString("createAccountDialog_name");
    	  		validate.notBlank(userName).withMessage("Please enter a user name.").onElement("#createAccountDialog_name").check();
    	  		
    	  		
 
    		  	
    	    	try {
    	      		session._user =  newUser(userName, email, password);;
    	      		
    	      			// create sample folder based on the current popular sites at the time of creating this user
    	      		/* This used to create a folder based on the popular folders - disable for now
    	    		CachedRecordStore<RssSite> cacheStore = (CachedRecordStore)RssSite.Meta.store();
    	    		List<RssSite> rssSites = cacheStore.getTopHits(5);

    	    		Node rootFolder = session._user._profile.load("Folder");
    	    		Node defaultFolder = PublicFolder.createFolder(rootFolder, "Tag", "Sample Folder", user._profile);
    	    		for (RssSite aSite : rssSites) {
    	    			Node itemNode = PublicFolder.addItemToFolder(defaultFolder, aSite.getClass().getSimpleName(), aSite._title, aSite.tx().idString(), user._profile);
    	    			itemNode.add("iconURL", aSite._iconUrl);
    	    		}
    	    		*/
    	      		
    	      		/* Simply going to use the Blog folders for now */
    	      		//user._profile = Category.Other.folder()._folderItems;
    	      		
    	    		//user._profile.save();
    	      	}
    	      	catch (Exception e) {
    	      		e.printStackTrace();
    	      		validate.isTrue(false).withMessage("Sorry, account of same email exists.").onElement("#createAccountDialog div.tips").check();      		
    		    }
        	}
        	catch (FormValidator.BreakOnError e) {    		
        	}
    	}
    }




 







}

