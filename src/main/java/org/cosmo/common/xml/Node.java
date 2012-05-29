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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.cosmo.common.util.New;
import org.cosmo.common.xml.Visitor.BreakException;


import ariba.util.io.ObjectInputStream;

public class Node implements Serializable
{

    public static final char XPathSeparator = '.';
    public static final char XPathSeparatorMask = '^';
	public static final Node Null = new Node("NULL");

    protected Object _id;
    protected Object _value;


    Node ()
    {
    }

    public Node (Object id)
    {
    	if (id == null) {
    		throw new IllegalArgumentException("Constructor Node requires non-null ID.");
    	}
        this._id = id;
    }

    public Node (Object id, Object value)
    {
        this(id);
        this._value = value;
    }

    public Object id ()
    {
        return _id;
    }

    public boolean idIs (Object o)
    {
    	return o == _id &&  o != null && _id != null && o.equals(_id);
    }

    public Object rawValue ()
    {
    	return _value;
    }


		// add child to Node's children via specific xpath if path exists
    public Node add (Object strPath, Object v)
    {
    	NodePath path = NodePath.getInstance(strPath);

       	if (path.isAttributeNotation()) {
   	    	return addAttribute(path.pathAsAttribute(), v);
    	}
    	if (path.reachedEnd()) {
    		return addChild(path.nodePath(), v);
    	}
    	else {
        	Node node = get(path.parent());
        	return node == null ? null : node.add(path.last(), v);
    	}
    }

    public Node add (Object... args)
    {
    	if (args != null) {
    		if (args.length == 1) {
    			return add(args[0], null);
    		}
        	if (args.length > 1) {
        			// handles both odd and even args
        		for (int i = 0; i < args.length; i++) {
        			Object strPath = args[i++];
        			Object value = i < args.length ? args[i] : null;
        			if (i + 1 >= args.length) {
        				return add(strPath, value);
        			}
        			add(strPath, value);
        		}
        	}
    	}
    	return this;
    }


	  // add child to the end of this node's children
      // caution: add takes a snapshot of the content of the node which means copy by value NOT reference
    public Node add (Node node)
    {
        Node addedNode = addChild(node.id(), node.value());
        for (Node attribute : node.attributes()) {
            addedNode.addAttribute(attribute.id(), attribute.value());
        }
        node._value = addedNode._value;
        return this;
    }



    public Node set (Object value)
    {
        if (_value == null) {
            _value = value;
            return this;
        }

        if (_value instanceof AttributeNode) {
            ((AttributeNode)_value).nodeValue = value;
        }
        else {
            _value = value;
        }
        return this;
    }

    public Node setRawValue (Object value)
    {
    	_value = value;
    	return this;
    }

    	// sets the xpath with the value , if any part of the xpath does not exist it creates it
    public Node set (Object strPath, Object value)
    {
    	NodePath path = NodePath.getInstance(strPath);
        Node node = get(path.nodePath());

	        // if no such child and not attribute notation, create as we go
	    if (node == null && !path.isAttributeNotation()) {

	            // if no such child but however has a value reset to null
	        if (_value != null && ! (_value instanceof  Node)) {
	            _value = null;
	        }
	        node = addChild(path.nodePath().toString());
	    }

	    	// if attribute notation set it regardless of attribute present of not
    	if (path.isAttributeNotation()) {
   	    	addAttribute(path.pathAsAttribute(), value);
   	    	return this;
    	}

	          // otherwise end of the xpath set value
    	if (path.reachedEnd()) {
	        node.set(value);
	        return node;
	    }
    	return node.set(path.next(), value);
    }


    public Node get (Object strPath)
    {
    	NodePath path = NodePath.getInstance(strPath);
    	if (path.isAttributeNotation() && hasAttribute()) {
   	    	return ((AttributeNode)_value).get(path.pathAsAttribute());
    	}

    	Object value = value();
        Node node = null;
        if (value instanceof Nodes) {
        	node = ((Nodes)value).getSibling(path);
        }
        else if (value instanceof Node) {
	        if (((Node)value).hasSameId(path.nodeId()) || "[0]".equals(strPath)) {
                node = (Node)value;
            }
        }

        if (path.reachedEnd()) {
            return !(value instanceof Node) ?  null : node;
        }
        else {
        	return node == null? null : node.get(path.next());
        }
    }


