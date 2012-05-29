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
package org.cosmo.common.util;

import java.util.ArrayList;
import java.util.List;

import cern.colt.list.IntArrayList;


public class FmtBytes extends Bytes
{
	IntArrayList _variablesMarkers;

	public FmtBytes ()
	{
		super();
	}

    public FmtBytes (int initializeSize)
    {
    	super(initializeSize);
    }


	public void write (byte[] b, boolean markVariable)
	{
    	super.write(b);
    	if (markVariable) {
    		mark();
    	}
	}

	public void mark ()
	{
    	if (_variablesMarkers == null) {
    		_variablesMarkers = new IntArrayList();
		}
    	_variablesMarkers.add(_count);
	}

	public String fmtStr (Object... args)
	{
		return new String(fmtBytes(args)._bytes);
	}


	public Bytes fmtBytes (Object... args)
	{
			// no variables, no need to format return string as is
		if (_variablesMarkers == null) {
			return this;
		}

			// calculate the actual size, if it's type of Object[] reuse the array to store raw bytes of the args
		Object[] variables = (args.getClass() == Constants.ObjectArrayClass) ? args : new Object[args.length];
		int totalByteSize = _count;
		for (int i = 0;  i < variables.length; i++) {
			variables[i] = Util.UTF8(args[i]);
			totalByteSize +=  ((byte[])variables[i]).length;
		}

		Bytes bytes = new Bytes(totalByteSize);
		int cursor = 0;
		int pos = 0;

			// for each vars defined  - note it's possible the passed in args is less than the number of defined vars
		for (int i = 0, size = _variablesMarkers.size(); i < size; i++) {
			pos = _variablesMarkers.get(i);
			bytes.write(_bytes, cursor, pos - cursor);
				// has arg for defined variable
			if (variables.length > i) {
				byte[] b = (byte[])variables[i];
				if (b.length == 0) {
					handleEmptyByte(b, bytes);
				}
				else {
					bytes.write(b);
				}
			}
			else {
				handleEmptyByte(Constants.NullByteMarker, bytes);
			}
			cursor = pos;
		}
		bytes.write(_bytes, cursor, _count - cursor);
		return bytes;
	}

		// by default write nothing - marker will either be Constants.EmptyByteMarker or Constants.NullByteMarker
	protected void handleEmptyByte (byte[] marker, Bytes dest)
	{
		return;
	}


	public static void main (String [] args) throws Exception
	{
		FmtBytes f = new FmtBytes();
		f.write(new byte[]{1,2}, true);
		f.write(new byte[]{5,6}, true);
		f.write(new byte[]{9}, true);
		Bytes format = f.fmtBytes(new byte[]{3,4}, new byte[]{7,8});
		System.out.println(new String(format._bytes));

	}

}
