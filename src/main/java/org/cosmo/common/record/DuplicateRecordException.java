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


public class DuplicateRecordException extends RecordException
{
	Record _record;

	public DuplicateRecordException (Record record)
	{
		this("Duplicate Reocrd Found.");
		_record = record;
	}

	private DuplicateRecordException (Exception e)
	{
		super(e);
	}

	private DuplicateRecordException (String errMsg)
	{
		super(errMsg);
	}



}
