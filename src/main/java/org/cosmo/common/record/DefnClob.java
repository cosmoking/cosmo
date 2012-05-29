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
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;

import org.cosmo.common.file.IncompleteWriteException;


public class DefnClob <T extends Record> extends DefnStr
{
	public static final Class[] TypeClasses = new Class[] {CharSequence.class, String.class};

	Class<? extends ClobStore> _clobStoreClass;
	ClobStore _clobStore;
	boolean _isFieldDeclaredAsString;

	public DefnClob (Meta declaringMeta)
	{
		super(declaringMeta, 0, false);
	}


	public DefnClob clobStore (Class<? extends ClobStore> clobStore)
	{
		_clobStoreClass = clobStore;
		return this;
	}

	public Class[] typeClasses ()
	{
			// CharSequnce allows LazyString which is Memory efficient.
		return TypeClasses;
	}


	public int size ()
	{
		return DefnLong.Size;
	}

	@Override
	public boolean hasAdditionalSizeInfo ()
	{
		return true;
	}


	@Override
	public void lazyFieldInit ()
	{
		super.lazyFieldInit();
		try {
			Constructor constructor = _clobStoreClass.getConstructor(new Class[] {Defn.class});
			_clobStore = (ClobStore)constructor.newInstance(new Object[] {this});
			_clobStore._channel.checkIncompleteWrite(new File(_clobStore._channel.filename()));
			_isFieldDeclaredAsString = _field.getType() == String.class;
		}
		catch (IncompleteWriteException e) {
			System.err.println(e.getMessage());
		}

		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	@Override
	public Object readImpl (ByteBuffer dataIO, boolean directConvertFromBytes)
	  throws IOException
	{
		if (directConvertFromBytes) {
			return org.cosmo.common.util.Util.string(dataIO.array());
		}

		long lobPos = dataIO.getLong();
		if (lobPos >= 0) {
				// _clobStore could return LazyString as CharSequence
				// which only works for CharSequence fields
			return _isFieldDeclaredAsString
				? _clobStore.read(lobPos).toString()
				: _clobStore.read(lobPos);
		}
		return null;
	}

	@Override
	public byte[] writeImpl (byte[] dst, Object src, long i)
	  throws IOException
	{
		CharSequence clob = (CharSequence)src;
		byte[] srcBytes = org.cosmo.common.util.Util.UTF8(clob);
		if (this._declaringMeta._mode._isMaster) {
			long lobPos = (clob != null) ? _clobStore.store(srcBytes) : -1;
	    	dst[1] = (byte)(lobPos >>> 56);
	    	dst[2] = (byte)(lobPos >>> 48);
	    	dst[3] = (byte)(lobPos >>> 40);
	    	dst[4] = (byte)(lobPos >>> 32);
	    	dst[5] = (byte)(lobPos >>> 24);
	    	dst[6] = (byte)(lobPos >>> 16);
	    	dst[7] = (byte)(lobPos >>>  8);
	    	dst[8] = (byte)(lobPos >>>  0);
		}
		return srcBytes;
	}




}
