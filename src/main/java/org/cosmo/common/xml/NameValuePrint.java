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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.Iterator;


public class NameValuePrint extends Visitor
{
    String separator;
    StringBuilder buf;
    boolean printAttributes;
    boolean showNullValuePair;
    Node node;
    int longestValueLength;
    int longestNameValueLength;
    String path;


    public NameValuePrint (Node node)
    {
        this(node, true, false, ": ");
    }

    public NameValuePrint (Node node, boolean printAttributes, boolean showNullValuePair, String separator)
    {
        this.buf = new StringBuilder();
        this.printAttributes = printAttributes;
        this.showNullValuePair = showNullValuePair;
        this.node = node;
        this.separator = separator;
        this.path = "NOT WORKING";
        node.visit(this);
    }

    public String toString ()
    {
        return buf.toString();
    }

    public void handleValue (Node node, int depth)
    {
        if (!showNullValuePair && (node.value() == null ||
            Util.nullOrEmptyOrBlankString(node.value().toString()))) {
            return;
        }
        else {
            // for each value we append it's full xpath and its value
            String line = path.toString() + separator + node.value() + "\n";
            buf.append(line);
            longestNameValueLength = (line.length() > longestNameValueLength)
                ? line.length() : longestNameValueLength;
            longestValueLength = (node.value().toString().length() > longestValueLength)
                ? node.value().toString().length() : longestValueLength;
        }
    }

    public void beginNode (Node node, Iterator<Node> attributes, int depth)
    {
        if (printAttributes) {
            for (Node attribute : node.attributes()) {
                if (!showNullValuePair && (attribute.value() == null ||
                    Util.nullOrEmptyOrBlankString(attribute.value().toString()))) {
                    continue;
                }
                else {
                    String line = path.toString() + "." + attribute.id() + separator + attribute.value() + "\n";
                    buf.append(line);
                    longestNameValueLength = (line.length() > longestNameValueLength)
                        ? line.length() : longestNameValueLength;
                    longestValueLength = (attribute.value().toString().length() > longestValueLength)
                        ? attribute.value().toString().length() : longestValueLength;
                }
            }
        }
    }

    public int getLongestValueLength ()
    {
        return this.longestValueLength;
    }

    public int getLongestNameValueLength ()
    {
        return this.longestNameValueLength;
    }

    public static void main (String[] args) throws Exception
    {
        System.out.println(new File(".").getAbsolutePath());
        Reader in = null;
        try {
            if (args == null || args.length < 1) {
                System.out.println("Tools requires Input XML File Name.");
                return;
            }
            in = new FileReader(args[0]);
            XML node = new XML(in);

            NameValuePrint print = new NameValuePrint(node);
            String xml = print.toString();
            System.out.println(xml);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        catch (Exception e) {
            System.out.println(Util.Fmt("Turn on -debug for further error detail: %s", e.getMessage()));
            if (args != null && args.length > 1 && args[1].toString().equalsIgnoreCase("-debug")) {
                e.printStackTrace();
            }
        }
        finally {
            try {
                in.close();
            }
            catch (Exception e) {
            }
        }
    }
}
