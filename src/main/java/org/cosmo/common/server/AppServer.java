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
package org.cosmo.common.server;

import static org.jboss.netty.channel.Channels.pipeline;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLEngine;

import org.cosmo.common.build.IPC;
import org.cosmo.common.build.IPC.Signal;
import org.cosmo.common.model.PublicFolder;
import org.cosmo.common.model.User;
import org.cosmo.common.record.Meta;
import org.cosmo.common.record.RecordLog;
import org.cosmo.common.server.WebResourceFiles.Type;
import org.cosmo.common.template.Parser;
import org.cosmo.common.util.ThirdPartyOverride;
import org.cosmo.common.util.Util;
import org.cosmo.common.view.UIRegion;
import org.cosmo.common.xml.FieldValue_Node;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;



public class AppServer {


	public static final boolean TransferEncodingChunking = false;
	public static final boolean Production =  Util.getProperty(Boolean.class, "Production", Boolean.FALSE);
	public static final boolean SSL = Util.getProperty(Boolean.class, "SSL", Boolean.FALSE);
	public static final int Port = Util.getProperty(Integer.class, "Port", 80);
	public static final String ResourceDir = Util.getProperty(String.class, "ResourceDir");
	public static final boolean IPCSignal = Util.getProperty(Boolean.class, "IPCSignal", Boolean.FALSE);
	public static final int MaxThreads = Util.getProperty(Integer.class, "MaxThreads", 50);
	public static final boolean RedisPubSub = Util.getProperty(Boolean.class, "RedisPubSub", Boolean.FALSE);
	
 	public static final ThreadPoolExecutor WorkerPool =  new ThreadPoolExecutor(20, MaxThreads,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());
	
	
	public static ServerPipelineFactory ServerPipelineFactory;


	static {
		WorkerPool.prestartAllCoreThreads();

		FieldValue_Node init = new FieldValue_Node();
		org.cosmo.common.util.Log.jrecord.hashCode();
		org.cosmo.common.util.Log.jrecord.setLevel(java.util.logging.Level.FINE);
		//util.Log.japp.hashCode();
		//util.Log.japp.setLevel(ariba.util.log.Log.DebugLevel);
		//util.Log.jlucene.hashCode();
		//util.Log.jlucene.setLevel(ariba.util.log.Log.DebugLevel);
		//util.Log.jcache.hashCode();
		//util.Log.jcache.setLevel(ariba.util.log.Log.DebugLevel);
		//util.Log.jfavIcon.hashCode();
		//util.Log.jfavIcon.setLevel(ariba.util.log.Log.DebugLevel);
		//util.Log.jfetchRss.hashCode();
		//util.Log.jfetchRss.setLevel(ariba.util.log.Log.DebugLevel);
		
       	// in production mode, we enable template cache, resource cache (gzip), and minify resources
		

	}


	public static void main (String[] args) throws Exception {
		AppServer server = new AppServer();
		server.start(new ServerPipelineFactory(new ServerHandler()), args);
		
	}
	
	
    public void start (ServerPipelineFactory serverPipelineFactory, String[] args) throws Exception {

		// Server Handler Overrides
    	ServerPipelineFactory = serverPipelineFactory;
    	
        if (Production) {
        	AbstractResourceHandler.EnableCache = true;
        	AbstractResourceHandler.EnableMinified = true;
        	//WebResourceFiles.UseCacheResourceDeclaration = true;
        	UIRegion.DebugMode = false;
        }
          	
  

		// set dns cache
		java.security.Security.setProperty("networkaddress.cache.ttl" , "-1");
		java.security.Security.setProperty("networkaddress.cache.negative.ttl" , "30");
        

 

        Parser.parse(new File(ResourceDir, "jwl"), false);
      

/*        
    	ThreadPoolExecutor clientWorkerPool =  new ThreadPoolExecutor(10, 20,
                 0L, TimeUnit.MILLISECONDS,
                 new LinkedBlockingQueue<Runnable>());
    	clientWorkerPool.prestartAllCoreThreads();

    	ThreadPoolExecutor bossWorkerPool =  new ThreadPoolExecutor(5, 10,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
    	bossWorkerPool.prestartAllCoreThreads();
*/


        // Configure the server.
        ServerBootstrap bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(WorkerPool, WorkerPool));

