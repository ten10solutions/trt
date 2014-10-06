package com.thetestpeople.trt.webdriver

import org.junit.runner.RunWith
import com.thetestpeople.trt.model.impl.DummyData
import com.thetestpeople.trt.mother.{ IncomingFactory ⇒ F }
import com.thetestpeople.trt.tags.SlowTest
import org.scalatest.junit.JUnitRunner

@SlowTest
@RunWith(classOf[JUnitRunner])
class SearchLogsTest extends AbstractBrowserTest {

  "Searching logs" should "work" in {
    automate { site ⇒
      val batch = F.batch(executions = List(
        F.execution(logOpt = Some("foo")),
        F.execution(logOpt = Some("bar"))))
      site.restApi.addBatch(batch)

      val searchScreen = site.launch().mainMenu.searchLogs()
      searchScreen.query = "foo"
      searchScreen.clickSearch()
      val Seq(executionRow) = searchScreen.executionRows
      executionRow.fragmentText should equal("foo")
    }
  }

}