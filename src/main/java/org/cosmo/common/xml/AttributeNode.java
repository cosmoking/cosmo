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

import org.cosmo.common.xml.Node.CopyProxy;

public final class AttributeNode extends Node
{

	    // stores belonging node value
		// note: only the root of the Attributes is type
		// of AttributeNode, remaining attributes are type of Node
    Object nodeValue;

    AttributeNode ()
    {
        super();
    }

    AttributeNode (Object id)
    {
        super (id);
    }

    AttributeNode (Object id, Object value)
    {
        super (id, value);
    }


        // Adds the value to the "belonging" node
    Node addValueToNode (Object id, Object value)
    {
            // adds the node to the nodesOriginal value
        nodeValue = new Node().set(nodeValue).addChild(id, value);
        return (Node)nodeValue;
    }

    public Node attributeValue ()
    {
    	return (Node)_value;
    }


    @Override
    protected Node newCopyInstance ()
    {
    	return new AttributeNode();
    }

    @Override
    public Node copy (Node.CopyProxy nodeCopyProxy, boolean isAttr)
    {
    	return AttributeNode.CopyProxy.Default.copy(this, nodeCopyProxy, isAttr);
    }


    public static class CopyProxy extends Node.CopyProxy
    {
    	public static final CopyProxy Default = new CopyProxy();

    	public Node copy (Node source, Node.CopyProxy nodeCopyProxy, boolean isAttr)
    	{
        	AttributeNode dst = (AttributeNode)super.copy(source, nodeCopyProxy, isAttr);
        	AttributeNode src = (AttributeNode)source;

        	if (src.nodeValue instanceof Nodes) {
        		dst.nodeValue = ((Nodes)src.nodeValue).copyIncludeSibling(nodeCopyProxy, false);
        	}
        	else if (src.nodeValue instanceof Node) {
        		dst.nodeValue = ((Node)src.nodeValue).copy(nodeCopyProxy, false);
        	}
        	else {
        		dst.nodeValue = src.nodeValue;
        	}
        	return dst;
        }
    }

}

