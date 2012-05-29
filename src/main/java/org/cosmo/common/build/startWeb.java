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



public class startWeb extends BuildBase {

	public static void main (String[] args) throws Exception
	{
		ConfigurationProfile profile = args.length > 0 ? ConfigurationProfile.valueOf(args[0]) : ConfigurationProfile.LocalWeb;
		Configuration config = ConfigurationProfile.class.getField(profile.name()).getAnnotation(Configuration.class);

		classpath.exec(InstallDir);

		String[] runCommand = new String[] {
				"java",
				config.VMArgs(),
				"-DServer=" + config.Server(),
				"-DMode=" + config.Mode(),
				"-DPort=" + config.Port(),
				"-DProduction=" + config.Production(),
				"-DRecordDir=" + config.RecordDir(),
				"-DResourceDir=" + resourceDir(),
				"-classpath",System.getProperty("java.class.path"),
				"appserver.AppServer"
		};

		int exitCode = run.exec(InstallDir, SystemOut, true, runCommand);
		ExitCode exit = ExitCode.resolve(exitCode);

		if (ExitCode.RestartSignal == exit) {
			String newVersion = version.versionStr(version.version() + 1);
			String newInstallDir = new File(InstallDir.getParentFile(), newVersion).getAbsolutePath();
			SystemOut.println("================");
			SystemOut.println("Restarting to build: " + newInstallDir);
			SystemOut.println("================");
			InstallDir = new File(newInstallDir);
			startWeb.main(args);
		}

	}

}
