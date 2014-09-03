package com.thetestpeople.trt.demo

import com.thetestpeople.trt.model.Configuration
import com.thetestpeople.trt.json.RestApi
import com.github.nscala_time.time.Imports._
import scala.util.Random
import org.joda.time.Duration
import com.thetestpeople.trt.mother.{ IncomingFactory ⇒ F }
import com.thetestpeople.trt.utils.UriUtils._
import com.thetestpeople.trt.service.Incoming
import com.ning.http.client.AsyncHttpClientConfig
import play.api.libs.ws.ning.NingWSClient

object DemoPopulator extends App {

  val wsClient = new NingWSClient(new AsyncHttpClientConfig.Builder().build())

  val restApi = RestApi(uri("http://localhost:9000"), wsClient)

  val startDate = 2.months.ago

  val numberOfBatches = 100

  val random = new Random

  val batchGap: (Duration, Duration) = (1.hour, 24.hours)

  def randomGap(): Duration = {
    val (low, high) = batchGap
    val spanMillis = high.getMillis - low.getMillis
    val millis = low.getMillis + math.abs(random.nextLong) % spanMillis
    Duration.millis(millis)
  }

  case class TestSpec(name: String, groupOpt: Option[String], passRate: Double) {

    def test: Incoming.Test = Incoming.Test(name, groupOpt)

    def randomPassed(fudge: Double) = random.nextDouble <= passRate * fudge

    def randomExecution(executionTime: DateTime, configuration: Configuration, fudge: Double) =
      F.execution(test, passed = randomPassed(fudge), executionTimeOpt = Some(executionTime), configurationOpt = Some(configuration),
        durationOpt = Some(Duration.millis(1000 + math.abs(random.nextInt) % 200)))

  }

  val configurations = Seq("Firefox", "IE", "Chrome", "Safari").map(Configuration.apply)

  val testSpecs = Seq(
    TestSpec("user_can_log_in_with_correct_credentials", Some("LoginTests"), 1),
    TestSpec("user_cannot_log_in_with_incorrect_credentials", Some("LoginTests"), 0.85),
    TestSpec("captcha_is_used_after_five_failed_logins", Some("LoginTests"), 0.75),
    TestSpec("a_logged_in_user_can_log_out", Some("LogoutTests"), 0.65),
    TestSpec("search_returns_matching_results", Some("SearchTests"), 0.55),
    TestSpec("search_returns_no_results", Some("SearchTests"), 0.45))

  for (configuration ← configurations) {
    var currentDate = startDate
    for (batchNumber ← 1 to numberOfBatches) {
      val fudge =
        if (40 <= batchNumber && batchNumber <= 60 && configuration == Configuration("IE"))
          0.05
        else
          1
      val batch = F.batch(
        nameOpt = Some(s"Batch $batchNumber"),
        urlOpt = Some(uri(s"http://www.example.com/batch/$batchNumber")),
        executionTimeOpt = Some(currentDate),
        executions = testSpecs.map(_.randomExecution(currentDate, configuration, fudge)).toList)

      println(s"Adding '$configuration' batch $batchNumber on $currentDate")
      restApi.addBatch(batch)
      currentDate = currentDate + randomGap()
    }
  }
  System.exit(0)

}