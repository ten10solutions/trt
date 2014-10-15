package com.thetestpeople.trt.model

object CIType {
  
  def Jenkins = CIType("Jenkins")
  def TeamCity = CIType("TeamCity")
  
}

case class CIType(name: String) {

}