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

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.zip.Checksum;
import java.util.zip.Deflater;

import org.cosmo.common.pool.ChecksumPool;
import org.jboss.netty.buffer.AbstractChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;


/*
	ByteArrayInputStream replacement

*/
public class Bytes extends OutputStream implements WritableByteChannel, ReadableByteChannel, ByteSequence
{
	public static final int SIZE128K = 128 * 1024;
	public static final int SIZE64K = 64 * 1024;
	public static final int SIZE32K = 32 * 1024;
	public static final int SIZE16K = 16 * 1024;
	public static final int SIZE8K = 8 * 1024;	
	public static final int SIZE4K = 4 * 1024;
	public static final int SIZE1K = 1024;
	public static final int BufferSize = SIZE4K;
	public static final Bytes Empty = new Bytes(0);

    protected byte[] _bytes;
    protected int _count;


    public static Bytes load (File file)
      throws IOException
    {
    	FileChannel channel = null;
    	try {
	    	channel = new RandomAccessFile(file, "r").getChannel();
	    	Bytes bytes = new Bytes((int)channel.size());
	    	channel.read(ByteBuffer.wrap(bytes._bytes));
	    	bytes._count = bytes._bytes.length;
	    	return bytes;
    	}
    	finally {
    		if (channel != null) {
    			channel.close();
    		}
    	}
    }
    
    public static Bytes load (DataInput input)
      throws IOException
    {
    	Bytes bytes = new Bytes();
    	try {
    		while (true) {
	    		byte b = input.readByte();
	    		bytes.write(b);
    		}
    	}
    	catch (EOFException e) {    		
    	}
    	return bytes.makeExact();  
    }    


    public Bytes()
    {
    	this(BufferSize);
    }

    public Bytes (byte[] bytes)
    {
    	set(bytes);
    }


    public Bytes (byte[]... sources)
    {
    	int totalSize = 0;
    	for (int i = 0 ; i < sources.length; i++) {
    		totalSize += sources[i].length;
    	}
    	_bytes = new byte[totalSize];
    	for (int i = 0 ; i < sources.length; i++) {
    		write(sources[i]);
    	}
    }

    public Bytes (int initializeSize)
    {
        _bytes = new byte[initializeSize];
    }

    @Override
    public void write(int b)
    {
    	write((byte)b);
    }

    public void writeInt (int i)
    {
    	write (BitsUtil.fromInt(i));
    }

    public void writeLong (long l)
    {
    	write (BitsUtil.fromLong(l));
    }

    public void write (byte b)
    {
    	if (_count + 1 > _bytes.length) {
    		_bytes = Arrays.copyOf(_bytes, Math.max(_bytes.length << 1, _count + SIZE1K));
    	}
    	_bytes[_count++] = b;
    }

    @Override
    public void write(byte[] b)
    {
    	write(b, 0, b.length);
    }

    public void write (Bytes bytes)
    {
    	write (bytes._bytes, 0, bytes._count);
    }

    public void write (CharSequence seq)
    {
    	write (Util.bytes(seq));
    }

    @Override
    public void write(byte[] b, int off, int len)
    {
    	/* XXX let's skip the assertions for now
		if ((off < 0) || (off > b.length) || (len < 0) ||
	            ((off + len) > b.length) || ((off + len) < 0)) {
		    throw new IndexOutOfBoundsException();
		}
		else if (len == 0) {
		    return;
		}
		*/
        int newcount = _count + len;
        if (newcount > _bytes.length) {
            _bytes = Arrays.copyOf(_bytes, Math.max(_bytes.length << 1, newcount));
        }
        System.arraycopy(b, off, _bytes, _count, len);
        _count = newcount;
    }

    public boolean isOpen()
    {
    	return true;
    }

    @Override
    public void close() throws IOException
    {
    }


    public int write(ByteBuffer src) throws IOException
    {
    	for (int i = 0, size = src.limit(); i < size; i++) {
    		write(src.get());
    	}

    	return src.limit();
    }
    

    public int read(ByteBuffer src) throws IOException
    {
    	src.put(_bytes, 0, _count);
    	return _count;
    }    


    public InputStream toInputStream ()
    {
    	return new SimpleBytesBufferRead(this);
    }

    public String toString ()
    {
    	if (_count != _bytes.length) {
	    	byte[] bytes = new byte[_count];
	    	System.arraycopy(_bytes, 0, bytes, 0, _count);
	    	return Util.string(bytes);
    	}
    	else {
    		return Util.string(_bytes);
    	}
    }
    
    public long checksum ()
    {
		Checksum checksum = ChecksumPool.Instance.newInstance();
    	try {
			checksum.update(_bytes, 0, _count);
			return checksum.getValue();
    	}
		finally {
			checksum.reset();
		}
    }

    public ChannelBuffer toChannelBuffer ()
    {
    	return ChannelBuffers.wrappedBuffer(_bytes, 0, _count);
    }

    public Bytes makeExact ()
    {
    	if (_count != _bytes.length) {
	    	byte[] exact = new byte[_count];
	    	System.arraycopy(_bytes, 0, exact, 0, _count);
	    	_bytes = exact;
    	}
    	return this;
    }

	public byte[] bytes ()
	{
		return _bytes;
	}

	public void set (byte[] bytes)
	{
    	_bytes = bytes;
    	_count = bytes.length;
	}

	public int count ()
	{
		return _count;
	}

	public Bytes reset ()
	{
		_count = 0;
		return this;
	}

}


	// Same impl as ByteArrayInputStream - length check is replaced by buf._count
class  SimpleBytesBufferRead extends InputStream
{
	Bytes _buf;

