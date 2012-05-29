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


	// basically sets the classpath on "java.class.path" system property from the "lib" folder relative to the currentDir
public class classpath extends BuildBase{


	public static void main (String[] args) throws Exception
	{
		if (args.length == 0) {
			exec(currentDir());
		}
		else {
			exec(new File(args[0]));
		}
	}

	public static String exec (File rootDir) throws Exception
	{
		File[] libFiles = new File(rootDir, libDir().getName()).listFiles();

		StringBuffer classpath = new StringBuffer();
		classpath.append(classDir()).append(File.pathSeparatorChar);
		for (File libFile : libFiles) {
			classpath.append(libFile.getAbsolutePath()).append(File.pathSeparatorChar);
		}
		System.setProperty("java.class.path", classpath.toString());
		return classpath.toString();

	}


}
