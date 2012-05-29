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
package org.cosmo.common.xml;

public class JSONFormat extends NodeFormat
{
	// we will use "Format" to indicate the "xpath" for the array value pairs in the data
	// since there is no way to explicit to indicate a single value as an array value just
	// by looking at the data

    // The LeadingIndent
    public int leadingIndent = 1;

}
