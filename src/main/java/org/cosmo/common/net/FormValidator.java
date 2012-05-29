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

import java.util.HashMap;
import java.util.Map;

import org.cosmo.common.util.Util;


public class FormValidator 
{
	public static enum Type
	{
		notBlank {
			public boolean pass (Object s)
			{
				return s!= null && !Util.isBlank(s.toString());
			}			
		},
		notNull {
			public boolean pass (Object s)
			{
				return s != null;
			}			
		},
		isTrue {
			public boolean pass (Object s)
			{
				return s != null && Boolean.TRUE == s;
			}			
		},

		isFalse {
			public boolean pass (Object s)
			{
				return s != null && Boolean.FALSE == s;
			}			
		};

		abstract public boolean pass (Object s);
		
		public boolean fail (Object s)
		{
			return !pass(s);
		}
	}
	
	
	public Map _validationResult;	
	public Object _content;
	public String _errMsg;
	public String _elementId;
	public Type _type;
	
	
	public FormValidator ()
	{
		_validationResult = new HashMap();
		clearParamsForNextCheck();
	}
	
	public FormValidator notNull (Object content)
	{
		_content = content;
		_type = Type.notNull;
		return this;
	}
	
	public FormValidator notBlank(Object content) 
	{
		_content = content;
		_type = Type.notBlank;
		return this;
	}
	
	public FormValidator isTrue (Object content)
	{
		_content = content;
		_type = Type.isTrue;
		return this;
	}
	
	public FormValidator isFalse (Object content) 
	{
		_content = content;
		_type = Type.isFalse;
		return this;
	}
	
	
	public FormValidator withMessage (String errMsg)
	{
		_errMsg = errMsg;
		return this;
	}
	
	public FormValidator onElement (String elementId)
	{
		_elementId = elementId;
		return this;
	}
	
	public void clearParamsForNextCheck ()
	{	
		_content = null;
		_errMsg = null;
		_elementId = null;
		_type = null;		
	}
	
	public boolean check (boolean throwException)
	{
		if (_type.fail(_content)) {
			_validationResult.put(_elementId, _errMsg);
			clearParamsForNextCheck();
			if (throwException) {
				throw BreakOnError.Instance; 
			}			
			return false;
		}
		clearParamsForNextCheck();		
		return true;
		
	}
	
	public boolean check ()
	{
		return check(true);
	}
	
	public boolean hasErrors ()
	{
		return !noErrors();
	}
	
	public boolean noErrors ()
	{
		return _validationResult.isEmpty();		
	}
	
	
	public static class BreakOnError extends RuntimeException 
	{
		public static final BreakOnError Instance = new BreakOnError();
	}
	
}
