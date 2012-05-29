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

public class NodePath
{
	public static final int NValue = Integer.MAX_VALUE;

	private String _path;
	private int _cursor;


	private NodePath ()
	{
	}

	NodePath (String path, int cursor)
	{
	    _cursor = cursor;
	    _path = path;
	}

		// this could potential have ABC[23]
	public String nodePath ()
	{
		return  _path.substring(0, _cursor);
	}

	public String nodeId()
	{
		String nodePath = nodePath();
		int i = nodePath.indexOf('[');
		return i > 0 ? nodePath.substring(0, i) : nodePath;
	}


	public String remainingPath ()
	{
		return _cursor + 1 <= _path.length() ? _path.substring(_cursor+1, _path.length()) : "";
	}

    public String path ()
    {
    	return _path;
    }

	public static NodePath getInstance (Object o)
	{
		if (o == null) {
			throw new IllegalArgumentException("parse() argument can not be null");
		}
		if (Util.nullOrEmptyOrBlankString(o.toString())) {
			throw new IllegalArgumentException("argument.toString() can not be null, empty, or blank");
		}
		if (o instanceof NodePath) {
			return (NodePath)o;
		}
		return parse(o.toString(), null);
	}

	private static NodePath parse (String s, NodePath recyclePath)
	{
        for (int i = 0, size = s.length(); i < size; i++) {
        	char c = s.charAt(i);
            if (c == Node.XPathSeparator) {
            	if (recyclePath == null) {
            		return new NodePath(s, i);
            	}
            	else {
            		recyclePath._path = s;
                	recyclePath._cursor = i;
            		return recyclePath;
            	}
            }
        }
        	// else
      	if (recyclePath == null) {
    		recyclePath = new NodePath(s, s.length());
    	}
      	else {
          	recyclePath._path = s;
          	recyclePath._cursor = s.length();
      	}

        return recyclePath;
	}


	public boolean isAttributeNotation ()
	{
       return _path.charAt(0) == '@';
	}

	public String pathAsAttribute ()
	{
		return isAttributeNotation() ? _path.substring(1, _path.length()) : "";
	}

	public boolean reachedEnd ()
	{
		return _cursor == _path.length();
	}


		//XXX deal with [n] vs abc[n] later
    public static boolean hasIndexNotation (Object id)
    {
    	if (id == null) {
    		return false;
    	}
    	char[] chars = id.toString().toCharArray();
    	return chars.length > 2 && chars[chars.length - 1]== ']';
    }

    	// either {[int]}  or {["nodeid"],[int]}
    public static Object[] getIndex (Object id)
    {
    	if (!hasIndexNotation(id)) {
    		return null;
    	}
        try {
        	String nodeName = null;
        	int idx = NValue;

        	String s = id.toString();
        	int bracketIdx = s.indexOf('[');
        	if (bracketIdx != 0) {
        		nodeName = s.substring(0, bracketIdx);
        	}

        	String strVal = s.substring(bracketIdx + 1, id.toString().length() -1);
           	int num = "n".equalsIgnoreCase(strVal)
           	    ? NValue
           	    : Integer.valueOf(strVal).intValue();

        	return nodeName == null ? new Object[] {num} : new Object[] {nodeName, num};

        }
        catch (Exception e) {
        	// regardless of NumberFormat Exception or any other exception
        	return null;
        }
    }



    public String parentPath ()
    {
    	if (reachedEnd()) {
    		return null;
    	}
    	return _path.substring(0, _path.lastIndexOf(Node.XPathSeparator));
    }

    public String lastPath ()
    {
    	if (reachedEnd()) {
    		return nodePath();
    	}
    	return _path.substring(_path.lastIndexOf(Node.XPathSeparator) + 1, _path.length());
    }


    public NodePath parent ()
    {
    	String parentPath = parentPath();
    	if (parentPath != null) {
    		return parse(parentPath, null);
    	}
    	return null;
    }

    public NodePath last ()
    {
    	String last = lastPath();
    	if (last != null) {
    		return parse(last, null);
    	}
    	return null;
    }

    public NodePath next ()
    {
    	return parse(remainingPath(), this);
    }

}
