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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.io.File;


@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
@Inherited
public @interface Configuration {



	//String 		Java()								default "java";
	String		VMArgs()							default "-mx512m";
	//String		ClassPath()							default "System.getProperty(\"java.class.path\")";

	String		Mode();
	String		Server();
	int			Port();
	//String		RecordDir();
	boolean		Production()						default false;

	int			RecordLogMaxConsumeRatePerBatch()	default 15000;
	boolean		EnableDefnHeaderException()			default false;

	//Class		ServerMainClass()					default net.Server.class;
	String		RecordDir();


}
