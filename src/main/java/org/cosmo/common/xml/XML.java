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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import org.cosmo.common.util.Constants;


public class XML extends Node
{

	// Root of XML Tree that contains comment info
	LexicalHandler lexicalHandler;


	protected XML ()
    {
    }

    public XML (String id)
    {
        super (id);
    }




    public static XML open (File file)
      throws IOException, SAXException, ParserConfigurationException
    {
    	Reader reader = null;
    	try {
    		FileInputStream fio = new FileInputStream(file);
    		reader = new InputStreamReader(fio, Constants.UTF8);
    		XML xml = new XML(reader);
    		return xml;
    	}
    	finally {
    		if (reader != null) {
    			try {
    				reader.close();
    			}
    			catch (Exception e) {
    				Util.assertNonFatal(true, e.toString());
    			}
    		}
    	}
    }


    /*
    public byte[] dump (File file, NodeFormat format)
      throws IOException
    {
    	FileOutputStream writer = null;
    	try {
    		writer = new FileOutputStream(file);
    		byte[] bytes = toBytes(format);
    		writer.write(bytes);
    		return bytes;
    	}
    	finally {
    		if (writer != null) {
    			try {
    				writer.close();
    			}
    			catch (Exception e) {
    				Util.assertNonFatal(true, e.toString());
    			}
    		}
    	}
    }
    */


    public XML (Reader reader)
      throws IOException, SAXException, ParserConfigurationException
    {
        constructor(reader, true, true);
    }

    public XML (InputStream stream)
      throws IOException, SAXException, ParserConfigurationException
    {
    	this(new InputStreamReader(stream, Constants.UTF8));
    }

    public XML (RandomAccessFile index, RandomAccessFile content)
	  throws IOException
    {
    	Node node = PrintIndex.load (index, content);
    	_id = node._id;
    	_value = node._value;
    }


    public XML (List xpaths, RandomAccessFile index, RandomAccessFile content)
	  throws IOException
    {
    	Node node = PrintIndex.loadPartial (xpaths, index, content);
    	_id = node._id;
    	_value = node._value;
    }


    void constructor (Reader reader, boolean validation, boolean withComments)
      throws IOException, SAXException, ParserConfigurationException
    {

        Node container = (withComments) ? new XML() : new Node();
        NodeConstructHandler handler = new NodeConstructHandler(container);
        lexicalHandler = (withComments) ? new XML.LexicalHandler(handler) : null;
        constructor(reader, validation, withComments, container, handler, lexicalHandler);
    }


    void constructor (Reader reader,
    		          boolean validation,
    		          boolean withComments,
    		          Node container,
    		          NodeConstructHandler handler,
    		          XML.LexicalHandler lexicalHandler)
      throws IOException, SAXException, ParserConfigurationException
    {
        SAXParser parser = Util.getSAXParser(validation);
        parser.setProperty("http://xml.org/sax/properties/lexical-handler", lexicalHandler);
        InputSource source = new InputSource(reader);
        parser.parse(source, handler);

        if (withComments) {
                // the comment(s) for the xml root element is stored at container
            addComment(this, (List)lexicalHandler.commentsTable.get(container.value()));
            Object o = lexicalHandler.commentsTable.get(container);
            String a = "sdf";
        }

            // assign values of the container to the actual root element
        _id = ((Node)container.value())._id;
        _value = ((Node)container.value())._value;
    }


    public static XML create (Reader reader)
    {
    	try {
    		return new XML(reader);
    	}
    	catch (Exception e) {
    		return null;
    	}
    }


    public String pathValue (String path)
    {
    	Object o = super.value(path);
    	return o == null ? null : o.toString();
    }


    public void addComment (Node node, String comment)
    {
        lexicalHandler.addComment(node, comment);
    }

    public void addComment (String xpath, String comment)
    {
        Node node = get(xpath);
        if (node != null) {
            addComment(node, comment);
        }
    }

    public void addComment (Node node, List comments)
    {
        if (node != null && comments != null) {
            lexicalHandler.addComment(node, comments);
        }
    }