    public Node delete (Object strPath)
    {
    	NodePath path = NodePath.getInstance(strPath);

       	if (path.isAttributeNotation()) {
   	    	return ((AttributeNode)_value).delete(path.pathAsAttribute());
    	}

    	if (path.reachedEnd()) {
	        Object value = value();
		    if (value instanceof Nodes) {
		        return ((Nodes)value).deleteSibling(this, path);
		    }
		    else if (value instanceof Node) {
		        if (((Node)value).hasSameId(path.nodeId()) || "[0]".equals(strPath)) {
                	if (this._value instanceof AttributeNode) {
                		((AttributeNode)this._value).nodeValue = null;
                	}
                	else {
                		this._value = null;
                	}
		            return (Node)value;
		        }
		    }
    	}
    	else {
        	Node node = get(path.parent());
        	return node == null ? null : node.delete(path.last());
    	}
    	return null;
    }


    	// add if not present else return current
	public Node dot (Object id)
	{
		if (id == null) {
			return null;
		}
		Node node = get(id);
		if (node == null) {
			return addChild(id.toString());
		}
		return node;
	}


    public Object value ()
    {
        return (hasAttribute()) ? ((AttributeNode)_value).nodeValue : _value;
    }


    public Object value (Object strPath)
    {
    	Node node = get(strPath);
    	return node != null ? node.value() : null;
    }

    public String stringValue (Object strPath)
    {
    	Object o = value(strPath);
    	return o == null ? null : o.toString();
    }

    public String stringValue (Object strPath, String defaultValue)
    {
    	Object o = value(strPath);
    	return o == null ? defaultValue : o.toString();
    }



    public boolean hasAttribute ()
    {
        return _value != null && _value instanceof AttributeNode;
    }


    public Iterable<Node> attributes ()
    {
        if (hasAttribute()) {
            Node node = ((AttributeNode)_value).attributeValue();
                // more than one attributes (store as "Nodes (att1) -> Nodes (att2) -> Node (att3)")
            return Nodes.siblings(node, true);
        }
        else {
            return Collections.EMPTY_LIST;
        }
    }


    public Node child ()
    {
        Object v = value();
        if (v != null && v instanceof Node) {
            return (Node)v;
        }
        return null;
    }


    public Iterable<Node> children ()
    {
        Object v = value();
        return (v != null && v instanceof Node)
            ? Nodes.siblings((Node)v, false)
            : Collections.EMPTY_LIST;
    }

    public Node[] childrenArray ()
    {
    	List<Node> children = new ArrayList();
    	for (Node node : children()) {
    		children.add(node);
    	}
    	return children.toArray(new Node[]{});
    }


    public Node[] sortedChildren (Comparator<Node> comparator)
    {
    	Node[] sortedChildren = childrenArray();
    	Arrays.sort(sortedChildren, comparator);
    	return sortedChildren;
    }


    public int size ()
    {
    	int size = 0;
    	for (Object o : children()) {
    		size++;
    	}
    	return size;
    }

/*
    public boolean addAll (Node node)
    {
    	if (node == null || node._id != _id) {
    		return false;
    	}
    	AddAll addAll = new AddAll(this);
        node.visit(addAll);
        return true;
    }

    public boolean removeAll (Node node)
    {
    	if (node == null || node._id != _id) {
    		return false;
    	}
    	RemoveAll removeAll = new RemoveAll(this);
        node.visit(removeAll);
        return true;
    }
*/

    public String toString ()
    {
        return toString(Print.DefaultNodeFormat);
    }


	public String toString (JSONFormat jsonFormat)
    {
		StringBuilder buf = new StringBuilder();

		if (_id != JSON.Anonymous) {
			buf.append(_id).append("=");
		}

			// json array
		if (value() instanceof List) {
			Print visitor = new JSONPrint(jsonFormat, buf);
			visitor.handleValue(this, 0);
		}
			// json object
		else {
			buf.append("{\n");
			 Print visitor = new JSONPrint(jsonFormat, buf);
	        ((Node)_value).visit(visitor);
	        buf.append("\n}");
		}
        return buf.toString();
    }


    public String toString (NodeFormat NodeFormat)
    {
        Print visitor = new Print(NodeFormat, new StringBuilder());
        visit(visitor);
        return visitor.toString();
    }

    public void visit (Visitor visitor)
    {
    	try {
    		visit(visitor, 0);
    	}
    	catch (BreakException e) {
    		throw e;
    	}
    	catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }

    public void visitChecked (Visitor visitor)
      throws Exception
    {
   		visit(visitor, 0);
    }

