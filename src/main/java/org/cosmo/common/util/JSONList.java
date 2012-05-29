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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jboss.netty.buffer.ChannelBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONString;

import org.cosmo.common.util.Bytes;
import org.cosmo.common.util.DeflatableBytes;
import org.cosmo.common.util.Constants;
import org.cosmo.common.util.Util;

public class JSONList extends JSONArray {
	
	// override methods
    public static void thirdPartyOverride ()
    {
    }	
	
	public JSONList (Object[] array)
	{
		this.myArrayList = Arrays.asList(array);
	}
	
	
	public JSONList (List list)
	{
		this.myArrayList = list;
	}

    public static String longArrayString (long[] array) throws JSONException, IOException
    {
    	return longArrayString(array, 0, array.length, true);
    }
    
	public static String longArrayString (long[] array, int begin, int end, boolean includeBrackets) throws JSONException, IOException
    {
    	StringBuffer sb = new StringBuffer();
        for (int i = begin; i < end; i++) {
        	if (i > begin) {
        		sb.append(',');
        	}
            sb.append(Long.toString(array[i], 10));
        }
    	return includeBrackets ? "[" + sb.toString() + "]" : sb.toString();
    }

    public void writeBytes (ChannelBuffer out) throws JSONException, IOException
    {
    	out.writeBytes(Constants.LeftSquareBytes);
        for (int i = 0, size = length(); i < size; i++) {
        	if (i > 0) {
        		out.writeBytes(Constants.CommaBytes);
        	}
            Object v = this.myArrayList.get(i);
            out.writeBytes(valueToBytes(v));
        }
        out.writeBytes(Constants.RightSquareBytes);
    }	
    
	
    public void writeBytes (Bytes out) throws JSONException, IOException
    {
    	out.write(Constants.LeftSquareBytes);
        for (int i = 0, size = length(); i < size; i++) {
        	if (i > 0) {
        		out.write(Constants.CommaBytes);
        	}
            Object v = this.myArrayList.get(i);
            out.write(valueToBytes(v));
        }
        out.write(Constants.RightSquareBytes);
    }	

    public Bytes getBytes () throws JSONException, IOException
    {
    	Bytes bytes = new Bytes();
    	writeBytes(bytes);
    	return bytes;
    }	    
    
	// copy of JSONObject.valueToString except if value is bytes[] we return right away
    public static byte[] valueToBytes(Object value) throws JSONException, IOException {
        if (value == null || value.equals(null)) {
            return Constants.NullByteMarker;
        }
        	// return raw bytes is value is byte[] - else use normal json string conversion
        if (value instanceof byte[]) {
        	return (byte[])value;
        }
        
        if (value instanceof long[]) {
        	return Util.UTF8(longArrayString((long[])value));
        }
        
        if (value instanceof JSONString) {
            Object o;
            try {
                o = ((JSONString)value).toJSONString();
            } catch (Exception e) {
                throw new JSONException(e);
            }
            if (o instanceof String) {
                return Util.UTF8(o);
            }
            throw new JSONException("Bad value from toJSONString: " + o);
        }
        if (value instanceof Number) {
            return Util.UTF8(JSONObject.numberToString((Number) value));
        }
        if (value instanceof Boolean || value instanceof JSONObject ||
                value instanceof JSONArray) {
            return Util.UTF8(value);
        }
        if (value instanceof Map) {
            return Util.UTF8(new JSONObject((Map)value));
        }
        if (value instanceof JSONList) {
            return ((JSONList)value).getBytes().makeExact().bytes();
        }
        if (value instanceof List) {
            return new JSONList((List)value).getBytes().makeExact().bytes();
        }
        if (value instanceof Collection) {
            return Util.UTF8(new JSONArray((Collection)value));
        }
        if (value.getClass().isArray()) {
            return Util.UTF8(new JSONArray(value));
        }
        return Util.UTF8(JSONObject.quote(value.toString()));
    }      
    
}
