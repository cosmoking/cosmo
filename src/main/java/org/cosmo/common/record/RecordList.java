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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/*
  	This List implementation is recommend to be used ONLY for getting all objects
  	of the list. Avoid uses of index and lookup table for many to many.
  	Small memory footprint if wish to go through the list. Faster updates

  	ie. you want to traverse through all object in the list

	RecordList operates differently than regular Record Object in which
	the Record is a linklist of Records.

	- The first RecordList is used as PageHeader and referenced by the other Records
	- The subsequent Recordlist is used as DataLists and referenced internal by the first RecordList(PageHeader)

	So for a Record that has a List - it will have a reference to a RecordList which will itself have
	reference to other RecordList and so on

	in the following example the first RecordList(headerpg) holds the information about this list. The remaining
	RecordList(dataPg) holds the actual data and points to the next RecordList(dataPg)



    // headerPg    |1 count    |2 nextPg(0) |3 lastPg(1)|4 nextOpenSpotInLastPage |             |
	// dataPg(0)   |6 record1  |7 record2   |8 record 3 |9 ......   |10 next(11)  |
	// dataPg(1)   |11 record7 |12record8   |13record 9 |14......   |15           |

	/XXX still need to fix count

*/
public class RecordList<T extends Record> implements IntrinsicRecord
{

	public static final Meta RecordMeta = Meta.Instance(RecordList.class);

		// RawBytes is the "storage space" for this list
		// It is  RecordSize (8 bytes) *  (number of entry  +  NextRecordId)
	public static final DefnBytes<RecordList> RawBytes = RecordMeta.DefnBytes(DefnLong.Size * (DefnList.PageSize + 1));

	private Tx<T> _proxy;

	public Meta _type;
	public byte[] _rawBytes;
	public List<T> _list;
	public List<Long> _deletions;
	public List<T> _additions;

	public RecordList ()
	{
		_proxy = new ListTx(this);
		_list = new ArrayList<T>();
		_deletions = new ArrayList<Long>();
		_additions = new ArrayList<T>();
		_rawBytes = new byte[RawBytes._variableSize];
		Arrays.fill(_rawBytes, (byte)-1);
	}


	public Tx<T> tx ()
	{
		return _proxy;
	}

	public Meta meta ()
	{
		return RecordMeta;
	}

		// gets called when RecordList is created by DefnRecord.setupRecord()
	public void setDefn (Defn defn)
	{
		if (_type == null) {
			_type = ((DefnList)defn)._declaringMeta;
			_list = new ArrayList<T>();
			_rawBytes = new byte[RawBytes._variableSize];
		}
	}

	public void add (T record)
	{
		if (_type == null) {
			_type = record.meta();
		}
		_list.add(record);
		_additions.add(record);
	}

	public void delete (T record)
	{
		if (_type == null) {
			_type = record.meta();
		}
		if (record.tx()._id > -1) {
			_list.remove(record);
			_additions.remove(record);
			_deletions.add(record.tx()._id);
		}
	}

	public Iterator<T> list ()
	{
		return _list.iterator();
	}

	void clearLists ()
	{
		_list.clear();
		_deletions.clear();
		_additions.clear();
	}
}




class ListTx<T extends Record> extends Tx <T>
{

	public ListTx (T record)
	{
		super(record);
	}


	public RecordStore store ()
	{
		return _record.meta().store();
	}

		// deal readLocalFirst later
	public T load (boolean readLocalFirst)
	{
		try {
			RecordList headerList = (RecordList)super.load(false);
			ByteBuffer headerListBytes = ByteBuffer.wrap(headerList._rawBytes);
			long count = headerListBytes.getLong();
			long nextListId = headerListBytes.getLong();
			long lastPageId = headerListBytes.getLong();
			RecordList dataList = (RecordList)store().read(nextListId);
			((RecordList)_record).clearLists();
			while (dataList != null) {
				ByteBuffer DataListBytes = ByteBuffer.wrap(dataList._rawBytes);
				for (int i = 0; i < DefnList.PageSize ; i++) {
					long id = DataListBytes.getLong();
					if (id > -1) {
						Record record = Meta.Instance(((RecordList)_record)._type._clazz).store().newInstance(id);
						if (record != null) {
							((RecordList)_record)._list.add((T)record);
						}
					}
				}
				nextListId = DataListBytes.getLong();
				dataList = nextListId < 0 ? null : (RecordList)store().read(nextListId);
			}
			return (T)_record;
		}
		catch (Exception e) {
			throw new RecordException(e);
		}
	}

	public boolean insert ()
	{
		update();
		return true;
	}

