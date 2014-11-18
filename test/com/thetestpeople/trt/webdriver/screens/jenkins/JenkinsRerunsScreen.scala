package com.thetestpeople.trt.webdriver.screens.jenkins

import org.openqa.selenium.WebDriver
import com.thetestpeople.trt.webdriver.screens.AutomationContext
import com.thetestpeople.trt.webdriver.screens.AbstractScreen
import com.thetestpeople.trt.webdriver.screens.HasMainMenu
import com.thetestpeople.trt.webdriver.screens.RichSelenium._
import org.openqa.selenium.By._
import org.openqa.selenium.WebElement
import com.thetestpeople.trt.utils.Utils

object JenkinsRerunsScreen {

  val RerunJobUrlLocator = id("rerunJobUrl")

}

class JenkinsRerunsScreen(implicit automationContext: AutomationContext) extends AbstractScreen with HasMainMenu with JenkinsConfigurationScreen {

  import JenkinsRerunsScreen._

  def rerunJobUrl: String =
    webDriver.waitForDisplayedAndEnabled(RerunJobUrlLocator).getAttribute("value")

  def rerunJobUrl_=(value: String) = {
    log(s"Set 'Rerun job URL': $value")
    webDriver.waitForDisplayedAndEnabled(RerunJobUrlLocator).setText(value)
  }

  def clickAddParameter() {
    log("Click 'Add parameter'")
    webDriver.waitForDisplayedAndEnabled(id("addParam")).click()
  }

  def clickSubmit() = {
    log("Click 'Submit'")
    webDriver.waitForDisplayedAndEnabled(id("submit")).click()
    this
  }

  def parameters: Seq[Parameter] = {
    val paramCount = webDriver.findElements(cssSelector(".paramSection")).size
    0 until paramCount map Parameter
  }

  case class Parameter(index: Int) {

    def parameter: String =
      webDriver.waitForDisplayedAndEnabled(id(s"params_${index}_name")).getAttribute("value")

    private def ordinalName = Utils.ordinalName(index + 1)

    def parameter_=(value: String) = {
      log(s"Set $ordinalName param's 'Parameter': $value")
      webDriver.waitForDisplayedAndEnabled(id(s"params_${index}_name")).setText(value)
    }

    def value: String =
      webDriver.waitForDisplayedAndEnabled(id(s"params_${index}_value")).getAttribute("value")

    def value_=(value: String) = {
      log(s"Set $ordinalName param's 'Value': $value")
      webDriver.waitForDisplayedAndEnabled(id(s"params_${index}_value")).setText(value)
    }

  }

}