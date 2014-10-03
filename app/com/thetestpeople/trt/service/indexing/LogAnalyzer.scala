package com.thetestpeople.trt.service.indexing

import org.apache.lucene.analysis._
import org.apache.lucene.analysis.core.LowerCaseFilter
import org.apache.lucene.analysis.core.StopAnalyzer
import org.apache.lucene.analysis.core.StopFilter
import org.apache.lucene.analysis.util.CharArraySet
import org.apache.lucene.analysis.util.StopwordAnalyzerBase
import org.apache.lucene.analysis.util.WordlistLoader
import org.apache.lucene.util.Version
import java.io.File
import java.io.IOException
import java.io.Reader
import org.apache.lucene.analysis.standard.StandardTokenizer
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents
import org.apache.lucene.analysis.standard.StandardFilter
import org.apache.lucene.analysis.util.StopwordAnalyzerBase

class LogAnalyzer(version: Version = Version.LUCENE_4_9) extends StopwordAnalyzerBase(version, StopAnalyzer.ENGLISH_STOP_WORDS_SET) {

  override protected def createComponents(fieldName: String, reader: Reader): TokenStreamComponents = {
    val tokenizer = new LetterDigitTokenizer(version, reader)
    var filter: TokenFilter = new StandardFilter(matchVersion, tokenizer)
    filter = new LowerCaseFilter(matchVersion, filter)
    filter = new StopFilter(matchVersion, filter, stopwords)
    return new TokenStreamComponents(tokenizer, filter)
  }
}