	public void update ()
	{
		try {
			if (!_loaded) {
					// since not store yet, create from list and reset additions and deletions
				create(((RecordList)_record)._list);
				((RecordList)_record)._deletions.clear();
				((RecordList)_record)._additions.clear();
			}
			else {
				RecordList<T> list = ((RecordList)_record);
				RecordList headerList = (RecordList)super.load(false);
				ByteBuffer headerListBytes = ByteBuffer.wrap(headerList._rawBytes);
				long count = headerListBytes.getLong();
				long nextListId = headerListBytes.getLong();
				long lastListId = headerListBytes.getLong();
				RecordList dataList = (RecordList)store().read(nextListId);

					// deletions
				if (!list._deletions.isEmpty()) {
					while (dataList != null && !list._deletions.isEmpty()) {
						ByteBuffer dataListBytes = ByteBuffer.wrap(dataList._rawBytes);
						boolean pageContainsDeletion = false;
						for (int i = 0; i < DefnList.PageSize && !list._deletions.isEmpty(); i++) {
							long id = dataListBytes.getLong();
							if (id > -1) {
								if (list._deletions.remove(id)) {
										// rewind and delete this record id by overwritting to -1
									dataListBytes.position(dataListBytes.position() - 8);
									dataListBytes.putLong(-1);
									pageContainsDeletion = true;
								}
							}
						}
							// update page if it has deletions made
						if (pageContainsDeletion) {
							store().write((T)dataList, dataList.tx()._id, false, false);
							pageContainsDeletion = false;
						}
						nextListId = dataListBytes.getLong();
						dataList = nextListId < 0 ? null : (RecordList)store().read(nextListId);
					}
				}

					// additions:
				if (!list._additions.isEmpty()) {
					RecordList lastList = (RecordList)store().read(lastListId);
					ByteBuffer dataListBytes = ByteBuffer.wrap(lastList._rawBytes);

						// goto last page and insert until page is full OR all added
					int lastOpenSpot = (int)headerListBytes.getLong();
					if (lastOpenSpot > 0 && lastOpenSpot < DefnList.PageSize) {
						dataListBytes.position(lastOpenSpot * 8);
						Iterator<T> _additions = list._additions.iterator();
						for (int i = lastOpenSpot; i < DefnList.PageSize; i++) {
							T record = _additions.hasNext() ? _additions.next() : null;
							if (record != null) {
								if (!record.tx()._loaded) {
									record.tx().insert();
								}
								dataListBytes.putLong(record.tx()._id);
								 _additions.remove();
							}
							if (!_additions.hasNext() && i != DefnList.PageSize) {
								lastOpenSpot = i + 1;
								break;
							}
						}
						store().write((T)lastList, lastList.tx()._id, false, false);
						headerListBytes.position(headerListBytes.position() - 8);
						headerListBytes.putLong((long)lastOpenSpot);
						store().write((T)headerList, headerList.tx()._id, false, false);
					}

						// if still more
					if (!list._additions.isEmpty()) {
							// insert the remains
						long newNextPageId = create(list._additions);

							// make old last page to link to the new next page from new inserts
						RecordList oldLastList = (RecordList)store().read(lastListId);
						dataListBytes = ByteBuffer.wrap(oldLastList._rawBytes);
						dataListBytes.position(oldLastList._rawBytes.length - 8);
						dataListBytes.putLong(newNextPageId);
						store().write((T)oldLastList, oldLastList.tx()._id, false, false);
					}
				}
				((RecordList)_record).clearLists();
			}
		}
		catch (Exception e) {
			throw new RecordException(e);
		}


	}

	private long create (List<T> additions)
	{
		if (additions.isEmpty()) {
			return -1;
		}
		try {
				// create pages (RecordList) needed to hold all the Records
			int noPages = additions.size() % DefnList.PageSize == 0
				? additions.size() / DefnList.PageSize
				: additions.size() / DefnList.PageSize + 1;
			RecordList dataLists[] = new RecordList[noPages];
			for (int i = 0; i < noPages; i++) {
				dataLists[i] = new RecordList();
				store().write((T)dataLists[i], false);
			}

				// for each DataLists write records
			Iterator<T> adds = additions.iterator();
			long lastOpenSpot = -1;
			for (int pageIdx = 0; pageIdx < dataLists.length; pageIdx++) {

					// fill each id in DataList write record
				RecordList dataList = dataLists[pageIdx];
				ByteBuffer dataListBuffer = ByteBuffer.wrap(dataList._rawBytes);
				for (int i = 0; i < DefnList.PageSize; i++) {
					T record = adds.hasNext() ? adds.next() : null;
					if (record != null) {
						if (!record.tx()._loaded) {
							record.tx().insert();
						}
						dataListBuffer.putLong(record.tx()._id);
						// adds.remove();
					}
					if (!adds.hasNext() && i != DefnList.PageSize) {
						lastOpenSpot = i + 1;
						break;
					}
				}

					// at the end of page set pointer to next page if not last
				long nextListId = pageIdx + 1 == dataLists.length ? -1 : dataLists[pageIdx + 1].tx()._id;
				dataListBuffer.putLong(nextListId);
					// update DataList
				store().write((T)dataList, dataList.tx()._id, false, false);
			}

				// setup HeaderList
			RecordList<T> headerList = ((RecordList)_record);
			ByteBuffer headerListBytes = ByteBuffer.wrap(headerList._rawBytes);
			long count = _loaded ? headerListBytes.getLong() + additions.size() : additions.size();
			long nextListId = _loaded? headerListBytes.getLong() : dataLists[0].tx()._id;
			long lastPageId = dataLists[dataLists.length - 1].tx()._id;
			headerListBytes.position(0);
			headerListBytes.putLong(count);
			headerListBytes.putLong(nextListId);
			headerListBytes.putLong(lastPageId);
			headerListBytes.putLong(lastOpenSpot);

			if (_loaded) {
				store().write((T)headerList, headerList.tx()._id, false, false);
			}
			else {
				store().write((T)headerList, false);
			}
			return dataLists[0].tx()._id; // returns first data page id
		}
		catch (Exception e) {
			throw new RecordException(e);
		}
	}



}









