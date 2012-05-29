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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ProcessBuilder;
import java.util.Arrays;
import java.util.List;


	// compiles files
public class compile extends BuildBase {

	public static void main (String[] args) throws Exception
	{
		exec(args);
	}

	public static void exec (String[] args) throws Exception
	{
		cleanDir(classDir());
		classpath.exec(parentDir());

		File[] packageDirs = parentDir().listFiles();
		for (File packageDir : packageDirs) {
			if (packageDir.isDirectory()) {

			String[] compileCommand = new String[] {
					"javac",
					"-classpath",System.getProperty("java.class.path"),
					"-nowarn",
					"-sourcepath","..",
					"-d", classDir().getAbsolutePath(),
					"-encoding", "utf8",
					packageDir.getAbsoluteFile() + File.separator + "*.java"};


				ByteArrayOutputStream out = new ByteArrayOutputStream();

				log("Compiling: ", packageDir.getAbsolutePath());

				int exitCode = run.exec(out, compileCommand);

				if (exitCode != 0) {

					String output = new String(out.toByteArray(), "UTF8");

						// for now it's iterating all directory, so for non-java dir, ignore compile error
					if (!output.startsWith("javac: file not found:")) {
						log("Exit Code ", exitCode);
						log(output);
						break;
					}
				}
			}
		}
	}



}
