package com.thetestpeople.trt.webdriver.screens.jenkins

import org.openqa.selenium.WebDriver
import com.thetestpeople.trt.webdriver.screens.AutomationContext
import com.thetestpeople.trt.webdriver.screens.AbstractScreen
import com.thetestpeople.trt.webdriver.screens.HasMainMenu

class JenkinsJobsScreen(implicit automationContext: AutomationContext) extends AbstractScreen with HasMainMenu with JenkinsConfigurationScreen {

}