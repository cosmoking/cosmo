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


public class jar extends BuildBase
{

	public static void main (String[] args) throws Exception
	{
			// increment version and refresh the install dir
		version.exec(version.InrcementCmd);
		updateInstallDirOnNewVersion();

		// cleans it
		cleanDir(InstallDir);
		InstallDir.mkdirs();


			// compile it
		compile.exec(args);

			// copy resources
		copy.exec(new File(parentDir(), resourceDir().getName()), new File(InstallDir, resourceDir().getName()));

			// copy lib jar files
		copy.exec(new File(parentDir(), libDir().getName()), new File(InstallDir, libDir().getName()));

			// copy Manifest
		copy.exec(ManifestFile.getParentFile(), new File(InstallDir, ManifestFile.getParentFile().getName()));

			// jars it
		run.exec("jar", "cvfm", jarFile().getAbsolutePath(), ManifestFile.getAbsolutePath(), "-C" , InstallDir.getAbsolutePath(), ".");
	}


	public static File jarFile () throws Exception
	{
		return new File(currentDir(), "freshinterval" + version.versionStr() + ".jar");
	}

}
