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

import java.io.FileOutputStream;


	// provides the version via Manifest.MF file - calling increment() will rewrite this file with version incremented
public class version extends BuildBase
{

	public static final String ParseToken = "Implementation-Version: ";
	public static final String InrcementCmd = "increment";


	public static int version ()
	  throws Exception
	{
		//File file = new File("META-INF","MANIFEST.MF");
		String versionFileContent = fileContent(ManifestFile);
		int b = versionFileContent.indexOf(ParseToken) + ParseToken.length();
		String verStr = versionFileContent.substring(b, b + 6);
		return Integer.valueOf(verStr);
	}

	public static String versionStr (int version)
	{
		String s = "000000" + String.valueOf(version);
		return s.substring(s.length() - 6, s.length());
	}

	public static String versionStr ()
	{
		try {
			return versionStr(version());
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new Error(e);
		}
	}


		// updates MANIFEST.MF with new version
	public static int increment ()
	  throws Exception
	{
		//File file = new File("META-INF","MANIFEST.MF");
		String versionFileContent = fileContent(ManifestFile);
		int version = version();
		versionFileContent = versionFileContent.replace(versionStr(version), versionStr(++version));
		FileOutputStream out = new FileOutputStream(ManifestFile, false);
		out.write(versionFileContent.getBytes());
		out.close();
		return version;
	}


	public static int exec (String cmd)
	  throws Exception
	{
				// increment
		if (InrcementCmd.equalsIgnoreCase(cmd)) {

				// recompile
			/*
			String[] compileCommand = new String[] {"javac", "-classpath",  "." , "-d", ".", "-sourcepath", "." ,  "version.java"};
			int exitCode = build.run.exec(SystemOut, compileCommand);
			if (exitCode != 0) {
				throw new IllegalArgumentException("Unable to increment Version");
			}
			*/

				// return new version
			return version.increment();
		}
		return version.version();
	}



	public static void main (String[] args) throws Exception
	{
		SystemOut.println(exec(args[0]));
	}
}

