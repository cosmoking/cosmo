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
package org.cosmo.common.file;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;

import org.cosmo.common.util.Bytes;


public class MetaBlob<T> extends OutputStream
{
	private static final byte[] MetaHeaderMagic = "MetaHeaderMagic".getBytes();
	private static final byte[] MetaBlobMagic = "MetaBlobMagic".getBytes();
	
	
	private FileChannel _channelHandle;
	private RandomAccessFile _fileHandle;
	private File _file;
	
	public T _meta;
	public int _metaSpace = Bytes.SIZE16K;
	public int _metaSize;
	public long _metaChecksum = -1;
	public long _offset;
	
	
	public enum Mode {
		Write,
		Read,		
	}
		
	

	public MetaBlob (File file, T meta, Mode mode)
	  throws Exception
	{
		_file = file;
		
		System.out.println("XXX"  + file.getAbsolutePath());
		
		if (!file.exists()) {
			
			file.getAbsoluteFile().getParentFile().mkdirs();
			_fileHandle = new RandomAccessFile(file, "rw");
			_channelHandle = _fileHandle.getChannel();
			_meta = meta;
			commitMeta();
			if (_fileHandle.getFilePointer() < _metaSpace) {
				_fileHandle.setLength(_metaSpace);
				//int fillerSize = _metaSpace - (int)_fileHandle.getFilePointer();
				//for (int i = 0; i < fillerSize; i++) {
				//	_fileHandle.write("\n".getBytes());
				//}
			}
			else {
				throw new IllegalStateException("Header size greater than allocated Size");
			}
		}
		else {
			_fileHandle = new RandomAccessFile(file, "rw");
			_channelHandle = _fileHandle.getChannel();	
			loadMeta();			
		}				
		_fileHandle.seek(_fileHandle.length());

	}
	
	
	public void delete ()
	  throws IOException
	{
		_fileHandle.setLength(0);
		_fileHandle.close();
		if (!_file.delete()) {
			throw new IOException("Unable to delete file" + _file.getAbsolutePath());
		}
	}
	
	
	private void loadMeta ()
	  throws IOException, ClassNotFoundException
	{
		_fileHandle.seek(0);
		_metaSize = _fileHandle.readInt();			
		Bytes bytes = new Bytes(new byte[_metaSize]);
		_fileHandle.read(bytes.bytes());
		ObjectInputStream oin = new ObjectInputStream(bytes.toInputStream());
		_meta = (T)oin.readObject();
	}
	

	public synchronized void commitMeta ()
	  throws IOException 
	{
		Bytes metaBytes = metaBytes();
		long newMetaChecksum = metaChecksum(metaBytes);
		if (newMetaChecksum != _metaChecksum) {
			_fileHandle.seek(0);
			_fileHandle.writeInt(metaBytes.count());
			_fileHandle.write(metaBytes.bytes(), 0, metaBytes.count());
			_metaChecksum = newMetaChecksum;
		}
	}
	
	
	private Bytes metaBytes ()
	  throws IOException
	{
		Bytes bytes = new Bytes();
		ObjectOutputStream out = new ObjectOutputStream(bytes);
		out.writeObject(_meta);
		
		
		
		return bytes;
	}
	
		

	
	private long metaChecksum (Bytes metaBytes)
	  throws IOException
	{
		return metaBytes.checksum();
	}
	
	
	
    @Override
    public void write(int b)
      throws IOException
    {
    	write((byte)b);
    }


    public void write (byte b)
      throws IOException
    {
    	_fileHandle.write(b);
    }

    @Override
    public void write(byte[] b)
      throws IOException
    {
    	write(b, 0, b.length);
    }


    @Override
    public void write(byte[] b, int off, int len)
      throws IOException
    {
    	//System.out.println("POS" + _fileHandle.getFilePointer());
    	_fileHandle.write(b, off, len);
    }	
	
	
    @Override
	public void close ()  
	  throws IOException
	{
    	commitMeta();
    	_fileHandle.close();
	}
	
    
    public Bytes read ()
      throws IOException
    {
    	int size = (int)_fileHandle.length() - (int)_metaSpace;
    	if (size > 0) {
	    	Bytes bytes = new Bytes(size);
	    	_channelHandle.transferTo(_metaSpace, size, bytes);
	    	return bytes;
    	}
    	else {
    		return Bytes.Empty;
    	}
	}
	
	
	
	
	public static void main (String[] args) throws Exception
	{
		File file = new File("metablob.file");
		System.out.println(file.getAbsolutePath());
		
		ArrayList data = new ArrayList(Arrays.asList(new int[]{1,2,3}));
		
		MetaBlob<ArrayList> metaBlob =  new MetaBlob(file,ArrayList.class, Mode.Write);
		
		metaBlob.write(new String("HelloWorld").getBytes());
		metaBlob._meta.addAll(data);
		metaBlob.commitMeta();
		
		
		MetaBlob<ArrayList> metaBlob2 = new MetaBlob(file,ArrayList.class, Mode.Write);
		
		
		Bytes bytes = metaBlob2.read();
		System.out.println(bytes.toString());
		
		
		
		
	}
	
}

class Meta {
	
	
}
