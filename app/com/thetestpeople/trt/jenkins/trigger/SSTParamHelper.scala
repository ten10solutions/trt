package com.thetestpeople.trt.jenkins.trigger

import com.thetestpeople.trt.model.QualifiedName

object SSTParamHelper {

  val VariableName = "SST_REGEXES"

  def sstRegexes(names: List[QualifiedName]): String =
    names.map(sstRegex).mkString(" ")

  private def sstRegex(fullName: QualifiedName): String = {
    val prefix = fullName.groupOpt.map(_ + ".").getOrElse("")
    val testId = prefix + fullName.name
    "^" + escapeRegex(testId) + "$"
  }

  /**
   * Escape characters to target Python re (which doesn't support \Q \E)
   */
  private def escapeRegex(s: String): String =
    s.flatMap {
      case c @ ('.' | '^' | '$' | '*' | '+' | '?' | '(' | ')' | '[' | '{' | '\\' | '|') ⇒ "\\" + c
      case c ⇒ c.toString
    }

}