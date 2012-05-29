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
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.document.Field.Index;
import org.cosmo.common.file.FixedFilePartition;
import org.cosmo.common.util.New;
import org.cosmo.common.util.Util;
import org.cosmo.common.xml.Node;
import org.json.JSONException;
import org.json.JSONObject;


abstract public class Defn <T extends Record> {

	public static final boolean EnableDefnHeaderException = Util.getProperty(Boolean.class, "EnableDefnHeaderException", true);

		// header
	public static enum Header {


		UnInitialized(false),
		IsNull(false),
		NonNull(false),
		Inactive(false),
		Corrupt(true);

			// by enable this - fields that are Corrupt and etc when throw an Exception

		public final byte _asByte;
		public final boolean _throwException;
		Header (boolean throwException)
		{
			_asByte = (byte)this.ordinal();
			_throwException = EnableDefnHeaderException ? throwException : false;
			//System.out.println("EnableDefnHeaderException" + EnableDefnHeaderException + " this " + _throwException);
		}
	}

	public static enum WriteMode {
		WriteOnly,
		LogOnly,
		WriteAndLog
	}

	public static final Header[] Headers = Header.values();
	public static final int HeaderByteSize = 1;
	public static int FilePartitionSizeInMB = 128;  // Mess with this on runtime and die

	public Meta _declaringMeta;
	Field _field;
	public boolean _isIndexField;
	public Index _luceneIndexType;
	public int _variableSize;  // for String, Byte, Clob
	public String _declaringFieldName;
	public DefnFormatter _formatter; 	// xxx : refactor this with the index() method to be one , ie use Lucene's formatter of filter
	public Object _defaultValue;
	public boolean _lazyLoad;  // when Record is read from recordstore.read() this field is not read (requires explict field read).
								// bcos: not fields are usually used, this increases performance

	transient FixedFilePartition _channel;
	private ByteBuffer _writeDataIO;
	private byte[] _writeBytes;
	private ByteBuffer _readDataIO;
	private byte[] _readBytes;
	private final Lock _readLock = new ReentrantLock();
	private final Lock _writeLock = new ReentrantLock();


	public Listener _listener;


	public int _arrayId; // idx within Meta.defns list


		// returns the actual "byte" size of the Defn
	abstract int size ();

	abstract Class[] typeClasses ();

		// read data from DataIO and convert to typed value, if directConversion is true, impl returns the direct converion of
		// object from rawBytes, ie DefnClob would return String of bytes, rather do another lookup
	abstract public Object readImpl (ByteBuffer dataIO, boolean directConvertFromBytes)
	  throws IOException;

	abstract public byte[] writeImpl (byte[] bytes, Object value, long i)
	  throws IOException;




	Defn (Meta declaringMeta)
	{
		this (declaringMeta, 0, null);
	}


	Defn (Meta declaringMeta, int variableSize, Object defaultValue)
	{
		_declaringMeta = declaringMeta;
		_variableSize = variableSize;
		_defaultValue = defaultValue;

				// update meta attributes for this defn
		this._arrayId = _declaringMeta._defns.size();
		_declaringMeta._defns.add(this);
		_formatter = new DefnFormatter(this);
		_isIndexField = false;
		_luceneIndexType = Index.NOT_ANALYZED;
	}


	public Field field ()
	{
			// yes, we are doing lazy init here, not synchronized, not volatile, this method WILL BE called by Meta.lazyInitCheck()
			// which is synchronized
		if (_field == null) {
			lazyFieldInit();
		}
		return _field;
	}

	public boolean isFieldTypePrimitive ()
	{
		return field().getType().isPrimitive();
	}

	public boolean hasAdditionalSizeInfo ()
	{
		return false;
	}


	public Object getValue (T record)
	{
		try {
			return _field.get(record);
		}
		catch (IllegalAccessException e) {
			throw new Error(e);
		}
	}

	public void setValue (T record, Object value)
	{
		try {
			_field.set(record, value);
		}
		catch (IllegalAccessException e) {
			throw new RecordException(e);
		}
	}





