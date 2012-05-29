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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.cosmo.common.net.StringTokens;

import org.apache.commons.io.IOUtils;

import org.cosmo.common.util.New;
import org.cosmo.common.util.Stack;
import org.cosmo.common.util.Util;

public class Parser
{
	// Page parsing
	public static boolean DEBUG = true;
	public static final char BindingChar = '^';


	// XXX NOTE! Need better parsing and validation!!!
	public static final String PageDeclToken = "^class";
	public static final String PageEndToken = ")^";
	public static final int PageBeginTokenSize = PageDeclToken.length();
	public static final int PageEndTokenSize = PageEndToken.length();


	public static List<Page> parse (String file)
	  throws Exception
	{
		return parse (new File(file), false);
	}

	public static List<Page> parse (File file, boolean reParse)
	  throws Exception
	{
		List<Page> pages = new ArrayList();
		if (file.isDirectory()) {
			for (File aFile : file.listFiles()) {
				pages.addAll(parse(aFile, reParse));
			}
		}
		else {
			List<String> pageStrings = parseSrcToPageStrings(file);
			for (String pageString : pageStrings) {
				Page page = parsePageString(pageString, file, reParse);
				pages.add(page);
			}
		}
		return pages;
	}


	public static List<String> parseSrcToPageStrings (File src)
	  throws IOException
	{
		List<String> pageSrcs = new ArrayList();
		if (src.getAbsolutePath().endsWith(".jwl")) {
			String srcContents = IOUtils.toString(new FileInputStream(src), "UTF8");


				// find begin and end index for inline pages (ie) ^class(a.bc)^
			List<Integer> inlineSrcIndexes = new ArrayList();
			int index = srcContents.indexOf(PageDeclToken);
			while (index >= 0) {
				inlineSrcIndexes.add(index);
				index = srcContents.indexOf(PageDeclToken, index + PageBeginTokenSize);
			}

				// make sure inline pages have proper enclosings
			if (inlineSrcIndexes.size() % 2 != 0) {
				throw new IllegalArgumentException("Invalid Page Src file - did not have proper enclosing inline page declarations");
			}


				// add inline page to the page src list
			for (int i = 0; i < inlineSrcIndexes.size(); i = i + 2) {
				int begin = inlineSrcIndexes.get(i);
				int end = inlineSrcIndexes.get(i + 1);
				pageSrcs.add(srcContents.substring(begin, end));
			}
		}
		return pageSrcs;
	}


