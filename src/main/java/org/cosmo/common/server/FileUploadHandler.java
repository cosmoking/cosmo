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
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.HashMap;

import org.cosmo.common.model.User;
import org.cosmo.common.net.Session;
import org.cosmo.common.net.StringTokens;
import org.cosmo.common.service.SessionManager;
import org.cosmo.common.service.UserManager;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.http.HttpRequest;

public class FileUploadHandler extends AjaxGetHandler
{

	public static final File UploadDir = UserManager.Instance.userMeta().createFolderForClass(FileUploadHandler.class, "upload");

	private static final HashMap<String, FileUploadHandler> FileUploadHandlers = new HashMap();

	@Override
    public AjaxGetHandler get (String handler, Session session)
      throws Exception
    {
    	FileUploadHandler fileUploadHandler = FileUploadHandlers.get(handler);
		if (fileUploadHandler == null) {
			Class handlerClazz = Class.forName("net." + handler);
			fileUploadHandler = (FileUploadHandler)handlerClazz.newInstance();
			FileUploadHandlers.put(handler, fileUploadHandler);
		}
		return fileUploadHandler;
    }
}



class uploadUserBookmarkClicked extends FileUploadHandler
{
	public void appendToResponse (AjaxResponse ajaxResponse, Session session, StringTokens args, HttpRequest request)
      throws Exception
    {
		args = StringTokens.on(args.next(), StringTokens.NameValueSeparatorChar);
		String name = args.next();
		String value = args.next();

		ChannelBuffer buf = (ChannelBuffer)request.getContent();

		File file = new File(UploadDir, session._user.generateRelativeFileNameFor(value));
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		FileChannel out =  new FileOutputStream(file).getChannel();
		out.write(buf.toByteBuffer());
		out.close();

		ajaxResponse.writeRawContent("{'success':true}");
    }
}




