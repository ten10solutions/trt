package com.thetestpeople.trt.webdriver

import org.junit.runner.RunWith
import com.thetestpeople.trt.model.impl.DummyData
import com.thetestpeople.trt.tags.SlowTest
import org.scalatest.junit.JUnitRunner

@SlowTest
@RunWith(classOf[JUnitRunner])
class SystemConfigurationScreenTest extends AbstractBrowserTest {

  "System configuration screen" should "let you update settings" in {
    automate { site ⇒
      val systemConfigScreen = site.launch().mainMenu.config().system()
      systemConfigScreen.brokenDurationThreshold = "1 hour"
      systemConfigScreen.brokenCountThreshold = "1"
      systemConfigScreen.healthyDurationThreshold = "2 hours"
      systemConfigScreen.healthyCountThreshold = "2"
      systemConfigScreen.clickUpdate()
      systemConfigScreen.waitForSuccessMessage()

      systemConfigScreen.brokenDurationThreshold should equal("1 hour")
      systemConfigScreen.brokenCountThreshold should equal("1")
      systemConfigScreen.healthyDurationThreshold should equal("2 hours")
      systemConfigScreen.healthyCountThreshold should equal("2")
    }
  }

  it should "not let you enter negative values" in {
    automate { site ⇒
      val systemConfigScreen = site.launch().mainMenu.config().system()
      systemConfigScreen.brokenDurationThreshold = "-1 hour"
      systemConfigScreen.brokenCountThreshold = "-1"
      systemConfigScreen.clickUpdate()
      systemConfigScreen.waitForValidationError()
      
      systemConfigScreen.errorsForBrokenCountThreshold should be ('defined)
      systemConfigScreen.errorsForBrokenDurationThreshold should be ('defined)
    }
  }

}