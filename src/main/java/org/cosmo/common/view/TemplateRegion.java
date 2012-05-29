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
package org.cosmo.common.view;


import java.util.List;

import org.cosmo.common.net.Session;
import org.cosmo.common.template.Binding;
import org.cosmo.common.template.BindingSrc;
import org.cosmo.common.template.Content;
import org.cosmo.common.template.Page;
import org.cosmo.common.template.PageBinding;
import org.cosmo.common.util.Constants;
import org.cosmo.common.util.JSONList;


/*
 *  2 Types:
 *
 *  1. first sends the entire chunk of html and do replacements and client side
 *  2. second sends the "bindings" only and "assemble" at client side.
 *
 *  NOTE: In templateRegion only static binding is supported! ie field and method or placehodlers!
 *  no case, bind, template etc
 *
 *  also, same bindings can be sent send multiple times, however, to reduce duplicate values
 *  over the wire, use "virtual bindings" so that it can still be references on the client side
 */

public class TemplateRegion extends DivRegion
{

		// called to initialize the template region
	public void initialize (Session session, Content container)
	  throws Exception
	{
		Binding pageParam = callBindings().get("name");
		Binding onLoadParam = callBindings().get("onLoad");
		Binding containerId = callBindings().get("containerId");
		Binding model = callBindings().get("model");
		Binding context = callBindings().get("context");
		Binding callback = callBindings().get("callback");
		
		Page page = Page.byName(pageParam.name());
		TemplateRegion templateRegion = (TemplateRegion)BindingSrc.instance(page._bindingSrc);
		templateRegion.writeTemplateRegionScript(session, container, onLoadParam, page, model, context, containerId, callback);
		
		
			// if there is a nested TemplateRegion defined inside this TemplateRegion
		if (renderNestedTemplateRegionIfAny (page, session, container)) {
			// System.out.prinltn("rendering nested templatePage");
		}
	}

		// when there is nested TemplateRegion inside a TemplateRegion - this call will
		// recursively generate the nested TemplateRegion Scripts (ie), and the bindingName of the 
		// nested TemplateRegion will be the passed in binding param "name" - see PageBinding.name() method
		// where it will return that value
		// <script id="root" ...
    	// <script id="nested1" ...
		// ..
	public boolean renderNestedTemplateRegionIfAny (Page page, Session session, Content container)
	  throws Exception
	{			
		for (Binding binding : page.bindingArray()) {
			if (binding instanceof PageBinding) {
				if (TemplateRegion.class.getName().equals(((PageBinding)binding).calleePageName())) {
					((PageBinding)binding).applyValue(page, this, container, session, null);
					return true;
				}
			}
		}
		return false;
	}
	
		// writes the javascript declarations including segments array and bindings array
	public void writeTemplateRegionScript (Session session, Content container, Binding onLoadParam, Page page, Binding model, Binding context, Binding containerId, Binding callback)
	  throws Exception
	{
		Page template = Page.byName(TemplateRegion.Script.class.getName());
		TemplateRegion.Script templateRegionScript = BindingSrc.instance(TemplateRegion.Script.class);
		templateRegionScript.renderSegmentsDivId = uniqueId(page._name);
		templateRegionScript.renderSegmentsTemplateName = page._name;
		templateRegionScript.bindingContainer = templateBindings(session);
		templateRegionScript.onLoad = onLoadParam == null ? "null" : onLoadParam.name();
		templateRegionScript.containerId = containerId == null ? "null" : containerId.name();
		templateRegionScript.context = context == null ? "null" : context.name();
		templateRegionScript.callback = callback == null ? "null" : callback.name();
		templateRegionScript.templateId = page._name;
		
		//Class modelClass = Class.forName(model.name());
		//templateRegionScript.model = modelClass.getSimpleName();
		
		
		template.append(session, container, templateRegionScript);
	}



		// this class is to be override by the subclass to provide the binding values
	public List templateBindings (Session session)
	  throws Exception
	{
		return Constants.EmptyList;
	}


		// script template for template region
	public static class Script extends BindingSrc {

		public String templateId;
		public String callback;
		public String onLoad;
		public String containerId;
		public String model;
		public String context;
		public String renderSegmentsDivId;
		public String renderSegmentsTemplateName;
		public List<?> bindingContainer;


		public Object renderHtmlSegments (Content c) throws Exception
		{
			Page page = Page.byName(renderSegmentsTemplateName);
			JSONList array = new JSONList(page.segmentArray());
			return array.getBytes();
		}

		public Object renderHtmlBindings (Content c) throws Exception
		{
			JSONList array = new JSONList(bindingContainer);
			return array.getBytes();
		}

		public Object renderBindingNames (Content c) throws Exception
		{
			Page template = Page.byName(renderSegmentsTemplateName);
			JSONList array = new JSONList(template.bindingNames());
			return array.getBytes();
		}

		public Object renderBindingAnnonations (Content c) throws Exception
		{
			Page template = Page.byName(renderSegmentsTemplateName);
			JSONList array = new JSONList(template.bindingAnnonations());
			return array.getBytes();
		}
	}
}
