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

import org.cosmo.common.util.Bytes;
import org.cosmo.common.util.FmtStr;

public class Script extends FmtStr {
	
	
		// applyBindingToId('#SomeDivId', HtmlSegmentsTable['SomeDivId'], $.parseJSON(data), callbckfunction());
	public static final Script ApplyBindings = new Script("applyBindingToId('#^', HtmlSegmentsTable['^'], $.parseJSON(data), '^');");
	   // applyBindingToId('#SomeDivId', HtmlSegmentsTable['SomeDivId'], $.parseJSON(data), callbckfunction()); processEventMeta($("#SomeDivId")); 	
	public static final Script ApplyBindingsAndEvent = new Script("applyBindingToId('#^', HtmlSegmentsTable['^'], $.parseJSON(data), '^');processEventMeta($('#^'));");
		//  processEventMeta($("#SomeDivId"));		
	public static final Script ApplyHtmlAndEvent = new Script("$('#^').html(data); processEventMeta($('#^'));");		
		// $('#SomeDivId').html(data);		
	public static final Script ApplyHtml = new Script("$('#^').html(data);");
		// $('#SomeDivId').attr('_ajaxData', eval(data));
	public static final Script ApplyAttributeData = new Script("$('#^').attr('_ajaxData', data);");
		// some function(data);  
	public static final Script ApplyCallback = new Script("^(eval(data), this._serverCallbackContext, ^)");
		// prompt login dialog
	public static final Script ApplyPromptLogin = new Script("$('#openLoginDialog').trigger('click');");
		// apply login timestamp
	public static final Script ApplyLoginTimeStamp = new Script("localStorage.lastLogin = new Date();");
		// applies new Html Body
	public static final Script ApplyNewBody = new Script("$('HTML BODY').html(data); initialize();");
	
		// should generialze this one
	public static final Script ApplyRssContent = new Script("$('#^ ._longDesc:first').html($.parseJSON(data));");
	
	public static final Script NoOp = new Script("");
	
	
	Bytes _formattedBytes;	
	
		// used to create a "formattable string" script, ie uses apply() to generated formatted bytes
	public Script (String script)
	{
		super(script);
	}
	
		// used to create "already" formatted script bytes 
	private Script (Bytes formattedBytes)
	{
		super(0);
		_formattedBytes = formattedBytes;
	}
	
	public Script apply (Object... args)
	{
		return new Script(fmtBytes(args));
	}

	public Bytes formattedBytes ()
	{
		return _formattedBytes == null 
			? this
			:_formattedBytes;
	}
	
}
