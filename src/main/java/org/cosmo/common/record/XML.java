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

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import org.cosmo.common.util.New;
import org.cosmo.common.xml.Node;

public class XML extends org.cosmo.common.xml.XML
{
	public static final String RootName = "ROOT";

	String _storeContext;
	// useful for creating ids in XML, this ensures both server side and ui DOM side has unique ids
	int _idCounter;
	String _namespace;
	XMLStore _xmlStore;
	// when new folder is added. this is set to true, so that when click "Refresh" folders are refresh as wel
	public volatile boolean _hasNewFolderAdded;


	XML () {}

	public XML (String namespace)
	{
		this (new org.cosmo.common.xml.XML(RootName), null, namespace, null);
	}

	public XML (org.cosmo.common.xml.XML xml, String storeContext, String namespace, XMLStore xmlStore)
	{
			// assign the value of idCounter to the XML if there is one
			// this id is used to generate unique id number for each Element
			// added in this XML - very useful for finding the element in both
			// the XML and data that was send to HTML . (ie jquery selector id)
		String idCounter = xml.getAttribute("idCounter");
		_idCounter = idCounter == null ? 0 :Integer.valueOf(idCounter);
		_namespace = namespace == null ? xml.stringValue("@namespace", "default") : namespace;

			// storing context
		_storeContext = storeContext;
		_xmlStore = xmlStore;

			// original id and value
		_id = xml.id();
		_value = xml.rawValue();

	}

	XML (InputStream in)
      throws IOException, SAXException, ParserConfigurationException
	{
		super(in);
	}

  	// override by Nodes and AttributeNodes
	@Override
    protected Node newCopyInstance ()
    {
    	return new XML();
    }

	@Override
	public Node copy()
	{
		XML xmlCopy = (XML)super.copy();
		xmlCopy._storeContext = _storeContext;
		xmlCopy._xmlStore = _xmlStore;
		xmlCopy._namespace = _namespace;
		xmlCopy._hasNewFolderAdded = _hasNewFolderAdded;
		xmlCopy._idCounter = _idCounter;
		return xmlCopy;
	}

		// get the next unique
	public int nextId ()
	{
		return _idCounter++;
	}

		// useful when this id eventually gets transfered to Html in which
		// a namespace is critical to uniquely identify the element.
		// So far most of xml data get mapped to JSON then to HTML,
		// ie "RssSite1" is more unique than "1" in HTML for jquery
	public String nextId (String ns)
	{
		return New.str(_namespace, "_", ns, "_", nextId());
	}

	public void replace (org.cosmo.common.xml.XML xml)
	{
		_id = xml.id();
		_value = xml.value();
	}

		// writes directly to Disk
	public byte[] save (boolean writeToFile)
	  throws IOException
	{
			// also save the current idCounter
		set("@storeContext", _storeContext);
		set("@idCounter", _idCounter);
		set("@namespace", _namespace);
		set("@hasNewFolderAdded", true);
		return writeToFile
			? _xmlStore.writeXML(this)
			: _xmlStore.toBytes(this);
	}

	public byte[] save ()
	  throws IOException
	{
		return save(true);
	}


	public Node load (String xpath)
	{
		Node node = get(xpath);
		if (node == null) {
			node = set(xpath, null);
		}
		return node;
	}
}