        // Set up the event pipeline factory.
        bootstrap.setPipelineFactory(serverPipelineFactory);

        // Bind and start to accept incoming connections.


        bootstrap.bind(new InetSocketAddress(Port));



        // kick of init




        // XXX remove one time task
        /*
        List<RssSite> sites = RssSite.Meta.store().readAll();
        for (RssSite site : sites) {
        	site.setCategory(Category.Blog);
        	RssSite.Categories.update(site.tx().id(), site._categories);
        }
        */



        /*
        List<RssSite> sites = RssSite.Meta.store().readAll();
        for (RssSite site : sites) {
        	String url = site._url;
        	if (url.startsWith("http://forsale.oodle.com")) {
        		String newURL = "will sub";
        		RssSite.Url.update(site.tx().id(), newURL);
        	}

        }
        */

        /*
        List<RssSite> sites = RssSite.Meta.store().readAll();
        for (RssSite site : sites) {
        	String tags = site._tags;
        	if (tags.equals("Career")) {
        		continue;
        	}
        	if (tags.contains("Career")) {
        		//System.out.println(tags);
        		StringTokens s = StringTokens.on(tags);
        		StringTokens after = s.remove("Career");
        		RssSite.Tags.update(site.tx().id(), after.toString());
        	}
        }
        */


        	// Start LogListeners - only applies to master
        RecordLog.notifyLogIsReady();
        RecordLog.toggleLogConsumers(true);

        	// verfiy all 3rdparth class override
        ThirdPartyOverride.verify(!Production);
        //System.out.println(com.sun.syndication.io.impl.Atom10Parser.class.getMethod("thirdPartyOverride", null).invoke(null, null));

        System.out.println("Server " + Meta.Mode + " started in " + (Production ? "PRODUCTION MODE": "TEST MODE"));

        if (IPCSignal) {
        	IPC.signalProcessStarted(IPC.ProcessType.WebServer, IPCSignalHandler.Instance);
        }
    }

    
    public static class IPCSignalHandler implements IPC.SignalHandler
    {
    	public static final IPCSignalHandler Instance = new IPCSignalHandler();

		public void handle (Signal signal)
		{
			if (Signal.Stop == signal || Signal.Restart == signal)
			{
	    		// do nothing for now
			}
		}

    }


    public static class ServerPipelineFactory implements ChannelPipelineFactory {
        
    	public ServerHandler _serverHandler;
    	
    	public ServerPipelineFactory (ServerHandler serverHandler) {
    		_serverHandler = serverHandler; 
    	}
    	
    	
    	public ChannelPipeline getPipeline() throws Exception {
            // Create a default pipeline implementation.

            ChannelPipeline pipeline = pipeline();

            // Uncomment the following line if you want HTTPS
            if (SSL) {
	            SSLEngine engine = WebSocketSslServerSslContext.getInstance().getServerContext().createSSLEngine();
	            engine.setUseClientMode(false);
	            pipeline.addLast("ssl", new SslHandler(engine));
            }
            
            pipeline.addLast("flashpolicydecoder", new FlashPolicyFileDecoder());
            pipeline.addLast("decoder", new HttpRequestDecoder(8192, 4096, 8192));


            // by adding this - when the request is file uplaod (chunked) it will handle all the collection
            // of data transparently, otherwise, what will happen is the channel will call
            // messageReceived(ChannelHandlerContext ctx, MessageEvent e) repeatly each time it receives
            // data which we would have to handle repeated messageReceivet() with some state instead
            // see changelist  676. overall the HttpChunkAggregator helps that. only problem is the size is
            // static - right now limit to 1 meg
            pipeline.addLast("aggregator", new HttpChunkAggregator(1048576));
            pipeline.addLast("encoder", new HttpResponseEncoder());

            if (AppServer.TransferEncodingChunking) {
                pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
            }
            pipeline.addLast("handler", _serverHandler);
            return pipeline;
        }
    }


}





