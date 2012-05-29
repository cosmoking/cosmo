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
package org.cosmo.common.record;

import java.io.File;
import java.io.IOException;

import org.cosmo.common.file.VariableFilePartition;
import org.cosmo.common.xml.Util;

/*
 *   XXX prelocate file size to avoid fragementation
 *
 */

public abstract class LobStore
{

	transient VariableFilePartition _channel;

	abstract public String getExtension ();


	public LobStore (Defn defn)
	{
		this(defn.field().getName(),
			 defn._declaringMeta.recordDir(true),
			 defn._declaringMeta.recordDir(defn._declaringMeta._mode._isMaster));
	}

		// usually read and write are the same (ie master, for slave it's different)
	public LobStore (String name, File readDir, File writeDir)
	{
		try {
			if (writeDir.exists()) {
				if (!writeDir.isDirectory()) {
					throw new IOException(Util.Fmt("Expect write dir [%s] to be a Directory", writeDir));
				}
			}
			else {
				writeDir.mkdirs();
			}


			if (readDir.exists()) {
				if (!readDir.isDirectory()) {
					throw new IOException(Util.Fmt("Expect read dir [%s] to be a Directory", writeDir));
				}
			}

			File readFile = new File(Util.Fmt("%s%s%s%s",
					readDir.getAbsolutePath(),
					File.separator,
					name,
					getExtension()));

			File writeFile = new File(Util.Fmt("%s%s%s%s",
					writeDir.getAbsolutePath(),
					File.separator,
					name,
					getExtension()));



			_channel = new VariableFilePartition(readFile, writeFile, 1024 * 1024 * 512, false);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}

	}


	public void close ()
	   throws IOException
	{
		_channel.close();
	}
}