    public byte[] toBytes (NodeFormat format)
    {
    	return format == null
		? org.cosmo.common.util.Util.UTF8(this)
		: org.cosmo.common.util.Util.UTF8(this.toString(format));
    }


    public String toString (NodeFormat NodeFormat)
    {
        Map commentTable = lexicalHandler == null ? new HashMap() : lexicalHandler.commentsTable;
        commentTable = NodeFormat.printComments ? commentTable : new HashMap();
        PrintWithComments visitor = new PrintWithComments(commentTable, NodeFormat, new StringBuilder());
       	visit(visitor);
        String xml = visitor.toString();
        if (NodeFormat.printDTDDeclaration && !Util.nullOrEmptyOrBlankString(lexicalHandler.dtdDeclaration)){
            xml = lexicalHandler.dtdDeclaration + "\n" + xml;
        }
        if (NodeFormat.printXMLDeclaration) {
             xml = NodeFormat.xmlDeclaration + "\n" + xml;
        }
        return xml;
    }

    public void dump (OutputStream content, OutputStream index)
      throws Exception
    {
    	Writer contentWriter = new OutputStreamWriter(content);
    	try {

	    	PrintIndex pi = new PrintIndex(this, contentWriter);
	    	visit(pi);
	    	contentWriter.flush();

	    	if (index != null) {
	    		pi.dumpIndex(index);
	    	}
    	}
    	finally {
    		contentWriter.close();
    	}
    }





    public static class PrintWithComments extends Print
    {
    		// The comments are stored as
    		// ["above node comment1", "above node comment2..",  EndNodeCommentsMarker, "below node comment 3", "below node comment4]
        public static final Object EndNodeCommentsMarker = new Object();

        	// Marker for comment at end of the document
        public static final Object EndDocumentCommentsMarker = new Object();

    	Map commentTable;


        public PrintWithComments (Map comments, NodeFormat NodeFormat, StringBuilder buf)
        {
            super(NodeFormat, buf);
            commentTable = comments;
        }

        public void beginNode (Node node, Iterator<Node> attributes, int depth)
        {
           if (commentTable.containsKey(node)) {
                Iterator i = ((List)commentTable.get(node)).iterator();
                while (i.hasNext()) {
                	Object comment = i.next();
                    if (comment == EndNodeCommentsMarker) {
                    	break;
                    }
                    appendIndent(depth);
                    appendString("<!-- ");
                    appendString(comment.toString());
                    appendString(" -->");
                    appendString("\n");
                }
            }
            super.beginNode(node, attributes, depth);
        }

        public void afterNextNode (Node node, int depth)
        {
            if (commentTable.containsKey(node)) {
                Iterator i = ((List)commentTable.get(node)).iterator();
                for (Object comment = i.next();
                     i.hasNext() && comment != EndNodeCommentsMarker;
                     comment = i.next())
                {
                }

                while (i.hasNext()) {
                	  Object comment = i.next();
                      appendIndent(depth);
	                  appendString("<!-- ");
	                  appendString(comment.toString());
	                  appendString(" -->");
                      appendString("\n");
                }
            }
            super.afterNextNode(node, depth);
        }

        public void endNode (Node node, int depth)
        {
        		// end of document
        	if (depth == 0) {
       		    if (commentTable.containsKey(EndDocumentCommentsMarker)) {
                    Iterator i = ((List)commentTable.get(EndDocumentCommentsMarker)).iterator();
                    while (i.hasNext()) {
                        Object comment = i.next();
                        appendIndent(depth);
                        appendString("<!-- ");
                        appendString(comment.toString());
                        appendString(" -->");
                        appendString("\n");
                    }
                }
        	}
        }
    }


    public static class LexicalHandler extends DefaultHandler implements org.xml.sax.ext.LexicalHandler
    {
        NodeConstructHandler handler;
        Map commentsTable;
        String dtdDeclaration;
        boolean endDTD;
        List focusComments;

        public LexicalHandler (NodeConstructHandler handler)
        {
            this.handler = handler;
            this.commentsTable = new HashMap();
            this.handler.listener = this;
        }


