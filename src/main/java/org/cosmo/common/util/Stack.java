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

import java.util.Iterator;
import java.util.NoSuchElementException;


public class Stack<T> implements Iterable<T>, Iterator<T>
{
	Node<T> _focus;

	public void push (T value)
	{
		Node node = new Node(value);
		if (_focus == null) {
			_focus = node;
		}
		else {
			node._next = _focus;
			_focus = node;
		}
	}

	public T pop ()
	{
		if (_focus == null) {
			return null;
		}
		T value = _focus._value;
		_focus = _focus._next == null
			? null
			: _focus._next;
		return value;
	}

	public T peek ()
	{
		return _focus == null ? null : _focus._value;
	}


    public boolean hasNext()
    {
    	return _focus != null;
    }

    public T next() {
    	return pop();
    }

    public void remove()
    {
    	pop();
    }

    public Iterator<T> iterator()
    {
    	return this;
    }

	public static void main (String[] args) throws Exception
	{
		Stack<String> stack = new Stack();
		stack.push("a");
		stack.push("b");
		stack.push("c");
		stack.push("d");

		for (String s : stack) {
			System.out.println(s);
		}

		//System.out.println(stack.pop());
		//System.out.println(stack.pop());
		//System.out.println(stack.pop());
		//System.out.println(stack.pop());
		//System.out.println(stack.pop());
	}

}

class Node<T>
{
	T _value;
	Node _next;

	public Node (T value)
	{
		_value = value;
	}
}
