package com.thetestpeople.trt.webdriver

import org.junit.runner.RunWith
import com.thetestpeople.trt.model.impl.DummyData
import com.thetestpeople.trt.mother.{ IncomingFactory ⇒ F }
import com.thetestpeople.trt.tags.SlowTest
import org.scalatest.junit.JUnitRunner
import com.github.nscala_time.time.Imports._
import com.thetestpeople.trt.webdriver.screens.WaitUtils
import com.thetestpeople.trt.webdriver.screens.AbstractScreen
import com.thetestpeople.trt.webdriver.screens.jenkins.JenkinsAuthScreen
import com.thetestpeople.trt.webdriver.screens.jenkins.JenkinsRerunsScreen
import com.xebialabs.overcast.host.CloudHostFactory

@SlowTest
@RunWith(classOf[JUnitRunner])
class JenkinsTest extends AbstractBrowserTest {

//  val jenkinsHost = "http://localhost:8080"

  private def withJenkins[T](p: String ⇒ T): T = {
    val host = CloudHostFactory.getCloudHost("jenkins")
    host.setup()
    val hostName = host.getHostName
    val port = host.getPort(8080)
    val url = s"http://$hostName:$port"
    try
      p(url)
    finally
      host.teardown()
  }

  "Jenkins" should "work" in {
    withJenkins { jenkinsHost ⇒
      automate { site ⇒
        val authScreen = site.launch().mainMenu.config.jenkins
        configureJenkinsAuth(authScreen, username = "Matt", apiToken = "d77a18cfe4a840432634f68a060f6f65")

        val rerunsScreen = authScreen.selectRerunsTab()
        configureJenkinsReruns(rerunsScreen, rerunJobUrl = s"$jenkinsHost/job/Rerun%20Test%20Job/", parameter = "test", value = "$MAVEN_TEST")

        var importsScreen = rerunsScreen.mainMenu.config.ciImports
        var importScreen = importsScreen.wizard.configureANewCiImport()
        importScreen.jobUrl = s"$jenkinsHost/job/Maven%20Test%20Project/"
        var importLogScreen = importScreen.clickCreate()

        var batchesScreen = importLogScreen.mainMenu.batches()
        WaitUtils.waitUntil() {
          batchesScreen.refresh()
          batchesScreen.batchRows.size == 3
        }

        var testsScreen = batchesScreen.mainMenu.tests()
        testsScreen = testsScreen.warningTab()
        val Seq(testRow) = testsScreen.testRows
        testRow.name should equal("testMethod1")
        testRow.selected = true
        testsScreen.clickRerunSelectedTests()

        importsScreen = testsScreen.mainMenu.config.ciImports
        importScreen = importsScreen.clickAddNew()
        importScreen.jobUrl = s"$jenkinsHost/job/Rerun%20Test%20Job/"
        importLogScreen = importScreen.clickCreate()

        WaitUtils.waitUntil() {
          importLogScreen.clickSyncButton()
          val buildRows = importLogScreen.buildRows
          buildRows.nonEmpty && buildRows.forall(_.isSuccess)
        }

        batchesScreen = importLogScreen.mainMenu.batches()
        WaitUtils.waitUntil() {
          batchesScreen.refresh()
          batchesScreen.batchRows.size == 4
        }

        val latestBatchRow = batchesScreen.batchRows.head
        latestBatchRow.total should equal(1)
        val batchScreen = latestBatchRow.viewBatch()

        val Seq(executionRow) = batchScreen.executionRows
        executionRow.name should equal("testMethod1")
      }
    }
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
    val Seq(param) = rerunsScreen.parameters
    param.parameter = parameter
    param.value = value
    rerunsScreen.clickSubmit()
  }

}