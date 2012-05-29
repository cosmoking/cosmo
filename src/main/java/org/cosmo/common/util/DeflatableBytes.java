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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.zip.Deflater;

import org.cosmo.common.pool.DeflaterPool;
import org.cosmo.common.pool.Pool;

import org.jboss.netty.buffer.AbstractChannelBuffer;

/*
 * 	A byte buffer that will automatically compresses the data when it
 *  reaches beyond the limit size which is 16KB now
 *  Not thread safe
 */
public class DeflatableBytes extends Bytes
{
	public static final int DataUncompressedLimit = 4 * BufferSize;
	public static final DeflatableBytes Empty = new DeflatableBytes();

    public boolean _compressed;
    private Deflater _deflater;
    private byte[] _buf;


    public static DeflatableBytes load (File file)
      throws IOException
    {
	  	DeflatableBytes deflatableBytes = new DeflatableBytes(0);
	  	deflatableBytes.write(Bytes.load(file)._bytes);
	  	deflatableBytes.end();
	  	return deflatableBytes;
    }

    public DeflatableBytes()
    {
        _bytes = new byte[DataUncompressedLimit];
    }

    public DeflatableBytes(int dataUncompressedLimit)
    {
        _bytes = new byte[dataUncompressedLimit];
    }


    @Override
    public void write(byte[] b, int off, int len)
    {

    		// if haven't reached the limit put into regular buffer
    	if (!_compressed && _count + len < _bytes.length) {
            System.arraycopy(b, off, _bytes, _count, len);
            _count += len;
            return;
    	}
    		// otherwise, we will compress the written bytes plus the new bytes
    	else {
    		_compressed = true;
    			// if data has been written(count > 0) and this is the first time (_buf == null)
    			// will ensure this to execute once only
    		if (_count > 0 && _buf == null) {
	    		int size = _count;
	    		_count = 0;
	    		//byte[] currentBytes = _bytes.clone();
	    		//write(currentBytes, 0, currentCount);
	    		write(_bytes, 0, size);
    		}
    	}

    		// lazy init
    	if (_buf == null) {
    		_buf = new byte[BufferSize];
    	}
    	if (_deflater == null) {
    		//_deflater = new Deflater();
    		_deflater = DeflaterPool.Instance.getInstance();
    	}


    		// below is exact replicate from DeflaterOutputStream.write()
		if (_deflater.finished()) {
		    throw new RuntimeException("write beyond end of stream");
		}
        if ((off | len | (off + len) | (b.length - (off + len))) < 0) {
        	throw new IndexOutOfBoundsException();
		}
        else if (len == 0) {
		    return;
		}
		if (!_deflater.finished()) {
            // Deflate no more than stride bytes at a time.  This avoids
            // excess copying in deflateBytes (see Deflater.c)
            int stride = _buf.length;
            for (int i = 0; i < len; i+= stride) {
                _deflater.setInput(b, off + i, Math.min(stride, len - i));
                while (!_deflater.needsInput()) {
                    deflate();
                }
            }
		}
    }


    private void deflate() {
		int len = _deflater.deflate(_buf, 0, _buf.length);
		if (len > 0) {
		    super.write(_buf, 0, len);
		}
    }



    public DeflatableBytes end()
    {
    	if (_compressed) {
	        finish();
    	}
    	return this;
    }


    private void finish() {
		if (!_deflater.finished()) {
		    _deflater.finish();
		    while (!_deflater.finished()) {
		    	deflate();
		    }
		}
    }
}





