package com.thetestpeople.trt.utils

import org.apache.commons.validator.routines._
import org.joda.time.Duration
import play.api.data._
import play.api.data.format.Formatter
import play.api.data.validation._
import java.net.URI
import com.thetestpeople.trt.model.Configuration

object FormUtils {

  private lazy val durationParser = DurationParser()

  implicit val durationFormat = new Formatter[Duration] {
    def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Duration] =
      data.get(key).flatMap(durationParser.parse).toRight(formError(key))

    private def formError(key: String) = Seq(FormError(key, "Not a valid duration", Nil))

    def unbind(key: String, value: Duration) = Map(key -> durationParser.asString(value))
  }

  def duration = Forms.of[Duration]

  implicit val urlFormat = new Formatter[URI] {

    def bind(key: String, data: Map[String, String]): Either[Seq[FormError], URI] =
      data.get(key).flatMap(parseURL).toRight(formError(key))

    private def formError(key: String) = Seq(FormError(key, "Not a valid URL", Nil))

    def unbind(key: String, value: URI) = Map(key -> value.toString)
  }

  def url = Forms.of[URI]

  private def parseURL(s: String): Option[URI] =
    if (urlValidator.isValid(s))
      Some(new URI(s))
    else
      None

  implicit val configurationFormat = new Formatter[Configuration] {

    def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Configuration] =
      data.get(key).map(Configuration.apply).toRight(formError(key))

    private def formError(key: String) = Seq(FormError(key, "Not a valid Configuration", Nil))

    def unbind(key: String, value: Configuration) = Map(key -> value.configuration)

  }

  def configuration = Forms.of[Configuration]

  private lazy val urlValidator: UrlValidator =
    new UrlValidator(Array("http", "https"), new RegexValidator("^([\\p{Alnum}\\-\\.]*)(:\\d*)?(.*)?"), 0)

  lazy val isUrl: Constraint[String] = Constraint(plainText ⇒
    if (urlValidator.isValid(plainText))
      Valid
    else
      Invalid(Seq(ValidationError("Not a well-formed URL"))))

}