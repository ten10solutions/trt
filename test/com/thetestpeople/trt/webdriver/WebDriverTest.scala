package com.thetestpeople.trt.webdriver

import org.junit.runner.RunWith
import com.thetestpeople.trt.model.impl.DummyData
import com.thetestpeople.trt.tags.SlowTest
import org.scalatest.junit.JUnitRunner

@SlowTest
@RunWith(classOf[JUnitRunner])
class WebDriverTest extends AbstractBrowserTest {

  "The app" should "totally work" in {
    automate { site ⇒
      val testsScreen = site.launch()
      val batchesScreen = testsScreen.mainMenu.batches()
      val executionsScreen = batchesScreen.mainMenu.executions()
      val systemConfigScreen = executionsScreen.mainMenu.config().system()
      systemConfigScreen.mainMenu.config().jenkins()
    }

  }

  "System configuration screen" should "let you update settings" in {
    automate { site ⇒
      val systemConfigScreen = site.launch().mainMenu.config().system()
      systemConfigScreen.failureDurationThreshold = "1 hour"
      systemConfigScreen.failureCountThreshold = "1"
      systemConfigScreen.passDurationThreshold = "2 hours"
      systemConfigScreen.passCountThreshold = "2"
      systemConfigScreen.clickUpdate()
      systemConfigScreen.waitForSuccessMessage()

      systemConfigScreen.failureDurationThreshold should equal("1 hour")
      systemConfigScreen.failureCountThreshold should equal("1")
      systemConfigScreen.passDurationThreshold should equal("2 hours")
      systemConfigScreen.passCountThreshold should equal("2")
    }
  }

  "Jenkins auth screen" should "persist changes" in {
    automate { site ⇒
      val jenkinsAuthScreen = site.launch().mainMenu.config().jenkins().selectAuthTab()
      jenkinsAuthScreen.username = DummyData.Username
      jenkinsAuthScreen.apiToken = DummyData.ApiToken
      jenkinsAuthScreen.clickSubmit()
      jenkinsAuthScreen.waitForSuccessMessage()

      jenkinsAuthScreen.username should equal(DummyData.Username)
      jenkinsAuthScreen.apiToken should equal(DummyData.ApiToken)
    }
  }

}