    protected void visit (Visitor visitor, int depth)
      throws Exception
    {
    	Object value = value();
   		visitor.beginNode(this, attributes().iterator(), depth);
        if (value != null) {

            // if the value is pointing to a node (ie child). recursively walk the child
            if (value instanceof Node) {
            	visitor.beforeNextNode(this, depth++);
                visitor._activeParentNode = this;
                ((Node)value).visit(visitor, depth);
                visitor.afterNextNode(this, --depth);
            }
            else {
                visitor.handleValue(this, depth);
            }
        }
        visitor.endNode(this, depth);
    }

    protected Node addChild (String id)
    {
        return addChild (id, null);
    }

    	// add child to the end of this node's children
    protected Node addChild (Object id, Object value)
    {
        if (_value == null) {
            _value = new Node(id, value);
            return (Node)_value;
        }
        else if (_value instanceof AttributeNode) {
            return ((AttributeNode)_value).addValueToNode(id, value);
        }
        else if (_value instanceof Node) {
            Nodes newChild = new Nodes (id, value);
            newChild.setSibling((Node)_value);
            _value = newChild;
            return newChild;
        }
        else {
            // if value is other objects - override
            _value = null;
            return addChild(id, value);
        }
    }

    	// Nodes can have siblings - this allows Nodes to override and return just Node
    public Node asNode ()
    {
    	return this;
    }


    protected boolean hasSameId (Object id)
    {
        return id != null && (_id == id || _id.equals(id));
    }


        // if attribute is added to this node then dynamically create an
        // attribute wrapper to store both the value and the attributes
        // saves space for nodes that does not have attributes
    protected Node addAttribute (Object attribute, Object value)
    {
            // if no value yet, create a new attributes wrapper
        if (_value == null) {
            AttributeNode attributes = new AttributeNode();
            attributes.addChild(attribute, value);
            _value = attributes;
        }
        else {
                // if an attributes wrapper already present, just add
                // new attribute
            if (_value instanceof AttributeNode) {
                ((AttributeNode)_value).set(attribute.toString(), value);
            }
                // else create an new wrapper, add new attribute, and
                // also store current node value in the wrapper
            else {
                AttributeNode attributes = new AttributeNode();
                attributes.nodeValue = _value;
                attributes.addChild(attribute, value);
                _value = attributes;
            }
        }
        return this;
    }


    public String getAttribute (Object id)
    {
    	if (hasAttribute()) {
    		Node node = ((AttributeNode)_value).get(id);
    		return node == null ? null : node.value().toString();
    	}
    	else {
    		return null;
    	}
    }


    public Node pop (Node node, String strPath)
    {
    	return node.get(strPath);
    }

    public Node pop (Node node)
    {
    	return node;
    }


    	// override by Nodes and AttributeNodes
    protected Node newCopyInstance ()
    {
    	return new Node();
    }

    	// copyProxy allows a chance to touch the copy
    public Node copy (Node.CopyProxy nodeCopyProxy, boolean isAttr)
    {
    	return nodeCopyProxy.copy(this, nodeCopyProxy, isAttr);
    }

    	// this is here so that Nodes can override
    public Node copyIncludeSibling (Node.CopyProxy nodeCopyProxy, boolean isAttr)
    {
    	return nodeCopyProxy.copy(this, nodeCopyProxy, isAttr);
    }

    	// shallow copy a node
    public Node copy ()
    {
    	return copy(Node.CopyProxy.Default,  false);
    }

    public static class CopyProxy
    {
    	public static final CopyProxy Default = new CopyProxy();

    	public Node copy (Node src, Node.CopyProxy nodeCopyProxy, boolean isAttr)
    	{
        	Node dst =  src.newCopyInstance();

        	if (src._value instanceof AttributeNode) {
        		dst._value = ((AttributeNode)src._value).copy(nodeCopyProxy, true);
        	}
        	else if (src._value instanceof Nodes) {
        		dst._value = ((Nodes)src._value).copyIncludeSibling(nodeCopyProxy, isAttr);
        	}
        	else if (src._value instanceof Node) {
        		dst._value = ((Node)src._value).copy(nodeCopyProxy, isAttr);
        	}

        		// XXX consider using ObjectCloner
        	dst._id = src._id;
       		dst._value = dst._value == null ? src._value : dst._value;

        	return dst;
        }
    }


    public Node deepCopy ()
    {
    	throw new RuntimeException("Not yet..");
    	// use ObjectCloner to deepCopy id and value
    }


