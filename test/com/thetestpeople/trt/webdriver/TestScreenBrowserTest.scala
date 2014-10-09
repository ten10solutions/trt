package com.thetestpeople.trt.webdriver

import org.junit.runner.RunWith
import com.thetestpeople.trt.model.impl.DummyData
import com.thetestpeople.trt.mother.{ IncomingFactory ⇒ F }
import com.thetestpeople.trt.tags.SlowTest
import org.scalatest.junit.JUnitRunner

@SlowTest
@RunWith(classOf[JUnitRunner])
class TestScreenBrowserTest extends AbstractBrowserTest {

  "Test screen" should "display execution data correctly" in {
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
      val List(executionRow) = executionsScreen.executionRows
      val testScreen = executionRow.viewTest()

      testScreen.name should equal(test.name)
      testScreen.groupOpt should equal(test.groupOpt)
      testScreen.configurationOpt should equal(None) // No configuration shown in single-configuration mode
      val List(executionRow2) = testScreen.executionRows
      executionRow2.passed should be(true)

    }
  }

}