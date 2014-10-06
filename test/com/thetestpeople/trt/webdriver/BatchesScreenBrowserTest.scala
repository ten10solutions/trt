package com.thetestpeople.trt.webdriver

import org.junit.runner.RunWith
import com.thetestpeople.trt.model.impl.DummyData
import com.thetestpeople.trt.mother.{ IncomingFactory ⇒ F }
import com.thetestpeople.trt.tags.SlowTest
import org.scalatest.junit.JUnitRunner

@SlowTest
@RunWith(classOf[JUnitRunner])
class BatchesScreenBrowserTest extends AbstractBrowserTest {

  "Batches screen" should "display batch data correctly" in {
    automate { site ⇒
      val batch = F.batch(
        nameOpt = Some(DummyData.BatchName),
        executions = List(F.execution(F.test(), passed = true)))
      site.restApi.addBatch(batch)

      val batchesScreen = site.launch().mainMenu.batches()
      val List(batchRow) = batchesScreen.batchRows
      batchRow.passed should be(true)
      batchRow.passes should equal(1)
      batchRow.fails should equal(0)
      batchRow.total should equal(1)
      batchRow.nameOpt should equal(batch.nameOpt)
    }
  }

}