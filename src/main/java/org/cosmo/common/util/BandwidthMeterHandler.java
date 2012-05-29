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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import java.util.concurrent.atomic.AtomicLong;

public class BandwidthMeterHandler extends SimpleChannelHandler {
    private AtomicLong bytesSent = new AtomicLong();
    private AtomicLong bytesReceived = new AtomicLong();

    @Override
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {

        if (e instanceof MessageEvent && ((MessageEvent)e).getMessage() instanceof ChannelBuffer) {
            ChannelBuffer b = (ChannelBuffer)((MessageEvent)e).getMessage();
            bytesReceived.addAndGet(b.readableBytes());
        }

        super.handleUpstream(ctx, e);
     }

    @Override
    public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        if (e instanceof MessageEvent && ((MessageEvent)e).getMessage() instanceof ChannelBuffer) {
            ChannelBuffer b = (ChannelBuffer)((MessageEvent)e).getMessage();
            bytesSent.addAndGet(b.readableBytes());
        }

        super.handleDownstream(ctx, e);
    }

    public void reset() {
        bytesSent.set(0);
        bytesReceived.set(0);
    }


    public long getBytesSent() {
        return bytesSent.get();
    }

    public long getBytesReceived() {
        return bytesReceived.get();
    }
}
