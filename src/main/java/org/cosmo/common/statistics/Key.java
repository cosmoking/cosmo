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
package org.cosmo.common.statistics;

import ariba.util.core.Fmt;

public class Key
{
	public static final Key Invalid = new Key(null);

	Object[] _keys;

	Key (Object[] keys)
	{
		_keys =  keys;
	}

	public Object[] keys ()
	{
		return _keys;
	}

	public boolean hasLabel ()
	{
		return false;
	}

	public Label label ()
	{
		return Label.Empty;
	}

	public boolean contains (Key selectKey)
	{
		int thisKeySize = keys().length;
		int selectKeySize = selectKey.keys().length;
		for (int i = 0; i < selectKeySize && i < thisKeySize; i++ ) {
			if (!selectKey.keys()[i].equals(keys()[i])) {
				return false;
			}
		}
		return true;
	}

	public String toString ()
	{
		return Fmt.S("Key [%s] Hash [%s]", printArray(_keys), hashCode());
	}


	public int hashCode ()
	{
		return arrayHashCode(this._keys);
	}

	public boolean equals (Object o)
	{
		if (o != null && o instanceof Key) {
			return arrayEquals(_keys, ((Key)o)._keys);
		}
		return false;
	}

	public static String printArray (Object[] args)
	{
		StringBuilder sb = new StringBuilder();
		String sign = "";
		for (Object o: args) {
			sb.append(sign);
			sb.append(o);
			sign = ",";
		}
		return sb.toString();
	}


	public static int arrayHashCode (Object[] keys)
	{
		if (keys == null) {
			return 0;
		}
		StringBuilder sb = new StringBuilder();
		for (Object key : keys) {
			if (key != null) {
				sb.append(String.valueOf(key));
			}
		}
		return sb.toString().hashCode();
	}


	public static boolean arrayEquals (Object[] X, Object[] Y)
	{
		if (X == null || Y == null) {
			return false;
		}
		Object[] A = X.length > Y.length ? X : Y;
		Object[] B = A == X ? Y : X;
		for (int i = 0; i < B.length; i++) {
			if (B[i] != null && !B[i].equals(A[i])) {
				return false;
			}
			if (A[i] != null && !A[i].equals(B[i])) {
				return false;
			}
		}
		for (int i = B.length; i < A.length; i++) {
			if (A[i] != null) {
				return false;
			}
		}
		return true;
	}
}



class KeyWithLabel extends Key
{
	private Label _label;

	KeyWithLabel (Object[] keys, Label label)
	{
		super(keys);
		_label = label;
	}

	public Label label ()
	{
		return _label;
	}

	public boolean hasLabel ()
	{
		return _label != null;
	}

	public String toString ()
	{
		return Fmt.S("Key [%s] Label [%s] Hash [%s]", printArray(_keys), _label, hashCode());
	}
}