		// process the pages recursively as well as all the referenced pages that are either declared in the page
		// or reference by the bindings. This process also caches all the pages that are processed
	public static Page parsePageString (String pageString, File src, boolean reParse)
	  throws Exception
	{
		ArrayList<byte[]> segmentContainer = new ArrayList();
		ArrayList<CharSequence> bindingContainer = new ArrayList();
		StringBuilder segment = new StringBuilder(256);
		StringBuilder binding = null;

		char[] pageChars = pageString.toCharArray();

			// extract args string
		int end = pageString.indexOf(PageEndToken, PageBeginTokenSize);
		String argString = pageString.substring(PageBeginTokenSize + 1, end);
		StringTokens args = StringTokens.on(argString);
		Options options = new Options(args);


			// extract get pageName and bindingSrc className , by default pageName IS The bindingSrc class name, unless options bindingSrc:xxx is specified
		String pageName = options._pageName;
		Class clazz = null;
		if (pageName.startsWith("!")) {
				// virtual page
			pageName = pageName.substring(1, pageName.length());  // strip ! so that it can be referenced without "!"
			clazz = options._bindingSrc == null ? BindingSrc.class : Class.forName(options._bindingSrc);
		}
		else {
			try {
			clazz = options._bindingSrc == null ? Class.forName(pageName) : Class.forName(options._bindingSrc);
			}
			catch (ClassNotFoundException e) {
				ClassNotFoundException cnfe = new ClassNotFoundException(e.getMessage() + " from page" + src, e);
				cnfe.fillInStackTrace();
				throw cnfe;
				
			}
		}


			// parse segments and bindings
		for (int i = end + PageEndTokenSize; i < pageChars.length; i++) {

			// handles bindings
			if (pageChars[i] == BindingChar) {
				if (binding == null) {
					binding = new StringBuilder();
					continue;
				}
				else {
					segmentContainer.add(options._jsonEncode ? Util.jsonQuoteBytes(segment) : Util.bytes(segment));
					segment = new StringBuilder(256);
					//indents.add(currentIndent);

					segmentContainer.add(options._jsonEncode ? Page.BindingMarkerBytesQuoted : Page.BindingMarkerBytes);
					bindingContainer.add(binding.toString());
					binding = null;
					continue;
				}
			}
			if (binding != null) {
				binding.append(pageChars[i]);
			}


			// handles trim and escapeQuote
			if (binding == null) {

				if (pageChars[i] == '\n' || pageChars[i] == '\r') {
					if (!options._trim) {
						segment.append(pageChars[i]);
					}
				}
				else {
					if (pageChars[i] == '\t') {
						if (!options._trim) {
							segment.append(pageChars[i]);
						}
					}
					else {
						if (options._escapeQuote && pageChars[i] == '\"') {
							segment.append("\\");
						}
						segment.append(pageChars[i]);
					}
				}
			}
		}

			// convert segments to raw bytes
		Page page = new Page(pageName, clazz, src, options, reParse);
		segmentContainer.add(options._jsonEncode ? Util.jsonQuoteBytes(segment) : Util.bytes(segment));
		page._segmentArray = (byte[][]) segmentContainer.toArray(new byte[][]{});

			// convert binding string to parsed bindings
		BindingContainer bindingsContainer = Parser.parseBindings(page, bindingContainer, 0, page._segmentArray.length, 0);
		page._bindingArray = (Binding[])bindingsContainer.toArray(new Binding[]{});

		for (Binding aBinding : page._bindingArray) {
			if (aBinding instanceof Binding.ValueBinding) {
				page._bindingMap.put(aBinding.name(), aBinding);
			}
		}
		return page;
	}


	public static Binding parseBinding (String bindingStr, Page page, Class<? extends BindingSrc> handlerClass, Options options)
	  throws Exception
	{
	  	BindingContainer bindingContainer = new BindingContainer();
	  	parseBindings(bindingStr, page, handlerClass, bindingContainer, options);
	  	return (Binding)bindingContainer.get(0);
	}


