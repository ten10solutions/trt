package com.thetestpeople.trt.webdriver.screens

import play.api.test.TestBrowser
import com.thetestpeople.trt.webdriver.screens.RichSelenium._
import org.openqa.selenium.By.id
import org.openqa.selenium.WebDriver
import com.thetestpeople.trt.webdriver.screens.jenkins.JenkinsJobsScreen
import com.thetestpeople.trt.model.Configuration
import org.openqa.selenium.By
import scala.collection.JavaConverters._

class ReportsMenu(implicit automationContext: AutomationContext) extends AbstractComponent {
  
  def clickStaleTests() = {
    log("Click 'Stale Tests'")
    webDriver.waitForDisplayedAndEnabled(id("stale-tests-report")).click()
    new StaleTestsScreen
  }
  
}

class TestsMenu(implicit automationContext: AutomationContext) extends AbstractComponent {

  def chooseConfiguration(configuration: Configuration): TestsScreen = {
    chooseConfiguration(configuration.configuration)
  }

  def chooseConfiguration(configuration: String): TestsScreen = {
    log(s"Click configuration '$configuration'")
    val configurationItems = webDriver.findElements(By.className("configuration-menu-item")).asScala
    val menuItem = configurationItems.find(_.getText == configuration)
      .getOrElse(throw new RuntimeException(s"Could not find menu item for configuration '$configuration'"))
    menuItem.click()
    new TestsScreen
  }

}

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

  def clickReports() = {
    clickMenuItem("menu-reports", itemName = "Reports")
    new ReportsMenu
  }

  def clickTests() = {
    clickMenuItem("menu-tests", itemName = "Tests")
    new TestsMenu
  }

  def clickExecutions(): ExecutionsScreen = {
    clickMenuItem("menu-executions", itemName = "Executions")
    new ExecutionsScreen
  }

  def clickSearchLogs(): SearchLogsScreen = {
    clickMenuItem("menu-search-logs", itemName = "Search Logs")
    new SearchLogsScreen()
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