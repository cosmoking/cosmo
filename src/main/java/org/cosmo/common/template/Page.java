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
package org.cosmo.common.template;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cosmo.common.util.New;
import org.cosmo.common.util.Util;
import org.json.JSONObject;

public class Page
{

	private static final HashMap<String, Page> PagesByName = new HashMap();
	private static final List<Page> PagesById = new ArrayList();

		// Binding handling
	public static final String BindingMarker = "!BM!";
	public static final byte[] BindingMarkerBytes = Util.UTF8(BindingMarker);
	public static final byte[] BindingMarkerBytesQuoted = Util.UTF8(JSONObject.quote(BindingMarker));

		// segment and binding
	byte[][] _segmentArray;
	Binding[] _bindingArray;
 	Map<String, Binding> _bindingMap;


	public final String _name;
	public final File _src;
	public final Options _options;
	public final Class<? extends BindingSrc> _bindingSrc;
	private int _arrayId;


	public static Page byId (int arrayId)
	{
		return Parser.DEBUG ?  PagesById.get(arrayId).reParse() : PagesById.get(arrayId) ;
	}

	public static Page byName (Class clazz)
	{
		return byName(clazz.getName());
	}
	
	public static Page byName (String name)
	{
		if (Parser.DEBUG) {
			Page page = PagesByName.get(name);
			if (page == null) {
				throw new IllegalArgumentException(New.str("Unable to find page named [", name, "]"));
			}
			return page.reParse();
		}

		return PagesByName.get(name);
	}

	Page (String name, Class<? extends BindingSrc> bindingSrc, File src,  Options options, boolean reParse)
	{
		_name = name;
		_src = src;
		_options = options;
		_bindingSrc = bindingSrc;
		_bindingMap = new HashMap();

		synchronized (Page.class) {

			if (!reParse) {

				if (PagesByName.containsKey(_name)) {
					throw new IllegalArgumentException(New.str("Page [", _name, "] already exists"));
				}
				PagesByName.put(_name, this);

				PagesById.add(this);
			}
			else {
				Page oldPage = PagesByName.put(_name, this);
				PagesById.set(oldPage._arrayId, this);
			}
		}
	}


		// for each binding in the page, pass it to binding handler to resolve to a value and store
		// in ContentContainer. Also pushed rest page contents into segments. ie:
		//
		// page:  abc ^binding1^ hijkl ^binding2()^
		// bindinghandler: resolves binding1 to efg and binding2() to mno
		//
		// after appendBindingValues the ContentContainer would have following to list:
		// bindings:  [efg,mno]
		// segments:  [abc,^BM!^,hijkl,^BM!^]
		//
		// when call writeTo() the result would be below
		// result: abc efg hijkl mno
		//
		// note: container can be passed in recursively for appending nested page
	void append (int segmentStartIdx, int segmentEndIdx, int bindingStartIdx, Object context, Content container, BindingSrc bindingSrc)
	  throws Exception
	{

		for (int segIdx = segmentStartIdx, bindingIdx = bindingStartIdx; segIdx < segmentEndIdx; segIdx++) {
		    byte[] segment = _segmentArray[segIdx];
			if (segment == BindingMarkerBytes || segment == BindingMarkerBytesQuoted) {
				Binding binding = _bindingArray[bindingIdx];
				if (binding instanceof BindBinding) {
					BindBinding bindBinding = (BindBinding)binding;
					segIdx = bindBinding._segmentEndIdx;
					bindingIdx = bindBinding._bindingEndIdx;
				}
				else if (binding instanceof CaseBinding) {
					CaseBinding caseBinding = (CaseBinding)binding;
					if (caseBinding.isEnclosedExpr()) {
						segIdx = caseBinding._segmentEndIdx;
						bindingIdx = caseBinding._bindingEndIdx;
					}
					bindingSrc.applyValue(binding, this, container, context, _options);
				}
				else {
					bindingSrc.applyValue(binding, this, container, context, _options);
				}
				bindingIdx++;
			}
			else {
				container.pushSegment(this, segIdx);
			}
		}
	}



