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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.cosmo.common.xml.AttributeNode.CopyProxy;


public class Nodes extends Node
{

    Node sibling;

    Nodes ()
    {
    }

    public Nodes (Object id, Object value)
    {
        super(id, value);
    }

    void setSibling (Node sibling)
    {
        this.sibling = sibling;
    }

    public Node getSibling ()
    {
        return sibling;
    }


    public Node getSibling (NodePath path)
    {
        // if use get by "[0]" or "ABC[0]" number can be 0...n.  , ie   ABC.[2].EFG, or  ABC[n].A.C

        return NodePath.hasIndexNotation(path.nodePath())
            ? getSibling(NodePath.getIndex(path.nodePath()))
            : getSibling(path.nodePath());

    }

    public Node getSibling (Object id)
    {
        if (hasSameId(id)) {
            return this;
        }
        else if (sibling != null) {

            if (sibling instanceof Nodes) {
                    // calls the next sibling
                return ((Nodes)sibling).getSibling(id);
            }
            else {
                    // this is end of it
                return (sibling.hasSameId(id)) ? sibling: null;
            }
        }
        return null;
    }


	// either {[int]}  or {["nodeid"],[int]}
    public Node getSibling (Object[] pathIds)
    {
        Iterator list = new LIFONodeIterator(this);
        if (pathIds.length == 1) {
            Node node = null;
        	int idx = (Integer)pathIds[0];
	        for (int i = -1; i < idx; i++) {
	            if (list.hasNext()) {
	                node = (Node)list.next();
	            }
	            else {
	            		// Treat MAX_VALUE as last element of the sibling or, [n]
	            	if (idx == Integer.MAX_VALUE) {
	            		return node;
	            	}
	                return null;
	            }
	        }
	        return node;
        }
        else {
           	int count = 0;
        	String nodeName = pathIds[0].toString();
        	int idx = (Integer)pathIds[1];
        	Node node;
        	while (list.hasNext()) {
        		node = (Node)list.next();
        		if (node.hasSameId(nodeName)) {
        			if (count == idx) {
        				return node;
        			}
        			count++;
        		}
        	}
        	return null;
        }

    }

/*
    public Node addSibling (Node parent, NodePath path, Object id, Object v)
    {
        return NodePath.hasIndexNotation(path.nodeId())
    	? addSibling(parent, NodePath.getIndexNotation(path.nodeId()), new Nodes(id, v))
        : addSibling(parent, path.nodeId(), new Nodes(id, v));
    }


    public Node addSibling (Node parent, int idx, Nodes newNode)
    {
    	LIFONodeIterator list = new LIFONodeIterator(parent, this);
        Node node = null;
        for (int i = -1; i < idx; i++) {
            if (list.hasNext()) {
                node = (Node)list.next();
            }
            else {
                return null;
            }
        }
        list.insertCurrent(newNode);
        return node;
    }


    public Node addSibling (Node parent, Object id, Nodes newNode)
    {
    	LIFONodeIterator list =  new LIFONodeIterator(parent, this);
        while (list.hasNext()) {
            Node node = (Node)list.next();
            if (node.hasSameId(id)) {
                node.addChild(newNode);
                return node;
            }
        }
        return null;
    }

*/
    public Node deleteSibling (Node parent, NodePath path)
    {
        return NodePath.hasIndexNotation(path.nodePath())
        	? deleteSibling(parent, NodePath.getIndex(path.nodePath()))
            : deleteSibling(parent, path.nodePath());

    }



    	// refactor to share same code with insert/add
    	// either {[int]}  or {["nodeid"],[int]}

    public Node deleteSibling (Node parent, Object[] pathIds)
    {
    	LIFONodeIterator list = new LIFONodeIterator(parent, this);
        if (pathIds.length == 1) {
	        Node node = null;
        	int idx = (Integer)pathIds[0];
	        for (int i = -1; i < idx; i++) {
	            if (list.hasNext()) {
	                node = (Node)list.next();
	            }
	            else {
	            		//  At end of list and idx is [n]
	                if (idx == NodePath.NValue) {
	                	break;
	                }
	                else {
	                	return null;
	                }
	            }
	        }
	        list.removeCurrent();
	        return node;
        }
        else {
        	int count = 0;
        	String nodeName = pathIds[0].toString();
        	int idx = (Integer)pathIds[1];
        	Node node;
        	while (list.hasNext()) {
        		node = (Node)list.next();
        		if (node.hasSameId(nodeName)) {
        			if (count == idx) {
        				list.removeCurrent();
        				return node;
        			}
        			count++;
        		}
        	}
        	return null;
        }

    }




    public Node deleteSibling (Node parent, Object id)
    {
    	LIFONodeIterator list =  new LIFONodeIterator(parent, this);
        while (list.hasNext()) {
            Node node = (Node)list.next();
            if (node.hasSameId(id)) {
                list.removeCurrent();
                return node;
            }
        }
        return null;
    }

    @Override
    public Node asNode ()
    {
    	return new Node(this._id, this._value);
    }

    @Override
    public String toString (NodeFormat NodeFormat)
    {
        Print visitor = new Print(NodeFormat, new StringBuilder());
    	if (NodeFormat.printNodesSiblings) {
	        visit(visitor);
    	}
    	else {
	        super.visit(visitor);
    	}
        return visitor.toString();
    }

    @Override
    protected Node newCopyInstance ()
    {
    	return new Nodes();
    }

