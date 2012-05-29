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
package org.cosmo.common.util;

import ariba.util.core.Fmt;

import java.io.Writer;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.logging.Logger;




public class Log
{
    //
    //  Log message category for logging Aribaweb framework
    //
    public static final Logger jrecord =  (Logger)Logger.getLogger("jrecord");
    public static final Logger jcache =  (Logger)Logger.getLogger("cache");
    public static final Logger japp =  (Logger)Logger.getLogger("japp");
    public static final Logger jlucene =  (Logger)Logger.getLogger("jlucene");
    public static final Logger jfavIcon =  (Logger)Logger.getLogger("jfavIcon");
    public static final Logger jfetchRss =  (Logger)Logger.getLogger("jfetchRss");


}