	public static void parseBindings (String bindingStr, Page page, Class<? extends BindingSrc> bindingSrc, BindingContainer container, Options options)
	  throws Exception
	{
		if (bindingStr.startsWith("case") && (bindingStr.length() == 4 || bindingStr.endsWith(")"))) {

			CaseBinding caseBinding = container._caseBindings.peek();


				// ^case^ implies end
			if ("case".equals(bindingStr)) {
				container.addBinding(CaseBinding.End);
				caseBinding.markCaseSegmentBoundary();
				container._caseBindings.pop();
				return;
			}
					// ^case(,)^ mark segement
			String caseEvalExpr = bindingStr.substring(5, bindingStr.length() - 1);
			if (",".equals(caseEvalExpr)) {
				container.addBinding(CaseBinding.NoOp);
				caseBinding.markCaseSegmentBoundary();
			}

				// ^case(????)^ create a case container and evaluate the caseEvalExpr
			else {
				caseBinding = new CaseBinding();
				container.addBinding(caseBinding);
				container._caseBindings.push(caseBinding);


				StringTokens caseTokens = StringTokens.on(caseEvalExpr);
				caseEvalExpr = caseTokens.next();
				caseBinding.setBindingExpr(caseEvalExpr);
				if (caseTokens.hasNext()) { // this means it's one line case(eval, binding1, binding2, binding n ..);
					caseBinding.setShortformBindings(caseTokens, page, bindingSrc, options);
					container._caseBindings.pop();
				}
			}
			return;
		}

		if (bindingStr.startsWith("bind") && (bindingStr.length() == 4 || bindingStr.endsWith(")"))) {

			// ^bind^ implies end
			if ("bind".equals(bindingStr)) {
				container.addBinding(BindBinding.End);
				container._bindBinding = null;
				return;
			}

				// ^bind(????)^ create a bind container and evaluate the caseEvalExpr
			else {
				container._bindBinding = new BindBinding();
				container.addBinding(container._bindBinding);
				String bindingName =  bindingStr.substring(5, bindingStr.length() - 1);
				page.setBindBinding(bindingName, container._bindBinding);
			}
			return;

		}

			// Method Binding,
			// resolve by  methodName(Context.class, ContentContainer.class),
			//			   methodName(Object.class, ContentContainer.class),
			//			   methodName(ContentContainer.class);
			// method can do it's own appending by calling ContentContainer.append()
			// or return an object which would be appended automatically
			//
			// XXX possible hack: page A  includes Page B  but both uses different Context class
			// so when Page B try to resolve method binding it will fail and fall back to look for
			// method that does not include context class.  currently, Widgets.jwl uses this in both
			// Body.jwl and BrowserAddExtrinSite.jwl.
		if (bindingStr.length() > 2 &&
			bindingStr.charAt(bindingStr.length() - 1) == ')' &&
			bindingStr.charAt(bindingStr.length() - 2) == '(') {
			MethodBinding binding = new MethodBinding(bindingStr);
			container.addBinding(binding);
			return;
		}


		if (bindingStr.charAt(0) == '`' && bindingStr.charAt(bindingStr.length() - 1) == '`') {
			StringBinding binding = new StringBinding(bindingStr);
			container.addBinding(binding);
			return;
		}


			// page Binding - ^org.app.Body{arg1,arg2}^
		if (bindingStr.endsWith("}")) {
			PageBinding binding = new PageBinding(bindingStr);
			container.addBinding(binding);
			return;
		}

			// XPath via AribaWeb FieldValue
		if (bindingStr.length() > 2 & bindingStr.indexOf('.') > 0) {
			XPathBinding binding = new XPathBinding(bindingStr);
			container.addBinding(binding);
			return;
		}

			// constant binding
		String bindingStrTest = bindingStr.toLowerCase();
		if ("true".equals(bindingStrTest) || "false".equals(bindingStrTest) || "null".equals(bindingStrTest) || Character.isDigit(bindingStrTest.charAt(0))) {
			ConstantBinding binding = new ConstantBinding(bindingStrTest);
			container.addBinding(binding);
			return;
		}

		// Virtual Binding
		if (bindingStr.length() > 0 && bindingStr.startsWith("!")) {
			VirtualBinding binding = new VirtualBinding(bindingStr);
			container.addBinding(binding);
			return;
		}


			// Field Binding
		if (bindingStr.length() > 0) {
			FieldBinding binding = new FieldBinding(bindingStr, bindingSrc);
			container.addBinding(binding);
			return;
		}

		throw new IllegalArgumentException(New.str("Invalid Binding [", bindingStr, "] for Page [", page._name, "]"));
	}






	public static BindingContainer parseBindings (Page page, ArrayList<CharSequence> bindingStrings, int segStartIdx, int segEndIdx, int bindingStartIdx)
	  throws Exception
	{
		BindingContainer bindingContainer = new BindingContainer();
		for (int segIdx = segStartIdx, bindingIdx = bindingStartIdx; segIdx < segEndIdx; segIdx++) {
			bindingContainer.recordCaseSegementAndBindingIndex(segIdx , bindingIdx);
		    byte[] segment = page._segmentArray[segIdx];
			if (segment == Page.BindingMarkerBytes || segment == Page.BindingMarkerBytesQuoted) {
				CharSequence binding = bindingStrings.get(bindingIdx++);
				Parser.parseBindings(binding.toString(), page, page._bindingSrc, bindingContainer, null);
				continue;
			}
		}
		return bindingContainer;
	}
}


class BindingContainer extends ArrayList
{
	public BindBinding _bindBinding;
	public Stack<CaseBinding> _caseBindings = new Stack();


	public void recordCaseSegementAndBindingIndex (int segIdx, int bindingIdx)
	{
		if (_caseBindings.peek() != null) {
			_caseBindings.peek().recordSegementAndBindingIndex(segIdx, bindingIdx);
		}

		if (_bindBinding != null) {
			_bindBinding.recordSegementAndBindingIndex(segIdx, bindingIdx);
		}

	}

	public void addBinding (Object binding)
	{
		this.add(binding);
	}


}
