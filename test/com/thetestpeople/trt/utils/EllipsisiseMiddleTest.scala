package com.thetestpeople.trt.utils

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest._
import com.thetestpeople.trt.utils.StringUtils._

@RunWith(classOf[JUnitRunner])
class EllipsisiseMiddleTest extends FlatSpec with Matchers {

  "An even length string" should "have an ellipsis inserted in the middle" in {

    ellipsisiseMiddle("1234567890", maxLength = 11) should equal("1234567890")
    ellipsisiseMiddle("1234567890", maxLength = 10) should equal("1234567890")
    ellipsisiseMiddle("1234567890", maxLength = 9) should equal("123...890")
    ellipsisiseMiddle("1234567890", maxLength = 8) should equal("12...890")
    ellipsisiseMiddle("1234567890", maxLength = 7) should equal("12...90")
    ellipsisiseMiddle("1234567890", maxLength = 6) should equal("1...90")
    ellipsisiseMiddle("1234567890", maxLength = 5) should equal("1...0")

  }

  "An odd length string" should "have an ellipsis inserted in the middle" in {

    ellipsisiseMiddle("123456789", maxLength = 9) should equal("123456789")
    ellipsisiseMiddle("123456789", maxLength = 8) should equal("12...789")
    ellipsisiseMiddle("123456789", maxLength = 7) should equal("12...89")
    ellipsisiseMiddle("123456789", maxLength = 6) should equal("1...89")
    ellipsisiseMiddle("123456789", maxLength = 5) should equal("1...9")

  }

  "Small strings" should "be unaffected" in {

    ellipsisiseMiddle("1", maxLength = 5) should equal("1")
    ellipsisiseMiddle("", maxLength = 5) should equal("")

  }

  "An exception" should "be thrown if maxLength is less than 5" in {

    an[IllegalArgumentException] should be thrownBy { ellipsisiseMiddle("12345", maxLength = 3) }

  }

}