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

import java.io.File;

public class del extends BuildBase
{

	public static void main (String[] files) throws Exception
	{
		for (String file : files) {
			exec(new File(file));
		}
	}

	public static void exec (Object file)
	  throws Exception
	{
		exec (new File(file.toString()));
	}


	public static void exec (File file)
	  throws Exception
	{
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				exec (f);
			}
			delete(file);
		}
		else {
			delete(file);
		}
	}

	private static void delete (File file)
	  throws Exception
	{
		file.delete();
		for (int i = 0; file.exists() && i < 20; i++) {
			file.delete();
			Thread.sleep(200);
		}

		if (file.exists()) {
			throw new Exception("Unable to delete " + file);
		}
	}

}

