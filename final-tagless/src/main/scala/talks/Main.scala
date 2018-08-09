package talks

import scala.concurrent._
import cats.{Monad, Id}

trait UserOps[M[_]] {
  def findByEmail(email: String): M[Option[User]]

  def createSession(user: User): M[SessionKey]
}

class UserService[M[_] : Monad](implicit userOps: UserOps[M]) {
  import cats.implicits._

  def login(email: String, pass: String): M[Either[DbError, SessionKey]] = {

    userOps.findByEmail(email).flatMap {
      case Some(user) =>
        if (user.pass == pass) {
          userOps.createSession(user) map (Right(_))
        } else Monad[M].pure(Left(PasswordsNotMatch))
      case _          => Monad[M].pure(Left(UserNotFound))
    }
  }
}



object Main extends App {
  import ExecutionContext.Implicits.global

  import talks.Implicits._
  import cats.implicits._

  val userServiceF = new UserService[Future]
  val userServiceI= new UserService[Id]

  println(userServiceF.login("email", "pass"))
  println(userServiceF.login("email1", "pass"))

  println(userServiceI.login("email", "pass"))
  println(userServiceI.login("email1", "pass"))

}
