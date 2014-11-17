package com.thetestpeople.trt.webdriver

import org.junit.runner.RunWith
import com.thetestpeople.trt.model.impl.DummyData
import com.thetestpeople.trt.mother.{ IncomingFactory ⇒ F }
import com.thetestpeople.trt.tags.SlowTest
import org.scalatest.junit.JUnitRunner
import com.github.nscala_time.time.Imports._

@SlowTest
@RunWith(classOf[JUnitRunner])
class JenkinsTest extends AbstractBrowserTest {

  "Jenkins" should "work" in {
    automate { site ⇒
      val authScreen = site.launch().mainMenu.config.jenkins
      authScreen.username = "Matt"
      authScreen.apiToken = "d77a18cfe4a840432634f68a060f6f65"
      authScreen.clickSubmit()

      val importsScreen = authScreen.mainMenu.config.ciImports
      val importScreen = importsScreen.wizard.configureANewCiImport()
      importScreen.jobUrl = "http://localhost:8080/job/Maven%20Test%20Project/"
      val importLogScreen = importScreen.clickCreate()

      Thread.sleep(5000)
      val batchesScreen = importLogScreen.mainMenu.batches()
      batchesScreen.batchRows.size should equal(3)
      
    }
  }

}