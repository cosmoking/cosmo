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

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.LetterTokenizer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.util.Attribute;
import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.Version;

import org.cosmo.common.record.Index;

public class WordUtil
{
	public static void main (String[] args)
	  throws Exception
	{

		StringReader reader = new StringReader("CNN, CNN news, CNN.com, CNN TV, news, news online, breaking news, U.S. news, world news, weather, business, CNN Money, sports, politics, law, technology, entertainment, education, travel, health, special reports, autos, developing story, news video, CNN Intl");
		/*
		LetterTokenizer tokenizer = new LetterTokenizer(reader);
		AttributeSource filter = new StopFilter(true, tokenizer, StopAnalyzer.ENGLISH_STOP_WORDS_SET, true);

		while (filter.hasAttributes()) {
			Attribute attribute = filter.captureState().
			System.out.println(attribute);
		}
		*/
		StopAnalyzer analyzer = new StopAnalyzer(Index.Version);
		Set<String> uniqueTerms = new HashSet();
		TokenStream tokenStream = analyzer.reusableTokenStream("anyting", reader);
		tokenStream.reset();
		while(tokenStream.incrementToken()) {
			TermAttribute term = tokenStream.getAttribute(TermAttribute.class);
			uniqueTerms.add(term.term());
		}
		tokenStream.end();
		tokenStream.close();

		System.out.println(Arrays.toString(uniqueTerms.toArray()));

	}


}
