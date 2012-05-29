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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util
{
	// Use this to write to the oringal system.out
	public static final PrintStream SystemOut = System.out;

		// use this to write to both original system.out and sysout.txt log file
	public static final PrintStream LogSysOut = SystemOutputStream.Instance;



	public static File currentDir ()
	{
		return new File(System.getProperty("user.dir"));
	}

	public static File parentDir ()
	{
		return new File(currentDir().getParent());
	}

	public static void log (Object... os)
	{
		for (Object o : os) {
			LogSysOut.print(o.toString());

		}
		LogSysOut.println("");
	}

	public static void cleanDir (File dir)
	  throws Exception
	{
		del.exec(dir);
		dir.mkdirs();
	}

		// creates the file atoimcally with content
		// ie, a tmp file is first created with the intent content, then renamed to the actual file
		// this is so that, when other process is waiting for this file, it will only read it with
		// content all written, rather than reading the content while it's been written -
		// right now used as way to pass signals
	public static void fileWithContentAtomically (File file, String content)
	  throws Exception
	{
		File tmpFile = File.createTempFile(UUID.randomUUID().toString()  + System.currentTimeMillis(), "tmp");
		FileOutputStream fileOut = new FileOutputStream(tmpFile);
		fileOut.write(content.getBytes());
		fileOut.close();
		rename(tmpFile, file);
	}


	public static String fileContent (File file)
	  throws Exception
	{
		FileInputStream in = new FileInputStream(file);
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		byte[] bytes = new byte[(int)file.length()];
		in.read(bytes);
		in.close();
		return new String(bytes, Charset.defaultCharset());

	}

	public static void rename (File from, File to)
	  throws Exception
	{
		if (!from.exists()) {
			throw new Exception ("Rename failed as " + from + " does not exists");
		}

		if (to.exists()) {
			throw new Exception ("Rename failed as " + to + " already exists");
		}


		from.renameTo(to);
		for (int i = 0; from.exists() && !to.exists() && i < 20; i++) {
			from.renameTo(to);
			Thread.sleep(200);
		}

		if (!to.exists()) {
			throw new Exception("Unable to rename from " + from + " to " + to);
		}
	}

		// basically extracts first number in the string
	public static String pid (String pidInfo)
	{
		String pidString =  ManagementFactory.getRuntimeMXBean().getName();
		Pattern p = Pattern.compile("-?\\d+");
		Matcher m = p.matcher(pidInfo);
		while (m.find()) {
		  return m.group();
		}
		throw new RuntimeException("Unable to find Pid from " + pidString);
	}

	public static String pid ()
	{
		String pidString =  ManagementFactory.getRuntimeMXBean().getName();
		return pid (pidString);
	}

	public static <T extends Object>T getProperty (Class<T> clazz, String property)
	{
		return getProperty(clazz, property, null);
	}

	public static <T extends Object>T getProperty (Class<T> clazz, String property, T defaultValue)
	{
		return getProperty(clazz, property, defaultValue, false);
	}

	public static <T extends Object>T getProperty (Class<T> clazz, String property, T defaultValue, boolean environmentVariablesAsBackup)
	{
			// either cache those methods or uses this as a one time thingy
		try {
			String param = System.getProperty(property);


			if (param == null) {
				if (environmentVariablesAsBackup) {
					param = System.getenv(property);
				}
			}

			if (param == null) {
				if (defaultValue == null) {
					throw new IllegalArgumentException("Property [" + property + "] not found");
				}
				//log("[" + property + "]= Default[" + defaultValue + "]");
				return defaultValue;
			}
			else {
				//log("[" + property + "]=[" + param + "]");
				if (Enum.class.isAssignableFrom(clazz)) {
					Method method = clazz.getMethod("valueOf", new Class[] {String.class});
					Object o = method.invoke(clazz, param);
					return (T)o;
				}
				else {
					Constructor<T> c = clazz.getConstructor(String.class);
					return c.newInstance(param);
				}
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}



// writing to this stream would write to both system.out and log file
class SystemOutputStream extends OutputStream
{
	public static final PrintStream Instance = new PrintStream(new SystemOutputStream());

	private static PrintStream LogFileOut;
	static {
		try {
			File sysout = new File(BuildBase.currentDir(), "sysout.txt");
			LogFileOut = new PrintStream(new FileOutputStream(sysout, true));
			LogFileOut.println("*********** " + new java.util.Date() + "***************");
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(ExitCode.FabricError._exitCode);
		}
	}

	private SystemOutputStream ()
	{
	}

	public void write(int b) throws IOException
	{
		LogFileOut.write(b);
		BuildBase.SystemOut.write(b);
	}
}


