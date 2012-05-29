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

public class startAgent extends BuildBase
{

	public static void main (String[] args) throws Exception
	{
		classpath.exec(InstallDir);

		IPC.createPidFile(IPC.ProcessType.Agent);
		String[] runCommand = new String[] {
				"java",
				"-classpath",System.getProperty("java.class.path"),
				"build.startApp"
		};
		run.exec(InstallDir, SystemOut, false, runCommand);


		/*
		while (true) {
			SystemOut.println("Waiting for signal...");
			String content = IPC.waitForSignal(500);
			SystemOut.println("Received signal " + content);
		}
		*/



	}

}
