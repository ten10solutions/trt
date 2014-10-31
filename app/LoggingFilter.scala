import play.api.Logger
import play.api.mvc._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object LoggingFilter extends Filter {

  def apply(nextFilter: (RequestHeader) ⇒ Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    val startTime = System.currentTimeMillis
    nextFilter(requestHeader).map { result ⇒
      val duration = System.currentTimeMillis - startTime
      if (!requestHeader.uri.startsWith("/assets/"))
        Logger.info(s"[${duration}ms] ${requestHeader.method} ${requestHeader.uri} => ${result.header.status}")
      result
    }
  }

}