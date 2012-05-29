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

public class NodeFormat
    {
            //
            // To include XML Header when print
            // <?xml version=\"1.0\"?>
            //
        public static boolean PrintXMLDeclaration = false;

            // XML Delcaration
        public static String XMLDeclaration = "<?xml version=\"1.0\"?>";

            // <A>value</A>
            //
            // <A>
            //   value
            // </A>
            //
        public static boolean OneLinePrint = true;

            //
            // <A attribute1="value1" attribute2="value2/>
            //
            // <A attribute1="value1"
            //    attribute2="valu32"/>
            //
        public static boolean IndentAttributes = true;

            // indentation spaces used when printing the node
        public static String Indent = "  ";

            // The LeadingIndent
        public static int LeadingIndent = 0;

            // Print DTD Declaration
        public static boolean PrintDTDDeclaration = false;

            // Print Comments
        public static boolean PrintComments = true;

        	// Replace Entity References
        public static boolean ReplaceEntityReference = false;

        	// Treat CDATA as regular String
        public static boolean CDATAAsString = true;

        	// Print Nodes's siblings when Nodes is the Root Object
        public static boolean PrintNodesSiblings = false;

        	// During an Circular Reference the node value will be the "xpath" of that parent reference
        	// takes alot of memory because each node is store in hashtable
        public static boolean PrintCircularAsReference = false;



        public boolean printXMLDeclaration = PrintXMLDeclaration;
        public String xmlDeclaration = XMLDeclaration;
        public boolean oneLinePrint = OneLinePrint;
        public boolean indentAttributes = IndentAttributes;
        public String indent = Indent;
        public int leadingIndent = LeadingIndent;
        public boolean printDTDDeclaration = PrintDTDDeclaration;
        public boolean printComments = PrintComments;
        public boolean replaceEntityReference = ReplaceEntityReference;
        public boolean cdataAsString = CDATAAsString;
        public boolean printNodesSiblings = PrintNodesSiblings;
        public boolean printCircularAsReference = PrintCircularAsReference;


        public NodeFormat leadingIndent (int i) {
            leadingIndent = i;
            return this;
        }

        public NodeFormat indentAttributes (boolean b)
        {
            indentAttributes = b;
            return this;
        }

        public NodeFormat indent (String s)
        {
            indent = s;
            return this;
        }

        public NodeFormat oneLinePrint (boolean b)
        {
            oneLinePrint = b;
            return this;
        }

        public NodeFormat printDTD (boolean b)
        {
        	printDTDDeclaration = b;
        	return this;
        }

        public NodeFormat printDeclaration (boolean b)
        {
        	printXMLDeclaration = b;
        	return this;
        }

        public NodeFormat printComments (boolean b)
        {
        	printComments = b;
        	return this;
        }

        public NodeFormat replaceEntityReference (boolean b)
        {
        	replaceEntityReference = b;
        	return this;
        }

        public NodeFormat cdataAsString (boolean b)
        {
        	cdataAsString = b;
        	return this;
        }

        public NodeFormat printNodesSiblings (boolean b)
        {
        	printNodesSiblings = b;
        	return this;
        }

        public NodeFormat printCircularAsReference (boolean b)
        {
        	printCircularAsReference = b;
        	return this;
        }
    }
