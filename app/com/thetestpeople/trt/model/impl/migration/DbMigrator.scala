package com.thetestpeople.trt.model.impl.migration

import liquibase.Liquibase
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.database.Database
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import javax.sql.DataSource
import java.sql.Connection
import liquibase.Contexts
import com.thetestpeople.trt.utils.HasLogger
import play.api.Play

class DbMigrator(dataSource: DataSource) extends HasLogger {

  def migrate() {
    logger.info("Updating DB if required")
    try
      withConnection { connection ⇒
        val connection = dataSource.getConnection
        val liquibase = new Liquibase("db/changeLog.xml", new ClassLoaderResourceAccessor, new JdbcConnection(connection))
        liquibase.update(null: Contexts)
      }
    catch {
      case e: Exception ⇒
        logger.error("Problem updating DB", e)
    }
  }

  private def withConnection[T](p: Connection ⇒ T): T = {
    val connection = dataSource.getConnection
    try
      p(connection)
    finally
      connection.close()
  }

}