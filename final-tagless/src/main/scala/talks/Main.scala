package talks

import scala.concurrent._
import cats.{Monad, Id}

trait UserOps[M[_]] {
  def findByEmail(email: String): M[Option[User]]

  def createSession(user: User): M[SessionKey]
}


trait LoggingOps[M[_]] {
  def info(message: String): M[Unit]

  def error(message: String, t: Throwable): M[Unit]
}

import cats.~>

class UserService[M[_] : Monad, F[_] : Monad](implicit userOps: UserOps[M], logger: LoggingOps[F], t: F ~> M) {

  import cats.implicits._

  def login(email: String, pass: String): M[Either[DbError, SessionKey]] =
    userOps.findByEmail(email).flatMap {
      case Some(user) =>
        if (user.pass == pass) {
          userOps.createSession(user)
            .flatMap { ret =>
              t(logger.info("Created session").map(_ => Right(ret)))
            }
        } else {
          t(logger.error("Sorry", new Exception("Password is bad")).map(_ => Left(PasswordsNotMatch)))
        }
      case _          =>
        t(logger.error("Sorry again", new Exception("User not found")).map(_ => Left(UserNotFound)))
    }
}


object Main extends App {

  import ExecutionContext.Implicits.global

  import talks.Implicits._
  import cats.implicits._

  implicit val transIdToFuture = new (Id ~> Future){
    override def apply[A](fa: Id[A]) = Future.successful(fa)
  }

  val userServiceF = new UserService[Future, Id]



  println(userServiceF.login("email", "pass"))
  println(userServiceF.login("email1", "pass"))


}
