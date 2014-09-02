package controllers.jenkins

import play.api.data.validation._
import java.net.URI
import viewModel.EditableJenkinsConfiguration

object JenkinsFormConstraints {

  val isApiToken: Constraint[String] = Constraint(plainText ⇒
    if (plainText == "")
      Valid
    else if (plainText.matches("[a-f0-9]{32}"))
      Valid
    else
      Invalid(Seq(ValidationError("Not an API key (32 hexadecimal characters)"))))

  val isJenkinsJob: Constraint[URI] = Constraint(uri ⇒
    if (uri.getPath().contains("/job/"))
      Valid
    else
      Invalid(Seq(ValidationError("Not a Jenkins job URL"))))

  val parametersAreAllDistinct = Constraint { config: EditableJenkinsConfiguration ⇒
    val duplicates = config.duplicateParamNames
    if (duplicates.isEmpty)
      Valid
    else
      Invalid(duplicates.map(param ⇒ ValidationError(s"Parameter '$param' has been specified more than once")))
  }

}