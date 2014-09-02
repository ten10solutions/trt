package com.thetestpeople.trt.utils

object StringUtils {

  /**
   * Insert an ellipsis in the middle of the given string so as to avoid the string exceeding maxLength.
   * For example:
   *
   * ellipsisiseMiddle("1234567890", maxLength = 9) should equal ("123...890")
   *
   * @param Maximum length of the string, must be at least 5
   */
  def ellipsisiseMiddle(s: String, maxLength: Int) = {
    require(maxLength >= 5)
    if (s.size > maxLength) {
      val excess = s.size - maxLength + 3
      val (left, right) = s.splitAt(s.size / 2)
      val rightHeavy = s.size % 2 == 1
      val (leftTrim, rightTrim) =
        if (rightHeavy)
          (halveRoundingDown(excess), halveRoundingUp(excess))
        else
          (halveRoundingUp(excess), halveRoundingDown(excess))
      val leftHalf = left.dropRight(leftTrim)
      val rightHalf = right.drop(rightTrim.toInt)
      s"$leftHalf...$rightHalf"
    } else
      s
  }

  private def halveRoundingDown(n: Int) = n / 2
  private def halveRoundingUp(n: Int) = math.ceil(n / 2.0).toInt

  /**
   * Abbreviate dotted segments, for example:
   *
   * foo.bar.baz.Wibble => f.b.b.Wibble
   */
  def abbreviateDottedPrefix(s: String): String = {
    val chunks = s.split("\\.")
    if (chunks.size > 1)
      chunks.init.map(_.headOption.getOrElse("")).mkString(".") + "." + chunks.last
    else
      s
  }

}