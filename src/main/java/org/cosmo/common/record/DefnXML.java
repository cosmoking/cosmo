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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;


	// here the "String" is store as the Field
	// it will be passed to XMLStore as a context to faciltate XMLStore to
	// retrive or safe XML files

public class DefnXML <T extends Record> extends DefnStr
{
	public static final Class[] TypeClasses = new Class[] {org.cosmo.common.record.XML.class};

	XMLStore _xmlStore;

	public DefnXML (Meta declaringMeta, int size)
	{
		super(declaringMeta, size, false);
	}


	public DefnXML xmlStore (Class <? extends XMLStore> xmlStore)
	{
		try {
			Constructor constructor = xmlStore.getConstructor(new Class[] {Defn.class});
			_xmlStore = (XMLStore)constructor.newInstance(new Object[] {this});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	public XMLStore getXMLStore ()
	{
		return _xmlStore;
	}

	public Class[] typeClasses ()
	{
		return TypeClasses;
	}

	public DefnStr index (boolean index)
	{
		super.index(index);
		return this;
	}

	public DefnXML lazyLoad (boolean lazyLoad)
	{
		super.lazyLoad(lazyLoad);
		return this;
	}

	public boolean hasAdditionalSizeInfo ()
	{
		return true;
	}


	@Override
	public Object readImpl (ByteBuffer dataIO, boolean directConvertFromBytes)
	  throws IOException
	{
		/*
		 *  Note. directConvertFromBytes is used when loading from LogEntries.
		 *  In that scenario,  the storeContext (relative pathname) is retrieved
		 *  from the XML file itself. This is different than how it's read from
		 *  record files (conventional way) in which the storeContext is stored as an DefnByte (entry)
		 *
		 */
		if (directConvertFromBytes) {
			try {
				org.cosmo.common.xml.XML xml =  new org.cosmo.common.xml.XML(new ByteArrayInputStream(dataIO.array()));
				String context = xml.getAttribute("storeContext");
				return _xmlStore.wrap(xml, context, null);
			}
			catch (SAXException e) {
				throw new IOException(e);
			}
			catch (ParserConfigurationException e) {
				throw new IOException(e);
			}
		}
		String context = (String)super.readImpl(dataIO, false);
		return _xmlStore.readXML(context);
	}

	@Override
	public byte[] writeImpl (byte[] dst, Object src, long i)
	  throws IOException
	{
		XML xml = (XML)src;
		byte[] xmlBytes = xml.save(_declaringMeta._mode._isMaster);
		super.writeImpl(dst, xml._storeContext.toString(), i);
		return xmlBytes;
	}

		// This version of write allows XML to be directly store/updated via XMLStore without going
		// through RecordStore
	public void directWrite (Record record)
	  throws Exception
	{
		XML xml = (XML)field().get(record);
		_xmlStore.writeXML(xml);
	}



}


