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
package org.cosmo.common.model;

import java.io.IOException;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.OpenBitSet;
import org.cosmo.common.record.DefnDate;
import org.cosmo.common.record.DefnInt;
import org.cosmo.common.record.DefnStr;
import org.cosmo.common.record.DefnXML;
import org.cosmo.common.record.Meta;
import org.cosmo.common.record.Record;
import org.cosmo.common.record.Tx;
import org.cosmo.common.record.XML;
import org.cosmo.common.record.XMLStore;
import org.cosmo.common.util.New;
import org.cosmo.common.util.Util;
import org.cosmo.common.xml.Node;

import cern.colt.list.LongArrayList;

/**
 *  - Sorted the items in folder before save! so no need to do sort eac time
 * 
 *
 * 
 */

public class PublicFolder <T extends Record> implements Record
{	
	public static Meta<PublicFolder> Meta = org.cosmo.common.record.Meta.Create(PublicFolder.class).
		postLazyInit(PostLazyInit.class);

	public static DefnXML FolderItems = Meta.DefnXML(512).xmlStore(XMLStore.class);
	public static DefnInt Version = Meta.DefnInt();
	public static DefnDate CreateDate = Meta.DefnDate();
	public static DefnDate LastUpdateDate = Meta.DefnDate();
	public static DefnStr Name = Meta.DefnStr(64);
	//public static DefnEnum Category = Meta.DefnEnum();

    public XML _folderItems;
    public int _version;
    public Date _createDate;
    public Date _lastUpdateDate;
    public String _name;
    //public Category _category;
    	
    
	public PublicFolder ()
	{
		_proxy = Meta.store().createTx(this);
	}

	private Tx<T> _proxy;
	
	public Tx<T> tx ()
	{
		return _proxy;
	}
		
	public Meta meta ()
	{
		return Meta;
	}

	
	
	

	
	/*
	  
	  	
	public static synchronized PublicFolder createFolder (Category category)
	  throws Exception
	{
		PublicFolder publicFolder = new PublicFolder();
		publicFolder._version = 1;			
		publicFolder._folderItems = FolderItems.getXMLStore().create(New.str(category.name(), "_", publicFolder._version, ".xml"), "sys");
		publicFolder._createDate = new Date();
		publicFolder._lastUpdateDate = publicFolder._createDate;
		publicFolder._name = category.name();
		publicFolder._category = category;
		
			// create root folder
		PublicFolder.createRootFolderOn(publicFolder._folderItems);
		publicFolder.tx().insert();
		return publicFolder;
	}

	public static synchronized void loadFromRssSites (PublicFolder publicFolder)
	  throws Exception
	{
		List<RssSite> rssSites = RssSite.Meta.store().readAll();
		
			// clear everything
		publicFolder._folderItems.setRawValue(null);
		
		Node rootFolder = PublicFolder.createRootFolderOn(publicFolder._folderItems);
		
		for (RssSite aSite : rssSites) {
				// XXX there will be many tags, but for now we pick first tag as the folder name			
			if (aSite.hasCategory(publicFolder._category)) {			
				String folderNames = aSite._tags + "";
				String folderName = StringTokens.on(folderNames).next().toLowerCase();
				if (StringUtils.isBlank(folderName)) {
					folderName = "Default";
				}
				Node folder = loadFolder(rootFolder, "Tag", folderName, publicFolder._folderItems);
				Node itemNode = addItemToFolder(folder, aSite.getClass().getSimpleName(), aSite._title, String.valueOf(aSite.tx().id()), publicFolder._folderItems);
				itemNode.add("iconURL", aSite._iconUrl);
			}
		}
		publicFolder._folderItems.save();
	}
	
	
	
	public static synchronized void loadOPML (File opmlFile)
	  throws Exception
	{
		FileInputStream input = new FileInputStream(opmlFile);
		xml.XML xml = new xml.XML(input);
		for (Node aSet : xml.get("body").children()) {
			String tagName = aSet.stringValue("@title");
			for (Node aSite : aSet.children()) {
				RssSiteCreator.createPublic(aSite.stringValue("@xmlUrl"), aSite.stringValue("@htmlUrl"), tagName, app.Category.Default.name());
			}
		}
	}
	
	public static synchronized void loadCSV (File csvFile)
	  throws Exception
	{
		LineNumberReader reader = new LineNumberReader(new InputStreamReader(new FileInputStream(csvFile),"UTF8"));
		try {
			for (String line = reader.readLine(); line != null;) {
				StringTokens token = StringTokens.on(line);
				String url = token.next();
				String categoriesStr = token.next();
				String tagsStr = token.next();
				StringTokens categoryStr = StringTokens.on(categoriesStr, StringTokens.PipeSeparatorChar);
				RssSiteCreator.createPublic(url, null, tagsStr, categoriesStr == null || categoriesStr.length() == 0 ? app.Category.Default.name() : categoriesStr);
				line = reader.readLine();				
			}
		}
		catch (Exception e) {
			throw new IOException(Fmt.S("Error proceessing line %s with error [%]", reader.getLineNumber(), e.getMessage()));
		}
		finally {		
			reader.close();
		}						
	}	
*/	
	
