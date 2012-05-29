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
import java.lang.reflect.Method;



/*
 * 	InstallDir is derived from the property "INSTALLDIR" + version (ie  d:/install + 000034 ->  d:/install/000034)
 *  It's the directory at which the application image is installed
 *
 *  ManifestFile is the file that carries the "version" of the images - it's install at the META-INF directory under install. This
 *  file "auto" incremented each time the command "build.jar increment" is invoked.
 *
 *  libDir - refers to 3rdParty jars under install
 *  resourceDir - refers to js/css/img resource files under install
 *  classDir - refers to the application class files
 *
 *  compile - compiles all java source and push into install/XXXXXX/classes dir
 *  jar - compiles and generates the freshintervalXXXXXX.jar including the lib/resrouce/classe files
 *  install - un jars the file into destinated directory, also copies the setup so that remote machine can setup and invoke targets, NOTE: it's important to MODIFY the setup.bat so that INSTALLDIR
 *  		  reflects correctly on the remote machine
 *  deploy - creates a new versioned jar, installes it, then restartes the process (AdminServer, WebServer)
 *
 * 	copy - file utility methods
 *  del - file utility methods
 *
 * 	run - process utility methods to invoke either native or java process using java
 *
 *  startApp - the jvm starts an process to run the AdminServer, also awaits "signals" such as "restart" and "stop" accordingly
 *  startWeb - the jvm starts an process to run the WebServer, also awaits "signals" such as "restart" and "stop" accordingly
 *  		pid files are put under InstallDir.parent ie (d:/install)
 *
 *  stop - seeks out the AdminServer and WebServer "pids" and generates the "stop siginal" to stop them
 *  restart - seeks out the AdminServer and WebServer "pids" and generates the "restart siginal" to stop them
 *
 *  version - util to spits out version, also support commands to increment version : XXX TODO, support auto checkout so it does get "access denied" issue
 *
 */

public class BuildBase extends Util
{
		// defines version which InstallDir depends on ie (version.versionStr()) as below
	public static final File ManifestFile = new File(currentDir(), "META-INF" + File.separator + "MANIFEST.MF");

		// value in this order
		// 1) "System.getProprety("INSTALLDIR")"
		// 2) "System.getEnv()"
		// 3) "currentDir()" -> "System.getProprety("user.dir")"
		//
		//	which means can change if 1) or 3) changes
	public static File InstallDir = updateInstallDirOnNewVersion();


	public static void require (Class clazz, String[]args)
	  throws Exception
	{
		Method method = clazz.getMethod("main", new Class[]{String[].class});
		method.invoke((Object)null, new Object[]{args});
	}


	public static File updateInstallDirOnNewVersion ()
	{
		InstallDir = new File(getProperty(File.class, "INSTALLDIR", currentDir(), true), version.versionStr());
		return InstallDir;
	}

	public static File libDir ()
	{
		return new File(InstallDir, "lib");
	}

	public static File resourceDir ()
	{
		return new File(InstallDir, "resource");
	}

	public static File classDir ()
	{
		return new File(InstallDir, "classes");
	}

}

