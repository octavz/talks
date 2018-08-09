package object talks {

  case class User(id: String, email: String, pass: String)

  type SessionKey = String

  sealed trait DbError

  case object PasswordsNotMatch extends DbError

  case object UserNotFound extends DbError
}
