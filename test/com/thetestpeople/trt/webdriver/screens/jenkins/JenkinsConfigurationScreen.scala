package com.thetestpeople.trt.webdriver.screens.jenkins

import com.thetestpeople.trt.webdriver.screens.AbstractScreen
import com.thetestpeople.trt.webdriver.screens.RichSelenium._
import org.openqa.selenium.By.id

trait JenkinsConfigurationScreen { self: AbstractScreen ⇒

  def selectAuthTab(): JenkinsAuthScreen = {
    log("Select 'Auth' tab")
    webDriver.waitForDisplayedAndEnabled(id("auth-tab-link")).click()
    new JenkinsAuthScreen
  }

  def selectRerunsTab(): JenkinsRerunsScreen = {
    log("Select 'Reruns' tab")
    webDriver.waitForDisplayedAndEnabled(id("reruns-tab-link")).click()
    new JenkinsRerunsScreen
  }

}