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

  private val Alphabet: Stream[String] = Stream("abcdefghijklmnopqrstuvwxyz".map(_.toString): _*)

  def wordsN(len: Int): Stream[String] =
    if (len <= 1)
      Alphabet
    else
      Alphabet.flatMap(c ⇒ wordsN(len - 1).map(w ⇒ c + w))

  /**
   * Stream of words: "a", "b", ..., "z", "aa", "ab", ..., "zz", ..
   */
  def words: Stream[String] = Stream.from(1).flatMap(wordsN)

  def ordinalName(n: Int): String = {
    val suffix =
      n match {
        case 11 | 12 | 13 ⇒ "th"
        case _ ⇒ (n % 10) match {
          case 1 ⇒ "st"
          case 2 ⇒ "nd"
          case 3 ⇒ "rd"
          case _ ⇒ "th"
        }
      }
    n + suffix
  }

}