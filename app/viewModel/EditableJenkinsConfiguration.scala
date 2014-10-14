package viewModel

import java.net.URI
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import com.thetestpeople.trt.utils.http.Credentials

object EditableJenkinsConfiguration {

  def apply(fullConfig: FullJenkinsConfiguration): EditableJenkinsConfiguration =
    EditableJenkinsConfiguration(
      credentialsOpt = fullConfig.config.credentialsOpt,
      rerunJobUrlOpt = fullConfig.config.rerunJobUrlOpt,
      authenticationTokenOpt = fullConfig.config.authenticationTokenOpt,
      params = fullConfig.params)

}

case class EditableJenkinsConfiguration(
    credentialsOpt: Option[Credentials],
    rerunJobUrlOpt: Option[URI],
    authenticationTokenOpt: Option[String],
    params: Seq[JenkinsJobParam]) {

  def usernameOpt: Option[String] = credentialsOpt.map(_.username)

  def apiTokenOpt: Option[String] = credentialsOpt.map(_.password)

  def asJenkinsConfiguration =
    FullJenkinsConfiguration(
      config = JenkinsConfiguration(
        usernameOpt = usernameOpt,
        apiTokenOpt = apiTokenOpt,
        rerunJobUrlOpt = rerunJobUrlOpt,
        authenticationTokenOpt = authenticationTokenOpt),
      params = params)

  def duplicateParamNames: Seq[String] = params.groupBy(_.param).filter(_._2.size > 1).map(_._1).toSeq.sorted

}