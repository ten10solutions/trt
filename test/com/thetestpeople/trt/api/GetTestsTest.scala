package com.thetestpeople.trt.api
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import com.thetestpeople.trt.FakeApplicationFactory
import java.net.URI
import org.junit.runner.RunWith
import org.openqa.selenium.WebDriver
import org.openqa.selenium.phantomjs.PhantomJSDriver
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import com.thetestpeople.trt.FakeApplicationFactory
import com.thetestpeople.trt.model.impl.DummyData
import com.thetestpeople.trt.tags.SlowTest
import com.thetestpeople.trt.webdriver.screens.AutomationContext
import com.thetestpeople.trt.webdriver.screens.Site
import play.api.test.Helpers
import play.api.test.TestServer
import org.scalatest.junit.JUnitRunner
import org.openqa.selenium.firefox.FirefoxDriver
import play.api.libs.ws.WS
import com.thetestpeople.trt.json.RestApi
import com.thetestpeople.trt.mother.{ IncomingFactory ⇒ F }
import com.thetestpeople.trt.model.TestStatus

@RunWith(classOf[JUnitRunner])
@SlowTest
class GetTestsTest extends FlatSpec with Matchers {

  private val port = 9001

  private val siteUrl = new URI("http://localhost:" + port)

  def withApi[T](p: RestApi ⇒ T): T = Helpers.running(TestServer(port, FakeApplicationFactory.fakeApplication)) {
    val restApi = new RestApi(siteUrl)
    restApi.deleteAll()
    p(restApi)
  }

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