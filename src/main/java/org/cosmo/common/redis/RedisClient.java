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


import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.cosmo.common.statistics.Clock;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.redis.Command;
import org.jboss.netty.handler.codec.redis.RedisDecoder;
import org.jboss.netty.handler.codec.redis.RedisEncoder;
import org.jboss.netty.handler.codec.redis.Reply;


public class RedisClient {
    
			
	final Channel _channel;
	RedisInboundProcessor _redisInboundProcessor;
	
	
	public RedisClient (ThreadPoolExecutor workerPool, RedisInboundProcessor inboundProcessor)
	  throws Exception
	{		
		ClientBootstrap cb = new ClientBootstrap(new NioClientSocketChannelFactory(workerPool, workerPool));
	    _redisInboundProcessor = inboundProcessor;
	        cb.setPipelineFactory(new ChannelPipelineFactory() {
	            public ChannelPipeline getPipeline() throws Exception {
	                ChannelPipeline pipeline = Channels.pipeline();
	                pipeline.addLast("redisEncoder", new RedisEncoder());
	                pipeline.addLast("redisDecoder", new RedisDecoder());
	                pipeline.addLast("redisHandler", _redisInboundProcessor);
	                return pipeline;
	            }
	        });	        
	    ChannelFuture redis = cb.connect(new InetSocketAddress("localhost", 6379));
	    while (!redis.isSuccess()) {
	    	System.out.println("Connecting to Redis.."); // log this instead
	    	Thread.sleep(1000);
	    }
	    // can't call below as it uses the same workerPool from Netty IO - will deadlock - hence use for loop for now
	    //redis.await().rethrowIfFailed();
	    _channel = redis.getChannel();	        
	}
	
	
		// XXX need to ensure the ordering of the exec!
	public synchronized void exec (Command command, RedisReplyCallback redisReplyCallback)
	{
		_redisInboundProcessor.addCallback(redisReplyCallback);
		_channel.write(command);
	}
		
	
	public static class RedisInboundProcessorQueue extends RedisInboundProcessor
	{
		
		final BlockingQueue<RedisReplyCallback> _redisReplyCallbackQueue;	
		

		public RedisInboundProcessorQueue ()
		{
			_redisReplyCallbackQueue = new LinkedBlockingQueue();	
		}
			
		@Override
		public void addCallback (RedisReplyCallback callback)
		{
			_redisReplyCallbackQueue.add(callback);
		}
		
		@Override		
		public RedisReplyCallback getCallback ()
		{
			return _redisReplyCallbackQueue.remove();
		}
				
		
	    @Override
	    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
	       throws Exception 
	    {
	    	    	  	    	
	    	RedisReplyCallback redisReplyCallback = getCallback();
	    	Object message = e.getMessage();    	
	    	redisReplyCallback.handle((Reply)message);
	    }	
	}


	public static class RedisInboundProcessor extends SimpleChannelUpstreamHandler
	{
		
		RedisReplyCallback _redisReplyCallback;
		
		
		public RedisInboundProcessor ()
		{			
		}
		
		public RedisInboundProcessor (RedisReplyCallback redisReplyCallback)
		{
			setCallback(redisReplyCallback);
		}
		
		public void setCallback (RedisReplyCallback redisReplyCallback)
		{
			_redisReplyCallback = redisReplyCallback;
		}
		
		
		public void addCallback (RedisReplyCallback callback)
		{
			// noop as it always uses the same callback - useful for synchronized redis call which uses Queuebased call back
			// in that regards it would store callback in a queue
		}
				
		public RedisReplyCallback getCallback ()
		{
			// always return the same callback
			return _redisReplyCallback;
		}
		
		
	    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
	       throws Exception 
	    {	    	    	  	    	
	    	Object message = e.getMessage();    	
	    	_redisReplyCallback.handle((Reply)message);
	    }			

	}	
	
	
	
	public static class TestRedisReplyCallback extends RedisReplyCallback
	{
		public AtomicInteger replyCounts = new AtomicInteger(0);
		public int _exitAfterReplyCounts;
		
		
		public TestRedisReplyCallback (int exitAfterReplyCounts)
		{
			_exitAfterReplyCounts = exitAfterReplyCounts;
		}
		
		public void callback (Reply reply)
		{
			if (replyCounts.incrementAndGet() % 1000 == 0) {
				Clock.timer().markAndCheckRunning(System.out);
				System.out.println(replyCounts.get());
			}
			
			if (replyCounts.get() > this._exitAfterReplyCounts) {
				System.exit(1);
			}
		}
	}
	
	
	
	public static void main (String[] args) throws Exception
	{
		final ThreadPoolExecutor WorkerPool =  new ThreadPoolExecutor(20, 50,
	            0L, TimeUnit.MILLISECONDS,
	            new LinkedBlockingQueue<Runnable>());
				
	    final byte[] VALUE = "value".getBytes();
 
		
		int CALLS = 100000;
		RedisClient redis = new RedisClient(WorkerPool, new RedisInboundProcessorQueue());

		TestRedisReplyCallback testRedisReplyCallbacknew = new TestRedisReplyCallback(CALLS);
		
		Clock.timer().markAndCheckRunning(System.out);
		
		byte[] SET_BYTES = "SET".getBytes();
        for (int i = 0; i < CALLS; i++) {
            redis.exec(new Command(SET_BYTES, String.valueOf(i).getBytes(), VALUE), testRedisReplyCallbacknew);
        }		
		
	
        Thread.sleep(Long.MAX_VALUE);
        
	}	
}





