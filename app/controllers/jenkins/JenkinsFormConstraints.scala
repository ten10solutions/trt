package controllers.jenkins

import play.api.data.validation._
import java.net.URI
import viewModel.EditableJenkinsConfiguration
import com.thetestpeople.trt.model.CiType

object JenkinsFormConstraints {

  val isApiToken: Constraint[String] = Constraint(plainText ⇒
    if (plainText == "")
      Valid
    else if (plainText.matches("[a-f0-9]{32}"))
      Valid
    else
      invalid("Not an API key (32 hexadecimal characters)"))

  val isCiJob: Constraint[URI] = Constraint(uri ⇒
    CiType.inferCiType(uri) match {
      case Some(_) ⇒ Valid
      case None    ⇒ invalid("Job URL must be a link to either a Jenkins job or a TeamCity configuration")
    })

  val isJenkinsJob: Constraint[URI] = Constraint(uri ⇒
    if (!uri.getPath.contains("/job/"))
      invalid("Job URL must contain /job/")
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