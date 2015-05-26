package com.thetestpeople.trt.webdriver

import org.junit.runner.RunWith
import com.thetestpeople.trt.model.impl.DummyData
import com.thetestpeople.trt.mother.{ IncomingFactory ⇒ F }
import com.thetestpeople.trt.tags.SlowTest
import org.scalatest.junit.JUnitRunner

@SlowTest
@RunWith(classOf[JUnitRunner])
class IgnoreTestTest extends AbstractBrowserTest {

  "A user" should "be able to ignore and unignore a test" in {
    automate { site ⇒

      val batch = F.batch(executions = List(F.execution(F.test())))
      site.restApi.addBatch(batch)

      var testsScreen = site.launch().mainMenu.tests()
      var testScreen = testsScreen.testRows.head.clickTestLink()

      testScreen.ignore()

      testsScreen = testScreen.mainMenu.tests()
      testsScreen.ignored should equal(1)

      testScreen = testsScreen.testRows.head.clickTestLink()
      testScreen.unignore()

      testsScreen = testScreen.mainMenu.tests()
      testsScreen.healthy should equal(1)
    }
  }

  "A user" should "be able to ignore and unignore multiple tests" in {
    automate { site ⇒

      val batch = F.batch(executions = Seq(
        F.execution(F.test(name = "test1")),
        F.execution(F.test(name = "test2")),
        F.execution(F.test(name = "test3"))))
      site.restApi.addBatch(batch)

      var testsScreen = site.launch().mainMenu.tests()
      testsScreen.testRows.take(2).foreach(_.selected = true)
      testsScreen.clickIgnoreSelectedTests()

      testsScreen.healthy should equal(1)
      testsScreen.ignored should equal(2)

      testsScreen.testRows.take(3).foreach(_.selected = true)
      testsScreen.clickUnignoreSelectedTests()

      testsScreen.healthy should equal(3)
    }
  }

}