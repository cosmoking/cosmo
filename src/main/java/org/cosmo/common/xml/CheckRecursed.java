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
package org.cosmo.common.xml;

import java.util.Iterator;
import java.util.Stack;

import ariba.util.core.Fmt;



public class CheckRecursed extends Visitor
{
    public NodeParentList _parentList;
    public Node _recursedNode;


	public CheckRecursed ()
	{
		_parentList = new NodeParentList();
	}

	public void beginNode (Node node, Iterator<Node> attributes, int depth)
    {
		boolean result = _parentList.addIfNotAnAncestor(node, _activeParentNode);
		if (!result) {

			Stack<Node> stack = new Stack();
			//stack.push(node);
			//stack.push(_activeParentNode);

			//System.out.println(node.stringValue("@name"));


				// recreate the xpath

			//Stack<Node> stack = new Stack();

			for (Node parent = _activeParentNode; parent != null;)
			{
				stack.push(parent);
				//System.out.println(parent.stringValue("@name"));
				parent = _parentList.getParent(parent);
			}
			//System.out.println(_activeParentNode.stringValue("@name"));
			//
			throw new CheckRecursed.Exception (stack);
		}
    }

	public static void isRecursed (Node node)
	{
		CheckRecursed checker = new CheckRecursed();
		node.visit(checker);
	}

	public static class Exception extends RuntimeException
	{
		public Stack<Node> _xpath;

		public Exception(Stack<Node> xpath)
		{
			super();
			_xpath = xpath;
		}

		public String xpathAsString (String xpathName) // ie "@name" prints every node's name as a -> b -> c
		{

			Stack<Node> xpath = getXpathStack();
			StringBuffer buf = new StringBuffer();
			Node aNode = xpath.pop();
			String firstNodeName = aNode.stringValue(xpathName);
			buf.append(firstNodeName);
			while(!xpath.empty()) {
				buf.append(" --> ");
				aNode = xpath.pop();
				buf.append(aNode.stringValue(xpathName));
			}
			return buf.toString() + " --> " + firstNodeName;
		}

		public Stack<Node> getXpathStack ()
		{
			return (Stack<Node>)_xpath.clone();
		}
	}
}

/*  Do this before add or something


					try {
						CheckRecursed.isRecursed(entry);
					}
					catch (CheckRecursed.Exception e) {
						_errorMessage =
						Fmt.S("Unable to Tag [%s] to [%s] because such link already exist: %s",
							e.getXpathStack().peek().stringValue("@name"), aTagName, e.xpathAsString("@name"));
						return null;
					}



 */

