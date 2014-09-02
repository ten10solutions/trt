package com.thetestpeople.trt.webdriver.screens

trait HasMainMenu { self: AbstractScreen ⇒

  def mainMenu: MainMenu = new MainMenu

}