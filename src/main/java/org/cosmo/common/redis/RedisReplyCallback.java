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
package org.cosmo.common.redis;

import org.jboss.netty.handler.codec.redis.ErrorReply;
import org.jboss.netty.handler.codec.redis.Reply;

public abstract class RedisReplyCallback {
	
	public static final RedisReplyCallback Ack = new RedisReplyCallback () {		
		public void callback (Reply reply) {
			// do nothing
		}
	};
		
	
	final void handle (Reply reply)
	{
		if (reply instanceof ErrorReply) {
			throw new IllegalArgumentException("ERROR" + reply.toString());
		}
		else {
			callback(reply);
		}
	}
	
	abstract public void callback (Reply reply);

	
		// provide an generic for adhoc anonymous class
	public static abstract class Generic<A, B> extends RedisReplyCallback
	{
		A _arg1;
		B _arg2;
		
		public Generic (A arg1, B arg2) {
			_arg1 = arg1;
			_arg2 = arg2;
		}
		
		abstract public void callback (Reply reply);

	}
}
