package com.thetestpeople.trt.webdriver

import org.junit.runner.RunWith
import com.thetestpeople.trt.model.impl.DummyData
import com.thetestpeople.trt.mother.{ IncomingFactory ⇒ F }
import com.thetestpeople.trt.tags.SlowTest
import org.scalatest.junit.JUnitRunner
import com.thetestpeople.trt.model.Configuration

@SlowTest
@RunWith(classOf[JUnitRunner])
class CommentTest extends AbstractBrowserTest {

  "A test" should "be able to have comments attached" in {
    automate { site ⇒

      val batch = F.batch(executions = List(F.execution(F.test())))
      site.restApi.addBatch(batch)

      val testsScreen = site.launch().mainMenu.tests()
      val Seq(testRow) = testsScreen.testRows
      var testScreen = testRow.clickTestLink()

      testScreen.comment should equal("")
      val commentDialog = testScreen.editComment()
      commentDialog.text = "This test intermittently fails"
      testScreen = commentDialog.save()

      testScreen.comment should equal("This test intermittently fails")
    }
  }

  "A batch" should "be able to have comments attached" in {
    automate { site ⇒
      val batch = F.batch()
      site.restApi.addBatch(batch)

      val batchesScreen = site.launch().mainMenu.batches()
      val List(batchRow) = batchesScreen.batchRows
      var batchScreen = batchRow.viewBatch()

      batchScreen.comment should equal("")
      val commentDialog = batchScreen.editComment()
      commentDialog.text = "This batch is totally broken"
      batchScreen = commentDialog.save()

      batchScreen.comment should equal("This batch is totally broken")
    }
  }

  "An execution" should "be able to have comments attached" in {
    automate { site ⇒
      val batch = F.batch(executions = List(F.execution()))
      site.restApi.addBatch(batch)

      val executionsScreen = site.launch().mainMenu.executions()
      val List(executionRow) = executionsScreen.executionRows
      var executionScreen = executionRow.viewExecution()

      executionScreen.comment should equal("")
      val commentDialog = executionScreen.editComment()
      commentDialog.text = "This execution is totally broken"
      executionScreen = commentDialog.save()

      executionScreen.comment should equal("This execution is totally broken")
    }
  }

}