        public void startDTD (String name, String publicId,
                   String systemId)
          throws SAXException
        {
            if (publicId != null) {
                dtdDeclaration =  Util.Fmt("<!DOCTYPE %s PUBLIC \"%s\">", name, publicId);
            }
            if (systemId != null) {
                dtdDeclaration =  Util.Fmt("<!DOCTYPE %s SYSTEM \"%s\">", name, systemId);
            }
        }

        public void endDTD ()
          throws SAXException
        {
            endDTD = true;
        }


        public void startEntity (String name)
          throws SAXException
        {
        }


        public void endEntity (String name)
          throws SAXException
        {
        }

        public void startCDATA ()
          throws SAXException
        {
        	handler.isCDATA = true;
        }

        public void endCDATA ()
          throws SAXException
        {
        }

        public void comment (char ch[], int start, int length)
          throws SAXException
        {
                 // indicates start of the xml comments
            if (endDTD || dtdDeclaration == null) {
                if (focusComments == null) {
                    focusComments = new ArrayList();
                }
                focusComments.add(new String(ch, start, length));
            }
        }

        public void startElement (String uri, String localName,
                  String qName, Attributes attributes)
          throws SAXException
        {
                // grab all the comments and
            if (focusComments != null) {
                addComment(handler.focusNode, focusComments);
                focusComments = null;
            }
        }

        public void endElement (String uri, String localName,
                String qName)
    	  throws SAXException
        {
            if (focusComments != null) {
                List l = (List)commentsTable.get(handler.focusNode);
                	// already have comments from startElements
                if (l != null) {
                	l.add(PrintWithComments.EndNodeCommentsMarker);
                	l.addAll(focusComments);
                }
                else {
                	focusComments.add(0, PrintWithComments.EndNodeCommentsMarker);
                    commentsTable.put(handler.focusNode, focusComments);
                }
                focusComments = null;
            }
        }

        public void endDocument ()
            throws SAXException
        {
            if (focusComments != null) {
            	commentsTable.put(PrintWithComments.EndDocumentCommentsMarker, focusComments);
                focusComments = null;
            }
        }



        public void addComment (Node node, String comment)
        {
            List comments = (List)commentsTable.get(node);
            if (comments == null) {
                comments = new ArrayList();
                commentsTable.put(node, comments);
            }
            comments.add(comment);
        }

        public void addComment (Node node, List comments)
        {
            commentsTable.put(node, comments);
        }

        public void fatalError (SAXParseException e)
    	  throws SAXException
        {
       	    throw e;
        }

        public Node focusNode ()
        {
        	return handler.focusNode;
        }

    }

    public static class CDATA
    {
    	private String str;

    	public CDATA (Object data)
    	{
    		if (data != null) {
    			str = data.toString();
    		}
    	}

    	public String toString ()
    	{
    		return str;
    	}

    	public String toCDATAString ()
    	{
    		return "<![CDATA[" + str + "]]>";
    	}
    }




