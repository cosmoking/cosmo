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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.cosmo.common.net.StringTokens;
import org.cosmo.common.util.Bytes;
import org.cosmo.common.util.CRCBytes;
import org.cosmo.common.util.New;
import org.cosmo.common.util.Util;

/*
 *  First the client lands on BootStrapHTML.jsl where resource tokens are gathered via getWebResourceFilesToken().
 *  Then it bounces back to AbstractContentHandler where the tokens are store in session.
 *  When attempt to render the main page - the DeclHeader then based on the token would return
 *  FullResourceBytes, CachedResrouceBytes, or HTMLDecl as the header.
 *  Once returned, on the javascript ready() event the main.js then calls setWebResourceFilesToken()
 *  to store the tokens.
 *
 *  XXXXXXXX NOTE- for js files that has decl - ie  "/*!"  remember to remove the "!" so that minify can work and compress to single line
 *
 */
public class WebResourceFiles {


		// Param to decide if use cache of not -> change the order with care, as the sequence of this value gets set is important
	public static boolean UseCacheResourceDeclaration = AppServer.Production;	
	
		// desktop and core
	public static WebResourceFiles CSSImg = new WebResourceFiles("CSSImg", Type.CSSIMG,
		"img.css"
	);
	
	public static WebResourceFiles CSSCore = new WebResourceFiles("CSSCore", Type.CSS,
		"default.css",
		"cosmo.css",
		"style.css",
		"main.css"
	);
	public static WebResourceFiles JS3rdParty = new WebResourceFiles("JS3rdParty", Type.JS,
		"jquery-1.6.3.js",
		"jquery-ui-1.8.16.custom.min.js",	
		"scrollTo.js",
		"jquery.mousewheel.js"
	);
	public static WebResourceFiles JSCore = new WebResourceFiles("JSCore", Type.JS,
		"util.js",
		"cosmo.js",
		"main.js"
	);

		// mobile
	public static WebResourceFiles MCSSCore = new WebResourceFiles("MCSSCore", Type.CSS,
			"mobile.css"
	);

	public static WebResourceFiles MJS3rdParty = new WebResourceFiles("MJS3rdParty", Type.JS,
			"jquery-1.6.3.js",
			"iscroll.js"
	);
	public static WebResourceFiles MJSCore = new WebResourceFiles("MJSCore", Type.JS,
			"mobile.js"
	);

		// tablet
	public static WebResourceFiles TCSSCore = new WebResourceFiles("TCSSCore", Type.CSS,
			"tablet.css"
	);

	public static WebResourceFiles TJSCore = new WebResourceFiles("TJSCore", Type.JS,
			"tablet.js"
	);






	String _name;
	long _checksumToken;
	Bytes _fullResourceBytesScript;
	Bytes _cachResourceBytesScript;
	String _id;
	String[] _resourceFileNames;
	Type _resourceType;

	public WebResourceFiles (String name, Type type, String... fileNames)
	{

		try {
			_name = name;
			_resourceFileNames = fileNames;
			_resourceType = type;
			_id = "'X" + name() + "'";  // ie, 'XJSCore' ease of use for referencing in javascript and html


				// combine all resource files into one byte stream and calculate checksum
			CRCBytes files = new CRCBytes();
			for (String file : fileNames) {
				Bytes bytes = resourceMinify(file, type);
				files.write(bytes);
			}
			files.update();

			_checksumToken = files.checksum();
			_fullResourceBytesScript = new Bytes();
			_cachResourceBytesScript = new Bytes();

				// generate both Full and Cached Script declaration into bytes depending on the type
			if (Type.JS == type) {
				_fullResourceBytesScript.write(Util.bytes("<script type='text/javascript' id=" + _id + " token=\"" + _checksumToken + "\">"));
				_fullResourceBytesScript.write(files);
				_fullResourceBytesScript.write(Util.bytes("</script>\n"));
				_cachResourceBytesScript.write(Util.bytes("<script type='text/javascript'>loadScript(" + _id + ",\"\");</script>\n"));
			}

			if (Type.CSS == type || Type.CSSIMG == type) {
				_fullResourceBytesScript.write(Util.bytes("<style id=" + _id + " token=\"" + _checksumToken + "\">"));
				_fullResourceBytesScript.write(files);
				_fullResourceBytesScript.write(Util.bytes("</style>\n"));
				_cachResourceBytesScript.write(Util.bytes("<script type='text/javascript'>loadStyle(" + _id + ",\"\");</script>\n"));
			}
		}
		catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}


		// returns the minified js/css resources
	static final String ImgURLBeginToken = "'/2/";
	static final String ImgURLEndToken = "'";
	private static Bytes resourceMinify (String file, Type type)
	  throws IOException
	{

			// basically if this is false (ie mode=test) we skip the minify part to safe time
		if (! UseCacheResourceDeclaration) {
			return new Bytes();
		}

		Bytes bytes = Util.resourceMinify(new File(New.str(AppServer.ResourceDir, type._relativeResourceDir, file)));
			// cssImg need to convert the style to DataURI
		if (Type.CSSIMG == type) {
			// css img parsing tokens in img.css, ie 	background:url('/2/x_black_bg.png') repeat !important;
			File imgResourceDir = new File(New.str(AppServer.ResourceDir, Type.IMG._relativeResourceDir));
			bytes.set(Util.bytes(Util.cssImgBase64DataURI(bytes, imgResourceDir, ImgURLBeginToken, ImgURLEndToken)));
		}
		return bytes;
	}

