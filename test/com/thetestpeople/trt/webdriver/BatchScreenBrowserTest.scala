package com.thetestpeople.trt.webdriver

import org.junit.runner.RunWith
import com.thetestpeople.trt.model.impl.DummyData
import com.thetestpeople.trt.mother.{ IncomingFactory ⇒ F }
import com.thetestpeople.trt.tags.SlowTest
import org.scalatest.junit.JUnitRunner

@SlowTest
@RunWith(classOf[JUnitRunner])
class BatchScreenBrowserTest extends AbstractBrowserTest {

  "Batch screen" should "display batch data correctly" in {
    automate { site ⇒
      val test = F.test(
        name = DummyData.TestName,
        groupOpt = Some(DummyData.Group))
      val batch = F.batch(
        nameOpt = Some(DummyData.BatchName),
        urlOpt = Some(DummyData.BuildUrl),
        logOpt = Some(DummyData.Log),
        executions = List(F.execution(test, passed = true)))
      site.restApi.addBatch(batch)

      val batchesScreen = site.launch().mainMenu.clickBatches()
      val List(batchRow) = batchesScreen.batchRows
      val batchScreen = batchRow.viewBatch()

      batchScreen.nameOpt should equal(batch.nameOpt)
      batchScreen.urlOpt should equal(batch.urlOpt.map(_.toString))
      batchScreen.hasBatchLog should be(true)
    
      val List(executionRow) = batchScreen.executionRows
      executionRow.passed should be (true)
      executionRow.name should equal(test.name)
      executionRow.groupOpt should equal(test.groupOpt)
    
      val batchLogScreen = batchScreen.clickBatchLog()
      batchLogScreen.log should equal(DummyData.Log)
    }
  }

}