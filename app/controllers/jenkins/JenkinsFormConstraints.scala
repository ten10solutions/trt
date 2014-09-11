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
      invalid("Not an API key (32 hexadecimal characters)"))

  val isJenkinsJob: Constraint[URI] = Constraint(uri ⇒
    if (!uri.getPath.contains("/job/"))
      invalid("Job URL must contain /job/")
    else if (!uri.getPath.endsWith("/")) // To try and make sure the import URL matches up with the job URL returned by the Jenkins API itself.
      invalid("Job URL must end with a /")
    else
      Valid)

  private def invalid(message: String) = Invalid(Seq(ValidationError(message)))

  val parametersAreAllDistinct = Constraint { config: EditableJenkinsConfiguration ⇒
    val duplicates = config.duplicateParamNames
    if (duplicates.isEmpty)
      Valid
    else
      Invalid(duplicates.map(param ⇒ ValidationError(s"Parameter '$param' has been specified more than once")))
  }

}