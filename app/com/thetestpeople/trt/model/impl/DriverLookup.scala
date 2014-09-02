package com.thetestpeople.trt.model.impl

import scala.slick.driver._

object DriverLookup {

  def apply(jdbcUrl: String): JdbcDriver =
    jdbcUrl.split(":")(1) match {
      case "h2"         ⇒ H2Driver
      case "derby"      ⇒ DerbyDriver
      case "mysql"      ⇒ MySQLDriver
      case "postgresql" ⇒ PostgresDriver
      case "sqllite"    ⇒ SQLiteDriver
      case "hsqldb"     ⇒ HsqldbDriver
    }

}