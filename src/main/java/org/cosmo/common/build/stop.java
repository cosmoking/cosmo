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

import org.cosmo.common.build.IPC.Signal;


public class stop extends BuildBase
{
	public static void main (String[] args) throws Exception
	{
		File signalDir = IPC.SignalDir;

		if (args.length > 0) {
			signalDir = new File(args[0]);
		}

		String appServerPidFile = null;
		String webServerPidFile = null;



		for (File file : signalDir.listFiles()) {
			if (file.getName().endsWith(".pid")) {

				String fileContent = fileContent(file);
				if ("appserver".equalsIgnoreCase(fileContent)) {
					IPC.createSignal(file, Signal.Stop.name());
					log("Stopping AppServer");
					Thread.sleep(2000);
				}
				if ("webserver".equalsIgnoreCase(fileContent)) {
					IPC.createSignal(file, Signal.Stop.name());
					log("Stopping WebServer");
					Thread.sleep(2000);
				}
			}
		}
	}
}


