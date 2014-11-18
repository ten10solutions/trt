package com.thetestpeople.trt.webdriver.screens

import org.openqa.selenium.WebDriver
import com.thetestpeople.trt.webdriver.screens.RichSelenium._
import org.openqa.selenium.By.id
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import scala.annotation.tailrec

object CiImportsScreen {

  val AddNewLinkLocator = id("add-new-link")
  val ConfigureNewCiImportLocator = id("add-import-spec-helper")

}

class CiImportsScreen(implicit automationContext: AutomationContext) extends AbstractScreen with HasMainMenu {

  import CiImportsScreen._

  object wizard {

    def configureANewCiImport() = {
      log("Click 'Configure a new CI import'")
      webDriver.waitFor(ConfigureNewCiImportLocator).click()
      new CiImportScreen
    }

  }

  def clickAddNew(): CiImportScreen = {
    log("Click 'Add new'")
    webDriver.waitForDisplayedAndEnabled(AddNewLinkLocator).click()
    new CiImportScreen
  }

  /**
   * Regardless of whether the screen is in wizard mode or not, click the appropriate link to create a new import spec.
   */
  def addNew(): CiImportScreen = {
    val WizardLink = DisplayedAndEnabled(ConfigureNewCiImportLocator)
    val RegularLink = DisplayedAndEnabled(AddNewLinkLocator)
    WaitUtils.waitForOneOf {
      case WizardLink(_)  ⇒ wizard.configureANewCiImport()
      case RegularLink(_) ⇒ clickAddNew()
    }
  }

}