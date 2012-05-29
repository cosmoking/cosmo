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
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.cosmo.common.util.New;
import org.cosmo.common.util.Util;

public class VariableFilePartition extends FilePartition
{
	public final int StoreCompleteMarker = 1075843080; //1000000001000000001000000001000
	public final int MetaBytes = 8; // 8 bytes extra for Len (int4) + Marker (int4)


	public VariableFilePartition (File readFile, File writeFile, int approxSizePerFile)
	  throws FileNotFoundException, IOException
	{
		this (readFile, writeFile, approxSizePerFile, true);
	}


	public VariableFilePartition (File readFile, File writeFile, int approxSizePerFile, boolean checkIncompleteWrite)
	  throws FileNotFoundException, IOException
	{
		super(readFile, writeFile, approxSizePerFile, 1);
		if (checkIncompleteWrite) {
			checkIncompleteWrite(readFile);
		}
	}


	public void checkIncompleteWrite (File readFile)
	  throws FileNotFoundException, IOException, IncompleteWriteException
	{
		for (int i = 0; i < _readChannels.length; i++) {
			long size = _readChannels[i].size();
			if (size > MetaBytes) { // there is data
				ByteBuffer buf = ByteBuffer.wrap(new byte[4]);
				_readChannels[i].read(buf, _readChannels[i].size() - 4);
				buf.rewind();
				int marker = buf.getInt();
				if (marker != StoreCompleteMarker) {
					throw new IncompleteWriteException(New.str("File [", Util.addSuffixToFile(readFile, i), "] did not complete last write operation"));
				}
			}
		}
	}


	public ByteBuffer readSizedEntry (int size, long position) throws IOException
	{
		return read(size, position + 4);
	}

	public int readSize (long position) throws IOException
	{
    	ByteBuffer redBuf = read(4, position);
    	return redBuf.getInt();
	}

    /**
     *   Writes an entry in which first 8 bytes stores the size of this entry follow by the actual content
     *   so an entry of 100 will take 108 bytes.
     *
     *   Also this operation appends to the end of the last file and returns the file position
     */
	public synchronized long writeSizedEntry (byte[] b, int off, int len) throws IOException
	{
			// 8 bytes extra for Len (int4) + Marker (int4)
		int chunkId = chunkIdForWrite(size(), MetaBytes + len);
		long writePosition = _writeChannels[chunkId].position();
		ByteBuffer buf = ByteBuffer.allocate(MetaBytes + len);
		buf.putInt(len);
		buf.put(b, off, len);
		buf.putInt(StoreCompleteMarker);
		buf.rewind();
		_writeChannels[chunkId].write(buf);
		return writePosition + (_sizePerFile * chunkId);
	}


    public static void main2 (String[] args) throws Exception
    {
    	File file = new File("d:/temp/data");
    	file.delete();
    	VariableFilePartition fileChannel = new VariableFilePartition(file, file, 512);

    		// size of sample data
    	int numberOfSampleData = 10;

    		// lenght of each sample data
    	int lengthOfSampleData = 100;
    	byte[][] sampleStr = new byte[numberOfSampleData][];
    	long[] dataPositions = new long[numberOfSampleData];

    	for (int i = 0, pos = 0; i < numberOfSampleData; i++, pos += lengthOfSampleData) {

    			// fill each sample data ie aaaaa, bbbb, cccc , etc
    		sampleStr[i] = new byte[lengthOfSampleData];
        	Arrays.fill(sampleStr[i], (byte)('a' + i));

        		// write it
        	dataPositions[i] = fileChannel.writeSizedEntry(sampleStr[i], 0, sampleStr[i].length);
    	}

    	fileChannel.close();
    	fileChannel = new VariableFilePartition(file, file, 512);

    	for (int i = 0, pos = 0; i < numberOfSampleData; i++, pos += lengthOfSampleData) {

    			// recreate sample data for read comparsion
    		sampleStr[i] = new byte[lengthOfSampleData];
        	Arrays.fill(sampleStr[i], (byte)('a' + i));

        		// read first 8 bytes first
        	int size = fileChannel.readSize(dataPositions[i]);
        	ByteBuffer redBuf = fileChannel.readSizedEntry(size, dataPositions[i]);

    			// validate
    		System.out.print(new String(redBuf.array(), "ASCII"));
    		if (Arrays.equals(sampleStr[i], redBuf.array())) {
    			System.out.println(" PASS");
    		}
    		else {
    			System.out.println(" FAIL");
    		}

    	}

    }


    public static void main (String[] args) throws Exception
    {
    	for (int x = -30 ; x < 30 ; x++) {

    			// cleanup dir
    		File dir = new File("d:/temp");
    		FileUtils.deleteDirectory(dir);
	    	dir.mkdir();

	    		// create instance
    	   	File file = new File("d:/temp/data");
    		int sizeOfRecord = 100 + x;
    		int partitionSize = 512;
	    	VariableFilePartition fileChannel = new VariableFilePartition(file, file, partitionSize);

    		System.out.println("Running with record size " + sizeOfRecord + " with file partition size " + fileChannel._sizePerFile);

	    		// size of sample data
	    	int numberOfSampleData = 10;

	    		// lenght of each sample data
	    	byte[][] sampleStr = new byte[numberOfSampleData][];
	    	long[] dataPositions = new long[numberOfSampleData];
	    	for (int i = 0, pos = 0; i < numberOfSampleData; i++, pos += sizeOfRecord) {

	    			// fill each sample data ie aaaaa, bbbb, cccc , etc
	    		sampleStr[i] = new byte[sizeOfRecord];
	        	Arrays.fill(sampleStr[i], (byte)('a' + i));

        		// write it
	        	dataPositions[i] = fileChannel.writeSizedEntry(sampleStr[i], 0, sampleStr[i].length);
	    	}

	    	fileChannel.close();
	    	fileChannel = new VariableFilePartition(file, file, partitionSize, true);

	    	for (int i = 0, pos = 0; i < numberOfSampleData; i++, pos += sizeOfRecord) {

	    			// recreate sample data for read comparsion
	    		sampleStr[i] = new byte[sizeOfRecord];
	        	Arrays.fill(sampleStr[i], (byte)('a' + i));

	        		// read

        		// read first 4 bytes first
	        	int size = fileChannel.readSize(dataPositions[i]);
	    		ByteBuffer redBuf = fileChannel.readSizedEntry(size, dataPositions[i]);

	    			// validate
	    		System.out.print(new String(redBuf.array(), "ASCII"));
	    		if (Arrays.equals(sampleStr[i], redBuf.array())) {
	    			System.out.println(" PASS");
	    		}
	    		else {
	    			System.err.println(" FAIL");

	    		}

	    	}

	    	fileChannel.close();
    	}

    }

}
