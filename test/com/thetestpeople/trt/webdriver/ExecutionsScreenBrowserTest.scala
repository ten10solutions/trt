package com.thetestpeople.trt.webdriver

import org.junit.runner.RunWith
import com.thetestpeople.trt.model.impl.DummyData
import com.thetestpeople.trt.mother.{ IncomingFactory ⇒ F }
import com.thetestpeople.trt.tags.SlowTest
import org.scalatest.junit.JUnitRunner

@SlowTest
@RunWith(classOf[JUnitRunner])
class ExecutionsScreenBrowserTest extends AbstractBrowserTest {

  "Executions screen" should "display execution data correctly" in {
    automate { site ⇒
      val test = F.test(
        name = DummyData.TestName,
        groupOpt = Some(DummyData.Group))
      val execution = F.execution(test,
        passed = true,
        logOpt = Some(DummyData.Log),
        summaryOpt = Some(DummyData.Summary),
        configurationOpt = Some(DummyData.Configuration1))
      val batch = F.batch(
        nameOpt = Some(DummyData.BatchName),
        executions = List(execution))
      site.restApi.addBatch(batch)

      val executionsScreen = site.launch().mainMenu.executions()
      val Seq(executionRow) = executionsScreen.executionRows

      executionRow.passed should be(true)
      executionRow.name should equal(test.name)
      executionRow.groupOpt should equal(test.groupOpt)
    }
  }

}