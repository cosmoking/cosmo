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

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.util.OpenBitSet;
import org.cosmo.common.util.SearchRange;

public class SearchContext
{
	public Defn _searchField;
	public Object _searchTerm;
	public OpenBitSet _searchResults;
	public Query _query;

	SearchContext ()
	{
	}

		// use this only for querying for list of results
	public SearchContext (Defn searchField, Object term)
	  throws IOException, ParseException
	{
		_searchField = searchField;
		_searchTerm = term;


		if (term instanceof org.apache.lucene.search.Query) {
			_searchResults = _searchField._declaringMeta.search().records((org.apache.lucene.search.Query)term);
		}
		else if (term instanceof SearchRange) {
			_query = Query.on().recordsRange(_searchField, ((SearchRange)_searchTerm)._from, ((SearchRange)_searchTerm)._to);
			_searchResults = _searchField._declaringMeta.search().records(_query);
		}
		else {
			_query = Query.on().records(_searchField, _searchTerm);
			_searchResults = _searchField._declaringMeta.search().records(_query);
		}
	}

	public String formattedSearchTerm ()
	{
		return _searchField.fmt(_searchTerm, Defn.FormatType.ForDisplay);
	}


	public SearchResult getSearchResult ()
	{
		return new SearchResult((OpenBitSet)_searchResults.clone(), this);
	}


	public static class Empty extends SearchContext
	{
		public Empty (Defn searchField, Object term)
		  throws IOException, ParseException
		{
			_searchField = searchField;
			_searchTerm = term;
		}
	}
}




