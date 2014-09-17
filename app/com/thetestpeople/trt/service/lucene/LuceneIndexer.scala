package com.thetestpeople.trt.service

import org.apache.lucene.store.RAMDirectory
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.store.Directory
import org.apache.lucene.util.Version
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.document.Field
import com.thetestpeople.trt.model.Execution
import com.thetestpeople.trt.model.EnrichedExecution
import org.apache.lucene.store.FSDirectory
import java.io.File

class LuceneIndexer {

  private def addExecution(indexWriter: IndexWriter, execution: EnrichedExecution) {
    val document = new Document
    val qualifiedName = execution.qualifiedName
    document.add(new TextField("name", qualifiedName.name, Field.Store.YES))
    for (group ← qualifiedName.groupOpt)
      document.add(new TextField("group", group, Field.Store.YES))
    for (log ← execution.logOpt)
      document.add(new TextField("log", log, Field.Store.YES))
    indexWriter.addDocument(document)
  }

  private def getIndexWriter(): IndexWriter = {
    val analyzer = new StandardAnalyzer(Version.LUCENE_47)
    val index: Directory = FSDirectory.open(new File("luceneIndex"))
    val config = new IndexWriterConfig(Version.LUCENE_47, analyzer)
    new IndexWriter(index, config)
  }

  def addExecutions(executions: List[EnrichedExecution]) {
    val indexWriter = getIndexWriter()

    for (execution ← executions)
      addExecution(indexWriter, execution)
    indexWriter.close()
  }
  
}