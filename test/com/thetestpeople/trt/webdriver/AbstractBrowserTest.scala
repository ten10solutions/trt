package com.thetestpeople.trt.webdriver

import java.net.URI
import org.junit.runner.RunWith
import org.openqa.selenium.WebDriver
import org.openqa.selenium.phantomjs.PhantomJSDriver
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import com.thetestpeople.trt.FakeApplicationFactory
import com.thetestpeople.trt.model.impl.DummyData
import com.thetestpeople.trt.tags.SlowTest
import com.thetestpeople.trt.webdriver.screens.AutomationContext
import com.thetestpeople.trt.webdriver.screens.Site
import play.api.test.Helpers
import play.api.test.TestServer
import org.scalatest.junit.JUnitRunner
import org.openqa.selenium.firefox.FirefoxDriver
import play.api.libs.ws.WS
import org.openqa.selenium.chrome.ChromeDriver

abstract class AbstractBrowserTest extends FlatSpec with Matchers {

  private val port = 9001

  private val siteUrl = new URI("http://localhost:" + port)

  private val retainBrowser: Boolean = Option(System.getenv("TRT_RETAIN_BROWSER")).exists(_.toBoolean)

  protected def automate(testBlock: Site ⇒ Any) =
    Helpers.running(TestServer(port, FakeApplicationFactory.fakeApplication)) {
      val webDriver: WebDriver = getWebDriver
      try {
        val automationContext = AutomationContext(webDriver, message ⇒ info(message))
        val site = new Site(automationContext, siteUrl)
        site.restApi.deleteAll()
        testBlock(site)
      } finally {
        if (!retainBrowser)
          webDriver.quit()
      }
    }

  private def getWebDriver: WebDriver = {
    val requestedBrowserOpt = Option(System.getenv("TRT_BROWSER"))
    Option(System.getenv("TRT_BROWSER")).map { requestedBrowser ⇒
      requestedBrowser.toLowerCase match {
        case "firefox"   ⇒ new FirefoxDriver
        case "chrome"    ⇒ new ChromeDriver
        case "phantomjs" ⇒ new PhantomJSDriver
        case _           ⇒ throw new RuntimeException(s"Unknown browser type '$requestedBrowser'. Valid options are 'firefox', 'chrome' and 'phantomjs'")
      }
    }.getOrElse(new PhantomJSDriver)
  }

}