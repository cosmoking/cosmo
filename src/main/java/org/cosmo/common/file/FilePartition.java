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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.cosmo.common.record.Defn;
import org.cosmo.common.util.Bytes;
import org.cosmo.common.util.New;
import org.cosmo.common.util.Util;


abstract public class FilePartition 
{

	private static String WritePermission = "rw"; //"rws";
	private static final String ReadPermission = "r";
	File _readFile;
	File _writeFile;
	long _sizePerFile;	// MUST BE long  - otherwise ie (_sizePerFile * i) is capped at 2^32 but could go beyond
	volatile FileChannel[] _readChannels;
	volatile FileChannel[] _writeChannels;

	// write files are lazy init - so calling
	// readFile and writeFile can be different which allows application can use one "instance" to deal with read and write
	// ie, in recordStore, the slave are reading from master, but writing to it's own place
	// it's perfectly fine to have readFile and writeFile of the same file
	FilePartition (File readFile, File writeFile, int approxSizePerFile, int fixedRecordSizeInFile)
	  throws FileNotFoundException, IOException
	{
		
		
			
			// init only the first file only - as it will grow ondemand later
		_readFile = readFile;
		_writeFile = writeFile;
		_writeFile.getParentFile().mkdirs();
		_writeChannels = new FileChannel[1];
		_writeChannels[0] = new RandomAccessFile(writeFile, WritePermission).getChannel();
		_writeChannels[0].position(_writeChannels[0].size());
		_readChannels = new FileChannel[1];
		_readChannels[0] = new RandomAccessFile(readFile, ReadPermission).getChannel();
		_sizePerFile = approxSizePerFile - (approxSizePerFile % fixedRecordSizeInFile);

			// initialize and open all files for reading
			// Note, files for write are opened on demand
		try {
			for (long i = 1, tryPos = _sizePerFile; true; i++, tryPos = _sizePerFile * i) {
				chunkIdForRead(tryPos, 1);
			}
		}
		catch (FileNotFoundException e) {
			// have opened to the end
		}

	}


	public long sizePerFile ()
	{
		return _sizePerFile;
	}


		// return the total file size - read files are all opened during instance creation
    public long size() throws IOException
    {
    		// (1) between calls _readChannels could change due to concurrency but it's fine as this it's grow only
    	int i = _readChannels.length - 1;
    	long size = _readChannels[i].size() + (_sizePerFile * i);
    	return size;
    }



	public ByteBuffer read (int size, long position) throws IOException
	{
		ByteBuffer buf = ByteBuffer.allocate(size);
		read(buf, position);
		return buf;
	}

	/*
	 * Math.max(0, position - (_sizePerFile * chunkId))
	 * position - (_sizePerFile * chunkId) is negative when advancing to the next chunk
	 * ie. (chunk0,size500) (chunk1,size500) (chunk2,size500) and write is pos 900 + size 200
	 *
	 * chunkId = 2 and pos = 900 - 1000 = -100
	 * hence use Max.max() to reset to 0  when this read/write goes on chunk 2
	 * result 2, 0, 200
	 *
	 */
	public int read(ByteBuffer dst, long position) throws IOException
	{

		if (dst.limit() > _sizePerFile) {
			throw new IOException(New.str("Use readFull() to read ", dst.limit(), " which is bigger than size per file ", _sizePerFile));
		}

		int chunkId = chunkIdForRead(position, dst.limit());
		int read = _readChannels[chunkId].read(dst, Math.max(0, position - (_sizePerFile * chunkId)));
		dst.rewind();
		return read;
	}

	private int index (long position, int size)
	{
    	long length = position + size;
    	int index = (int)(length / _sizePerFile);
    		// if exact size
    	if (index > 0 && (int)(length % _sizePerFile) == 0) {
    		index = Math.max(0, index - 1);
    	}
    	return index;
	}

    int chunkIdForRead(long position, int size)
      throws FileNotFoundException
    {
		int index = index(position, size);
		int readChannelsLength = _readChannels.length; //(1) see above
		if (index > readChannelsLength - 1) {
			FileChannel[] readChannels = new FileChannel[index + 1];
				// copy all existing readChannels if exist
			for (int i = 0; i <= index; i++) {
				readChannels[i] = i < readChannelsLength ? _readChannels[i] : null;
			}
				// lazy init
			if (readChannels[index] == null) {
				readChannels[index] = new RandomAccessFile(Util.addSuffixToFile(_readFile, index), ReadPermission).getChannel();
			}
			_readChannels = readChannels;
		}
		return index;
	}


