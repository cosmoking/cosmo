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

import java.util.HashMap;
import java.util.Map;

public class Test extends BindingSrc
{
	public Object exprBindingTrue = true;
	public Object exprBindingFalse = false;


	public Object fieldA = "fieldA";


	public Object methodB (HashMap context, Content content) throws Exception
	{
		return "MethodB";
	}


	public static void main (String[] args) throws Exception
	{
		Parser.DEBUG = true;
		Parser.parse("D:/aribaweb-5.ORC3/aribaweb-5.0RC3/project/jackapp/template/Test.jwl");

		Page page = Page.byName(Test.class.getName());
		Content container = new Content();
		Map context = new HashMap();
		page.append(context, container);
		container.writeTo(System.out);
	}

	public static class InnerClass extends BindingSrc
	{
		public Object fieldD = "fieldD";
	}

}

/*

^class(template.Test)^
	^fieldA^
	^methodB()^
	^template.Test2{}^
	^template.Test$InnerClass{}^
	^bindingLess.template{}^
	^template.Test3{passedInBindings1:fieldA,passedInBindings2:methodB()}^




^class^

^class(template.Test2)^
	^test2FieldC^
	^template.Test$InnerClass{}^

	^case(exprBindingTrue)^
		^case(exprBindingTrue)^
			^case(exprBindingTrue,success)^
			^case(exprBindingTrue)^
				^case(exprBindingTrue,test2FieldC)^
				NESTEDCASETRUE!
			^case(,)^
				ERROR!
			^case^
		^case(,)^
			ERROR!
		^case^
	^case(,)^
		ERROR!
	^case^

	^case(exprBindingTrue)^
		testTrue
	^case(,)^
		ERROR!
	^case^

	^case(exprBindingTrue)^
		testTrue2
	^case^

	^case(exprBindingFalse)^
		ERROR!
	^case(,)^
		testFalse
	^case^

	^case(exprBindingFalse)^
		ERROR!
	^case^

	^case(exprBindingThree)^
		ERROR!
	^case(,)^
		ERROR!
	^case(,)^
		ERROR!
	^case(,)^
		TestThree
	^case^

	^case(exprBindingTrue,test2FieldC)^
^class^

^class(template.Test$InnerClass)^
	^fieldD^
^class^

^class(bindingLess.template!)^
	bindingLess.template
^class^



^class(template.Test3)^
     ^passedInBindings1^
     ^passedInBindings2^
^class^




*/
