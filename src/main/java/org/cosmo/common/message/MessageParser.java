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
package org.cosmo.common.message;

import org.cosmo.common.net.StringTokens;
import org.cosmo.common.util.Util;


/*
 *  Does 2 things:
 *  
 *  - parses raw byte data into messages
 *  
 *  - parses message into  "type","topic","uid","payload"
 * 
 */
public interface MessageParser<M>
{
	public static MessageParser Default = new StringMessageParser();
	
	public M bytesToMessage (byte[] rawMessage);
		
	public M[] bytesToMessages (byte[] rawMessage);
	
	public byte[] rawMessageBytesEntryMarker ();
	
	public StringTokens messageToTokens(M message);

		
}


class StringMessageParser implements MessageParser<String>
{
	
	static final String MessageBoundaryMarker = "MessageBoundaryMarker!"; //for now
	static final byte[] MessageBoundaryMarkerBytes = MessageBoundaryMarker.getBytes(); // for now	
	static final String[] EmptyMessages = new String[]{};
	
	 	
	public String[] bytesToMessages (byte[] rawMessageBytes)
	{
		
		String historyString = Util.string(rawMessageBytes);
		if (historyString.length() > 0) {
			if (historyString.endsWith(MessageBoundaryMarker)) {
				historyString = historyString.substring(0, historyString.length() - MessageBoundaryMarker.length());
				return historyString.split(MessageBoundaryMarker);		
			}	
			else {
				throw new IllegalStateException("Message Corrupted");
			}
		}
		return EmptyMessages;
	}
	
	public String bytesToMessage (byte[] rawMessage)
	{
		return Util.string(rawMessage);
	}
	
	
	public byte[] rawMessageBytesEntryMarker ()
	{
		return MessageBoundaryMarkerBytes;
	}
	
	public StringTokens messageToTokens(String message)
	{
		return StringTokens.on(message, StringTokens.NewlineSeparatorChar); 
	}
	
	
}
