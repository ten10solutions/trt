//package com.thetestpeople.trt.service
//
//import java.io.File
//import java.io.IOException
//import java.io.Reader
//import org.apache.lucene.analysis._
//import org.apache.lucene.analysis.core.LowerCaseFilter
//import org.apache.lucene.analysis.core.StopAnalyzer
//import org.apache.lucene.analysis.core.StopFilter
//import org.apache.lucene.analysis.util.CharArraySet
//import org.apache.lucene.analysis.util.StopwordAnalyzerBase
//import org.apache.lucene.analysis.util.WordlistLoader
//import org.apache.lucene.util.Version
//
//object LogAnalyser {
//
//  /** Default maximum allowed token length */
//  val DEFAULT_MAX_TOKEN_LENGTH = 255
//
//  val maxTokenLength = DEFAULT_MAX_TOKEN_LENGTH
//
//  /**
//   * An unmodifiable set containing some common English words that are usually not
//   * useful for searching.
//   */
//  val STOP_WORDS_SET = StopAnalyzer.ENGLISH_STOP_WORDS_SET
//}
//
//class LogAnalyser(matchVersion: Version, stopWords: CharArraySet = STOP_WORDS_SET) extends StopwordAnalyzerBase(version, stopWords) {
//  import LogAnalyser._
//
//  override def createComponents(fieldName: String, reader: Reader): TokenStreamComponents = {
//    val src = new StandardTokenizer(matchVersion, reader)
//    src.setMaxTokenLength(maxTokenLength)
//    var tok: TokenStream = new StandardFilter(matchVersion, src)
//    tok = new LowerCaseFilter(matchVersion, tok)
//    tok = new StopFilter(matchVersion, tok, stopwords)
//    new TokenStreamComponents(src, tok) {
//      override def setReader(reader: Reader) {
//        src.setMaxTokenLength(StandardAnalyzer.this.maxTokenLength)
//        super.setReader(reader)
//      }
//    }
//  }
//}
