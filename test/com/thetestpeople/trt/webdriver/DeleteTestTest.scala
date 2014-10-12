package com.thetestpeople.trt.webdriver

import org.junit.runner.RunWith
import com.thetestpeople.trt.model.impl.DummyData
import com.thetestpeople.trt.mother.{ IncomingFactory ⇒ F }
import com.thetestpeople.trt.tags.SlowTest
import org.scalatest.junit.JUnitRunner

@SlowTest
@RunWith(classOf[JUnitRunner])
class DeleteTestTest extends AbstractBrowserTest {

  "A deleted test" should "not be visible on the tests screen" in {
    automate { site ⇒

      val batch = F.batch(executions = List(F.execution(F.test())))
      site.restApi.addBatch(batch)

      var testsScreen = site.launch().mainMenu.tests()
      val testScreen = testsScreen.testRows.head.clickTestLink()
      testScreen.delete()

      testsScreen = testScreen.mainMenu.tests()
      testsScreen.total should equal(0)
    }
  }

  "A deleted test" should "be visible on the deleted tests screen" in {
    automate { site ⇒
      val batch = F.batch(executions = List(F.execution(F.test(name = DummyData.TestName))))
      site.restApi.addBatch(batch)

      var testsScreen = site.launch().mainMenu.tests()
      val testScreen = testsScreen.testRows.head.clickTestLink()
      testScreen.delete()

      val deletedTestsScreen = testScreen.mainMenu.reports().deletedTests()
      val Seq(testRow) = deletedTestsScreen.testRows
      testRow.name should equal(DummyData.TestName)
    }
  }
}