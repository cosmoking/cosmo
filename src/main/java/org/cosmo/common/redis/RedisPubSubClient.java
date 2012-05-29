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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.cosmo.common.statistics.Clock;
import org.jboss.netty.handler.codec.redis.Command;
import org.jboss.netty.handler.codec.redis.MultiBulkReply;
import org.jboss.netty.handler.codec.redis.Reply;


public class RedisPubSubClient {
	
	static final String Channel = "redis";
	
	final RedisClient _publisher;
	final RedisClient _subscriber;
	final AtomicBoolean _isReady = new AtomicBoolean(false);
	
	public RedisPubSubClient (ThreadPoolExecutor workerPool, RedisClient.RedisInboundProcessor inboundProcessor)
	  throws Exception
	{		
		_publisher = new RedisClient(workerPool, new RedisClient.RedisInboundProcessor(AckRedisReplyCallback.Instance));
		_subscriber = new RedisClient(workerPool, inboundProcessor);
		
			// "subscriber" client needs to issue subscribe command first - so set the initial callback to AckRedisReplyCallBack
			// after succesfull switch back to original callback
		RedisReplyCallback orginalCallback = inboundProcessor.getCallback();
		RedisReplyCallback subscribeCallback = new RedisReplyCallback.Generic<RedisClient.RedisInboundProcessor, RedisReplyCallback>(inboundProcessor, orginalCallback) {
			public void callback (Reply reply) {
				_arg1.setCallback(_arg2); // inboundProcessor.setCallback(orginalCallback);
				_isReady.set(true);

			}
		};
		inboundProcessor.setCallback(subscribeCallback);
		_subscriber.exec(new Command("subscribe", Channel), subscribeCallback);
		
		while (!_isReady.get()) {
			Thread.sleep(500); // XXX log it instead
			System.out.println("Waiting for redis subscribe");
		}
		
	}
	
	
	public static void main (String[] args) throws Exception
	{
		final ThreadPoolExecutor WorkerPool =  new ThreadPoolExecutor(20, 50,
	            0L, TimeUnit.MILLISECONDS,
	            new LinkedBlockingQueue<Runnable>());		
		
		RedisPubSubClient redisPubSub = new RedisPubSubClient(WorkerPool, new RedisClient.RedisInboundProcessor(new SubscribeRedisReplyCallback()));
		int counts = 10;
		
		for (int i = 0; i < counts; i++) {
			redisPubSub.publish(i + " message");	
		}
		
		
		Thread.sleep(Long.MAX_VALUE);
	}
	
	
		// XXX optmize this! no way we are going to create new command each time
	public void publish (String message)
	{
		_publisher.exec (new Command("publish", Channel, message), AckRedisReplyCallback.Instance);
	}
	
}



class AckRedisReplyCallback extends RedisReplyCallback
{
	
	static final AckRedisReplyCallback Instance = new AckRedisReplyCallback();
	
	@Override
	public void callback (Reply reply)
	{
		// do nothing as it's just an ack		
	}
}

class SubscribeRedisReplyCallback extends RedisReplyCallback
{
	

	
	public void callback (Reply reply)
	{
		if (reply instanceof MultiBulkReply) {
			//System.out.println(message.getClass().getSimpleName() + Thread.currentThread());
			
			//System.out.println(new String((byte[])reply.byteArrays[2]));
			System.out.println(reply);
			
			Clock.timer().markAndCheckRunning(System.out);
		}
		else {
			System.out.println(reply);
		}	
		
	}
}

