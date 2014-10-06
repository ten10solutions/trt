package com.thetestpeople.trt.webdriver

import org.junit.runner.RunWith
import com.thetestpeople.trt.model.impl.DummyData
import com.thetestpeople.trt.mother.{ IncomingFactory ⇒ F }
import com.thetestpeople.trt.tags.SlowTest
import org.scalatest.junit.JUnitRunner
import com.github.nscala_time.time.Imports._

@SlowTest
@RunWith(classOf[JUnitRunner])
class StaleTestsScreenTest extends AbstractBrowserTest {

  "A test that hasn't been run for a while" should "be listed in the stale tests screen" in {
    automate { site ⇒

      val batch = F.batch(executions = List(
        F.execution(F.test(name = "test1"), executionTimeOpt = Some(1.hour.ago)),
        F.execution(F.test(name = "test2"), executionTimeOpt = Some(2.hours.ago)),
        F.execution(F.test(name = "test3"), executionTimeOpt = Some(3.hours.ago)),
        F.execution(F.test(name = "test4"), executionTimeOpt = Some(4.hours.ago)),
        F.execution(F.test(name = "test5"), executionTimeOpt = Some(5.weeks.ago))))
      site.restApi.addBatch(batch)
      
      val staleTestsScreen = site.launch().mainMenu.reports().staleTests()
      val Seq(row) = staleTestsScreen.testRows
      row.name should equal("test5")
    }
  }

}