    public List<Node> searchNode (Object id, Object... args)
    {
    	SearchVisitor search = new SearchVisitor(id, false, args);
    	this.visit(search);
    	return (List<Node>)search._matchResults;
    }

    public Node searchFirstNode (Object id, Object... args)
    {
    	SearchVisitor search = new SearchVisitor(id, true, args);
    	try {
    		this.visit(search);
    	}
    	catch (BreakException e) {
    	}
    	return (Node)search._matchResults;
    }

    public Node deleteFirstNode (Object id, Object... args)
    {
    	DeleteVisitor search = new DeleteVisitor(id, true, args);
    	try {
    		this.visit(search);
    	}
    	catch (BreakException e) {
    	}
    	return (Node)search._matchResults;
    }


}



/*
 * 	if id is null then match base on xpath
 *  if args is null then search base on id
 *  can return one or many
 */

class SearchVisitor extends Visitor
{
	public Object _id;
	public Object[] _searchArgs;
	public boolean _getOne;
	public Object _matchResults;

	public SearchVisitor (Object id, boolean getOne, Object... args)
	{
		_id = id;
		_searchArgs = args.length == 0 ? null : args;
		_getOne = getOne;
		if (!getOne) {
			_matchResults = new ArrayList();
		}
	}

    public void beginNode (Node node, Iterator<Node> attributes, int depth) throws Exception
    {
    		// null indicates skip id match
    	if (_id == null || _id == node._id || _id.equals(node._id)) {

    			// null search args indicates match by id only
    		if (_searchArgs == null) {
    			handle(node, null, null);
    		}

    			// compare predicates
    		else {
	    		for (int i = 0; i < _searchArgs.length; i++) {
	    			Object searchKey = _searchArgs[i];
	    			Object compareValue = _searchArgs[++i];
	    			Node nodeResult = node.get(searchKey);
	    			Object nodeValue = nodeResult == null ? null : nodeResult.value();
	    			if (compareValue == nodeValue || compareValue.equals(nodeValue)) {
	    				handle(node, searchKey, compareValue);
	    			}
	    		}
    		}
    	}
   	}


    void handle (Node node, Object searchKey, Object compareValue)
    {
    	if (_getOne) {
			_matchResults = node.asNode();
			breakVisit();
		}
		else {
			((List)_matchResults).add(node.asNode());
		}
    }
}


/*
 * 	if id is null then match base on xpath
 *  if args is null then search base on id
 *  can return one or many
 */

class DeleteVisitor extends SearchVisitor
{
	public DeleteVisitor (Object id, boolean getOne, Object... args)
	{
		super(id, getOne, args);
	}

    void handle (Node node, Object searchKey, Object compareValue)
    {
   		int i = 0;
   		for (Node child : _activeParentNode.children()) {
   			if (compareValue.equals(child.stringValue(searchKey))) {
   				_activeParentNode.delete(New.str("[", i, "]"));
   				break;
   			}
   			i++;
   		}
   		super.handle(node, searchKey, compareValue);
    }
}


/*
public Node add (Object strPath, Object id, Object v)
{
	NodePath path = NodePath.getInstance(strPath);

   	if (path.isAttributeNotation()) {
	    	return addAttribute(path.pathAsAttribute(), v);
	}

	if (path.reachedEnd()) {
        Object value = value();
	    if (value instanceof Nodes) {
	        return ((Nodes)value).addSibling(this, path, id, v);
	    }
	    else if (value instanceof Node) {
	        if (((Node)value).hasSameId(path.nodeId())) {
	            return this.addChild(id, v);
	        }
	    }
	}
	else {
    	Node node = get(path.parent());
    	return node == null ? null : node.add(path.last(), id, v);
	}
	return null;
}

*/






/*
public String dot (String path)
{
	Node node = get(path);
	return node != null
			? node.value() == null
				? null
				: node.value().toString()
		    : null;
}

public String at (String path)
{
	Object attribute = getAttribute(path);
	return attribute == null ? null : attribute.toString();
}



public Node pop (Node node)
{
	return node;
}

public Node pop (Node node, String strPath)
{
	return node.getX(strPath);
}

*/

/*

    Node addAttributes (Object... args) {

    	if (args != null) {
    		for (int i = 0; i < args.length; i += 2) {
    			if (i + 1 <= args.length) {
	    			Object attribute = args[i];
	    			Object value = args[i + 1];
	    			addAttribute(attribute, value);
    			}
    		}
    	}
		return this;
    }

 */


