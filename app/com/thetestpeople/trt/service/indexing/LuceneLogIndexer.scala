package com.thetestpeople.trt.service.indexing

import java.io.File
import java.io.StringReader

import scala.collection.mutable.ListBuffer

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.document._
import org.apache.lucene.index._
import org.apache.lucene.search._
import org.apache.lucene.search.highlight._
import org.apache.lucene.store._
import org.apache.lucene.util.Version

import com.thetestpeople.trt.model._
import com.thetestpeople.trt.utils._

object LuceneLogIndexer {

  def fileBackedIndexer(indexDirectory: File): LuceneLogIndexer = {
    val directory: Directory =
      try
        FSDirectory.open(indexDirectory)
      catch {
        case e: Exception ⇒ throw new RuntimeException("Problem opening Lucene index at " + indexDirectory, e)
      }
    new LuceneLogIndexer(directory)
  }

  def memoryBackedIndexer: LuceneLogIndexer = new LuceneLogIndexer(new RAMDirectory)

  private val ExecutionId = "executionId"
  private val Log = "log"
  private val Name = "name"
  private val Group = "group"
  private val ExecutionTime = "executionTime"
  private val Configuration = "configuration"
  private val Summary = "summary"

  private val HighlightContextChars = 500

}

class LuceneLogIndexer(directory: Directory) extends LogIndexer with HasLogger {

  import LuceneLogIndexer._

  private val analyzer = new StandardAnalyzer(Version.LUCENE_4_9)

  // Ensure that the directory is set-up for queries
  withIndexWriter { indexWriter ⇒ }

  def addExecutions(executions: Seq[EnrichedExecution]) = Utils.time("addExecutions") {
    withIndexWriter { indexWriter ⇒
      for (execution ← executions)
        addExecution(indexWriter, execution)
    }
  }

  private def tokenizePhrase(queryString: String): Seq[String] = {
    val tokens = ListBuffer[String]()
    val stream = analyzer.tokenStream(Log, new StringReader(queryString))
    stream.reset()
    while (stream.incrementToken())
      tokens += stream.getAttribute(classOf[CharTermAttribute]).toString
    stream.end()
    stream.close()
    tokens
  }

  private def combineQuery(tokens: Seq[String]): Query = {
    val booleanQuery = new BooleanQuery
    for (token ← tokens) {
      val tokenQuery = new TermQuery(new Term(Log, token))
      booleanQuery.add(tokenQuery, BooleanClause.Occur.MUST)
    }
    booleanQuery
  }

  private def fetchHit(searcher: IndexSearcher, query: Query)(hit: ScoreDoc): ExecutionHit = {
    val document = searcher.doc(hit.doc)
    val executionId = Id[Execution](document.get(ExecutionId).toInt)
    val log = document.get(Log)
    val matchingFragment = highlightMatch(log, query)
    ExecutionHit(executionId, matchingFragment)
  }

  def searchExecutions(queryString: String, startingFrom: Int = 0, limit: Int = Integer.MAX_VALUE): SearchResult = {
    val reader = DirectoryReader.open(directory)
    val searcher = new IndexSearcher(reader)
    val query = combineQuery(tokenizePhrase(queryString))
    val sort = new Sort(new SortField(ExecutionTime, SortField.Type.LONG, true))
    val resultLimit = math.min(startingFrom.toLong + limit, Integer.MAX_VALUE).toInt
    val topDocs = searcher.search(query, resultLimit, sort)
    val hits = topDocs.scoreDocs.toSeq.drop(startingFrom).take(limit).map(fetchHit(searcher, query))
    SearchResult(hits, topDocs.totalHits)
  }

  private def highlightMatch(log: String, query: Query): String = {
    val formatter = new SimpleHTMLFormatter("<span class='search-result'>", "</span>")
    val encoder = new SimpleHTMLEncoder
    val queryScorer = new QueryScorer(query)
    val highlighter = new Highlighter(formatter, encoder, queryScorer)
    highlighter.setTextFragmenter(new SimpleSpanFragmenter(queryScorer, HighlightContextChars))
    highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE)
    highlighter.getBestFragment(analyzer, Log, log)
  }

  private def addExecution(indexWriter: IndexWriter, execution: EnrichedExecution) {
    for (document ← makeDocument(execution)) {
      logger.debug(s"Indexing execution ${execution.id}")
      indexWriter.addDocument(document)
    }
  }

  private def makeDocument(execution: EnrichedExecution): Option[Document] = execution.logOpt.map { log ⇒
    val document = new Document
    val qualifiedName = execution.qualifiedName
    document.add(new StringField(ExecutionId, execution.id.asString, Field.Store.YES))
    document.add(new StringField(Name, qualifiedName.name, Field.Store.YES))
    for (group ← qualifiedName.groupOpt)
      document.add(new StringField(Group, group, Field.Store.YES))
    document.add(new StringField(Configuration, execution.configuration.configuration, Field.Store.YES))
    for (summary ← execution.summaryOpt)
      document.add(new StringField(Summary, summary, Field.Store.YES))
    document.add(new TextField(Log, log, Field.Store.YES))
    document.add(new LongField(ExecutionTime, execution.executionTime.getMillis, Field.Store.YES))
    document
  }

  def deleteExecutions(ids: Seq[Id[Execution]]) {
    logger.debug("Deleting indexed executions: " + ids.mkString(", "))
    withIndexWriter { indexWriter ⇒
      val terms = ids.map(id ⇒ new Term(ExecutionId, id.asString))
      indexWriter.deleteDocuments(terms: _*)
    }
  }

  def deleteAll() {
    logger.debug("Deleting all indexed logs")
    withIndexWriter { indexWriter ⇒
      indexWriter.deleteAll()
    }
  }

  private def withIndexWriter[T](f: IndexWriter ⇒ T): T = {
    val config = new IndexWriterConfig(Version.LUCENE_4_9, analyzer)
    val indexWriter = new IndexWriter(directory, config)
    try
      f(indexWriter)
    finally
      indexWriter.close()
  }

}