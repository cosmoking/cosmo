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
import java.io.Serializable;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import org.cosmo.common.util.Constants;
import org.cosmo.common.util.New;
import org.cosmo.common.xml.NodeFormat;


public class XMLStore implements Serializable
{

	Defn _defn;
	NodeFormat _format;
	protected File _dir;


	// XXX come back revisit this - now all XMLStore stores in master space
	public XMLStore (Defn defn)
	{
		_defn = defn;
		_format = new NodeFormat().replaceEntityReference(true);
		_dir = defn._declaringMeta.createFolderForClass(XMLStore.class, "xml", true);
	}


	public byte[] writeXML (org.cosmo.common.record.XML xml)
	  throws IOException
	{
		if (xml != null) {
			File file = resolveFile(xml._storeContext);
			if (!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			File[] dumpFileNames = org.cosmo.common.util.Util.backupFile(file);
			System.out.println(New.str("Writing XML File ", dumpFileNames[0]));
			return org.cosmo.common.util.Util.writeToFile(dumpFileNames[0], toBytes(xml));
		}
		return Constants.EmptyByteMarker;
	}

	public byte[] toBytes (org.cosmo.common.record.XML xml)
	{
		return xml.toBytes(_format);
	}

	public org.cosmo.common.record.XML readXML (String storeContext)
	  throws IOException
	{
		File file = resolveFile(storeContext);
		if (file == null) {
			return null;
		}
		try {
			org.cosmo.common.xml.XML xml = XML.open(file);
			return wrap(xml, storeContext, null);
		}
		catch (SAXException se) {
			throw new IOException(se);
		}
		catch (ParserConfigurationException pe) {
			throw new IOException(pe);
		}
	}

	public org.cosmo.common.record.XML wrap (org.cosmo.common.xml.XML xml, String storeContext, String namespace)
	{
		return new org.cosmo.common.record.XML(xml, storeContext, namespace, this);
	}

	public org.cosmo.common.record.XML create (String storeContext, String namespace)
	{
		return wrap(new org.cosmo.common.xml.XML(org.cosmo.common.record.XML.RootName), storeContext, namespace);
	}

	public File resolveFile (String storeContext)
	  throws IOException
	{
		if (storeContext == null) {
			return null;
		}
		return new File(_dir, storeContext);
	}
}
