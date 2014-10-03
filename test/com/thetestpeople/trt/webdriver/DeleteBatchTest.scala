package com.thetestpeople.trt.webdriver

import org.junit.runner.RunWith
import com.thetestpeople.trt.model.impl.DummyData
import com.thetestpeople.trt.mother.{ IncomingFactory ⇒ F }
import com.thetestpeople.trt.tags.SlowTest
import org.scalatest.junit.JUnitRunner
import com.thetestpeople.trt.model.Configuration

@SlowTest
@RunWith(classOf[JUnitRunner])
class DeleteBatchTest extends AbstractBrowserTest {

  "Deleting a batch" should "remove a batch and its executions" in {
    automate { site ⇒
      val batch = F.batch(executions = List(F.execution(F.test())))
      val batchId = site.restApi.addBatch(batch)

      var batchesScreen = site.launch().mainMenu.clickBatches()
      val Seq(batchRow) = batchesScreen.batchRows
      val batchScreen = batchRow.viewBatch()

      batchesScreen = batchScreen.clickDelete().clickOK()
      batchesScreen.batchRows should be('empty)

      val executionsScreen = batchesScreen.mainMenu.clickExecutions()
      executionsScreen.executionRows should be('empty)
    }
  }
}