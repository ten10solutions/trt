package com.thetestpeople.trt.model.impl

import java.sql.DriverManager

import org.h2.jdbcx.JdbcDataSource
import org.junit.runner.RunWith
import org.postgresql.ds.PGSimpleDataSource
import org.scalatest.junit.JUnitRunner

import com.thetestpeople.trt.FakeApplicationFactory
import com.thetestpeople.trt.model.impl.migration.DbMigrator
import com.thetestpeople.trt.tags.SlowTest

@SlowTest
@RunWith(classOf[JUnitRunner])
class SlickDaoTest extends DaoTest {

  private lazy val (dataSource, url) = {
    val (dataSource, url) = h2DataSource
    val dbMigrator = new DbMigrator(dataSource)
    dbMigrator.migrate()
    (dataSource, url)
  }

  override def createDao: SlickDao = {
    val dao = new SlickDao(url, dataSourceOpt = Some(dataSource))
    dao.transaction {
      dao.deleteAll()
    }
    dao
  }

  private def h2DataSource = {
    DriverManager.registerDriver(new org.h2.Driver)
    val url = FakeApplicationFactory.TestJdbcUrl
    val dataSource = new JdbcDataSource
    dataSource.setURL(url)
    (dataSource, url)
  }

  private def postgresDataSource = {
    DriverManager.registerDriver(new org.postgresql.Driver)
    val dataSource = new PGSimpleDataSource
    val serverName = "localhost"
    val dbName = "testresults"
    val username = "trt"
    val password = "trt"
    val url = s"jdbc:postgresql://$serverName/$dbName"
    dataSource.setServerName(serverName)
    dataSource.setDatabaseName(dbName)
    dataSource.setUser(username)
    dataSource.setPassword(password)
    (dataSource, url)
  }

}