    public SimpleBytesBufferRead(Bytes buf)
    {
    	this._buf = buf;
        this.pos = 0;
    }


    /**
     * The index of the next character to read from the input stream buffer.
     * This value should always be nonnegative
     * and not larger than the value of <code>count</code>.
     * The next byte to be read from the input stream buffer
     * will be <code>buf[pos]</code>.
     */
    protected int pos;

    /**
     * The currently marked position in the stream.
     * ByteArrayInputStream objects are marked at position zero by
     * default when constructed.  They may be marked at another
     * position within the buffer by the <code>mark()</code> method.
     * The current buffer position is set to this point by the
     * <code>reset()</code> method.
     * <p>
     * If no mark has been set, then the value of mark is the offset
     * passed to the constructor (or 0 if the offset was not supplied).
     *
     * @since   JDK1.1
     */
    protected int mark = 0;



    /**
     * Reads the next byte of data from this input stream. The value
     * byte is returned as an <code>int</code> in the range
     * <code>0</code> to <code>255</code>. If no byte is available
     * because the end of the stream has been reached, the value
     * <code>-1</code> is returned.
     * <p>
     * This <code>read</code> method
     * cannot block.
     *
     * @return  the next byte of data, or <code>-1</code> if the end of the
     *          stream has been reached.
     */
    public int read() {
	return (pos < _buf._count) ? (_buf._bytes[pos++] & 0xff) : -1;
    }

    /**
     * Reads up to <code>len</code> bytes of data into an array of bytes
     * from this input stream.
     * If <code>pos</code> equals <code>count</code>,
     * then <code>-1</code> is returned to indicate
     * end of file. Otherwise, the  number <code>k</code>
     * of bytes read is equal to the smaller of
     * <code>len</code> and <code>count-pos</code>.
     * If <code>k</code> is positive, then bytes
     * <code>buf[pos]</code> through <code>buf[pos+k-1]</code>
     * are copied into <code>b[off]</code>  through
     * <code>b[off+k-1]</code> in the manner performed
     * by <code>System.arraycopy</code>. The
     * value <code>k</code> is added into <code>pos</code>
     * and <code>k</code> is returned.
     * <p>
     * This <code>read</code> method cannot block.
     *
     * @param   b     the buffer into which the data is read.
     * @param   off   the start offset in the destination array <code>b</code>
     * @param   len   the maximum number of bytes read.
     * @return  the total number of bytes read into the buffer, or
     *          <code>-1</code> if there is no more data because the end of
     *          the stream has been reached.
     * @exception  NullPointerException If <code>b</code> is <code>null</code>.
     * @exception  IndexOutOfBoundsException If <code>off</code> is negative,
     * <code>len</code> is negative, or <code>len</code> is greater than
     * <code>b.length - off</code>
     */
    public int read(byte b[], int off, int len) {
	if (b == null) {
	    throw new NullPointerException();
	} else if (off < 0 || len < 0 || len > b.length - off) {
	    throw new IndexOutOfBoundsException();
	}
	if (pos >= _buf._count) {
	    return -1;
	}
	if (pos + len > _buf._count) {
	    len = _buf._count - pos;
	}
	if (len <= 0) {
	    return 0;
	}
	System.arraycopy(_buf._bytes, pos, b, off, len);
	pos += len;
	return len;
    }

    /**
     * Skips <code>n</code> bytes of input from this input stream. Fewer
     * bytes might be skipped if the end of the input stream is reached.
     * The actual number <code>k</code>
     * of bytes to be skipped is equal to the smaller
     * of <code>n</code> and  <code>count-pos</code>.
     * The value <code>k</code> is added into <code>pos</code>
     * and <code>k</code> is returned.
     *
     * @param   n   the number of bytes to be skipped.
     * @return  the actual number of bytes skipped.
     */
    public long skip(long n) {
	if (pos + n > _buf._count) {
	    n = _buf._count - pos;
	}
	if (n < 0) {
	    return 0;
	}
	pos += n;
	return n;
    }

    /**
     * Returns the number of remaining bytes that can be read (or skipped over)
     * from this input stream.
     * <p>
     * The value returned is <code>count&nbsp;- pos</code>,
     * which is the number of bytes remaining to be read from the input buffer.
     *
     * @return  the number of remaining bytes that can be read (or skipped
     *          over) from this input stream without blocking.
     */
    public int available() {
	return _buf._count - pos;
    }

    /**
     * Tests if this <code>InputStream</code> supports mark/reset. The
     * <code>markSupported</code> method of <code>ByteArrayInputStream</code>
     * always returns <code>true</code>.
     *
     * @since   JDK1.1
     */
    public boolean markSupported() {
	return true;
    }

    /**
     * Set the current marked position in the stream.
     * ByteArrayInputStream objects are marked at position zero by
     * default when constructed.  They may be marked at another
     * position within the buffer by this method.
     * <p>
     * If no mark has been set, then the value of the mark is the
     * offset passed to the constructor (or 0 if the offset was not
     * supplied).
     *
     * <p> Note: The <code>readAheadLimit</code> for this class
     *  has no meaning.
     *
     * @since   JDK1.1
     */
    public void mark(int readAheadLimit) {
	mark = pos;
    }

    /**
     * Resets the buffer to the marked position.  The marked position
     * is 0 unless another position was marked or an offset was specified
     * in the constructor.
     */
    public void reset() {
	pos = mark;
    }

    /**
     * Closing a <tt>ByteArrayInputStream</tt> has no effect. The methods in
     * this class can be called after the stream has been closed without
     * generating an <tt>IOException</tt>.
     * <p>
     */
    public void close() throws IOException {
    }
}




