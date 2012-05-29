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

import static org.jboss.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.cosmo.common.net.HttpError;
import org.cosmo.common.net.HttpResponse;
import org.cosmo.common.net.StringTokens;
import org.cosmo.common.pool.ChecksumPool;
import org.cosmo.common.util.Bytes;
import org.cosmo.common.util.New;
import org.cosmo.common.util.Util;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.stream.ChunkedStream;


// same level as the AbstractContentHandler but just handles the resource
// skips sessions and client information stuff
// TODO: skip parsing header, return gzip
// TODO: remove separte IECache. consider user GZIPOutputStream so we can just use "gzip" in HttpResponseHeader
abstract public class AbstractResourceHandler extends ServerHandler
{

	public static boolean EnableCache = false;  // use for debugging purpose.. rapid turnaround.., loads from file everytime
	public static boolean EnableMinified = false; // uses YUI to minified the js and css

	abstract public String resourceDir ();


	private static final Map<String, ResourceFileCacheEntry> Cache = new HashMap();



	@Override
	public ChannelFuture handleRequest (ChannelHandlerContext ctx, MessageEvent e, StringTokens args, StringTokens bootStrapArgs)
	  throws Exception
	{
		HttpRequest request = (HttpRequest) e.getMessage();
		String path = args.remainingPath();
		if (path == null) {
			throw new HttpError(BAD_REQUEST, request.getUri());
		}

		ResourceFileCacheEntry cache = Cache.get(path);
		if (cache == null || !EnableCache) {
			File file = new File(New.str(AppServer.ResourceDir, resourceDir(), path));
			if (!file.isFile()) {
				throw new HttpError(NOT_FOUND, request.getUri());
			}
			cache = getResourceFileCacheEntry(file, HttpResponse.OK_Gzip, AppServer.TransferEncodingChunking);
			Cache.put(path, cache);
		}


		if (AppServer.TransferEncodingChunking) {
			ctx.getChannel().write(cache._responseHeader.toChannelBuffer());
			return ctx.getChannel().write(new ChunkedStream(cache._gzippedPayload.toInputStream()));
		}
		else {
			return ctx.getChannel().write(cache._gzippedPayload.toChannelBuffer());
		}
	}


	public ResourceFileCacheEntry getResourceFileCacheEntry (File file, HttpResponse response, boolean transferEncodingChunking)
	  throws IOException
	{
		return new ResourceFileCacheEntry(file, HttpResponse.OK_Gzip, AppServer.TransferEncodingChunking);
	}
}


	// basically gzip the resource
class ResourceFileCacheEntry
{
	Bytes _responseHeader;
	Bytes _gzippedPayload;
	long _checksum;

	public ResourceFileCacheEntry (File file, HttpResponse response, boolean transferEncodingChunking)
	  throws IOException
	{
		Bytes rawPayload = loadBytes(file);
		Bytes gzipBytes = new Bytes();
		GZIPOutputStream gzip = new GZIPOutputStream(gzipBytes);
		gzip.write(rawPayload.bytes(), 0, rawPayload.count());
		gzip.finish();
		Bytes responseBytes = response.fmtBytes(gzipBytes.count());
		if (transferEncodingChunking) {
			_responseHeader = responseBytes;
			_gzippedPayload = gzipBytes;
		}
		else {
			_responseHeader = null;
			_gzippedPayload = new Bytes(responseBytes.bytes(), gzipBytes.makeExact().bytes());
		}
		_checksum = ChecksumPool.Instance.generateChecksum(_gzippedPayload);
	}

	public Bytes loadBytes (File file)
	  throws IOException
	{
		return Bytes.load(file);

	}
}

	// use YUI to minify javascripts - not css yet as file are pretty small
class MinifiedResourceFileCacheEntry extends ResourceFileCacheEntry
{
	public MinifiedResourceFileCacheEntry (File file, HttpResponse response, boolean transferEncodingChunking)
	  throws IOException
	{
		super(file, response, transferEncodingChunking);
	}

	@Override
	public Bytes loadBytes (File file)
	  throws IOException
	{
		if (!AbstractResourceHandler.EnableMinified) {
			return super.loadBytes(file);
		}
		return Util.resourceMinify(file);
	}
}





/*

class WebResource
{
	final long _checksum;
	final Bytes _bytes;

	public WebResource ()
	{
	}

	public void load (File resourceFile)
	{
		_bytes = Bytes.load(resourceFile);
		_checksum = ChecksumPool.Instance.generateChecksum(_bytes);
	}
}

class MinifiedWebResource extends WebResource
{

	@Override
	public void load (File resourceFile)
	{
		_bytes = Util.resourceMinify(resourceFile);
		_checksum = ChecksumPool.Instance.generateChecksum(_bytes);
	}
}

class DataURIMinifiedImageWebResource extends MinifiedWebResource
{

	@Override
	public void load (File resourceFile)
	{
		super.load(resourceFile);
		CharSequence cssImgBase64DataURI = Util.cssImgBase64DataURI(_bytes, resourceFile.getParentFile());
		_checksum = ChecksumPool.Instance.generateChecksum(_bytes);
	}
}

class CombinedWebResource extends WebResource
{
	@Override
	public void load (File resourceFile)
	{
		super.load(resourceFile);
		CharSequence cssImgBase64DataURI = Util.cssImgBase64DataURI(_bytes, resourceFile.getParentFile());
		_checksum = ChecksumPool.Instance.generateChecksum(_bytes);
	}
}

*/
