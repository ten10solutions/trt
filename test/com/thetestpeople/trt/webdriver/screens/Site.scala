package com.thetestpeople.trt.webdriver.screens

import play.api.test.TestBrowser
import org.openqa.selenium.WebDriver
import java.net.URI

class Site(automationContext: AutomationContext, startUrl: URI) {

  def launch(): TestsScreen = {
    automationContext.webDriver.navigate().to(startUrl.toURL)
    new TestsScreen()(automationContext)
  }
  
  def restApi = new RestApi(startUrl)

}
