package com.thetestpeople.trt.webdriver.screens

import org.openqa.selenium.WebDriver
import org.joda.time.DateTime

case class AutomationContext(webDriver: WebDriver, logHandler: String => Unit) {

  def log(message: String) = logHandler(s"[${new DateTime}] $message")
  
}