    public static void main (String[] args)
    {

        Node po = new Node("PO");

        System.out.println(new File(".").getAbsolutePath());
        Reader in = null;
        try {
            if (args == null || args.length < 1) {
                System.out.println("Tools requires Input XML File Name.");
                return;
            }


            XML node = XML.open(new File(args[0]));
            System.out.println(node);
            //System.out.println(node.toString(new NodeFormat().printComments(true).cdataAsString(false)));
            //System.out.println(node.getX("Header.To.[n].Identity"));
            //System.out.println(node.getX("Header.To.[2].Identity"));


            //node.insertX("Header.To.[n]", "3", "Value");
            //node.insertX("Header.To.[1]", "2", "Value");
            //node.insertX("Header.To.[0]", "1", "Value");
            //node.insertX("Header.To", "0", "Value");

            //node.deleteX("Header.To");

            //System.out.println(node.toString(new NodeFormat().printComments(true).cdataAsString(false)));




//            System.out.println(node.toString());


			node.dump(new FileOutputStream("c:/xml.txt"), new FileOutputStream("c:/index.txt"));



			RandomAccessFile index = new RandomAccessFile("c:/index.txt", "r");
			RandomAccessFile content = new RandomAccessFile("c:/xml.txt", "r");

			XML loadednode = new XML(index, content);

			loadednode.add("Response.newelement", "newelement");
			loadednode.add("@newattribute", "newttribute");
			loadednode.add("Response.newelement2", "newelement2");
			loadednode.add("Response.[1].newelement3", "newelement3");

			loadednode.add("Post1", "some1","Post2");
			loadednode.add("Post3", "some","Post4","sdfsdfds");

			loadednode.add("Post5").add("@topic", "df").add("@author", "me").add("@views", "123").add("@replies", "3243");


			System.out.println(loadednode);
			//System.out.println(loadednode.value("Response.Status.ProviderSetupResponse.StartPage.URL"));
			//System.out.println(loadednode.value("Response.Status.ProviderSetupResponse.StartPage"));
			System.out.println(loadednode.value("Response.Status.ProviderSetupResponse"));


			//System.out.println("hesfsefesl");

/*



            String duns = "1123123123";
            Node cxml = new Node("cXML");
			cxml.addAttribute("a", "b").addAttribute("c", "d").add("Header")
				.add("From")
					.add("Credential").addAttribute("domain2", "DUNS")
					    .add("Identity",duns).pop(cxml, "Header")
				.add("To")
				     .add("Credential").addAttribute("domain2", "DUNS")
				         .add("Identity",duns).pop(cxml, "Header")
				.add("Sender")
				     .add("Credential").addAttribute("domain2", "DUNS")
				         .add("Identity",duns);




			System.out.println(cxml);

			System.out.println(cxml.getX("Header.Sender"));

/*
			node.addAll(cxml);


//			node.insertX("Request.PunchOutSetupRequest.Extrinsic", "XXX1-insert", "XXX1");
//			node.insertX("Request.PunchOutSetupRequest.ShipTo", "XXX2-insert", "XXX2");

			node.setX("Request.PunchOutSetupRequest.Extrinsic.XXX1-set", "XXX1");
			node.setX("Request.PunchOutSetupRequest.ShipTo.XXX2-set", "XXX2");
			node.setX("@timestamp", "xxxtimexxx");
			node.setX("Header.From.Credential.@domain", "xxxdomainxxx");

			node.deleteX("Request.PunchOutSetupRequest.[n]");





			System.out.println(node.getX("Header.From.Credential").value());
			System.out.println(node.getX("@timestamp"));

			System.out.println(node);
			System.out.println("=====================================");







			node.dump(new FileOutputStream("c:/content.txt"), new FileOutputStream("c:/index.txt"));



			RandomAccessFile index = new RandomAccessFile("c:/index.txt", "r");
			RandomAccessFile content = new RandomAccessFile("c:/xml.txt", "r");

//			XML loadednode = new XML(index, content);

//			System.out.println(loadednode);

			List<String> xpaths = Arrays.asList("cXML.Header.To.Credential.@domain", "cXML.Header.Sender.Credential.SharedSecret");
			XML loadPartial = new XML(xpaths, index, content);


			System.out.println(loadPartial);

			FieldValue_Node init = new FieldValue_Node();



			System.out.println(ariba.util.fieldvalue.FieldValue.getFieldValue(loadPartial, "Header.To.Credential.@domain"));
			ariba.util.fieldvalue.FieldValue.setFieldValue(loadPartial, "fieldValueID.b.@abc", "somevalue");
			System.out.println(loadPartial);

			System.out.println(ariba.util.fieldvalue.FieldValue.getFieldValue(loadPartial, "Header.#children.iterator"));
			System.out.println(ariba.util.fieldvalue.FieldValue.getFieldValue(loadPartial, "Header.To.Credential.@domain.#value"));



			XML module = new XML("Module");
			module
				.add("ModulePrefs").addAttributes("title", "Hello World!","title2", "helloword2","title3","dfsdf")
					.add("Require").addAttribute("feature", "opensocial").pop(module)
				.add("Content").addAttribute("type", "html")
					.set(new CDATA("HELLO, WOrld"));


			System.out.println(module);

*/

//



        }
        catch (Exception e) {
            e.printStackTrace();
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
