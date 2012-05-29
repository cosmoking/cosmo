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

import java.util.IdentityHashMap;
import java.util.TreeSet;


public class NodeParentList
{
	public static int MaxTreeDepth = 1000;  // max possible tree length

		// node, and it's parent
	IdentityHashMap<Node, Node> _map = new IdentityHashMap();


		// adds a entry of node and his parent ONLY IF the parent is not an
	    // ancestor of node
		//  ie  A -> B -> C -> A    at point of process (node) A we want to see if A already has process by checking
		// (parent) C's parent recusively
	public boolean addIfNotAnAncestor (Node node /* A */ , Node parent /* C */)
	{
			// keep on checking's parent's parent to see if  node is in there already
		if (isNodeAnAncestor(parent, node)) {
			return false;
		}
		_map.put(node, parent);
		return true;
	}


	public boolean isNodeAnAncestor (Node node, Node possibleAncestor)
	{
		return isNodeAnAncestor (node, possibleAncestor, 0);
	}


	boolean isNodeAnAncestor (Node node, Node possibleAncestor, int startCount)
	{
		if (startCount > MaxTreeDepth) {
			throw new RuntimeException ("Exceed Maximum Node Parent List of " + MaxTreeDepth);
		}

		if (node == null || possibleAncestor == null) {
			return false;
		}

		Node parent = _map.get(node);
		if (parent == null) {
			return false;
		}
		else {
				// check if node's parent is equal to possibleANcestor
			if (parent == possibleAncestor) {
					return true;
			}
				// if not check node's parent for next call
			else {
				return isNodeAnAncestor(parent, possibleAncestor, ++startCount);
			}
		}
	}

	public boolean isNodeInList (Node node)
	{
		return _map.get(node) != null;
	}

	public Node getParent(Node node)
	{
		return _map.get(node);
	}

	public static void main (String[] args)
	{

		NodeParentList checker = new NodeParentList();

		Node A1 = new Node("A1");
		A1.set("A2", null);
		A1.set("A3", null);
		A1.set("A2.A4", null);
		A1.set("A2.A5", null);
		A1.set("A2.A4.A1", null);
		A1.set("A2.A4.A3", null);
		System.out.println(A1);




		Node A2 = new Node("A2");
		Node A3 = new Node("A3");
		Node A4 = new Node("A4");
		Node A5 = new Node("A5");


		A1.add(A2);
		A1.add(A3);
	    A2.add(A4);
	    A2.add(A5);
	    A4.add(A1);
	    A4.add(A3);


	    boolean testA = checker.isNodeAnAncestor(A4, A1);
	    boolean testb = checker.isNodeAnAncestor(A4, A3);




	}








}
