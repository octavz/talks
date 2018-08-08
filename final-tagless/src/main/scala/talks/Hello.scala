package talks

import scala.concurrent._
import java.util.UUID._

object Types {

  case class User(id: String, email: String, pass: String)

  type SessionKey = String

  sealed trait DbError

  case object PasswordsNotMatch extends DbError

  case object UserNotFound extends DbError

}


import Types._

trait UserOps {
  def findByEmail(email: String): Future[Option[User]]

  def createSession(user: User): Future[SessionKey]
}

trait UserOpsMemory extends UserOps {
  def findByEmail(email: String): Future[Option[User]] =
    Future.successful(Some(User("1", "email", "pass")))

  def createSession(user: User): Future[SessionKey] =
    Future.successful(randomUUID().toString)

}

class UserService(userOps: UserOps) {
  def login(email: String, pass: String)
    (implicit ec: ExecutionContext): Future[Either[DbError, SessionKey]] =
    userOps.findByEmail(email) flatMap {
      case Some(user) =>
        if (user.pass == pass) {
          userOps.createSession(user) map (Right(_))
        } else Future.successful(Left(PasswordsNotMatch))
      case _          => Future.successful(Left(UserNotFound))
    }
}

object Hello extends App {

  import ExecutionContext.Implicits.global
  import duration._

  val userService = new UserService(new UserOpsMemory {})
  val key = Await.result(userService.login("email", "pass"), 10.seconds)
  println(s"Session key is $key")
}
