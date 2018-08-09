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
}
