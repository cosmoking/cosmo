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
package org.cosmo.common.model;

import java.io.Externalizable;
import java.io.File;
import java.io.IOException;

import org.cosmo.common.record.DefnStr;
import org.cosmo.common.record.DefnXML;
import org.cosmo.common.record.Meta;
import org.cosmo.common.record.Record;
import org.cosmo.common.record.Tx;
import org.cosmo.common.record.XML;
import org.cosmo.common.record.XMLStore;
import org.cosmo.common.service.UserManager;
import org.cosmo.common.util.New;


public class User <T extends Record> implements Record
{
	
	public static Meta Meta = org.cosmo.common.record.Meta.Create(User.class).
		requiresUniqueness(UniqueIdFactory.class).
		readCache(100, 10, F);
	
	public static DefnStr<User> UserName = Meta.DefnStr(512).index(T);
	public static DefnStr<User> Password = Meta.DefnStr(512).index(T);	
	public static DefnStr<User> Email = Meta.DefnStr(512).index(T);
	public static DefnXML<User> Profile = Meta.DefnXML(512).xmlStore(XMLStore.class);

    protected String _userName;
    protected String _password;
    protected String _email;
    	// XXX watch memory consumption of this - do lazy get, and bit vector for sites
    public XML _profile;
    

	protected Tx<T> _proxy;

	
	public User ()
	{
		_proxy = meta().store().createTx(this);
	}
	
	
	public User (String userName, String email, String password, String... args)
	  throws IOException
	{		
		User user = UserManager.Instance.userByEmail(email);
	  	if (user == null) {
			_proxy = meta().store().createTx(this);
	  		initUser(userName, email, password, args);
	    	tx().insertSync(); 	    		// InsertSync applies that this call will return only after Master has created
	  	}
	  	else {
	  		throw new IllegalArgumentException("User of same email already exist:" + email);
	    }
	}
	
	
	public void initUser (String userName, String email, String password, String... args)
	  throws IOException
	{
		_email = email;
    	_userName = userName;
    	_password = password;
    	_profile = ((DefnXML)meta().defnByName("Profile")).getXMLStore().create(generateRelativeFileNameFor("profile.xml"), "usr");
    	PublicFolder.createRootFolderOn(_profile);		
	}
	
		
	public Tx<T> tx ()
	{
			return _proxy;
	}
		
	public Meta meta ()
	{
		return Meta;
	}	

    public String getUserName ()
    {
        return _userName;
    }

    public void setUserName (String userName)
    {
        this._userName = userName;
    }

    public String getPassword ()
    {
        return _password;
    }

    public void setPassword (String password)
    {
        // todo: needs to be replaced with code that applies one-way hash to stored passwords
        this._password = password;
    }

    public boolean matchingPassword (String candidate)
    {
        // Todo: password should be one-way hashed!
        return (candidate != null && candidate.equals(_password));
    }

    public String getEmail ()
    {
        return _email;
    }

    public void setEmail (String email)
    {
        this._email = email;
    }
    

    public String generateRelativeFileNameFor (String fileName)
    {
       	// returns a relative file name for given fileName
    	// i.e   user@abc.com/filename .. when used becomes xml@XMLStore/user@abc.com/filename
    	return New.str(this._email, "/", fileName);
    }
    
    
    public T cast ()
    {
    	return (T)this;
    }
    
    
	public static class UniqueIdFactory extends Meta.UniqueIdFactory
	{
		public UniqueIdFactory (Meta meta)
		{
			super(meta);
		}
		
		
		public String generate (Record record)
		{
			User user = (User)record;			
			String uid = user._email;			
			return uid;
		}
	}
}
