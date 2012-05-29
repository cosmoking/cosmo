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

abstract public class KeyResolver
{

	public static final Object ExplicitNull = new Object()
	{
		public String toString ()
		{
			return "ExplicitNull";
		}
	};

	Category _category;

	public KeyResolver (Category category)
	{
		_category = category;
	}


	public Key getKey (Object... objects)
	{
		if (objects == null || objects.length == 0 ||  _category == null
			|| objects.length > _category._meta._keySize) {
			return Key.Invalid;
		}
		Object[] keys = new Object[objects.length];
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] != null && objects[i] instanceof Label) {
				return Key.Invalid;
			}
			Object key = convertToKeyType(i, objects[i]);
			if (key == null) {
				return Key.Invalid;
			}
			if (i == 0 && key == ExplicitNull) {
				return Key.Invalid;
			}
			keys[i] = key == ExplicitNull ? null : key;
		}
		if (keys[0] == null) {
			return Key.Invalid;
		}
		return new Key(keys);
	}

	public Key getKey (Label label, Object... objects)
	{
		Key key = getKey(objects);
		if (key == Key.Invalid) {
			return key;
		}
		if (label != null && label != Label.Empty) {
			return new KeyWithLabel(key._keys, Label.trimToFit(
			    label, _category._meta._labelSize, _category._meta._labelMaxLength));
		}
		return key;
	}

		// return Key.ExplicitNull is consider Explicit NULL value
	abstract public Object convertToKeyType (int idx, Object o);

	abstract public Class getKeyType();
}
