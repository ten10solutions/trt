package com.thetestpeople.trt.webdriver.screens

import play.api.test.TestBrowser
import com.thetestpeople.trt.webdriver.screens.RichSelenium._
import org.openqa.selenium.By.id
import org.openqa.selenium.WebDriver
import com.thetestpeople.trt.webdriver.screens.jenkins.JenkinsJobsScreen

class MainMenu(implicit automationContext: AutomationContext) extends AbstractComponent {

  webDriver.waitForDisplayedAndEnabled(id("navbar"))

  private def clickMenuItem(itemId: String, itemName: String) = {
    if (!webDriver.isDisplayedAndEnabled(id(itemId)))
      for (navbarButton ← webDriver.findImmediateDisplayedAndEnabled(id("collapsed-navbar"))) {
        log("Expand menu bar")
        navbarButton.click()
      }
    log(s"Click '$itemName'")
    webDriver.waitForDisplayedAndEnabled(id(itemId)).click()
  }

  def clickBatches(): BatchesScreen = {
    clickMenuItem("menu-batches", itemName = "Batches")
    new BatchesScreen
  }

  def clickConfigurations(): ConfigurationsScreen = {
    clickMenuItem("menu-configurations", itemName = "Configurations")
    new ConfigurationsScreen
  }

  def clickExecutions(): ExecutionsScreen = {
    clickMenuItem("menu-executions", itemName = "Executions")
    new ExecutionsScreen
  }

  def clickConfig(): ConfigMenu = {
    clickMenuItem("menu-config", itemName = "Config")
    new ConfigMenu()
  }

  class ConfigMenu {

    def clickSystem(): SystemConfigurationScreen = {
      log(s"Click 'System'")
      webDriver.waitForDisplayedAndEnabled(id("menu-config-system")).click()
      new SystemConfigurationScreen
    }

    def clickJenkins(): JenkinsJobsScreen = {
      log(s"Click 'Jenkins'")
      webDriver.waitForDisplayedAndEnabled(id("menu-config-jenkins")).click()
      new JenkinsJobsScreen
    }

  }

}