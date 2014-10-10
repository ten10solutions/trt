package controllers

import play.api.data.validation.ValidationError
import play.api.data.validation.Valid
import play.api.data.validation.Constraint
import play.api.data.validation.Invalid
import org.joda.time.Duration

object SystemConfigurationFormConstraints {

  val isNonNegativeDuration: Constraint[Duration] = Constraint(duration ⇒
    if (duration.getMillis >= 0)
      Valid
    else
      invalid("Duration cannot be negative"))

  val isNonNegative: Constraint[Int] = Constraint(n ⇒
    if (n >= 0)
      Valid
    else
      invalid("Count cannot be negative"))

  private def invalid(message: String) = Invalid(Seq(ValidationError(message)))

}