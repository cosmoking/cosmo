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
import java.io.FileNotFoundException;

public class install extends BuildBase
{

		// arg 0 - install dir  arg 1 - jar file (optional)
	public static void main (String[] args) throws Exception
	{
		File jarFile = jar.jarFile();
		File installDir = InstallDir;

		if (args.length > 0) {
			installDir = new File(args[0], version.versionStr());
		}

		if (args.length > 1) {
			jarFile = new File(args[1]);
		}

		cleanDir(installDir);

		run.exec(installDir,  "jar", "xf", jarFile.getAbsolutePath());

		// copy setupFile
		copy.exec(new File(currentDir(), "setup.bat"), new File(installDir, "setup.bat"));

		// copy ConfigurationProfile
		copy.exec(new File(currentDir(), ConfigurationProfile.class.getSimpleName() + ".java"), new File(installDir, ConfigurationProfile.class.getSimpleName() + ".java"));

	}
}
