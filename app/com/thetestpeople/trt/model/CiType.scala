package com.thetestpeople.trt.model

object CiType {
  
  def Jenkins = CiType("Jenkins")
  def TeamCity = CiType("TeamCity")
  
}

case class CiType(name: String) {

}