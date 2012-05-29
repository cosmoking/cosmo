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

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.util.Map;


public class IPC extends Util
{

		// ie ->  d:/install/000034  the dir is d:/install
	public static final File SignalDir = BuildBase.InstallDir.getParentFile();

	public static enum ProcessType {
		Agent,
		WebServer,
		AppServer;
	}


		// A key used in System.environment or ProcessBuilder.environment to pass the pid of the running PID
	public static final String IPCCreatorProcessID = "IPCCreatorProcessID";


	public static void setCreatorPID (ProcessBuilder pb)
	{
		pb.environment().put(IPC.IPCCreatorProcessID, pid());
	}

	public static String getCreatorPID ()
	{
		return System.getenv(IPCCreatorProcessID);
	}

	public static boolean hasCreatorPID ()
	{
		return getCreatorPID() != null;
	}


		// called by "executed" jvm - this should be called by the running jvm when it's done starting up
	public static void  signalProcessStarted (ProcessType processType, SignalHandler signalHandler)
	{
		try {
				// create pid file
			Closeable pidFileHandle = createPidFile(processType);

				// if called via run.exec ()
			if (hasCreatorPID()) {
					// signal ready to run.exec() pid
				IPC.createSignal(SignalDir, getCreatorPID(), pid());

					// start listening signal for this jvm
				new GenericThread (pidFileHandle, signalHandler) {
					public void runIt () throws Exception
					{
						SignalHandler signalHandler = (SignalHandler)threadParam[1];
						RandomAccessFile pidFileHandle = (RandomAccessFile) threadParam[0];

							// wait for signal
						String signalName = IPC.waitForSignal(3000);
						Signal signal = Signal.valueOf(signalName);

							// let app handle it first
						signalHandler.handle(signal);

							// then invoke it
						signal.invoke(pidFileHandle);
					}
				};
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(ExitCode.FabricError._exitCode);
		}
	}


		// blocked until "pid.sig" file is created by the signaling process - pid is it's own process id
		// upon "signaled" it will return the content of the signal file and deletes the signal file
	 public static String waitForSignal (long checkingInterval)
	   throws Exception
	 {
		File signalFile = new File(SignalDir, pid() + ".sig");
		//SystemOut.println(signalFile);
		while (!signalFile.exists()) {
			Thread.sleep(checkingInterval);
		}

		String signalContent = fileContent(signalFile);
		signalFile.deleteOnExit();
		del.exec(signalFile);
		return signalContent;
	 }


	 	//  creates the "pid.sig" file to signal the process
	 public static void createSignal (File dir, String pid, String signal) throws Exception
	 {
		File sigFile = new File (dir, pid + ".sig");
		fileWithContentAtomically(sigFile, signal);
	 }

	 public static void createSignal (File pidFile, String signal) throws Exception
	 {
		String pid = pid(pidFile.getName());
		createSignal(pidFile.getParentFile(), pid, signal);
	 }

		// dump pid file - THE PID FILE is intentionally not closed so that
		// 1) provides pid info
		// 2) can not be deleted to signify process still running
		// 3) used by "stop" to dertermine which process to stop
		// when process ends gracefully, pid gets deleted and signifies and normal exit (1)
	public static Closeable createPidFile (ProcessType processType)
	  throws Exception
	{
		File pidFile = new File(SignalDir, processType.name() + "_" + pid() + ".pid");
		pidFile.deleteOnExit();
		RandomAccessFile pidFileHandle = new RandomAccessFile(pidFile, "rw");
		pidFileHandle.writeBytes(processType.name());
		// out.close(); XXX intentionally  be closed at Signal.Terminate.invoke()
		return pidFileHandle;
	}


	public static enum Signal
	{
		 Stop {
			 public void invoke (Object... arg) throws Exception {
				for (int i = 10; i > 0 ; i--) {
					System.out.println("Exiting System in " + i);
					Thread.sleep(1000);
				}

					// close pidFileHandle - so that it can be deletedOnExit()
				RandomAccessFile pidFileHandle = (RandomAccessFile) arg[0];
				pidFileHandle.close();

					// now exit it with the associating exitCode -> this will get picked up the the run.Exec();
				System.exit(ExitCode.StopSignal._exitCode);
			 }
		 },
		 Restart {
			 public void invoke (Object... arg) throws Exception {
				for (int i = 10; i > 0 ; i--) {
					System.out.println("Restarting System in " + i);
					Thread.sleep(1000);
				}

					// close pidFileHandle - so that it can be deletedOnExit()
				RandomAccessFile pidFileHandle = (RandomAccessFile) arg[0];
				pidFileHandle.close();

					// now exit it with the associating exitCode -> this will get picked up the the run.Exec();
				System.exit(ExitCode.RestartSignal._exitCode);
			 }
		 },
		 ThreadDump {
			 public void invoke (Object... arg) throws Exception {
			 }
		 };


		 abstract public void invoke (Object... arg) throws Exception;
	}


	public static interface SignalHandler
	{
		public void handle (Signal signal);
	}

	public static void main (String[] args) throws Exception
	{
		System.out.println("XXX" + IPC.SignalDir);

	}
}
