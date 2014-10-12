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

  def staleTests(): StaleTestsScreen = {
    log("Click 'Stale Tests'")
    webDriver.waitForDisplayedAndEnabled(id("stale-tests-report")).click()
    new StaleTestsScreen
  }

  def deletedTests(): DeletedTestsScreen = {
    log("Click 'Deleted Tests'")
    webDriver.waitForDisplayedAndEnabled(id("deleted-tests")).click()
    new DeletedTestsScreen
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

  def batches(): BatchesScreen = {
    clickMenuItem("menu-batches", itemName = "Batches")
    new BatchesScreen
  }

  def reports() = {
    clickMenuItem("menu-reports", itemName = "Reports")
    new ReportsMenu
  }

  def tests() = {
    clickMenuItem("menu-tests", itemName = "Tests")
    new TestsScreen
  }

  def testsMenu() = {
    clickMenuItem("menu-tests", itemName = "Tests")
    new TestsMenu
  }

  def executions(): ExecutionsScreen = {
    clickMenuItem("menu-executions", itemName = "Executions")
    new ExecutionsScreen
  }

  def searchLogs(): SearchLogsScreen = {
    clickMenuItem("menu-search-logs", itemName = "Search Logs")
    new SearchLogsScreen()
  }

  def config(): ConfigMenu = {
    clickMenuItem("menu-config", itemName = "Settings")
    new ConfigMenu()
  }

  class ConfigMenu {

    def system(): SystemConfigurationScreen = {
      log(s"Click 'System'")
      webDriver.waitForDisplayedAndEnabled(id("menu-config-system")).click()
      new SystemConfigurationScreen
    }

    def jenkins(): JenkinsJobsScreen = {
      log(s"Click 'Jenkins'")
      webDriver.waitForDisplayedAndEnabled(id("menu-config-jenkins")).click()
      new JenkinsJobsScreen
    }

  }

}