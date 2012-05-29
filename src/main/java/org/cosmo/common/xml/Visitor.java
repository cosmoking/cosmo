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


public abstract class Visitor
{
		// this carries the Parent Node of the active node being processed;
	protected Node _activeParentNode;

	public Visitor ()
	{
	}

    public void beginNode (Node node, Iterator<Node> attributes, int depth) throws Exception
    {
    }

    public void beforeNextNode (Node node, int depth) throws Exception
    {
    }

    public void afterNextNode (Node node, int depth) throws Exception
    {
    }

    public void handleValue (Node node, int depth) throws Exception
    {
    }

    public void endNode (Node node, int depth) throws Exception
    {
    }



    public void breakVisit ()
    {
    	throw new BreakException();
    }


    	// An Exception signify to break the visit while process, i.e, found match, skip rest
    public static class BreakException extends RuntimeException
    {
    }
}
