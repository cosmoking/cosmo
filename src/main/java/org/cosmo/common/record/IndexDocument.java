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
package org.cosmo.common.record;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.Term;

import ariba.util.core.Assert;
import ariba.util.core.Fmt;



public class IndexDocument
{
	public static final String RecordId = "id";

	Document _document;
	Index _index;
	long _id;

	public IndexDocument (Index index, long id)
	{
		_document = new Document();
		_index = index;
		_id = id;
	}

	public void addField (Defn defn, Object fieldValue)
	{
		if (defn._isIndexField && fieldValue != null) {
			fieldValue = defn.fmt(fieldValue, Defn.FormatType.ForIndex);
				//XXX Field.Index.ANALYZED will tokenize the terms ie search "A" will return "A B C" but not "A B C"
			addField(defn.getIndexFieldName(), (String)fieldValue, defn.getIndexType());
		}
	}

	public void addField (String fieldName, String fieldValue, Field.Index type)
	{
		Field field = new Field(fieldName, fieldValue , Field.Store.NO, type);
		_document.add(field);
	}

	public void addNumericField (String fieldName, long fieldValue)
	{
		NumericField field = new NumericField(fieldName, Field.Store.NO, true);
		field.setLongValue(fieldValue);
	}


	public void save (boolean insert)
	  throws IOException
	{
		if (insert) {
			_document.add(new Field(RecordId, String.valueOf(_id), Field.Store.YES, Field.Index.NOT_ANALYZED));
			_index._indexWriter.addDocument(_document);
		}
		else {
			Term term = new Term(RecordId, String.valueOf(_id));
			_index._indexWriter.updateDocument(term, _document);
		}
	}


		// XXX This actually changes lucene docid to recordid mapping because
		// the updated docid will now be inconsistent with recordid
		// need to have a way to resolve that during search
		// perhaps introduce a new Field "Header" type
	public void update (Defn defn, Object fieldValue)
	  throws IOException
	{
			// XX remove this when we have a fix
		if (true) {
			return;
		}

		if (defn._isIndexField && fieldValue != null) {
			Record record = defn._declaringMeta.store().read(_id);
			List<Defn> defns = defn._declaringMeta._defns;
			for (Defn aDefn : defns) {
				if (aDefn == defn || aDefn.equals(defn)) {
					addField(aDefn, fieldValue);
					continue;
				}

				if (aDefn._isIndexField && aDefn.getValue(record) != null) {
					addField(aDefn, aDefn.getValue(record));
				}
			}
			Term term = new Term(RecordId, String.valueOf(_id));
			_index._indexWriter.updateDocument(term, _document);
		}
	}

}
