package com.thetestpeople.trt.webdriver.screens

import play.api.test.TestBrowser
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.WebDriver
import org.joda.time.DateTime

abstract class AbstractComponent(implicit automationContext: AutomationContext) {

  protected def log(message: String) = automationContext.log(message)

  protected implicit def webDriver = automationContext.webDriver

}

