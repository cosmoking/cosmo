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
package org.cosmo.common.build;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class stopQueue extends BuildBase {

	public static void main (String[] args) throws Exception
	{
		URL url = new URL("http://192.168.1.102:81/8/stopRecordOperations/true");
		exec(url);
	}

	public static void exec (URL url)
	  throws IOException
	{
		HttpURLConnection uc = null;
		try {
			Object c = url.openConnection();
			if (!(c instanceof HttpURLConnection)) {
				throw new IOException("Expect url protocol but get " + url);
			}
			uc = (HttpURLConnection)url.openConnection();
			uc.setConnectTimeout(5000);
			uc.setReadTimeout(4000);
			uc.setAllowUserInteraction(false);
			uc.setUseCaches(false);
			uc.setDoOutput(false);
			uc.setRequestMethod("GET");
			uc.addRequestProperty("User-Agent",	"Mozilla");

			InputStream in = uc.getInputStream();
			int b = in.read();
			while (b > 0) {
				log((char)b);
				b = in.read();
			}
			in.close();

		}
		finally {
			if (uc != null) {
				try {
					uc.disconnect();
				}
				catch (Exception e) {
				}
			}
		}
	}
}
