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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;


public class run extends BuildBase {


	public static void main (String[] args) throws Exception
	{
		int exitCode = run.exec(args);
		if (exitCode != 0) {
			log("Exit Code " + exitCode);
		}
	}

	public static int exec (String... cmds)
	  throws Exception
	{
		return exec(currentDir(), LogSysOut, false, cmds);
	}


	public static int exec (OutputStream out, String... cmds)
	  throws Exception
	{
		return exec(currentDir(), out, false, cmds);
	}

	public static int exec (File workingDir, String... cmds)
	  throws Exception
	{
		return exec(workingDir, LogSysOut, false, cmds);
	}


	public static int exec (File workingDir, OutputStream out, String... cmds)
	  throws Exception
	{
		return exec(workingDir, out, false, cmds);
	}

	// called by the long running "process" execute by this class
	// since the long running process don't exit right away we need a way to signal this class that it has successfully started.
	// in that regards the long running "process" would call this method which would write to the file "ProcessNotificationLogFile" designated by this run class in System.env()
	// since our "run" would monitor this, so when it sees it is written , the "run" can end
	// there are many ways to do the "sub process" calling "caller" (IPC), for now we use this method

	public static int exec (File workingDir, OutputStream out, boolean join, String... cmds)
	  throws Exception
	{
		log(Arrays.toString(cmds));
		ProcessBuilder pb = new ProcessBuilder(cmds);
		IPC.setCreatorPID(pb);
		pb.directory(workingDir);
		pb.redirectErrorStream(true);
		Process process = pb.start();
		int[] exitCodeHolder = new int[1];

			// this thread pulls from process output stream and writes to "out"
		Thread output = new GenericThread(process, out) {
			public void runIt () throws Exception {
				Process process = (Process)threadParam[0];
				OutputStream out = (OutputStream)threadParam[1];
				InputStream output = process.getInputStream();
				int b = output.read();
				while (true) {
					if (b > 0) {
						out.write(b);
					}
					else {
						return;
					}
					b = output.read();
				}
			}
		};


		 /*
			// main thread waits and wait either of the following to notify to end :
			// 1) wait for sub process end and notify done  OR
			// 2) wait for sub process to write to "ProcessNotificationLogFile" to notify done
		 */


			// this thread simply runs and wait for process to end to indicate process successfully started and exited
		new GenericThread(process, output,  exitCodeHolder) {
			public void runIt () throws Exception {
				Process process = (Process)threadParam[0];
				int[] exitCodeHolder = (int[])threadParam[2];
				Thread output = (Thread)threadParam[1];

					// wait for process to end
				int exitCode = process.waitFor();
					// wait for log thread finishes logging
				output.join();
				synchronized (exitCodeHolder) {
					exitCodeHolder[0] = exitCode;
					exitCodeHolder.notify();
				}
			}
		};

			// this thread simply runs and use "ipc" and "waitForProcesseStarted" to indicate process successfully started and running
		new GenericThread(exitCodeHolder, join) {
			public void runIt () throws Exception {
				int[] exitCodeHolder = (int[])threadParam[0];
				boolean join = (Boolean)threadParam[1];

					// awaits until running process signal it's success with it's pid
				String pid = IPC.waitForSignal(500);

					// if join, only log, otherwise notify exitCodeHolder to exit
				if (join) {
					log("exec successfully - joining running process [" + pid + "] ctrl-break to exit run.exec()");
				}
				else {
					synchronized (exitCodeHolder) {
						exitCodeHolder[0] = 0;
						exitCodeHolder.notify();
					}
				}
			}
		};

			// main thread waits and wait either of the following to notify to end :
			// 1) wait for sub process end and notify done
			// 2) wait for sub process to write to "ProcessNotificationLogFile" to notify done
		synchronized (exitCodeHolder) {
			exitCodeHolder.wait();
		}

		log("exec completed");
		return exitCodeHolder[0];
	}
}





