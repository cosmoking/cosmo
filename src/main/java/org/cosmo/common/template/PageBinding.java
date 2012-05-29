package org.cosmo.common.template;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.cosmo.common.util.ArgList;
import org.cosmo.common.util.New;
import org.cosmo.common.view.TemplateRegion;

public class PageBinding extends Binding
{
	String _calleePageName;
	Page _calleePage;

	Page _callerPage;
	BindingSrc _callerBindingSrc;

	String _callBindingArgs;
	Map<String, Binding> _callBindings;


	public PageBinding (String bindingStr)
	{
		_calleePageName = bindingStr.substring(0, bindingStr.indexOf("{"));
		_callBindingArgs = bindingStr.substring(bindingStr.indexOf("{") + 1, bindingStr.indexOf("}"));
	}

	@Override
	public Object applyValue (Page page, BindingSrc bindingSrc, Content container, Object context, Options options)
	  throws Exception
	{
			// lazy init - move this to constructor until Template parsing dependency is fixed
		if (_calleePage == null) {
			_calleePage = Page.byName(_calleePageName);
			if (_calleePage == null) {
				throw new IllegalArgumentException(New.str("Page [", _calleePageName, "] is not found"));
			}

			if (_callBindingArgs.length() > 0) {
				_callBindings = new HashMap();
				Iterator<String> bindingTokens = new ArgList(_callBindingArgs, ':',',','`').iterator();
				while (bindingTokens.hasNext()) {
			    	String bindingName = bindingTokens.next();
			    	Binding binding = Parser.parseBinding(bindingTokens.next(), page, page._bindingSrc, options);
			    	if (!(binding instanceof Binding.ValueBinding)) {
			    		throw new IllegalArgumentException(New.str("Only allow Binding.AllowPassing for [", bindingName, "] not [", binding.getClass().getSimpleName()));
			    	}
			    	_callBindings.put(bindingName, binding);

			    }
			}
			_callBindings = _callBindings != null && _callBindings.size() > 0 ? _callBindings : null;
			_callerPage = page;
			_callerBindingSrc = bindingSrc;
		}


		BindingSrc pageBindingSrc = BindingSrc.instance(_calleePage._bindingSrc);
		if (_callBindings != null) {
			pageBindingSrc._caller = this;
		}
		_calleePage.append(0, _calleePage._segmentArray.length, 0, context, container, pageBindingSrc);
		return null;
	}

	
	public Map callArgsToMap (String callBindingArgs)
	{
		Map callBindings = new HashMap();
		if (callBindingArgs.length() > 0) {
			Iterator<String> bindingTokens = new ArgList(callBindingArgs, ':',',','`').iterator();
			while (bindingTokens.hasNext()) {
		    	String bindingName = bindingTokens.next();
		    	String bindingValue = bindingTokens.next();
		    	callBindings.put(bindingName, bindingValue);
		    }
		}		
		return callBindings;
	}
	

	public Object getValue (Page page, BindingSrc bindingSrc, Object context, Options options) throws Exception
	{
		throw new IllegalArgumentException("Not Supported");
	}


	public String name ()
	{	
			// if callerPage has a callingArg of "name" then return it  -
			// ie TemplateRegion page  expects an arg of "name"  so in this case it will return that value
		Map values = callArgsToMap(_callBindingArgs);
		Object value = values.get("name");
		if (value != null) {
			return new StringBinding(value.toString()).name();
		}
		throw new IllegalArgumentException("Not Supported");
	}
	
	public String calleePageName ()
	{
		return this._calleePageName;
	}



}