    @Override
    public Node copy (Node.CopyProxy nodeCopyProxy, boolean isAttr)
    {
    		// take off sibling otherwise it just makes redundant copies
    	Node node = asNode();
    	return nodeCopyProxy.copy(node, nodeCopyProxy, isAttr);

    }


	@Override
    public Node copyIncludeSibling (Node.CopyProxy nodeCopyProxy, boolean isAttr)
    {
    	return Nodes.CopyProxy.Default.copy(this, nodeCopyProxy, isAttr);
    }


    public static class CopyProxy extends Node.CopyProxy
    {
    	public static final CopyProxy Default = new CopyProxy();

    	public Node copy (Node source, Node.CopyProxy nodeCopyProxy, boolean isAttr)
    	{
        	Nodes dst = (Nodes)nodeCopyProxy.copy(source, nodeCopyProxy, isAttr);
        	Nodes src = (Nodes)source;

        	if (src.sibling instanceof Nodes) {
        		dst.sibling = ((Nodes)src.sibling).copyIncludeSibling(nodeCopyProxy, isAttr);
        	}
        	else if (src.sibling instanceof Node) {
        		dst.sibling = ((Node)src.sibling).copy(nodeCopyProxy, isAttr);
        	}
        	else {
        		dst.sibling = src.sibling;
        	}
        	return dst;
        }
    }


    @Override
    public void visit (Visitor visitor)
    {
    	super.visit(visitor);
    }


    	// XXX would get StackOverFlow problem  - may need to convert this to For loop
    	// or increase Thread Stack size -Xss
    @Override
    protected void visit (Visitor visitor, int depth)
      throws Exception
    {
                // recursively call all childern's walk in this link list
            if (sibling != null) {

            	Node currentParent = visitor._activeParentNode;
                ((Node)sibling).visit(visitor,  depth);
                visitor._activeParentNode = currentParent;
                    // walk the content of this node
                super.visit(visitor,  depth);
            }
            	// sibling can be null if it got removed during delete()
            else {
                super.visit(visitor,  depth);
            }

    }


    public static Iterable<Node> siblings (Node node, boolean fifo)
    {
        if (node instanceof Nodes) {
        	LIFONodeIterator siblings = new LIFONodeIterator(node);
            if (fifo) {
               	List siblingsList = new ArrayList();
                while (siblings.hasNext()) {
                	siblingsList.add(siblings.next());
                }
                return siblingsList;
            }
            else {
            	return siblings;
            }
        }
            // just one attribute (stores as "Context (attr1)")
        else if (node instanceof Node) {
                // lazy impl by creating a list
            List l = new ArrayList();
            l.add(node);
            return l;
        }
        else {
                // lazy impl by creating a list
            return Collections.EMPTY_LIST;
        }
    }


/*
    public static class FIFONodeIterator<E> implements Iterator, Iterable
    {
        Stack stack = new Stack();

        FIFONodeIterator (Node thisInstance)
        {
            LIFONodeIterator<Node> nodes = new LIFONodeIterator<Node>(thisInstance);
            while (nodes.hasNext()) {
                stack.push(nodes.next());
            }
        }

        public boolean hasNext ()
        {
            return (stack.size() > 0 && stack.peek() != null);
        }

        public Object next ()
        {
            if (hasNext()) {
                return stack.pop();
            }
            return null;
        }

        public void remove()
        {
        }

        public Iterator iterator()
        {
        	return this;
        }
    }
*/

    public static class LIFONodeIterator<E> implements Iterator, Iterable
    {
        Node next;
        Node current;
        Node previous;
        Node parent;

        LIFONodeIterator (Node parent, Node thisInstance)
        {
            this.next = thisInstance;
            this.parent = parent;
    		if (parent.value() !=  thisInstance) {
    			throw new IllegalArgumentException("Parent node invalid.");
    		}
        }


        LIFONodeIterator (Node thisInstance)
        {
            next = thisInstance;
        }


        public boolean hasNext ()
        {
            return next != null;
        }

        public Object next ()
        {
            if (hasNext()) {
                previous = current;
                current = next;
                next = (next instanceof Nodes) ? ((Nodes)next).sibling : null;
                return current;
            }
            return null;
        }

        public void remove ()
        {
            // not supported
        }


        Node insertCurrent (Nodes node)
        {
            if (current instanceof Nodes) {
	            node.sibling = ((Nodes)current).sibling;
	            ((Nodes)current).sibling = node;

            }
            else {
            	Nodes newCurrent = new Nodes(current._id, current._value);
            	newCurrent.sibling = node;
            	((Nodes)previous).sibling = newCurrent;
            }
	        return previous;
        }

        Node removeCurrent()
        {
            if (parent != null) {
                    // first node
                if (parent.value().equals(current)) {
                    if (current instanceof Nodes) {
                    	if (parent._value instanceof AttributeNode) {
                    		((AttributeNode)parent._value).nodeValue = ((Nodes)current).sibling;
                    	}
                    	else {
                    		parent._value = ((Nodes)current).sibling;
                    	}
                    }
                }
                else {
                    if (current instanceof Nodes) {
                        ((Nodes)previous).sibling = ((Nodes)current).sibling;
                    }
                        // last node
                    else {
                            // XXX here we are setting the sibling to null to
                            // mark this as a last node. Note. the last element in
                            // the lists no longer contains type of "Node" only but can
                            // contain "Nodes" with sibling set to null
                        ((Nodes)previous).sibling = null;
                    }
                }
            }
            return previous;
        }

        public Iterator iterator ()
        {
        	return this;
        }
    }
}

