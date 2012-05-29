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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.DefaultHttpMessage;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;


import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.*;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.*;
import org.jboss.netty.handler.codec.http.HttpVersion;
import static org.jboss.netty.handler.codec.http.HttpVersion.*;

import org.cosmo.common.util.Bytes;
import org.cosmo.common.util.FmtBytes;
import org.cosmo.common.util.Util;


public class HttpResponse extends FmtBytes
{

	public static final byte[] SP = Util.ASCII(" ");
	public static final byte[] LF = Util.ASCII("\n");
	public static final byte[] CR = Util.ASCII("\r");
	public static final byte[] COLON = Util.ASCII(":");
	public static final Object Variable = new Object();

		// used at resource only
	public static final HttpResponse OK_Gzip = new HttpResponse(HTTP_1_1, OK).add(CONTENT_LENGTH, Variable).add(CONTENT_ENCODING, "gzip").done();

		// used for content - IE
	public static final HttpResponse OK_GzipNoCache = new HttpResponse(HTTP_1_1, OK).add(CONTENT_TYPE, "text/html").add(CONTENT_LENGTH, Variable).add(CONTENT_ENCODING, "gzip").add(CACHE_CONTROL, "no-cache").add(PRAGMA, "no-cache").add(EXPIRES, "-1").done();
	public static final HttpResponse OK_PlainNoCache = new HttpResponse(HTTP_1_1, OK).add(CONTENT_TYPE, "text/html").add(CONTENT_LENGTH, Variable).add(CACHE_CONTROL, "no-cache").add(PRAGMA, "no-cache").add(EXPIRES, "-1").done();


		// used for content - Rest
	public static final HttpResponse OK_Deflate = new HttpResponse(HTTP_1_1, OK).add(CONTENT_TYPE, "text/html").add(CONTENT_LENGTH, Variable).add(CONTENT_ENCODING, "deflate").done();
	public static final HttpResponse OK_Plain = new HttpResponse(HTTP_1_1, OK).add(CONTENT_TYPE, "text/html").add(CONTENT_LENGTH, Variable).done();



	private HttpResponse (HttpVersion version, HttpResponseStatus status)
	{
		super();
	    write(Util.ASCII(version.toString()));
	    write(SP);
	    write(Util.ASCII(String.valueOf(status.getCode())));
	    write(SP);
	    write(Util.ASCII(status.getReasonPhrase()));
	    write(CR);
	    write(LF);
	}

	private HttpResponse add (String name, Object value)
	{
		write(Util.ASCII(name));
		write(COLON);
		write(SP);
		if (value == Variable) {
			mark();
		}
		else {
			write(Util.ASCII(value.toString()));
		}
		write(CR);
		write(LF);
		return this;
	}

	private HttpResponse done ()
	{
        write(CR);
        write(LF);
	    makeExact();
	    return this;
	}
}
