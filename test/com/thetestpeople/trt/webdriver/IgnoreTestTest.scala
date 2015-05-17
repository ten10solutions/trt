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

}