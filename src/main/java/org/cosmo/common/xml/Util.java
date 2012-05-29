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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import ariba.util.io.ObjectInputStream;


public class Util
{

	public static List toList (Iterable i)
	{
    	List list = new ArrayList();
    	for (Object o : i) {
    		list.add(o);
    	}
    	return list;
	}

	public static String Fmt (String format, Object... args)
	{
		return new Formatter().format(format, args).toString();
	}

	public static boolean nullOrEmptyOrBlankString (String string)
	{
        int length = string == null ? 0 : string.length();
        if(length > 0)
        {
            for(int i = 0; i < length; i++)
            {
                if(!java.lang.Character.isWhitespace(string.charAt(i)))
                {
                    return false;
                }
            }
        }
        return true;
	}

    public static boolean nullOrEmptyString(String string)
    {
        return string == null || string.length() == 0;
    }


    public static SAXParser getSAXParser (boolean validating)
      throws ParserConfigurationException, SAXException
    {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        spf.setValidating(validating);
        SAXParser sp = spf.newSAXParser();
        return sp;
    }

    public static void assertNonFatal(boolean test, String msg)
    {
    	if (!test) {
    		Exception e = new Exception(msg);
    		e.printStackTrace(System.out);
    	}
    }

    public static void assertFatal(boolean test, String msg)
    {
    	if (!test) {
    		throw new RuntimeException(msg);
    	}
    }


    public static boolean readerToWriter(java.io.Reader reader, java.io.Writer writer)
    {
        return readerToWriter(reader, writer, new char[2048]);
    }


    public static boolean readerToWriter(java.io.Reader reader, java.io.Writer writer, char buffer[])
    {
        assert buffer != null :  "Char [] buffer must not be null.";
        try
        {
            int bufferSize = buffer.length;
            for(int charsRead = reader.read(buffer, 0, bufferSize); charsRead > 0; charsRead = reader.read(buffer, 0, bufferSize))
            {
                if(writer != null)
                {
                    writer.write(buffer, 0, charsRead);
                }
            }
        }
        catch(java.io.IOException ioe)
        {
            return false;
        }
        return true;
    }

    public static void initStaticFields (Class clazz)
    {
		try {
			for (Field field : clazz.getDeclaredFields()) {
				if (Modifier.isStatic(field.getModifiers())) {
					Object o = field.get(clazz);
					break;
				}
			}
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
    }


    public static class ObjectCloner
    {
       // so that nobody can accidentally create an ObjectCloner object
       private ObjectCloner(){}
       // returns a deep copy of an object
       static public Object deepCopy(Object oldObj)
         throws IOException
       {
          ObjectOutputStream oos = null;
          ObjectInputStream ois = null;
          try
          {
             ByteArrayOutputStream bos =
                   new ByteArrayOutputStream(); // A
             oos = new ObjectOutputStream(bos); // B
             // serialize and pass the object
             oos.writeObject(oldObj);   // C
             oos.flush();               // D
             ByteArrayInputStream bin =
                   new ByteArrayInputStream(bos.toByteArray()); // E
             ois = new ObjectInputStream(bin);                  // F
             // return the new object
             return ois.readObject(); // G
          }
          catch(Exception e)
          {
             throw new RuntimeException(e);
          }
          finally
          {
             oos.close();
             ois.close();
          }
       }
    }



}
