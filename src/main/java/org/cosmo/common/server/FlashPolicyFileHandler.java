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

import org.cosmo.common.server.ServerHandler;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.util.CharsetUtil;

import java.util.concurrent.Executor;

/**
 * Responds with a Flash socket policy file.
 * <p/>
 * <p>
 * This implementation is based on the
 * <a href="https://github.com/waywardmonkeys/netty-flash-crossdomain-policy-server"
 * >waywardmonkeys/netty-flash-crossdomain-policy-server</a> project and the
 * <a href="http://www.adobe.com/devnet/flashplayer/articles/socket_policy_files.html"
 * ><em>Setting up a socket policy file server</em></a> article.
 * </p>
 */

public class FlashPolicyFileHandler extends ServerHandler {

    private ChannelBuffer FLASH_POLICY_RESPONSE = ChannelBuffers
            .copiedBuffer("<?xml version=\"1.0\"?>\r\n"
                    + "<!DOCTYPE cross-domain-policy SYSTEM \"/xml/dtds/cross-domain-policy.dtd\">\r\n"
                    + "<cross-domain-policy>\r\n"
                    + "  <site-control permitted-cross-domain-policies=\"master-only\"/>\r\n" 
                    + "  <allow-access-from domain=\"*\" to-ports=\"*\" />\r\n"
                    + "</cross-domain-policy>\r\n", CharsetUtil.US_ASCII);

  	
    public FlashPolicyFileHandler() {
    	super();
    }


    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    	System.err.println("XXX Returned POLICY!");
        Channel ch = e.getChannel();
        ChannelBuffer response = getPolicyFileContents();
        ChannelFuture future = ch.write(response);
        future.addListener(ChannelFutureListener.CLOSE);
        ctx.getPipeline().remove((ChannelHandler)this);
    }

    private ChannelBuffer getPolicyFileContents() throws Exception {

        return ChannelBuffers
                .copiedBuffer("<?xml version=\"1.0\"?>\r\n"
                        + "<!DOCTYPE cross-domain-policy SYSTEM \"/xml/dtds/cross-domain-policy.dtd\">\r\n"
                        + "<cross-domain-policy>\r\n"
                        + "  <site-control permitted-cross-domain-policies=\"master-only\"/>\r\n" 
                        + "  <allow-access-from domain=\"*\" to-ports=\"*\" />\r\n"
                        + "</cross-domain-policy>\r\n", CharsetUtil.US_ASCII);
    }


}
