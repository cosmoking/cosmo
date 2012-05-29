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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.cosmo.common.model.PublicFolder;
import org.cosmo.common.statistics.Clock;
import org.cosmo.common.util.New;
import org.cosmo.common.util.Util;
import org.cosmo.common.xml.Node;


/*
 *
 *  Remember XMLStore still writes to Master
 *
 *  Fix the lazy init on the "performIndexAndRecordCountSanityCheck" should only done by Master
 *
 *  should set readonly when mode is slave? what about hybrid?
 */
public class Meta <T extends Record> implements Serializable
{
	public static final String RecordDir = Util.getProperty(String.class, "RecordDir");
    public static final Mode Mode = Util.getProperty(Meta.Mode.class, "Mode");
	

	
		// for now defined here - this should be per instance but for now per meta
	public static enum Mode {
		master(false),
		slave1(true),
		slave2(true),
		slave3(true);

		public static final Mode[] Lookup = new Mode[] {master, slave1, slave2, slave3};


			// some are duplicate info but used for caching
		public final File _dir;
		public final String _dirStr;
		public final boolean _isSlave;
		public final boolean _isMaster;
		public final File _logFile;
		public final String _logFilename;

		Mode (boolean isSlave) {
			_isSlave = isSlave;
			_isMaster = !isSlave;		
			_dir = _isSlave ?  new File(RecordDir, name()) : new File(RecordDir);
			_dirStr = _dir.getAbsolutePath();
			_logFile = new File(_dir, RecordLog.LogFilePrefix + name());
			_logFilename = _logFile.getAbsolutePath();
		}

	}



		// Singleton map and set for Meta from Class, and ClassName
	static final Map<Object, Meta> ClassMetaMap = new HashMap();
	static final List<Meta> ClassMetaList = new ArrayList(); // not really final..

		// meta
	final Class _clazz;
	List<Defn> _defns;
	boolean _hasIndexFields;
	int _arrayId;


		// services
	private UniqueIdFactory _uniqueIdFactory;
	private PostLazyInit _postLazyInit;
	private RecordStore _recordStore;
	private Index _index;
	private Search _search;


		// CacheRecordStore meta
	private boolean _readCache;
	int _maxAllowedCacheSize;  // this can change at runtime
	int _initialCacheSize;
	boolean _allowGCCache;


		// used for lazy init
	private volatile Boolean _initialized = Boolean.FALSE;
    public final Mode _mode;


