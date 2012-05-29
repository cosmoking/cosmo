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

import org.cosmo.common.server.AppServer;



	// basically what this does is iterate throug all RssContent,
	// get the description, and regenerate short description, and update in the db
	// in the end, it exit itself.
	//
	// prerequist,
	// 1. is copy the master_logs in temp dir, as each time an update happens it's writes to log
	// 2. delete the shortDescription.clob file, so that it can be regernaeated
	// 3. copy back the master_logs when it's done
	// 4. this is run with the SAME VM and args as in net.Server

public class TaskServer extends AppServer
{
/*
	public static void main (String[] args) throws Exception
	{
		Server.main(args);

		int records = RssContent.Meta.store().count();



		for (int i = 0; i < records; i++) {
			String description = RssContent.Description.read(i).toString();

			RssContent aContent = new RssContent();
			try {
				JSONTokener t = new JSONTokener(description);
				aContent._description = t.nextValue().toString();
			}
			catch (Exception e) {
				e.printStackTrace();
				aContent._description = "NO CONTENT";
			}
			boolean converted = convertShortDescription(aContent);
			if (converted) {
				RssContent.ShortDescription.update(i, aContent._shortDescription);
			}

			if (i % 100 == 0) {
				System.out.println ("Updated : " + i);
			}
		}
		Control.prepareShutdown();
		System.exit(0);

	}

	public static boolean convertShortDescription (RssContent aContent) throws Exception
	{
		if (Util.UTF8(aContent._description.toString()).length > ClobStore.InMemoryLobSize) {

			String description = aContent._description.toString();
			StringBuilder buf = new StringBuilder();


				// get one object if there is one
			int begin = description.indexOf("<object");
			int end = 0;
			String object = null;
			if (begin > 0) {
				end = description.indexOf("</object>", begin);
				object = description.substring(begin + 7, end);
				buf.append("<object" + object + "</object>");
			}

			// get one iframe if there is one
			begin = description.indexOf("<iframe");
			end = 0;
			String iframe = null;
			if (begin > 0) {
				end = description.indexOf("</iframe>", begin);
				iframe = description.substring(begin + 7, end);
				buf.append("<iframe" + iframe + "</iframe>");
			}


			begin = description.indexOf(RssContentParser.ImgOriginalSrcAttrToken);
			// up to 5 images and pick one best - if it happens first 6 are ads. then what is going to happen is
			// all images get send to client side but got filtered and only text gets displayed
			for (int i = 0; i < 6 && begin > 0; i++) {
				begin = begin + RssContentParser.ImgOriginalSrcAttrToken.length() + 2;
				end = description.indexOf('\"', begin);
				String imgUrl = description.substring(begin, end);

				char[] imgHtml = New.ch("<img ", RssContentParser.ImgOriginalSrcAttrToken, "=\"", imgUrl, "\"/>");
				buf.append(imgHtml);
				begin = description.indexOf(RssContentParser.ImgOriginalSrcAttrToken, end);
			}

			// extract short description as well
			String plainDescription = aContent.plainDescription(256);

			aContent._shortDescription = JSONObject.quote(New.str("<p>", plainDescription, DefnStr.Ellipses, "</p>", buf.toString()));
			return true;
		}
		return false;
	}
*/
}
