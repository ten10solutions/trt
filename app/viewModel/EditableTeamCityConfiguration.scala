package viewModel

import java.net.URI
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import com.thetestpeople.trt.utils.http.Credentials

object EditableTeamCityConfiguration {

  def apply(config: TeamCityConfiguration): EditableTeamCityConfiguration =
    EditableTeamCityConfiguration(credentialsOpt = config.credentialsOpt)

}

case class EditableTeamCityConfiguration(credentialsOpt: Option[Credentials]) {

  def usernameOpt: Option[String] = credentialsOpt.map(_.username)

  def passwordOpt: Option[String] = credentialsOpt.map(_.password)

  def asTeamCityConfiguration = TeamCityConfiguration(usernameOpt = usernameOpt, passwordOpt = passwordOpt)

}