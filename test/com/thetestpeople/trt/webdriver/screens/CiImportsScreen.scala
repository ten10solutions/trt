package com.thetestpeople.trt.webdriver.screens

import org.openqa.selenium.WebDriver
import com.thetestpeople.trt.webdriver.screens.RichSelenium._
import org.openqa.selenium.By.id

class CiImportsScreen(implicit automationContext: AutomationContext) extends AbstractScreen with HasMainMenu {

  object wizard {
    
    def configureANewCiImport()  = {
      log("Click 'Configure a new CI import'")
      webDriver.waitFor(id("add-import-spec-helper")).click()
      new CiImportScreen
    }
    
  }
  
}