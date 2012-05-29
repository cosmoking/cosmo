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

import org.cosmo.common.build.Configuration;

public enum ConfigurationProfile {

		@Configuration (
			Server = "localhost",
			Mode = "master",
			Port = 81,
			RecordDir = "d:/records"

		)
		LocalApp,


		@Configuration (
			Server = "localhost",
			Mode = "slave1",
			Port = 80,
			RecordDir = "d:/records"
		)
		LocalWeb,

		@Configuration (
				Server = "98.248.55.93",
				Mode = "master",
				Port = 5717,
				Production = true,
				RecordDir="D:/rsssites"
		)
		ProductionApp,


		@Configuration (
				Server = "98.248.55.93",
				Mode = "slave1",
				Port = 5716,
				Production = true,
				RecordDir="D:/rsssites"
		)
		ProductionWeb
	}
