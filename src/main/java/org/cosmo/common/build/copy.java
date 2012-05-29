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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

public class copy extends BuildBase
{

	public static void main (String[] args) throws Exception
	{
		exec (args[0], args[1]);
	}


	public static void exec (Object from, Object to)
	  throws Exception
	{
		exec(new File(from.toString()), new File(to.toString()));
	}


	public static void exec (File from, File to)
	  throws Exception
	{
		if (from.isDirectory()) {
			copyDir(from, to);
		}
		else {
			copyFile(from, to);
		}
	}

	private static void copyFile (File from, File to)
	  throws Exception
	{
		del.exec(to);
		to.getParentFile().mkdirs();
		FileChannel out = new FileOutputStream(to).getChannel();
		FileChannel in = new FileInputStream(from).getChannel();
		in.transferTo(0, in.size(), out);
		in.close();
		out.close();
		log("Copy from [", from.getAbsolutePath(), "] to [", to.getAbsolutePath(), "]");
	}


	private static void copyDir (File from, File to)
	  throws Exception
	{
		del.exec(to);
		for (File f : from.listFiles()) {
			if (f.isFile()) {
				copyFile(f, new File(to, f.getName()));
			}
			else {
				copyDir(f, new File(to, f.getName()));
			}
		}
	}


}
