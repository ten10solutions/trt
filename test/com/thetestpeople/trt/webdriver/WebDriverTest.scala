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