		// for "native" types this is fine, for DefnClob, DefnXML, DefnBlob, DefnRecord, which
		// this the bytes as a "reference" to the actual stored data, this needs to be overriden
/*
	public Object bytesToValue (ByteBuffer bytes)
	  throws IOException
	{
		return this.readImpl(bytes);
	}
*/

	
	private static Field getField(Class clazz, String fieldName) throws NoSuchFieldException {
		    try {
		      return clazz.getDeclaredField(fieldName);
		    } catch (NoSuchFieldException e) {
		      Class superClass = clazz.getSuperclass();
		      if (superClass == null) {
		        throw e;
		      } else {
		        return getField(superClass, fieldName);
		      }
		    }
	}




	public void lazyFieldInit ()
	{
		
		String declaredFieldName =  getDeclaringFieldName();
		String declaredFieldName1 = Character.toLowerCase(declaredFieldName.charAt(0)) + new String(declaredFieldName.toCharArray(), 1, declaredFieldName.length() - 1);
		String declaredFieldName2 = "_" + declaredFieldName1;
		
		try {
			_field = getField(_declaringMeta._clazz, declaredFieldName1);
		}
		catch (NoSuchFieldException e) {
			try {
				_field = getField(_declaringMeta._clazz, declaredFieldName2);
			}
			catch (NoSuchFieldException e2) {
				
			}
		}
		
		if (_field != null) {
			_field.setAccessible(true);
		}		
		else {
			throw new RuntimeException(New.str("Unable to find match field variable for ", declaredFieldName1, " or ", declaredFieldName2, " for meta ", this._declaringMeta._clazz.getName()));
		}
		Class fieldClass = _field.getType();
		boolean valid = false;
			// check if the variable declared matches the Defn type, for enum and record losen up
		for (Class typeClass : typeClasses()) {
			if (typeClass.equals(fieldClass)
					|| (this instanceof DefnEnum && typeClass.isAssignableFrom(fieldClass))
					|| (this instanceof DefnRecord && typeClass.isAssignableFrom(fieldClass))) {
				valid = true;
			}
		}
		if (!valid) {
			throw new RecordException(New.str("Invalid Type [", fieldClass, "] for field: " + _field.getType().getSimpleName()));
		}

		if (_defaultValue == null) {
			// set defaultValue if field is primitive type
			if (isFieldTypePrimitive()) {
				if (fieldClass == boolean.class) {
					_defaultValue = false;
				}
				else if (fieldClass == int.class) {
					_defaultValue = 0;
				}
				else if (fieldClass == long.class) {
					_defaultValue = (long)0;
				}
				else if (fieldClass == short.class) {
					_defaultValue = (short)0;
				}
				else if (fieldClass == byte.class) {
					_defaultValue = (byte)0;
				}
				else {
					throw new RecordException("Unknown primitive type " + fieldClass.getName());
				}
			}
		}


		try {
				// read is always from master
			String readFile = New.str(_declaringMeta.recordDir(true), File.separator, _field.getName());
			String writeFile = New.str(_declaringMeta.recordDir(_declaringMeta._mode._isMaster), File.separator, _field.getName());

				// should read size from Defn
			_channel = new FixedFilePartition(new File(readFile), new File(writeFile), 1024 * 1024 * FilePartitionSizeInMB, size());
			_writeBytes = new byte[size()];
			_writeDataIO = ByteBuffer.wrap(_writeBytes);
			_readBytes = new byte[size()];
			_readDataIO = ByteBuffer.wrap(_readBytes);
		}
		catch (IOException e) {
			throw new RecordException(e);
		}
	}


	public void notifyListener (Object value, long id, boolean enable)
	{
		if (_listener != null && enable) {
			_listener.notify(value, id);
		}
	}


	public int readCount ()
	{
		try {
			return (int)(_channel.size() / size());
		}
		catch (IOException ioe) {
			throw new RecordException(ioe);
		}
	}


	public T read (long i, T toRecord)
	  throws IOException, IllegalArgumentException, IllegalAccessException
	{
		Object fieldValue = read(i);
		_field.set(toRecord, fieldValue);
		return toRecord;
	}