	public String name ()
	{
		return _name;
	}


		// appends resource bytes to the sink based on the token value, ie
	public void append (Bytes sink, Tokens resourceTokens)
	{
		// checks if the token is the same or different in order
		// to append the right resource Bytes
		if (_checksumToken != resourceTokens.next()) {
			sink.write(_fullResourceBytesScript);
		}
		else {
			sink.write(_cachResourceBytesScript);
		}
	}

		// the regular HTML resource declarations
	public String htmlDeclaration ()
	{
		StringBuffer buf = new StringBuffer();
		for (String aFileName : _resourceFileNames) {
			buf.append(_resourceType._htmlDeclaration.toString().replace("%s", aFileName));
			buf.append("\n");
		}
		return buf.toString();
	}



	//DO NOT CHANGE THIS as the id is a properly format "String" in javascript
	@Override
	public String toString ()
	{
		return _id;
	}


	public static enum Type {
		JS ("/js/", "<script src='/4/%s' type='text/javascript'></script>"),
		CSS ("/css/", "<link href='/3/%s' type='text/css' rel='stylesheet'>"),
		CSSIMG ("/css/", "<link href='/3/%s' type='text/css' rel='stylesheet'>"),
		IMG("/img/", "TBD");

		public final String _relativeResourceDir;
		public final String _htmlDeclaration;

		private Type (String relativeResourceDir, String htmlDeclaration)
		{
			_relativeResourceDir = relativeResourceDir;
			_htmlDeclaration = htmlDeclaration;
		}
	};



	public static class CacheBundle {


			// a token pattern that represents that client browser does not have any resource files/tokens stored
		private final String EmptyCacheTokens = "NaN,NaN,NaN,NaN,";
			// a token pattern that represents that client browser have all the resource, ie "4199647106,1575227193,2741382808,3370212672,"
		private final String FullCacheTokens;

			// The Resource file used in JS ie // ['XJS3rdParty', 'XJSCore', 'XCSSCore', 'XCSSImg']
		public final String ArrayString;

			// pre generated the Bytes Cache for both "Emtpy" and "Full" and "Regular HTML decl"
		public final Bytes FullResourceBytes;
		public final Bytes CacheResourceBytes;
		public final Bytes HTMLResourceBytes;
		public final WebResourceFiles[] WebResourceFiles;
		
		// Cache Bundle Set declaration for each medium
	public static CacheBundle Desktop = new CacheBundle(new WebResourceFiles[]{CSSImg, CSSCore, JS3rdParty, JSCore});
	public static CacheBundle Mobile = new CacheBundle(new WebResourceFiles[]{CSSImg, CSSCore, MCSSCore, MJS3rdParty, JSCore, MJSCore});
	public static CacheBundle Tablet = new CacheBundle(new WebResourceFiles[]{CSSImg, CSSCore, TCSSCore, JS3rdParty, JSCore, TJSCore});
			

		public CacheBundle (WebResourceFiles[] webResourceFiles)
		{
			WebResourceFiles = webResourceFiles;
			ArrayString = Arrays.toString(webResourceFiles);

				// 3 types of cached resource
			FullResourceBytes = new Bytes();
			CacheResourceBytes = new Bytes();
			HTMLResourceBytes = new Bytes();

			StringBuffer buf = new StringBuffer();
			for (WebResourceFiles aWebResourceFiles: webResourceFiles) {
				FullResourceBytes.write(aWebResourceFiles._fullResourceBytesScript);
				CacheResourceBytes.write(aWebResourceFiles._cachResourceBytesScript);
				HTMLResourceBytes.write(aWebResourceFiles.htmlDeclaration());
				buf.append(aWebResourceFiles._checksumToken).append(",");
			}
			FullResourceBytes.makeExact();
			CacheResourceBytes.makeExact();
			FullCacheTokens = buf.toString();
		}

		// validate and generate ALL resourceBytes as a Byte
		public Bytes validateAndGenerateResourceBytes (Tokens webResourceFilesTokens)
		{
			Bytes bytes = new Bytes(); // try pool later
			for (WebResourceFiles aWebResourceFile : WebResourceFiles) {
				aWebResourceFile.append(bytes, webResourceFilesTokens);
			}
			return bytes.makeExact();
		}

		public boolean isEmptyToken (Tokens tokens)
		{
			return EmptyCacheTokens.equals(tokens._tokens.path());
		}

		public boolean isFullToken (Tokens tokens)
		{
			return FullCacheTokens.equals(tokens._tokens.path());
		}
	}

		// abstraction used to track the token passed in between the client browser and server
		// It's basically a comma separated value of checksums
	public static class Tokens
	{
		StringTokens _tokens;

			// expect format to be  "4199647106,3316518467,2741382808,3370212672," ie from BootStrapHTML.jwl  getWebResourceFilesToken()
		public Tokens (String requestArgs)
		{
			_tokens = StringTokens.on(requestArgs);
		}

			// gets next token
		public long next ()
		{
			String token = _tokens.next();
			return token == null || token.isEmpty() ? Long.MIN_VALUE : Util.parseLong(token);
		}
	}
}
