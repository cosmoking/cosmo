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

import static org.cosmo.common.record.DefnDate.Precision.yyyyMMdd;

import java.io.File;

import org.apache.commons.io.FileUtils;

import org.cosmo.common.statistics.Clock;
import org.cosmo.common.util.Bytes;
import org.cosmo.common.util.Util;


public class Test <T extends Record> implements Record
{
	public static File TestDir = new File("d:/test");

	static {
		System.setProperty("mode", "master");
		System.setProperty("RecordDir", TestDir.getAbsolutePath());
	}


	public static Meta<Test> RecordMeta = Meta.Create(Test.class);

		// Site Info for Internal
	public static DefnStr String = RecordMeta.DefnStr(128);
	public static DefnDate Date = RecordMeta.DefnDate().precision(yyyyMMdd);//.label("Create Time");
	public static DefnBoolean Boolean = RecordMeta.DefnBoolean();
	public static DefnByte Byte = RecordMeta.DefnByte();
	public static DefnShort Short = RecordMeta.DefnShort();
	public static DefnInt Int = RecordMeta.DefnInt();
	public static DefnLong Long = RecordMeta.DefnLong();
	public static DefnBlob Blob = RecordMeta.DefnBlob().blobStore(BlobStore.class);
	public static DefnClob Clob = RecordMeta.DefnClob().clobStore(ClobStore.class);
	public static DefnEnum Enum  = RecordMeta.DefnEnum();
	public static DefnXML XML = RecordMeta.DefnXML(200).xmlStore(XMLStore.class);



	public String _string;
	public long _date;
	public int _int;
	public long _long;
	public Bytes _blob;
	public String _clob;
	public Type _enum;
	public boolean _boolean;
	public byte _byte;
	public short _short;
	public XML _xml;

	public static enum Type {
		A, B;
	}


	private Tx<T> _proxy;


	public Test ()
	{
		_proxy = RecordMeta.store().createTx(this);
	}

	public Tx<T> tx ()
	{
		return _proxy;
	}

	public Meta meta ()
	{
		return RecordMeta;
	}

	public static class UniqueIdFactory extends Meta.UniqueIdFactory
	{
		int i = 0;
		public UniqueIdFactory (Meta meta)
		{
			super(meta);
		}

		public String generate (Record record)
		{
			return java.lang.String.valueOf(i++);
		}
	}

	public static void main (String[] args) throws Exception
	{
		FileUtils.deleteDirectory(TestDir);



		Clock clock = Clock.timer().reset();
		clock.markAndCheckRunning(System.out);
		for (int i = 0; i < 10000; i++) {

			Test t = new Test();
			t._string = "String" + i;
			t._date = System.currentTimeMillis() + i;
			t._int = java.lang.Integer.MAX_VALUE - i;
			t._long = java.lang.Long.MAX_VALUE - i;
			t._blob = new Bytes(("Blob" + i).getBytes());
			t._clob = new String("Clob" + i);
			t._enum = Type.B;
			t._boolean = true;
			t._byte = (byte)(i % 127);
			t._short = (short)(java.lang.Short.MAX_VALUE - (short)i);
			t._xml = XML.getXMLStore().create("test.xml", "test");

			t.tx().insert();
		}
		clock.markAndCheckRunning(System.out);

		if (Test.RecordMeta._mode._isSlave) {
			//TestLog.replay();
		}
		else {
			for (int i = 0; i < 100; i++) {
				Test t = Test.RecordMeta.store().read(i);
				//System.out.println(t._clob);
			}
		}

		clock.markAndCheckRunning(System.out);
		//RecordLog.Instance.replay();

			// CachedRecordStore.CachePurger() has a background thread.
		System.exit(0);
	}

}