	public Object read (long i)
	  throws IOException
	{
		_readLock.lock();
		try {
			_readDataIO.rewind();
			_channel.read(_readDataIO, i * size());
			byte headerType = _readDataIO.get();

			if (Header.IsNull == Headers[headerType]) {
				return _defaultValue;
			}
			else if (Header.UnInitialized == Headers[headerType]) {
				if (Headers[headerType]._throwException) {
					throw new DefnHeaderException(i, this, Headers[headerType]);
				}
				return _defaultValue;
			}
			else if (Header.Corrupt == Headers[headerType]) {
				System.err.println(New.str("Read Corrupt [", this._declaringFieldName, "] for record ", i));
				if (Headers[headerType]._throwException) {
					throw new DefnHeaderException(i, this, Headers[headerType]);
				}
				return _defaultValue;
			}
			else {
				return readImpl(_readDataIO, false);
			}
		}
		finally {
			_readLock.unlock();
		}
	}


		// read raw data (including header byte) in byte buffer
	public ByteBuffer readFullRawBytes (int maxCounts)
	  throws IOException
	{
		int count = Math.min(readCount(), maxCounts);
		return _channel.readFull(count * size());
	}


		// read refined data (with removed header byte) in array
	public Object readAll (int maxCounts)
	  throws IOException
	{
		int count = Math.min(readCount(), maxCounts);
		byte[] buf = readFullRawBytes(maxCounts).array();
		ByteBuffer readDataIO = ByteBuffer.allocate(size());
		Object elements = null;

		if (this instanceof DefnRecord) {
			elements = Array.newInstance(long.class, count);
			for (int i = 0, offset = 0, size = size(), c = count; i < c; i++, offset += size) {
				readDataIO.put(buf, offset, size);
				readDataIO.rewind();
				readDataIO.get(); // skip header byte
			    Array.set(elements, i, readDataIO.getLong());
				readDataIO.rewind();
			}
		}
		else {
			elements = Array.newInstance(field().getType(), count);
			for (int i = 0, offset = 0, size = size(), c = count; i < c; i++, offset += size) {
				readDataIO.put(buf, offset, size);
				readDataIO.rewind();
				readDataIO.get(); // skip header byte
			    Array.set(elements, i, readImpl(readDataIO, false));
				readDataIO.rewind();
			}
		}

		return elements;
	}


	public Object readAll ()
	  throws IOException
	{
		return readAll(readCount());
	}


	public void update (long i, Object fieldValue)
	  throws IOException
	{
		_writeLock.lock();
		try {
			RecordLog.Entry logEntry = _declaringMeta.store().newRecordLogEntry(false, i);

				// write to file
			write(i, fieldValue, null, false, logEntry);

				// XXX this should be in the write method ?? update cached Record instance if there is one
			if (_declaringMeta.store() instanceof CachedRecordStore) {
				T record = (T)((CachedRecordStore)_declaringMeta.store()).readCached(i);
				if (record != null) {
					setValue(record, fieldValue);
				}
			}

			if (this._isIndexField) {
				//throw new RuntimeException("XXX implement this indexField");
			}

			_declaringMeta.store().writeRecordLogEntry(logEntry);
		}
		finally {
			_writeLock.unlock();
		}

	}

	public void write (long i, Object src, IndexDocument idx, boolean create, RecordLog.Entry logEntry)
	  throws IOException
	{
		_writeLock.lock();
		try {
			  // XXX Note: rawSrcBytes[0] will be header for primitive types (while Clob, Blob, XML will not)
			byte[] rawSrcBytes = src == null ? null : writeImpl(_writeBytes, src, i);
			notifyListener(src, i, Meta.Mode._isMaster);
			_writeBytes[0] = src == null ? Header.IsNull._asByte : Header.NonNull._asByte;

			if (logEntry != null) {
				logEntry.addDefn(this, _writeBytes[0], rawSrcBytes);
			}

				// done and return
			if (this._declaringMeta._mode._isSlave) {
				return;
			}

			_writeDataIO.rewind();
			_channel.write(_writeDataIO, i * size());
			if (create && idx != null) {
				idx.addField(this, src);
			}
			if (!create && _isIndexField) {
				idx = new IndexDocument(_declaringMeta.index(), i);
				idx.update(this, src);
			}
		}
		finally {
			_writeLock.unlock();
		}
	}



