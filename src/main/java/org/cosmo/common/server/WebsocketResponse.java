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
import org.cosmo.common.util.Bytes;
import org.cosmo.common.util.DeflatableBytes;
import org.cosmo.common.util.Util;
import org.json.JSONException;


public class WebsocketResponse extends Bytes implements AjaxResponse
{
	public void writeRawContent (Object o) throws IOException
	{
		super.write(Util.bytes(o));
	}
	
	public void generateContentFor (Object data, Script script) throws IOException
	{
		
	}
	
	public void generateContentFor (Map<? extends CharSequence, Object> data, Script script) throws IOException
	{
		
	}
	
	public void generateContentFor (List<? extends CharSequence> data, Script script) throws JSONException, IOException
	{
	
	}
	

	public void generateContentFor (Map<? extends CharSequence, Object> data, String DivId) throws IOException
	{
		
	}
	
	public void generateContentFor (Script script) throws IOException
	{
		
	}
	
	public DeflatableBytes responsePayload ()
	{
		throw new IllegalStateException("Not implemented");
	} 
}
