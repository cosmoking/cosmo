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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.cosmo.common.net.Script;
import org.cosmo.common.net.Session;
import org.cosmo.common.template.Binding;
import org.cosmo.common.template.BindingSrc;
import org.cosmo.common.template.Content;
import org.cosmo.common.template.Page;
import org.cosmo.common.util.Bytes;
import org.cosmo.common.util.DeflatableBytes;
import org.cosmo.common.util.JSONList;
import org.cosmo.common.util.Util;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.json.JSONException;
import org.json.JSONObject;


public interface AjaxResponse
{
	public void writeRawContent (Object o) throws IOException;		
	
	public void generateContentFor (Object data, Script script) throws IOException;
	
	public void generateContentFor (Map<? extends CharSequence, Object> data, Script script) throws IOException;	
	
	public void generateContentFor (List<? extends CharSequence> data, Script script) throws JSONException, IOException;
		
	public void generateContentFor (Map<? extends CharSequence, Object> data, String DivId) throws IOException;
	
	public void generateContentFor (Script script) throws IOException;
	
	public DeflatableBytes responsePayload ();
	
	
	public static class AsyncImpl extends Impl 
	{
		public AjaxGetHandler _handler;		
		public Session _session;
		public MessageEvent _e;
		
		
		public AsyncImpl (AjaxGetHandler handler, MessageEvent e, Session session)
		{
			super();
			_handler = handler;
			_e = e;
			_session = session;
		}
		
		public void writeResponse ()
		{
			_handler.writeResponse(_e, _session, this);
		}
		
		public DeflatableBytes responsePayload ()
		{
			return AbstractContentHandler.AsyncResponsePayload;
		}		
	}


	public static class Impl extends DeflatableBytes implements AjaxResponse
	{

		public static final byte[] DataBoundaryMarker = Util.UTF8("1234567890");
		

		public void generateContentFor (Script script)
		  throws IOException
		{
			writeObjectTo("");
			writeObjectTo(script.formattedBytes());
		}

		public void generateContentFor (Object data, Script script)
		  throws IOException
		{
			writeObjectTo(data);
			writeObjectTo(script.formattedBytes());
		}

		public void generateContentFor (List<? extends CharSequence> data, Script script)
		  throws JSONException, IOException
		{
			writeArrayTo(data);
			writeObjectTo(script.formattedBytes());
		}


		public void generateContentFor (Map<? extends CharSequence, Object> data, Script script)
		  throws IOException
		{
			writeMapTo(data);
			writeObjectTo(script.formattedBytes());
		}

		public void generateContentFor (String templateName, String bindingName, Session session, Script script)
		  throws Exception
		{
			Page page = Page.byName(templateName);
			Content container = new Content();

				// if there is a specific binding
			if (bindingName != null) {
				BindingSrc bindingHandler = BindingSrc.instance(page._bindingSrc);
				Binding binding = page.BindingByName(bindingName);
				bindingHandler.applyValue(binding, page, container, session, page._options);
			}
			else {
				page.append(session, container);
			}

			writeContentTo(container);
			writeObjectTo(script.formattedBytes());
		}


		public void generateContentFor (Map<? extends CharSequence, Object> data, String DivId)
		  throws IOException
		{
			generateContentFor(data, Script.ApplyAttributeData.apply(DivId));
		}



		public void writeRawContent (Object o)
		  throws IOException
		{
			if (o instanceof Bytes) {
				write((Bytes)o);
			}
			else {
				write (Util.bytes(o));
			}
		}


		public void writeContentTo (Content container)
		  throws IOException
		{
			write(DataBoundaryMarker);
			container.writeTo(this);
			write(DataBoundaryMarker);
		}


		public void writeMapTo (Map map)
		  throws IOException
		{
			write(DataBoundaryMarker);
			JSONObject jsonMap = new JSONObject(map);
			writeRawContent(jsonMap.toString());
			write(DataBoundaryMarker);
		}


		public void writeObjectTo (Object obj)
		  throws IOException
		{
			write(DataBoundaryMarker);
			writeRawContent(obj);
			write(DataBoundaryMarker);
		}


		public void writeArrayTo (List array)
		  throws JSONException, IOException
		{
			write(DataBoundaryMarker);
			JSONList jsonArray = new JSONList(array);
			jsonArray.writeBytes(this);
			write(DataBoundaryMarker);
		}
		
			
		public DeflatableBytes responsePayload ()
		{		
				// if it's async we will simply return the AysncResponse Marker back 
				// as the content will be written later.
				// Otherwise return the response
			return this;
		
		}
	}	

}




