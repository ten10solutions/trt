package com.thetestpeople.trt.webdriver

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.thetestpeople.trt.tags._
import com.thetestpeople.trt.webdriver.screens._
import com.thetestpeople.trt.webdriver.screens.WaitUtils._
import com.thetestpeople.trt.webdriver.screens.jenkins._
import com.xebialabs.overcast.host.CloudHostFactory
import play.api.libs.ws.WS
import play.api.Play.current
import scala.concurrent.Await
import scala.concurrent.duration._
import com.thetestpeople.trt.utils.WsUtils
import com.thetestpeople.trt.utils.HasLogger

@DockerTest
@SlowTest
@RunWith(classOf[JUnitRunner])
class JenkinsTest extends AbstractBrowserTest with HasLogger {

  "A user" should "be able to import results from Jenkins, and rerun selected tests" in {
    withJenkins { jenkinsHost ⇒
      automate { site ⇒
        importBuildsFromJenkins(site, jenkinsHost)
        rerunFailingTest(site)
        importRerunBuildFromJenkins(site, jenkinsHost)
      }
    }
  }

  private def importBuildsFromJenkins(site: Site, jenkinsHost: String) {
    val authScreen = site.launch().mainMenu.config.jenkins
    configureJenkinsAuth(authScreen, username = "Matt", apiToken = "d77a18cfe4a840432634f68a060f6f65")

    val rerunsScreen = authScreen.selectRerunsTab()
    configureJenkinsReruns(rerunsScreen, rerunJobUrl = s"$jenkinsHost/job/Rerun%20Test%20Job/", parameter = "test", value = "$MAVEN_TEST")

    addNewImportSpecAndSync(rerunsScreen, jobUrl = s"$jenkinsHost/job/Maven%20Test%20Project/")
  }

  private def rerunFailingTest(site: Site) {
    val testsScreen = site.launch().mainMenu.tests().warningTab()
    val Seq(testRow) = testsScreen.testRows
    testRow.name should equal("testMethod1")
    testRow.selected = true
    testsScreen.clickRerunSelectedTests()
  }

  private def importRerunBuildFromJenkins(site: Site, jenkinsHost: String) {
    val importLogScreen = addNewImportSpecAndSync(site.launch(), jobUrl = s"$jenkinsHost/job/Rerun%20Test%20Job/")

    val batchesScreen = importLogScreen.mainMenu.batches()
    WaitUtils.waitUntil {
      batchesScreen.refresh()
      batchesScreen.batchRows.size == 4
    }

    val batchScreen = batchesScreen.batchRows.head.viewBatch()
    val Seq(executionRow) = batchScreen.executionRows
    executionRow.name should equal("testMethod1")
  }

  private def addNewImportSpecAndSync(startScreen: AbstractScreen, jobUrl: String): ImportLogScreen = {
    val importsScreen = startScreen.mainMenu.config.ciImports
    val importScreen = importsScreen.addNew()
    importScreen.jobUrl = jobUrl
    val importLogScreen = importScreen.clickCreate()

    WaitUtils.waitUntil {
      importLogScreen.clickSyncButton()
      val buildRows = importLogScreen.buildRows
      buildRows.nonEmpty && buildRows.forall(_.isSuccess)
    }
    importLogScreen
  }

  private def configureJenkinsAuth(authScreen: JenkinsAuthScreen, username: String, apiToken: String): JenkinsAuthScreen = {
    authScreen.username = username
    authScreen.apiToken = apiToken
    authScreen.clickSubmit()
    authScreen
  }

  private def configureJenkinsReruns(rerunsScreen: JenkinsRerunsScreen, rerunJobUrl: String, parameter: String, value: String) {
    rerunsScreen.rerunJobUrl = rerunJobUrl
    rerunsScreen.clickAddParameter()
    val Seq(paramSection) = rerunsScreen.parameters
    paramSection.parameter = parameter
    paramSection.value = value
    rerunsScreen.clickSubmit()
  }

  private def withJenkins[T](p: String ⇒ T): T = {
    logger.info("Starting Jenkins")
    val host = CloudHostFactory.getCloudHost("jenkins")
    host.setup()
    val hostName = host.getHostName
    val port = host.getPort(8080)
    val url = s"http://$hostName:$port"
    try {
      logger.info("Waiting for Jenkins to fully start...")
      WaitUtils.waitUntil(jenkinsHasBooted(url))
      p(url)
    } finally {
      logger.info("Stopping Jenkins")
      host.teardown()
    }
  }

  private def jenkinsHasBooted(url: String): Boolean = {
    val wsClient = WsUtils.newWsClient
    val future = wsClient.url(s"$url/api/xml").get()
    try {
      val result = Await.result(future, 5.seconds)
      result.status == 200
    } catch {
      case e: Exception ⇒
        false
    }
  }

}