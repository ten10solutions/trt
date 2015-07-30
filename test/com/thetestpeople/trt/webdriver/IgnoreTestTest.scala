package com.thetestpeople.trt.webdriver

import org.junit.runner.RunWith
import com.thetestpeople.trt.model.impl.DummyData
import com.thetestpeople.trt.mother.{ IncomingFactory ⇒ F }
import com.thetestpeople.trt.tags.SlowTest
import org.scalatest.junit.JUnitRunner
import com.thetestpeople.trt.webdriver.screens.TestsScreen
import com.thetestpeople.trt.utils.StringUtils

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

  "The tests screen" should "include ignored tests only in the correct tabs" in {
    automate { site ⇒
      def makeExecution(n: Int) = F.execution(F.test(name = s"Healthy $n"), passed = true)
      val batch = F.batch(executions = (1 to 10).map(makeExecution))
      site.restApi.addBatch(batch)

      var testsScreen = site.launch().mainMenu.tests()
      testsScreen.testRows.take(6).foreach(_.selected = true)
      testsScreen.clickIgnoreSelectedTests()

      testsScreen.testRows.size should equal(10)
      testsScreen.testRows.count(_.isIgnored) should equal(6)

      testsScreen.selectHealthyTab()
      testsScreen.testRows.size should equal(4)
      testsScreen.testRows.count(_.isIgnored) should equal(0)

      testsScreen.selectIgnoredTab()
      testsScreen.testRows.size should equal(6)
      testsScreen.testRows.count(_.isIgnored) should equal(6)
    }
  }

  "Paging through two pages of ignored tests" should "work correctly" in {
    automate { site ⇒
      val testNames = StringUtils.wordsN(3).take(2 * TestsScreen.DefaultPageSize)
      val executions = testNames.map(name ⇒ F.execution(F.test(name = name)))
      val batch = F.batch(executions)
      site.restApi.addBatch(batch)

      var testsScreen = site.launch().mainMenu.tests()
      testsScreen.testRows.foreach(_.selected = true)
      testsScreen.clickIgnoreSelectedTests()
      testsScreen.clickNextPage()
      testsScreen.testRows.foreach(_.selected = true)
      testsScreen.clickIgnoreSelectedTests()

      testsScreen.selectIgnoredTab()
      val firstScreenNames = testsScreen.testRows.map(_.name)
      testsScreen.clickNextPage()
      val secondScreenNames = testsScreen.testRows.map(_.name)
      firstScreenNames ++ secondScreenNames should equal(testNames)
    }
  }

  "Ignored tests page count" should "be calculated correctly" in {
    automate { site ⇒
      def makeExecution(n: Int) = F.execution(F.test(name = s"Test $n"), passed = true)
      val executions = (1 to 5 * TestsScreen.DefaultPageSize).map(makeExecution)
      val batch = F.batch(executions = executions)
      site.restApi.addBatch(batch)

      var testsScreen = site.launch().mainMenu.tests()
      testsScreen.lastPageNumberDisplayed should be(Some(5))

      for (n ← 1 to 2) {
        testsScreen.testRows.foreach(_.selected = true)
        testsScreen.clickIgnoreSelectedTests()
        testsScreen.clickNextPage()
      }

      testsScreen.selectIgnoredTab()
      testsScreen.lastPageNumberDisplayed should be(Some(2))
    }
  }

}