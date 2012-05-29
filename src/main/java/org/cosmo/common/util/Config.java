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

import org.cosmo.common.xml.Node;

public class Config
{

	private static final Node XML = configNode();

	private static Node configNode ()
	{
		/*
		Node root = new
		Node("root"); root
		    .add("1a", "somevalue").pop(root)
			.add("1b")
			    .add("2c")
					.add("3d").pop(root, "1b.2c")
					.add("3e");

		return root;
		*/
		Node root = new
		Node("root"); root
		    .add("1a", "somevalue").pop(root)
			.add("1b")
			    .add("2c")
					.add("3d").pop(root, "1b.2c")
					.add("3e");

		return root;
	}


	public static String value (String path)
	{
		return XML.stringValue(path);
	}


	public static void main (String[] args)
	{
		System.out.println(Config.XML);
		System.out.println(Config.value("1a"));
	}

}


class configuration
{

	public static final Integer noOfSeconds = 1;
	public static final String directoryName = "d:/sdfsdf";

	public static class Nodes
	{
		public static final Object[] values = new Object[] {
			"abc",
			1
		};
	}




	public static void main (String[] args) {




	}



}


