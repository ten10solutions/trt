package com.thetestpeople.trt.utils

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest._
import com.thetestpeople.trt.utils.StringUtils._

@RunWith(classOf[JUnitRunner])
class AbbreviateDottedPrefixTest extends FlatSpec with Matchers {

  "It" should "work" in {

    abbreviateDottedPrefix("com.example.Test") should equal("c.e.Test")
    abbreviateDottedPrefix("com.Test") should equal("c.Test")
    abbreviateDottedPrefix("Test") should equal("Test")

    abbreviateDottedPrefix("") should equal("")
    abbreviateDottedPrefix(".") should equal(".")
    abbreviateDottedPrefix("..") should equal("..")
    abbreviateDottedPrefix("..Foo") should equal("..Foo")

  }

}