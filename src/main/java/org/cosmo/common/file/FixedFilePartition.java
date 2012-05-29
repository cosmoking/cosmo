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
import org.cosmo.common.util.Bytes;


public class FixedFilePartition extends FilePartition
{


	public FixedFilePartition  (File readFile, File writeFile, int approxSizePerFile, int fixedRecordSizeInFile)
	  throws FileNotFoundException, IOException
	{
		super(readFile, writeFile, approxSizePerFile, fixedRecordSizeInFile);
	}

		// starts at 0 and read up to size
	public ByteBuffer readFull (int size)
	  throws IOException
	{
		Bytes bytes = new Bytes(size);

			// read each file fully and write to buf
		for (int i = 0, remainSize = size; remainSize > 0; i++) {
			ByteBuffer buf = ByteBuffer.allocate(Math.min(size, (int)_sizePerFile));
			int read = _readChannels[i].read(buf, 0);
			bytes.write(buf.array(), 0, Math.min(remainSize, (int)_sizePerFile));
			remainSize = remainSize - read;
		}
		return ByteBuffer.wrap(bytes.bytes());
	}


    public static void main (String[] args) throws Exception
    {
    	for (int x = -25 ; x < 25 ; x++) {

    			// cleanup dir
    		File dir = new File("d:/temp");
    		FileUtils.deleteDirectory(dir);
	    	dir.mkdir();

	    		// create instance
    	   	File file = new File("d:/temp/data");
    		int sizeOfRecord = 100 + x;
    		int partitionSize = 300;
	    	FixedFilePartition fileChannel = new FixedFilePartition(file, file, partitionSize, sizeOfRecord);

    		System.out.println("Running with record size " + sizeOfRecord + " with file partition size " + fileChannel._sizePerFile);

	    		// size of sample data
	    	int numberOfSampleData = 200;

	    		// lenght of each sample data
	    	byte[][] sampleStr = new byte[numberOfSampleData][];
	    	for (int i = 0, pos = 0; i < numberOfSampleData; i++, pos += sizeOfRecord) {

	    			// fill each sample data ie aaaaa, bbbb, cccc , etc
	    		sampleStr[i] = new byte[sizeOfRecord];
	        	Arrays.fill(sampleStr[i], (byte)('a' + i));

	        		// write it
	        	fileChannel.write(ByteBuffer.wrap(sampleStr[i]), pos);
	    	}

	    	fileChannel.close();
	    	fileChannel = new FixedFilePartition(file, file, partitionSize, sizeOfRecord);

	    	for (int i = 0, pos = 0; i < numberOfSampleData; i++, pos += sizeOfRecord) {

	    			// recreate sample data for read comparsion
	    		sampleStr[i] = new byte[sizeOfRecord];
	        	Arrays.fill(sampleStr[i], (byte)('a' + i));

	        		// read
	    		ByteBuffer redBuf = ByteBuffer.wrap(new byte[sizeOfRecord]);
	    		fileChannel.read(redBuf, pos);

	    			// validate
	    		System.out.print(new String(redBuf.array(), "ASCII"));
	    		if (Arrays.equals(sampleStr[i], redBuf.array())) {
	    			System.out.println(" PASS");
	    		}
	    		else {
	    			throw new Exception("Error");
	    		}

	    	}

	    	fileChannel.close();
    	}

    }


}
