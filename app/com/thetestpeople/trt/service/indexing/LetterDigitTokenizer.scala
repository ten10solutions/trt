package com.thetestpeople.trt.service.indexing

import org.apache.lucene.analysis.util.CharTokenizer
import org.apache.lucene.util.Version
import java.io.Reader

class LetterDigitTokenizer(version: Version = Version.LUCENE_4_9, reader: Reader) extends CharTokenizer(version, reader) {

  override def isTokenChar(c: Int) = Character.isLetterOrDigit(c)

  override def normalize(c: Int) = Character.toLowerCase(c)

}