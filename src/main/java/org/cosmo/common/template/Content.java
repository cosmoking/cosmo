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
package org.cosmo.common.template;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.cosmo.common.util.Bytes;
import org.cosmo.common.util.Util;
import org.jboss.netty.buffer.ChannelBuffer;
import org.json.JSONObject;



/** TODO implment an interface so BindingOnlyContainer don't have the segements and as such */
/* TODO write directly to Bytes rather than another list of strings */
// also pool these as it's been instantiated very frequently
public class Content<T>
{
	
	public static final String BindingMarker = "!BM!"; 	
	public static final byte[] BindingMarkerBytes = Util.UTF8(BindingMarker);
	public static final byte[] BindingMarkerBytesQuoted = Util.UTF8(JSONObject.quote(BindingMarker));
	public static final String ORMarker = "||"; // in cases where OR expression is used this can be used as ^OR^	
	
	
	public List _segments;
	public List _bindings;

	public Content ()
	{
		this(new ArrayList());
	}

	Content (List<byte[]> parsedTemplate, List<Object> bindingContainer)
	{
		_segments = parsedTemplate;
		_bindings = bindingContainer;
	}

	Content (List<byte[]> parsedTemplate)
	{
		_segments = parsedTemplate;
		_bindings = new ArrayList();
	}


	public void pushSegment (Page page, int segmentId)
	{
		_segments.add((page.segmentArray()[segmentId]));
	}

	public void pushBinding (Object bindingValue)
	{
		_segments.add(BindingMarkerBytes);
		_bindings.add(bindingValue);
	}


	public Bytes dump ()
	  throws IOException
	{
		Bytes bytes = new Bytes();
		writeTo(bytes);
		return bytes;
	}


	public void writeTo (ChannelBuffer out)
	  throws IOException
	{
		int bindingIdx = 0;
		for (Object segment: _segments) {
			if (segment == BindingMarkerBytes || segment == BindingMarkerBytesQuoted) {
				Object bindingValue = _bindings.get(bindingIdx++);
				if (bindingValue == null) {
					continue;
				}
				if (bindingValue instanceof Bytes) {
					Bytes bytes = (Bytes)bindingValue;
					out.writeBytes(bytes.bytes(), 0, bytes.count());
				}
				else {
					out.writeBytes(Util.bytes(bindingValue));
				}
			}
			else {
				if (segment instanceof Bytes) {
					Bytes bytes = (Bytes)segment;
					out.writeBytes(bytes.bytes(), 0, bytes.count());
				}
				else {
					out.writeBytes((byte[])segment);
				}
			}
		}
	}


	public void writeTo (Bytes out)
	  throws IOException
	{
		int bindingIdx = 0;
		for (Object segment: _segments) {
			if (segment == BindingMarkerBytes || segment == BindingMarkerBytesQuoted) {
				Object bindingValue = _bindings.get(bindingIdx++);
				if (bindingValue == null) {
					continue;
				}
				if (bindingValue instanceof Bytes) {
					Bytes bytes = (Bytes)bindingValue;
					out.write(bytes.bytes(), 0, bytes.count());
				}
				else {
					out.write(Util.bytes(bindingValue));
				}
			}
			else {
				if (segment instanceof Bytes) {
					Bytes bytes = (Bytes)segment;
					out.write(bytes.bytes(), 0, bytes.count());
				}
				else {
					out.write((byte[])segment);
				}
			}
		}
	}


	public void writeTo (PrintStream out)
	  throws IOException
	{
		Bytes bytes = new Bytes();
		writeTo(bytes);
		out.println(bytes);
	}

}



