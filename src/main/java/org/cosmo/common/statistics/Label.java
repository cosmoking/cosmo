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

public class Label
{
	public static final Label Empty = new Label();

	String[] _labels = new String[0];

	public static Label with (String... labels)
	{
		Label label = new Label();
		if (labels != null && labels.length > 0) {
			label._labels = new String[labels.length];
			for (int i = 0; i < label._labels.length; i++) {
				label._labels[i] = labels[i];
			}
		}
		return label;
	}

	Label ()
	{
		_labels = new String[]{};
	}

	public static Label trimToFit (Label label, int maxCounts, int maxLength)
	{
		if (label == null || maxCounts < 0 || maxLength < 0) {
			return null;
		}
		int size = label._labels.length >= maxCounts ? maxCounts : label._labels.length;
		String[] labels = new String[size];
		for (int i = 0; i < size; i++) {
			if (label._labels[i] != null) {
				String labelStr = label._labels[i].length() > maxLength
				? label._labels[i].substring(0, maxLength) : label._labels[i];
				labels[i] = labelStr;
			}
		}
		label._labels = labels;
		return label;
	}


	public String[] labels ()
	{
		return _labels;
	}

	public String toString ()
	{
		return Fmt.S("Labels [%s]", Key.printArray(_labels));
	}
}
