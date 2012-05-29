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

import java.util.Collections;
import java.util.Iterator;

import org.cosmo.common.xml.Node;


public class DefnList <T extends Record> extends DefnRecord
{

	public Meta _listClassMeta;
	public static final int PageSize = 32;  // PageSize is number of RecordId (long) in a page ie. 32 implies 32 records in a page

	public DefnList (Meta declaringMeta)
	{
		super(declaringMeta);
	}

	public DefnList type (Meta listClassMeta)
	{
		_listClassMeta = listClassMeta;
		return this;
	}

	public DefnList index (boolean index)
	{
		_isIndexField = index;
		return this;
	}

	public Class[] typeClasses ()
	{
		return new Class[] {RecordList.class};
	}

/*
	public RecordList readImpl (ByteBuffer dataIO, long i)
	  throws IOException
	{
		return (RecordList)super.readImpl(dataIO, i);
	}

	public void writeImpl (ByteBuffer dataIO, Object value, long i)
	  throws IOException
	{
		super.writeImpl(dataIO, value, i);
	}
*/

	public Node fieldToXML (Record record)
	{
		try {
			Node field= new Node(field().getName());
			RecordList list = (RecordList) field().get(record);
			for (Iterator listIterator = list == null
					? Collections.EMPTY_LIST.iterator()
					: list.list()
				; listIterator.hasNext()
				;)
			{
				Record listRecord = (Record)listIterator.next();
				listRecord.tx().load();
				field.add(listRecord.tx().recordToXML());
			}
			return field;
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}

	}
}