    synchronized int chunkIdForWrite (long position, int size)
      throws FileNotFoundException, IOException
    {
    	int index = index(position, size);

		int writeChannelsLength = _writeChannels.length;
 		if (index > writeChannelsLength - 1) {
			FileChannel[] writeChannels = new FileChannel[index + 1];
				// copy all existing writeChannels if exist
			for (int i = 0; i <= index; i++) {
				writeChannels[i] = i < writeChannelsLength ? _writeChannels[i] : null;
			}
				// lazy init
			if (writeChannels[index] == null) {
				writeChannels[index] = new RandomAccessFile(Util.addSuffixToFile(_writeFile, index), WritePermission).getChannel();
				writeChannels[index].position(writeChannels[index].size());
			}
			_writeChannels = writeChannels;

 				// XXX important so that read and file handles are in sync!! so that methods like size() will work
 			chunkIdForRead(position, size);
 		}
 		return index;
    }




    public synchronized void write (ByteBuffer buf, long position) throws IOException
    {
		int chunkId = chunkIdForWrite(position, buf.limit());
		_writeChannels[chunkId].write(buf, Math.max(0, position - (_sizePerFile * chunkId)));
    }


    	// refine this later for the try - finally close crap
    public void close() throws IOException
    {
    	closeWrite();
    	closeRead();
    }

    	// refine this later for the try - finally close crap
    public void closeRead() throws IOException
    {
    	for (int i = 0; i < _readChannels.length; i++) {
    		if (_readChannels[i] != null) {
    			_readChannels[i].close();
    		}
    	}
    }

    	// refine this later for the try - finally close crap
    public void closeWrite() throws IOException
    {
    	for (int i = 0; i < _writeChannels.length; i++) {
    		if (_writeChannels[i] != null) {
    			_writeChannels[i].close();
    		}
    	}
    }

    public synchronized void setLength (long size) throws IOException
    {
		int chunkId = chunkIdForWrite(size, 0);
			// find the right size of this partition
		long actualSize = size - (chunkId * this._sizePerFile);

			// if greater than current file size, append else trucnate
		if (actualSize > _writeChannels[chunkId].size()) {
			long paddingSize = actualSize - _writeChannels[chunkId].size();
			Bytes pad = new Bytes(new byte[(int)paddingSize]);

				// if fileIsNew means we added a new "Defn"
			boolean fileIsNew = _writeChannels[chunkId].size() == 0 ? true : false;

				// if new Defn, set Unitialized, otherwise this alteration was due to inconsistency, use Corrupt
			pad.bytes()[0] = fileIsNew
				? (byte)Defn.Header.UnInitialized.ordinal()
				: (byte)Defn.Header.Corrupt.ordinal();
			_writeChannels[chunkId].write(ByteBuffer.wrap(pad.bytes()), _writeChannels[chunkId].size());
		}
		else {
			_writeChannels[chunkId].truncate(actualSize);
		}

    }

    public String filename ()
    {
    	return _readFile.getAbsolutePath();
    }
}


/*

	if (mode.equals("rws"))
		    imode |= O_SYNC;
		else if (mode.equals("rwd"))
		    imode |= O_DSYNC;


"rwd" updates just the contents ensuring full data retrieval, but
glosses over all but essential metadata (i.e., new block allocation
metadata is handled, but directory timestamp updates are skipped).

     "rws" updates not only the contents, but also non-essential
metadata (directory timestamps (e.g., "modified time", "access time")
et al.).


Specified by the O_DSYNC open flag. When a file is opened using the
O_DSYNC open mode, the write () system call will not return until the
file data and all file system meta-data required to retrieve the file
data are both written to their permanent storage locations.

Specified by the O_SYNC open flag. In addition to items specified
by O_DSYNC, O_SYNC specifies that the write () system call will not
return until all file attributes relative to the I/O are written to
their permanent storage locations, even if the attributes are not
required to retrieve the file data.
-=-

*/