	public static enum FormatType {ForIndex, ForSearch, ForDisplay};

		// sub class can override this for search,index, or display
		// ie Remove special chars for indexing..
	public String fmt (Object o, FormatType type)
	{
		if (o == null) {
			throw new RecordException("Can not format NULL value");
		}
		return _formatter.fmt(o, type);
	}

	public Object parse (String s)
	{
		throw new RuntimeException(this.getClass().getName() + " needs to implemented");
	}

	public String toString ()
	{
		return defnToXML().toString();
	}

	public Node defnToXML ()
	{
		return new Node(getClass().getSimpleName()).add("@field", _field.getName(), "@size", size(), "@indexed", _isIndexField);
	}
			

	public Node fieldToXML (Record record)
	{
			// probably should show field name and etc
		Node field= new Node(getDeclaringFieldName());
		try {
			field.set(_field.get(record));
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		return field;
	}
	
	public void fieldToJSON (T record, JSONObject json, boolean skipNullFields, boolean load)
	  throws JSONException
	{
		Object value = getValue(record);		
		if (value == null && skipNullFields) {
			return;
		}		
		json.put(_declaringFieldName, value);		
	}
	

	public String getDeclaringFieldName ()
	{
		try {
			if (_declaringFieldName == null) {
				Field fields[] = _declaringMeta._clazz.getFields();
				for (Field field : fields) {
					Object value = field.get(null);
					if (value != null && value.equals(this)) {
						_declaringFieldName = field.getName();
						break;
					}
				}
			}
			return _declaringFieldName;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String getIndexFieldName ()
	{
		return getDeclaringFieldName();
	}

	protected Defn index (boolean index)
	{
		if (index) {
			_declaringMeta._hasIndexFields = true;
		}
		_isIndexField = index;
		return this;
	}

	protected Defn index (boolean index, Index indexType)
	{
		index(index);
		_luceneIndexType = indexType;
		return this;
	}

	protected Defn lazyLoad (boolean lazyLoad)
	{
		_lazyLoad = lazyLoad;
		return this;
	}

	protected Defn listener (Class<? extends Listener> listenerClass)
	{
		return listener(listenerClass, null);
	}


	protected Defn listener (Class<? extends Listener> listenerClass, Class paramClass)
	{
		try {
				// create an instance of listener
			Listener listener = null;
			if (paramClass != null) {
				Constructor constructor = paramClass.getConstructor(new Class[] {});
				Object param = constructor.newInstance(new Object[] {});
				while (paramClass != null) {
					try {
						constructor = listenerClass.getConstructor(new Class[] { paramClass});
						break;
					}
					catch (Exception e) {
						if (paramClass == Object.class) {
							throw e; // tried everything... throw up
						}
						paramClass = paramClass.getSuperclass();
					}
				}

				listener = (Listener)constructor.newInstance(new Object[] { param});
			}
			else {
				Constructor constructor = listenerClass.getConstructor(new Class[] {});
				listener = (Listener)constructor.newInstance(new Object[] {});


			}
			listener._defn = this;

				// if there is more than one chain them up
			if (_listener == null) {
				_listener = listener;
			}
			else {
				for (Listener lastListener = _listener; lastListener != null; lastListener = lastListener._next) {
					if (lastListener._next == null) {
						lastListener._next = listener;
						break;
					}
				}
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		return this;
	}



	public Index getIndexType ()
	{
		return _luceneIndexType;
	}


	public boolean ifTrueForFunction (Object rightValue, String function, Object leftValue)
	{
		throw new IllegalArgumentException(function);
	}




	public long[] sort ()
	{
		return null;
	}

	public void sort (Collection<Long> ids)
	{

	}

	public void sort (Iterable<Long> ids)
	{

	}

	public void sort (long[] ids)
	{

	}


}
