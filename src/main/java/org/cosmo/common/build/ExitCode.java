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


public enum ExitCode
{
	Normal (0),
	Error (1),
	FabricError (2),
	StopSignal (100),
	RestartSignal (101);


	public final int _exitCode;
	private ExitCode (int exitCode)
	{
		_exitCode = exitCode;
	}

	public static ExitCode resolve (int exitCode)
	{
		for (ExitCode aExitCode : values()) {
			if (exitCode == aExitCode._exitCode) {
				return aExitCode;
			}
		}
		return Error;
	}
}
