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

import java.util.List;

import org.cosmo.common.model.User;
import org.cosmo.common.xml.Node;
import org.json.JSONException;
import org.json.JSONObject;

public class Tx <T extends Record>
{
	long _id;
	T _record;

		// "loaded" implies ALL the fields of the record is read from RecordStore. otherwise it means the record "instance"
		// is there only. The only time this is false is when not using CachedRecordStore and the Record is created
		//  via DefnRecord.readImpl  where the Record is a member field.

	volatile boolean _loaded;


	Tx (T record)
	{
		_record = record;
		_id = Long.MIN_VALUE;
		_loaded = false;
	}

		// absolute location id in the file
	public long id ()
	{
		return _id;
	}

		// unique id of the record used for search
	public String uid ()
	{
		return _record.meta().uniqueIdFactory().generate(_record);
	}

	public String idString ()
	{
		return Long.toString(_id);
	}

	public T load (boolean readCached)
	{
		if (!_loaded || !readCached) {
			synchronized (this) {
				if (!_loaded || !readCached) {
					uncachedRead(_id, _record);
					_loaded = true;
				}
			}
		}
		return _record;
	}

		// allows CachedTx to override to do uncached read
	protected void uncachedRead (long id, T toRecord)
	{
		_record.meta().store().read(_id, _record, true);
	}

	public T load ()
	{
		return load(true);
	}


	public boolean insert ()
	{
		return insert(false, true);
	}


		//This really is used by slave to ensure that data created from slave is
		// created synchronize-ly on master and only exit this method until it does.
		// how it works is first insert() is called in which a RecordLog entry
		// will be created, right after we put this slave in wait state,
		// basically waiting for RecordLog entry to be consumed by master, master will then create logs
		// then consumed by this slave. in RecordLog.notifyDefnListener(). upon receiveing
		// new records it will do notify(). hence exiting this method. the notification comes in sequential
		// order it's not required that "same" record was created
		// Note - need to deal with duplicationException! - currently timeout or
	public synchronized boolean insertSync ()
	{
		if (_record.meta()._mode._isSlave) {
			boolean result = insert();
			synchronized (_record.meta().store().NotifyOnInsert) {
				try {
					_record.meta().store().NotifyOnInsert.wait(15000); // may have to tweak this
		    		User.Meta.search().refreshReader(); // slave search() (ReadOnlySearch.java) requires manual refresh
				}
				catch (Exception e) {
					e.printStackTrace();
					return false;
				}
			}
			return result;
		}
		else {
			return insert();
		}
	}

		// Caution!  this method attempts to insert the same record AGAIN to the DB
		// will get DuplicateRecordException
		//
		// Note. this is used in a scenario where slave loads all data and attempt to insert to master on same data
		// by flippin the _loaded the data can be inserted again
	public boolean insertDuplicate ()
	{
		_loaded = false;
		return insert(false, false);
	}



	public boolean insert (boolean ignoreDuplicateException, boolean autoCommit)
	{
		if (_loaded) {
			throw new RecordException("Attempt to Insert already [Inserted] record at [" + _id + "]");
		}
		try {
			_record.meta().store().write(_record, autoCommit);
			return true;
		}
		catch (DuplicateRecordException e) {
				// ignore exception and return false to indicate record did not get inserted
			if (ignoreDuplicateException) {
				return false;
			}
			throw e;
		}
	}


	public Node recordToXML ()
	{
		return recordToXML(false, true);
	}

	public Node recordToXML (boolean skipNullFields, boolean load)
	{
		Node record = new Node(_record.meta()._clazz.getSimpleName()).add("@id", _record.tx().id());
		if (load) {
			_record.tx().load();
		}

		Meta meta = _record.meta();
		List<Defn> defns = meta._defns;
		for (Defn defn : defns) {
			Node field = (defn.fieldToXML(_record));
			if (field.value() == null && skipNullFields) {
				continue;
			}
			record.add(field);
		}
		return record;
	}

	public JSONObject recordToJSON ()
	{
		try {
			return recordToJSON(false, true);
		}
		catch (JSONException e) {
			throw new IllegalArgumentException(e);
		}
		
	}
	
	public JSONObject recordToJSON (boolean skipNullFields, boolean load)
	  throws JSONException
	{
		JSONObject json = new JSONObject(); 
		if (load) {
			_record.tx().load();
		}

		Meta meta = _record.meta();
		List<Defn> defns = meta._defns;
		for (Defn defn : defns) {

			defn.fieldToJSON(_record, json, skipNullFields, load);
				
		}
		return json;
	}	
	

	public String toString ()
	{
		return recordToXML().toString();
	}


}