	public void append (Object context, Content container, BindingSrc bindingSrc)
	  throws Exception
	{
		append(0, _segmentArray.length, 0, context, container, bindingSrc);
	}


		// use passed-in container then appends bindings via default binding handler
	public void append (Object context, Content container)
	  throws Exception
	{
		BindingSrc handler = BindingSrc.instance(_bindingSrc);
		append (context, container, handler);
	}

		// creates a default empty container then append bindings via default binding handler.
	public Content append (Object context)
	  throws Exception
	{
		Content contentList = new Content();
		BindingSrc bindingSrc = BindingSrc.instance(_bindingSrc);
		append (context, contentList, bindingSrc);
		return contentList;
	}

		// creates a default empty container then does a straight binding to segment copying -
	public Content apply (List<Object> bindingValues)
	{
		List<byte[]> segments = Arrays.asList(this._segmentArray);
		return new Content(segments, bindingValues);
	}

	public Content apply (Object... bindingValues)
	{
		return apply(Arrays.asList(bindingValues));
	}


	public byte[][] segmentArray ()
	{
		return _segmentArray;
	}

	public byte[] segment (int i)
	{
		return _segmentArray[i];
	}

	public byte[] stringContent ()
	{
		if (_segmentArray.length > 1) {
			throw new RuntimeException("Not supported for Page that has bindings");
		}
		return segment(0);
	}

	public List<String> bindingNames ()
	{
		List<String> names = new ArrayList();
		for (Binding aBinding : _bindingArray) {
			names.add(aBinding.name());
		}
		return names;
	}

	public List<Map<String, String>> bindingAnnonations ()
	{
		List<Map<String, String>> bindingAnnonations = new ArrayList();
		for (Binding aBinding : _bindingArray) {
			if (aBinding instanceof Binding.ValueBinding) {
				bindingAnnonations.add(((Binding.ValueBinding)aBinding).getAnnonations());
			}
			else {
				bindingAnnonations.add(Collections.EMPTY_MAP);
			}
		}
		return bindingAnnonations;
	}

	
	public Binding[] bindingArray ()
	{
		return (Binding[])_bindingArray.clone();
	}
	

	public Page reParse ()
	{
		try {
			List<Page> pages = Parser.parse(_src, true);
			for (Page page : pages) {
				if (_name.equals(page._name)) {
					return page;
				}
			}
		}
		catch (Exception e) {
			throw new IllegalArgumentException(New.str("Unable to parse page [", _name, "]", e));
		}
		throw new IllegalArgumentException(New.str("Unable to find page [", _name, "] for repase"));
	}

	public Binding BindingByName (String name)
	{
		return this._bindingMap.get(name);
	}


	/* bindbinding */

	HashMap<String, BindBinding> _bindBindings;


	BindBinding getBindBinding (String bindingName)
	{
		return _bindBindings.get(bindingName);
	}

	void setBindBinding (String name, BindBinding bindBinding)
	{
		if (_bindBindings == null) {
			_bindBindings = new HashMap();
		}
		if (name.startsWith("!")) {
			name = name.substring(1, name.length());
		}
		_bindBindings.put(name, bindBinding);
	}

	public int arrayId ()
	{
		return _arrayId;
	}


}



/*
keywords

^class(a.b.c)^
^class^

^class(a.b.c!)^
^class^


^case(expr)^
  result0
^case(,)^
  result1
^case^

^case(expr,result0, result1,...)^

^this(dsfadsf)^

^field^
^method()^
^x.f.i.e.l.d^
^x.m.e.t.h.o.d()^
^app.body{dsafsd:dsf,dsfsdf;adfd}^

^true^
^false^
^null^
^112^


operators

!
>
<
>=
<=
&
|


*/
