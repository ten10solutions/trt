package com.thetestpeople.trt.api

import org.junit.runner.RunWith
import com.thetestpeople.trt.tags.SlowTest
import org.scalatest.junit.JUnitRunner
import com.thetestpeople.trt.mother.{ IncomingFactory ⇒ F }
import com.thetestpeople.trt.model.impl.DummyData
import com.thetestpeople.trt.model.TestStatus

@SlowTest
@RunWith(classOf[JUnitRunner])
class GetTestsApiTest extends AbstractApiTest {

  "When there are no test results, getting tests" should "return an empty list" in {
    withApi { restApi ⇒
      restApi.getTests(configurationOpt = None, statusOpt = None) should equal(Seq())
    }
  }

  "Getting tests" should "test info including status" in {
    withApi { restApi ⇒
      val batch = F.batch(
        executions = Seq(
          F.execution(F.test(name = DummyData.TestName, groupOpt = Some(DummyData.Group)), passed = true)))
      restApi.addBatch(batch)
      restApi.analyseAllExecutions()

      val Seq(test) = restApi.getTests()
      test.name should equal(DummyData.TestName)
      test.groupOpt should equal(Some(DummyData.Group))
      test.ignored should equal(false)
      test.statusOpt should equal(Some(TestStatus.Healthy))
    }
  }

  "Getting tests" should "let you filter by configuration" in {
    withApi { restApi ⇒
      val batch = F.batch(
        executions = Seq(
          F.execution(F.test(name = "test1"), configurationOpt = Some(DummyData.Configuration1)),
          F.execution(F.test(name = "test2"), configurationOpt = Some(DummyData.Configuration2))))
      restApi.addBatch(batch)
      restApi.analyseAllExecutions()

      val Seq(test) = restApi.getTests(configurationOpt = Some(DummyData.Configuration1))
      test.name should equal("test1")
    }
  }

  "Getting tests" should "let you filter by status" in {
    withApi { restApi ⇒
      val batch = F.batch(
        executions = Seq(
          F.execution(F.test(name = "test1"), passed = true),
          F.execution(F.test(name = "test2"), passed = false)))
      restApi.addBatch(batch)
      restApi.analyseAllExecutions()

      val Seq(test) = restApi.getTests(statusOpt = Some(TestStatus.Healthy))
      test.name should equal("test1")
    }
  }

}