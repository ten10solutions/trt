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

      {
        val testsScreen = site.launch().mainMenu.tests()
        testsScreen.total should equal(1)
        val Seq(testRow) = testsScreen.testRows
        val testScreen = testRow.clickTestLink()
        testScreen.delete()
      }

      {
        val testsScreen = site.launch().mainMenu.tests()
        testsScreen.total should equal(0)
      }

    }
  }

}