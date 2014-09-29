import scala.concurrent.duration._
import com.thetestpeople.trt.analysis.AnalysisService
import com.thetestpeople.trt.Config
import com.thetestpeople.trt.model.impl.SlickDao
import com.thetestpeople.trt.model.impl.migration.DbMigrator
import com.thetestpeople.trt.service._
import com.thetestpeople.trt.utils._
import com.thetestpeople.trt.utils.RichConfiguration._
import com.thetestpeople.trt.utils.http.Http
import com.thetestpeople.trt.utils.http.PathCachingHttp
import com.thetestpeople.trt.utils.http.WsHttp
import com.thetestpeople.trt.jenkins.importer._
import com.thetestpeople.trt.service.indexing.LuceneLogIndexer
import controllers._
import play.api.Configuration
import play.api.Play
import play.api.db._
import play.api.mvc.Controller
import java.io.File
import com.thetestpeople.trt.service.indexing.LogIndexer

/**
 * Lightweight dependency injection: constructs the objects used by the application
 */
class Factory(configuration: Configuration) {

  import Config._

  lazy val jdbcUrl: String = configuration.getString(Db.Default.Url).getOrElse(
    throw new RuntimeException(s"No value set for property '${Db.Default.Url}'"))

  import play.api.Play.current
  lazy val dataSource = DB.getDataSource()

  lazy val dbMigrator = new DbMigrator(dataSource)

  lazy val dao: SlickDao = new SlickDao(jdbcUrl, Some(dataSource))

  lazy val clock: Clock = SystemClock

  lazy val initialDelay: Duration =
    configuration.getDuration(Http.Timeout, default = 60.seconds)

  lazy val urlFetchTimeout: Duration =
    configuration.getDuration(Http.Timeout, default = 60.seconds)

  lazy val useCachingHttp: Boolean =
    configuration.getBoolean(Http.UseCache).getOrElse(false)

  lazy val http: Http =
    if (useCachingHttp)
      new PathCachingHttp(new WsHttp(urlFetchTimeout))
    else
      new WsHttp(urlFetchTimeout)

  lazy val analysisService = new AnalysisService(dao, clock, async = true)

  lazy val service: Service = new ServiceImpl(dao, clock, http, analysisService, jenkinsImportStatusManager, batchRecorder, jenkinsImportWorker, logIndexer)

  lazy val adminService = new AdminServiceImpl(dao, logIndexer, analysisService)

  lazy val controller = new Application(service, adminService)

  lazy val jenkinsController = new JenkinsController(service)

  lazy val jenkinsImportStatusManager: JenkinsImportStatusManager = new JenkinsImportStatusManager(clock)

  lazy val jsonController = new JsonController(service, adminService)

  lazy val batchRecorder = new BatchRecorder(dao, clock, analysisService, logIndexer)

  lazy val jenkinsImporter = new JenkinsImporter(clock, http, dao, jenkinsImportStatusManager, batchRecorder)

  lazy val jenkinsImportWorker = new JenkinsImportWorker(dao, jenkinsImporter)

  lazy val luceneInMemory: Boolean =
    configuration.getBoolean(Lucene.InMemory).getOrElse(false)

  lazy val luceneIndexLocation: String = configuration.getString(Lucene.IndexDirectory).getOrElse(
    throw new RuntimeException(s"No value set for property '${Lucene.IndexDirectory}'"))

  lazy val logIndexer: LogIndexer =
    if (luceneInMemory)
      LuceneLogIndexer.memoryBackedIndexer
    else
      LuceneLogIndexer.fileBackedIndexer(new File(luceneIndexLocation))

  def getControllerInstance[A](clazz: Class[A]): A = controllerMap(clazz).asInstanceOf[A]

  lazy val controllerMap: Map[Class[_], Controller] = Map(
    classOf[Application] -> controller,
    classOf[JenkinsController] -> jenkinsController,
    classOf[JsonController] -> jsonController)
}
