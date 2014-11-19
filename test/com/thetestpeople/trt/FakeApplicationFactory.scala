package com.thetestpeople.trt

import play.api.test.FakeApplication
import com.thetestpeople.trt.Config._

object FakeApplicationFactory {

  val TestJdbcUrl = "jdbc:h2:mem:tests;DB_CLOSE_DELAY=-1"

  def fakeApplication = new FakeApplication(
    additionalConfiguration = Map(
      Ci.Poller.Enabled -> false,
      Db.Default.Driver -> "org.h2.Driver",
      Db.Default.Url -> TestJdbcUrl,
      Db.Default.User -> "",
      Db.Default.Password -> "",
//      Http.UseTestingClient -> true,
      Lucene.InMemory -> "true"),
    additionalPlugins = Seq(classOf[DontStopBoneCPPlugin].getName))

}