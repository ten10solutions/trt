package com.thetestpeople.trt.model

object CiType {
  
  val Jenkins = CiType("Jenkins")
  val TeamCity = CiType("TeamCity")
  
}

case class CiType(name: String) {

}