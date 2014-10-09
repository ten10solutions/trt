package extensions

import play.api.mvc.PathBindable
import play.api.mvc.QueryStringBindable
import com.thetestpeople.trt.model.Id
import com.thetestpeople.trt.model.EntityType
import java.net.URI
import java.net.URISyntaxException
import com.thetestpeople.trt.model.Configuration
import com.thetestpeople.trt.model.TestStatus
import viewModel.Sort

object Binders {

  implicit val uriQueryStringBinder: QueryStringBindable[URI] = new QueryStringBindable[URI] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, URI]] =
      for {
        values ← params.get(key)
        value ← values.headOption
      } yield {
        try
          Right(new URI(value))
        catch {
          case _: URISyntaxException ⇒ Left(s"Invalid URI $value")
        }
      }
    override def unbind(key: String, uri: URI): String = s"$key=${uri.toString}"
  }

  implicit val testStatusPathBinder: PathBindable[TestStatus] = new PathBindable[TestStatus] {
    override def bind(key: String, value: String): Either[String, TestStatus] =
      TestStatus.unapply(value) match {
        case Some(status) ⇒ Right(status)
        case None         ⇒ Left(s"Unknown status type '$value'")
      }
    override def unbind(key: String, status: TestStatus): String = TestStatus.identifier(status)
  }

  implicit val testStatusQueryStringBinder: QueryStringBindable[TestStatus] = new QueryStringBindable[TestStatus] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, TestStatus]] =
      for {
        values ← params.get(key)
        value ← values.headOption
      } yield {
        TestStatus.unapply(value) match {
          case Some(status) ⇒ Right(status)
          case None         ⇒ Left(s"Unknown status type '$value'")
        }
      }
    override def unbind(key: String, statusFilter: TestStatus): String = s"$key=${TestStatus.identifier(statusFilter)}"
  }

  implicit val configurationQueryStringBinder: QueryStringBindable[Configuration] = new QueryStringBindable[Configuration] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Configuration]] =
      for {
        values ← params.get(key)
        value ← values.headOption
      } yield Right(Configuration(value))
    override def unbind(key: String, configuration: Configuration): String = s"$key=$configuration"
  }

  implicit def idQueryStringBinder[T <: EntityType](implicit longBinder: QueryStringBindable[String]) = new QueryStringBindable[Id[T]] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Id[T]]] =
      for {
        values ← params.get(key)
        value ← values.headOption
      } yield {
        Id.parse[T](value) match {
          case Some(status) ⇒ Right(status)
          case None         ⇒ Left(s"Unable to parse '$value' as an ID")
        }
      }
    override def unbind(key: String, id: Id[T]): String = id.toString
  }

  implicit def idPathBinder[T <: EntityType] = new PathBindable[Id[T]] {
    override def bind(key: String, value: String): Either[String, Id[T]] =
      Id.parse[T](value) match {
        case Some(id) ⇒ Right(id)
        case None     ⇒ Left(s"Unable to parse '$value' as an ID")
      }
    override def unbind(key: String, id: Id[T]): String = id.toString
  }

  implicit val sortQueryStringBinder: QueryStringBindable[Sort] = new QueryStringBindable[Sort] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Sort]] =
      for {
        values ← params.get(key)
        value ← values.headOption
      } yield Right(Sort(value).get)
    override def unbind(key: String, sort: Sort): String = s"$key=$sort"
  }

}