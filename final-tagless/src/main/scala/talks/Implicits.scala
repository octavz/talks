package talks

import scala.concurrent._
import java.util.UUID._
import cats.Id

object Implicits {

  implicit val userOpsFuture: UserOps[Future] = new UserOps[Future] {
    def findByEmail(email: String): Future[Option[User]] =
      Future.successful {
        if (email == "email") Some(User("1", "email", "pass")) else None
      }

    def createSession(user: User): Future[SessionKey] =
      Future.successful(randomUUID().toString)
  }

  implicit val userOpsId: UserOps[Id] = new UserOps[Id] {
    def findByEmail(email: String): Id[Option[User]] =
      if (email == "email") Some(User("1", "email", "pass")) else None

    def createSession(user: User): Id[SessionKey] = randomUUID().toString
  }

  implicit val loggingOpsFuture = new LoggingOps[Future] {
    override def info(message: String) =
      Future.successful(println(message))

    override def error(message: String, t: Throwable) =
      Future.successful(println(s"$message\n${t.getMessage}"))
  }

  implicit val loggingOpsId = new LoggingOps[Id] {
    override def info(message: String) = println(message)

    override def error(message: String, t: Throwable) = println(s"$message\nException: ${t.getMessage}")
  }
}
