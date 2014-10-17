import scala.concurrent.duration._
import play.api._
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import com.thetestpeople.trt.utils.RichConfiguration._
import com.thetestpeople.trt.Config._
import com.thetestpeople.trt.utils.HasLogger
import scala.concurrent.Future

object Global extends GlobalSettings with HasLogger {

  private lazy val factory = new Factory(Play.current.configuration)

  override def onStart(app: Application) {
    factory.dbMigrator.migrate()

    initialiseCiImportWorker(app)
    initialiseJenkinsPoller(app)
    initialiseAnalyseExecutionsPoller(app)
  }

  private def initialiseCiImportWorker(app: Application) {
    Future {
      factory.ciImportWorker.run()
    }
  }

  private def getDuration(configuration: Configuration, key: String, default: FiniteDuration): FiniteDuration =
    configuration.getMilliseconds(key).map(_.millis).getOrElse(default)

  private def initialiseJenkinsPoller(app: Application) {
    val conf = app.configuration
    val initialDelay = conf.getDuration(Jenkins.Poller.InitialDelay, default = 1.minute)
    val interval = conf.getDuration(Jenkins.Poller.Interval, default = 1.minute)

    if (conf.getBoolean(Jenkins.Poller.Enabled).getOrElse(true)) {
      Akka.system(app).scheduler.schedule(initialDelay, interval) {
        factory.service.syncAllJenkins()
      }
      logger.info("Initialised Jenkins poller")
    }
  }

  private def initialiseAnalyseExecutionsPoller(app: Application) {
    val conf = app.configuration
    val initialDelay = conf.getDuration(CountsCalculator.Poller.InitialDelay, default = 5.seconds)
    val interval = conf.getDuration(CountsCalculator.Poller.Interval, default = 2.minutes)

    Akka.system(app).scheduler.scheduleOnce(Duration.Zero) {
      factory.service.analyseAllExecutions()
    }
    Akka.system(app).scheduler.schedule(initialDelay, interval) {
      factory.service.analyseAllExecutions()
    }
    logger.info("Scheduled analysis of all executions")
  }

  override def onStop(app: Application) {
    factory.ciImportWorker.stop()
  }

  override def getControllerInstance[A](clazz: Class[A]): A = factory.getControllerInstance(clazz)

}