	public static Meta Create (Class<? extends Record> clazz)
	{
		try {
			Meta meta = ClassMetaMap.get(clazz);
			if (meta == null) {
				meta = new Meta(clazz);
				synchronized (ClassMetaMap) {
					ClassMetaMap.put(meta._clazz, meta);
					ClassMetaMap.put(meta._clazz.getName(), meta);
					if (!ClassMetaList.contains(meta)) {
						meta._arrayId = ClassMetaList.size();
						ClassMetaList.add(meta);
					}
				}
			}
			
				// check if there is a parent class "Meta" defined.
				// if there is it was loaded because  it was referenced by this class
				// it needs to be removed as it was not supposed to be maintained
			Class parentClass = clazz.getSuperclass();
			if (ClassMetaMap.get(parentClass) != null) {
				ClassMetaList.remove(ClassMetaMap.get(parentClass));
				ClassMetaMap.remove(parentClass.getName());
				ClassMetaMap.remove(parentClass);
			}
			
			
		
			return meta;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}






	public static Collection<Meta> metas ()
	{
		return ClassMetaList;
	}


	public static Meta Instance (String clazz)
	{
		try {
			Class theClazz = Class.forName((String)clazz);
			return Instance(theClazz);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


		// can be either Class or Name of Class
	public static Meta Instance (Class<? extends Record> clazz)
	{
		Meta meta = ClassMetaMap.get(clazz);
			// this call  possiblely be made before the this "clazz" has actually
			// kicked off the static initializer to make RecordMeta initialized.
			// ie. Class A has a member in Class B, and accessed a Instance A with
			// member B before Class B is referenced. In that case DefnRecord.readImpl() would
			// call this method before it's init. To resolve that, this method iterate through
			// all method and call get() to kick of the static initaizer.
		if (meta == null) {
			for (Field field : clazz.getFields()) {
				try {
					field.get(null);
				}
				catch (Exception e) {
				}
			}
			meta = ClassMetaMap.get(clazz);
			if (meta == null) {
				throw new RuntimeException(New.str("Invalid Meta class: ", clazz.getName()));
			}
		}
		return meta;
	}



	private Meta (Class clazz)
	{
		_clazz = clazz;
		_defns = new ArrayList();
		_mode = Mode;

        /* NO need for create - as it will be created when xml is actually stored. In addition, when classes are override, 
         * This director won't get created for super classes
         
        if (!_mode._dir.exists()){
			_mode._dir.mkdirs();
		}

		if (!_mode._dir.isDirectory()) {
			throw new Error("Dir " + _mode._dir + " does not exist");
		}
		*/
	}


	public Class metaClass ()
	{
		return _clazz;
	}


	public File createFolderForClass (Class owner, String name, boolean master)
	{
		String dir = org.cosmo.common.xml.Util.Fmt("%s%s%s@%s%s",
                recordDir(master).getAbsolutePath(),
                File.separator,
                name,
                owner.getSimpleName(),
                File.separator);
		
		File file = new File(dir);
        /* NO need for create - as it will be created when xml is actually stored. In addition, when classes are override, 
         * This director won't get created for super classes
		if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
        	file.mkdir();
        }
        */
        return file;
	}


	public File createFolderForClass (Class owner, String name)
	{
		return createFolderForClass(owner, name, false);
	}

		// read CachedRecordStore for more doc on the params
	public Meta readCache (int maxAllowedCacheSize, int initialCacheSize, boolean allowGCCache)
	{
		_readCache = true;
		_maxAllowedCacheSize = maxAllowedCacheSize;
		_initialCacheSize = initialCacheSize;
		_allowGCCache = allowGCCache;
		return this;
	}

	public String dirValue ()
	{
		return _mode._dirStr;
	}


	public Meta requiresUniqueness (Class<? extends Meta.UniqueIdFactory> factory)
	{
		try {
			Constructor constructor = factory.getConstructor(new Class[] {Meta.class});
			_uniqueIdFactory = (Meta.UniqueIdFactory)constructor.newInstance(new Object[] {this});
				// uniqueIds are indexed thus set to true
			_hasIndexFields = true;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	public Meta postLazyInit (Class<? extends Meta.PostLazyInit> postInitClass)
	{
		try {
			Constructor constructor = postInitClass.getConstructor(new Class[] {});
			_postLazyInit = (Meta.PostLazyInit)constructor.newInstance(new Object[] {});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		return this;

	}

	public DefnBoolean DefnBoolean ()
	{
		return new DefnBoolean(this);
	}


	public DefnEnum DefnEnum ()
	{
		return new DefnEnum(this, null);
	}

	public DefnEnum DefnEnum (Enum defaultValue)
	{
		return new DefnEnum(this, defaultValue);
	}



	public DefnStr DefnStr (int size, boolean trimToFit) // in bytes
	{
		return new DefnStr(this, size, trimToFit);
	}

	public DefnStr DefnStr (int size) // in bytes
	{
		return DefnStr(size, false);
	}

	public DefnByte DefnByte ()
	{
		return new DefnByte(this);
	}

	public DefnShort DefnShort ()
	{
		return new DefnShort(this);
	}

	public DefnInt DefnInt ()
	{
		return new DefnInt(this);
	}

	public DefnLong DefnLong ()
	{
		return new DefnLong(this);
	}

	public DefnDate DefnDate ()
	{
		return new DefnDate(this);
	}

	public DefnBytes DefnBytes (int size) // in bytes
	{
		return new DefnBytes(this, size);
	}

	public DefnList DefnList ()
	{
		return new DefnList(this);
	}

	public DefnRecord DefnRecord ()
	{
		return new DefnRecord(this);
	}


	public DefnClob DefnClob ()
	{
		return new DefnClob(this);
	}

	public DefnBlob DefnBlob ()
	{
		return new DefnBlob(this);
	}


	public DefnXML DefnXML (int sizeForPathNameInBytes)
	{
		return new DefnXML(this, sizeForPathNameInBytes);
	}

	public UniqueIdFactory uniqueIdFactory ()
	{
		return _uniqueIdFactory;
	}

	public File recordDir ()
	{
		return recordDir(false);
	}


	public File recordDir (boolean master)
	{
		String dirValue = master ? Meta.Mode.master._dirStr : _mode._dirStr;
		File mainDir = new File(dirValue);
		if (mainDir.exists() ) {
			if (!mainDir.isDirectory()) {
				throw new RecordException (dirValue + " must be a directory");
			}
		}
        // NO need for create - as it will be created when xml is actually stored. In addition, when classes are override, 
        //This director won't get created for super classes
		//else {
		//	mainDir.mkdir();
		//}

		File recordDir = new File(dirValue + File.separator + _clazz.getSimpleName());

		if (recordDir.exists() ) {
			if (!recordDir.isDirectory()) {
				throw new RecordException (recordDir + " must be a directory");
			}
		}
        //NO need for create - as it will be created when xml is actually stored. In addition, when classes are override, 
        // This director won't get created for super classes
		//else {
			//recordDir.mkdir();
		//}
		return recordDir;
	}

	public String toString ()
	{
		return toXML().toString();
	}

	public Node toXML ()
	{
		Node xml = new Node("Meta").add("@class", _clazz.getName());
		for (Defn defn : _defns) {
			xml.add(defn.defnToXML());
		}
		return xml;
	}

	public String name ()
	{
		return metaClass().getSimpleName();
	}

	private void lazyInitCheck ()
	{
		if (_index == null) {
			synchronized (_initialized){
				if (_initialized == Boolean.FALSE) {
					synchronized (this) {
						if (_index == null &&_initialized == Boolean.FALSE) {
							try {
									// this kicks of lazy init for java.lang.reflrect.Field for Defns
								for (Defn defn: _defns) {
									defn.field();
								}

								_index = Meta.Mode._isMaster ? new Index(this) : new ReadOnlyIndex(this);
								_search = Meta.Mode._isMaster ? new Search(this) : new ReadOnlySearch(this);

								if (_mode._isMaster) {
									performIndexAndRecordCountSanityCheck();
								}

								_recordStore = _readCache ? new CachedRecordStore(this) : new RecordStore(this);


									// take a snapshot of current count and read upto the snapshot count.
									// with local files - snapshot count is not required since no writing is
									// happening. however if reading from remote files which writing is happening at same time
									// then we need a snapshot count so that we don't get out of sync or IndexArrayOutofBound
								int count = _recordStore.count();

								New.prt("Syncing Listeners for Meta [" , name() ,"] ", Clock.timer().markAndCheckRunning());


								for (Defn defn : _defns) {

									org.cosmo.common.record.Listener listener = defn._listener;

									while (listener != null) {
										listener.sync(count);
										New.prt("Done Syncing Listener [" , name() ,"] [" , defn.getDeclaringFieldName() , "] [", listener.getClass().getSimpleName(), "] with count " , count, " ", Clock.timer().markAndCheckRunning()); // " and final size " , defn.getFieldCache().totalSize() / 1048576 , "MB");
										listener = listener._next;
									}

									/*
									if (defn.getFieldCache() != null) {
										defn.getFieldCache().sync(count);
										System.out.println("Init FieldCache [" + name() +"] [" + defn.getDeclaringFieldName() + "] with count " + count + " and final size " + defn.getFieldCache().totalSize() / 1048576 + "MB");
									}
									if (defn instanceof DefnRecord) {
										DefnRecord defnRecord = (DefnRecord)defn;
										if (defnRecord._groupByList != null) {
											int totalSynced = defnRecord._groupByList.sync(count);
											System.out.println("Init GroupBy [" + name() +"] [" + defn.getDeclaringFieldName() + "] with count " + count + " and loaded " + totalSynced + " and final size " + defn.getFieldCache().totalSize() / 1048576 + "MB");
										}
									}
									*/
								}

									// run any post lazy init for the class
								if (_postLazyInit != null) {
									_postLazyInit.recordMetaPostInitialization();
								}

								_defns = Collections.unmodifiableList(_defns);


							}
							catch (IOException e) {
								throw new RuntimeException(e);
							}
							catch (Exception e) {
								throw new RuntimeException(e);
							}

							_initialized = Boolean.TRUE;



						}
					}
				}
			}
		}
	}


	/*
	 * 	This sanity ensures that the index is always consistent with the recordstore
	 *  This is to guard the case in recordstore.write() that record is written but
	 *  jvm crashes during index.commit(). when that is the case, upon restart this method
	 *  will remove any record since that index.commit() - basically the size of index.
	 *
	 *  Also, during the index.commit() the system also does check if in consistency is found
	 *  if it does the system will exit() upon restart this is the place where system will correct
	 *  itself.
	 *
	 *  If no index on this meta, it uses the largest record count as the bases and sync all the files
	 *
	 *  Note. todo - cleanup lobstore and xmlstore if this is the case.
	 */
	synchronized void performIndexAndRecordCountSanityCheck ()
	{
		if (_hasIndexFields) {
			int baseLineRecordCount = _search._indexReader.maxDoc();
			alignDefnFileRecordCount(baseLineRecordCount, "index");
		}
		else {
				// pick the biggest count as the baseline.
			int baseLineRecordCount = 0;
			for (Defn defn : _defns) {
				int defnRecordCount = defn.readCount();
				if (defnRecordCount > baseLineRecordCount) {
					baseLineRecordCount = defnRecordCount;
				}
			}
			alignDefnFileRecordCount(baseLineRecordCount, "baseLineRecord");
		}
	}

	synchronized void alignDefnFileRecordCount (int baseLineRecordCount, String baseLineType)
	{
		try {
			for (Defn defn : _defns) {
				int defnRecordCount = (int)(defn._channel.size() / defn.size());
				if (baseLineRecordCount != defnRecordCount) {
					String msg = New.str("Meta [", _clazz.getSimpleName(), "] field [", defn.getDeclaringFieldName(), "] correcting size inconsistency [ ", baseLineType,":", baseLineRecordCount, "  vs  record:", defnRecordCount, "]");
					System.err.println(msg);
					defn._channel.setLength((long)((long)baseLineRecordCount * (long)defn.size()));
				}
			}
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}


	public RecordStore<T> store ()
	{
		lazyInitCheck();
		return _recordStore;
	}

	public Index index ()
	{
		lazyInitCheck();
		return _index;
	}

	public Search search ()
	{
		lazyInitCheck();
		return _search;
	}


	public static Node recordMetaTree () throws Exception
	{
		XML container = new XML("metaTree");
		Node rootFolder = PublicFolder.createRootFolderOn(container);
		for (Meta aMeta : Meta.metas()) {
			Node metaFolder = PublicFolder.createFolder(rootFolder, "Meta", aMeta._clazz.getSimpleName(), container);
			List<Defn> defns = aMeta._defns;
			for (int i = 0; i < defns.size(); i++) {
				Defn aDefn = defns.get(i);
				PublicFolder.addItemToFolder(metaFolder, "Defn", aDefn._declaringFieldName,
						New.str(aMeta._clazz.getName(), ',' , aDefn._declaringFieldName), container);
			}
		}
		return container;
	}


	public Defn defnByName (String defnName)
	{
		for (Defn defn : _defns) {
			if (defnName.equals(defn._declaringFieldName)) {
				return defn;
			}
		}
		return null;
	}


	public List<Defn> defns ()
	{
		return _defns;
	}

	public List<Defn> defnsByNames (Set<String> defnNames)
	{
		List matchedDefns = new ArrayList();
		for (String defnName : defnNames) {
			Defn defn = defnByName(defnName);
			if (defn != null) {
				matchedDefns.add(defn);
			}
		}
		return matchedDefns;
	}





	public static abstract class UniqueIdFactory
	{
		private Meta _meta;
		public static final String Padding = "Pad";

		public UniqueIdFactory (Meta meta)
		{
			_meta = meta;
		}

		public String md5Hex (String s)
		{
			return DigestUtils.md5Hex(s + Padding);
		}

			// this should only be Object that has consistent hashCode ie Number, String (?)
		public String hashString (Object... obj)
		{
			StringBuffer s = new StringBuffer();
			for (int i = 0; i < obj.length; i++) {
				s.append(Integer.toHexString(obj[i].hashCode()));
			}
			s.append(Padding);
			return s.toString();
		}

		public abstract String generate (Record r);
	}


	public interface PostLazyInit
	{
		public void recordMetaPostInitialization () throws Exception;
	}


}