	public Node rootFolder ()
	{
		return _folderItems.get("Folder").child();
	}	

	public static Node loadFolder (Node parent, String type, String name, XML root)
	{		
		Node existingFolder = parent.searchFirstNode("Folder", "@name", name);
		if (existingFolder == null) {
			return createFolder(parent, type, name, root);
		}
		else {
				// a copy of the same node
			//return new Node(existingFolder.id().toString(), existingFolder.rawValue());
			return existingFolder;
		}
	}	
	
	public T getAllItemRecordIdss  (Class<T> returnType, org.cosmo.common.xml.XML  rootFolder)
	{
		return null;
	}
	
	
	public static OpenBitSet getAllItemRecordIdSet (org.cosmo.common.xml.XML rootFolder) 
	{
				// track duplicates - faster than HashSet
			OpenBitSet recordIdSet = new OpenBitSet();
			
			Node folder = rootFolder.get("Folder"); 
			List<Node> items = folder.searchNode("Item");
			for (Node item : items) {
				recordIdSet.set(org.cosmo.common.util.Util.parseLong(item.getAttribute(("recordId"))));
			}
	
			return recordIdSet;
	}
	
	
	public static long[] getAllItemRecordIds (org.cosmo.common.xml.XML rootFolder) 
	{
		try {
				// track duplicates - faster than HashSet
			OpenBitSet recordIdSet = getAllItemRecordIdSet(rootFolder);
			LongArrayList recordIds = new LongArrayList((int)recordIdSet.cardinality());
			DocIdSetIterator recordIdIterator = recordIdSet.iterator();
			for (int docId = recordIdIterator.nextDoc(); docId != DocIdSetIterator.NO_MORE_DOCS;) {
			    recordIds.add((long)docId);
			    docId = recordIdIterator.nextDoc();
			}
			return recordIds.elements();
		}
		catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
	
	
	public static Node createRootFolderOn (XML container)
	{
		Node rootFolder = PublicFolder.createFolder(XML.RootName, XML.RootName, container); 
		container.add(rootFolder);
		return rootFolder;
	}
	
	public static Node createFolder (String type, String name, XML root)
	{
		Node newFolder = new Node("Folder");
		newFolder.add(new Node("Items"));
		newFolder.add(new Node("Folders"));
		newFolder.add("@type", type, "@name", name, "@id", root.nextId(type));
		return newFolder;
	}
	
	public static Node createFolder (Node parentFolder, String type, String name, XML root)
	{
		Node newFolder = createFolder(type, name, root);
		parentFolder.get("Folders").add(newFolder);
		return newFolder;
	}
	
	
	public static Node addItemToFolder (Node folder, String type, String name, String recordId, XML root)
	{	
		Node newItem = new Node("Item");
		newItem.add("@type", type, "@name", name, "@id", root.nextId(type), "@recordId", recordId);
		folder.get("Items").add(newItem);
		return newItem;
	}
	
	public static Comparator<Node> NameComparator = new Comparator<Node> () {
		 		
			// for words not begin with letter we append '{'  so that it
			// appear after 'z' ('{' appears after 'z' in ASCII)
		public int compare(Node node1, Node node2) 
		{ 
			String nodeName1 = node1.getAttribute("name").toLowerCase();
			
			if (Util.isBlank(nodeName1)) {
				System.err.println("XXXX is blank");
			}
			
			if (!Character.isLetter(nodeName1.charAt(0))) {
				nodeName1 = New.str('{',nodeName1);
			}

			String nodeName2 = node2.getAttribute("name").toLowerCase();
			if (!Character.isLetter(nodeName2.charAt(0))) {
				nodeName2 = New.str('{',nodeName2);
			}
			return nodeName1.equals(nodeName2) 
				? 0
				: nodeName1.compareTo(nodeName2);
		}
	};


	public static class PostLazyInit implements Meta.PostLazyInit
	{
		public void recordMetaPostInitialization () 
		  throws Exception
		{
			return;
		}
	}	

	
}

/*
file name: the name associated with the file (recall, this can be any type of file)

modification date: the date the file was last modified, i.e. a "time-stamp". If the file has not been modified within the last year (or six months for Linux), the year of last modification is displayed.

size: the size of the file in bytes (i.e. characters).2

group: associated group for the file

owner: the owner of the file

number of links: the number of other links associated with this file

permission modes: the permissions assigned to the file for the owner, the group and all others.
 
*/



