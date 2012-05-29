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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.jboss.netty.util.CharsetUtil;

import java.util.concurrent.Executor;

/**
 * Checks the received {@link org.jboss.netty.buffer.ChannelBuffer
 * ChannelBuffer}s for Flash policy file requests.
 * <p/>
 * <p>
 * If this decoder detects a Flash policy file request it adds a
 * {@link FlashPolicyFileHandler} to the
 * {@link org.jboss.netty.channel.ChannelPipeline ChannelPipeline} and removes
 * itself from the pipeline. If a Flash policy file request is not detected in
 * the first 23 bytes of the buffer, the decoder removes itself from the
 * pipeline.
 * <p>
 * <p/>
 * <p>
 * This implementation is based on the
 * "replacing a decoder with another decoder in a pipeline" section of the
 * {@link org.jboss.netty.handler.codec.frame.FrameDecoder FrameDecoder}
 * documentation.
 * </p>
 */
public class FlashPolicyFileDecoder extends FrameDecoder {
    private static final ChannelBuffer FLASH_POLICY_REQUEST = ChannelBuffers
            .copiedBuffer("<policy-file-request/>\0", CharsetUtil.US_ASCII);


    public FlashPolicyFileDecoder() {
        super(true);

    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {

        // Will use the first 23 bytes to detect the policy file request.
        if (buffer.readableBytes() >= 23) {
            ChannelPipeline p = ctx.getPipeline();
            ChannelBuffer firstMessage = buffer.readBytes(23);

            //System.err.println(">> " + new String(firstMessage.array()));
            
            if (FLASH_POLICY_REQUEST.equals(firstMessage)) {
                p.addAfter("flashpolicydecoder", "flashpolicyhandler",
                        new FlashPolicyFileHandler());
            }

            p.remove(this);

            if (buffer.readable()) {
                return new Object[]{firstMessage, buffer.readBytes(buffer.readableBytes())};
            } else {
                return firstMessage;
            }

        }

        // Forward the current buffer as is to handlers.
        return buffer.readBytes(buffer.readableBytes());

    }

}
