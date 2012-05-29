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

import org.cosmo.common.model.User;
import org.cosmo.common.record.Meta;



public class UserManager
{
	public static UserManager Instance = new UserManager(User.Meta);
	
	
	final Meta _userMeta;
	
		
	public UserManager (Meta userMeta) {
		_userMeta = userMeta;
	}
	
	public Meta userMeta ()
	{
		return _userMeta;
	}
	
	public User createUser ()
	{
		return null;
	}
	
	public User userByEmail (String email)
	  throws IOException
	{
		 return (User)_userMeta.search().firstRecord(_userMeta.defnByName("Email"), email, true);		
	}
	
	public boolean hasUser (String email)
	  throws IOException
	{
		return userByEmail(email) != null;
	}
}