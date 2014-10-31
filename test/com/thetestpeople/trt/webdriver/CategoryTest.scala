package com.thetestpeople.trt.webdriver

import org.junit.runner.RunWith
import com.thetestpeople.trt.model.impl.DummyData
import com.thetestpeople.trt.mother.{ IncomingFactory ⇒ F }
import com.thetestpeople.trt.tags.SlowTest
import org.scalatest.junit.JUnitRunner

@SlowTest
@RunWith(classOf[JUnitRunner])
class CategoryTest extends AbstractBrowserTest {

  "A user" should "be able to add a test to a category and remove it again" in {
    automate { site ⇒
      val batch = F.batch(executions = Seq(F.execution(F.test())))
      site.restApi.addBatch(batch)

      var testsScreen = site.launch().mainMenu.tests()
      val testScreen = testsScreen.testRows.head.clickTestLink()
      testScreen.categoryWidgets should equal(Seq())

      val categoryDialog = testScreen.clickAddCategory()
      categoryDialog.category = DummyData.Category
      categoryDialog.clickSave()

      val Seq(categoryWidget) = testScreen.categoryWidgets
      categoryWidget.categoryName should equal(DummyData.Category)

      categoryWidget.clickRemoveCategory()
      testScreen.categoryWidgets should equal(Seq())

    }
  }

  "A user" should "be able to filter tests by category" in {
    automate { site ⇒
      val batch = F.batch(executions = Seq(
        F.execution(F.test(name = "test1", categories = Seq(DummyData.Category))),
        F.execution(F.test(name = "test2", categories = Seq()))))
      site.restApi.addBatch(batch)

      var testsScreen = site.launch().mainMenu.tests()
      testsScreen.testRows.size should equal(2)

      val filterOptions = testsScreen.expandFilterOptions
      filterOptions.category = DummyData.Category
      filterOptions.clickSearch()

      testsScreen.testRows.size should equal(1)
    }
  }

}