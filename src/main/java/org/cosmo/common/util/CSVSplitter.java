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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ariba.util.core.Assert;
import ariba.util.io.CSVConsumer;
import ariba.util.io.CSVReader;
import ariba.util.io.CSVWriter;

public class CSVSplitter
{
	File _file;
	CSVSplitHandler _dataSplitHandler;
	String _columnName;
	ColumnToFileResolver _fileResolver;

	public CSVSplitter (File file,
			            String columnName,
			            ColumnToFileResolver fileResolver)
	{
		_file = file;
		_columnName = columnName;
		_fileResolver = fileResolver;
		_dataSplitHandler = new CSVSplitHandler(this);
	}


	public void split ()
	  throws IOException
	{
		CSVReader csvReader = new CSVReader(_dataSplitHandler);
		csvReader.read(_file, "UTF8");
		_dataSplitHandler.flushToDisk();
	}


	public static class CSVSplitHandler implements CSVConsumer
	{
		public static final int LinesMarkerCountLimit = 100;

		List _header = null;
		CSVSplitter _spliter;
		int _columnIndex;
		Map<File, List<List>> _linesPerFile = new HashMap();
		int _linesMarkerCount;


		public CSVSplitHandler (CSVSplitter spliter)
		{
			_spliter = spliter;
		}

	    public void consumeLineOfTokens (String path, int lineNumber, List line)
	    {
	    	if (_header == null) {
	    		_header = line;
	    		for (int i = 0 ; i < _header.size(); i++) {
	    			if (_spliter._columnName.equals(_header.get(i).toString())) {
	    				_columnIndex = i;
	    				_header.remove(_columnIndex);
	    			}
	    		}
	    	}
	    	else {
	    		Object columnValue = line.remove(_columnIndex);
	    		File file = _spliter._fileResolver.resolve(columnValue);
	    		List lines = _linesPerFile.get(file);
	    		if (lines == null) {
	    			lines = new ArrayList();
	    			_linesPerFile.put(file, lines);
	    			lines.add(_header);
	    		}
	    		lines.add(line);
	    		_linesMarkerCount++;
	    		if (_linesMarkerCount > LinesMarkerCountLimit) {
	    			flushToDisk();
	    			_linesMarkerCount = 0;
	    		}
	    	}
	    }



	    public void flushToDisk ()
	    {
	    	for (Map.Entry<File, List<List>> entry : _linesPerFile.entrySet()) {
	    		if (!entry.getValue().isEmpty()) {
	    			FileOutputStream out = null;
	    			CSVWriter writer = null;
	    			try {
	    				out = new FileOutputStream(entry.getKey(), true);
		    			writer = new CSVWriter(new OutputStreamWriter(out));
		    			for (List aLine : entry.getValue()) {
		    				writer.writeLine(aLine);
		    			}
		    			writer.close();
	    			}
	    			catch (FileNotFoundException e) {
	    				Assert.assertNonFatal(false, "Unable to open file [%s]", entry.getKey());
	    			}
	    			finally {
	    				if (writer != null) {
	    					writer.close();
	    				}
	    			}
    	    		entry.getValue().clear();
	    		}
	    	}
	    }

	}

	public static class ColumnToFileResolver
	{
		public File resolve(Object columnName)
		{
			return new File("C:/" + columnName.toString() + ".csv");
		}
	}


	public static void main (String[] args) throws Exception
	{
		File file = new File("c:/data.csv");
		ColumnToFileResolver columnToFileResolver = new ColumnToFileResolver();
		CSVSplitter splitter = new CSVSplitter(file, "Realm", columnToFileResolver);
		splitter.split();
	}


}





