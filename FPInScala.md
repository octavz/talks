



-> # Introduction to types with application in `final tagless` paradigm


-> # How to abstract away effects using types


---

# The problem

We often need to implement a `repository` and we basically do it like this:

```scala
trait UserOps {
  def findByEmail(email: String): Future[Option[User]]
  def createSession(user: User): Future[SessionKey]
}

class UserService(userOps: UserOps) {
  def login(email: String, pass: String)
    (implicit ec: ExecutionContext): Future[Either[Error, SessionKey]] = 
    userOps.findByEmail(email) flatMap { 
      case Some(user) => 
          if(user.pass == pass) {
            userOps.createSession(user) map ( Right(_) )
          } else Future.successful(Left(PasswordsNotMatch))
      case _ => Future.successful(Left(UserNotFound))
    }
}

```

---

# Do we really need `Future` anywhere ?

```scala
trait UserOps[F[_]] { //F is here a higher kinded type 
  def findByEmail(email: String): F[Option[User]]
  def createSession(user: User): F[SessionKey]
}

class UserService(userOps: UserOps) {
  def login(email: String, pass: String): F[Either[Error, SessionKey]] = 
    userOps.findByEmail(email) flatMap { 
      case Some(user) => 
          if(user.pass == pass) {
            userOps.createSession(user) map ( Right(_) )
          } else ???(Left(PasswordsNotMatch))
      case _ => ???(Left(UserNotFound))
    }
  } 
}
```

---

-> # What are kinds ?

## In type theory a kind is the type of a `type contructor`

-> # Alternative definitions

## A kind is a type of a `data type`, 

## A kind is an `arity specifier`

---

# Not very helpful...

## Let's see some examples: 

 - The kind of `Int` is `*`
 - The kind of `String` is `*`
 - The kind of `List[Double]` is `*`
 - The kind of `Map[String, List[(String, Int)]]` is `*`

We call `*` simple types.
All the types a compiler knows when is done compiling are simple types.
(supposing we are not using Java :) )

## Let's see some more:

 - The kind of `Option[T]` is `* -> \*`
 - The kind of `List[T]` is `* -> \*`
 - The kind of `Future[T]` is `* -> \*`
 - The kind of `Either[L, R]` is `* -> \* -> \*`
 - The kind of `Map[K, V]` is `* -> \* -> \*`
 - The kind of `[A,B,C,D]` is `* -> \* -> \* -> \* -> \*`

We can see the type constructors (ie. `List[A]`) 
as function of a type and returning another type 

(ie. `List[A]` given a `Double` will return a `List[Double]`)

---

# All static typed languages have `kinds` at some level (ie. generics, macros, etc.)

Given: `List[T]` most of the language cannot abstract over the `T` 
(they can enforce rules but not abstract).  

Some languages can.
How ?

 - `List` and `Future` abstract over a type `T`
 - `List` and `Future` have a `map` function.

---

# OOP 

```java
interface TypeWithMap<A, F<???>> {
  T<B> map<B>(f: Function<A,B>)
}

class List extends TypeWithMap<A, List<?>> {
  List<B> map<B>(f: Function<A,B>)  = …
}

class Future extends TypeWithMap<A, Future<?>> {
  Future<B> map<B>(f: Function<A,B>)   = …
}
```

But this will not work in Java/C#.
They can't parameterize using a `\* -> \*` kind.
They can only use simple types (kinds like `\*`).

---

# Scala 

However Scala can do it 

```scala
trait TypeWithMap[A, F[_]] {
  T[B] map[B](f: A => B)
}

class List extends TypeWithMap[A, List] {
  List[B] map[B](f: A => B)  = …
}

class Future extends TypeWithMap[A, Future] {
  Future[B] map[B](f: A => B)  = …
}
```

This means Scala *supports* `higher kinded types`

---

# Do we really need `Future` anywhere ?

```scala
trait UserOps[F[_]] { //F is here a higher kinded type 
  def findByEmail(email: String): F[Option[User]]
  def createSession(user: User): F[SessionKey]
}

class UserService(userOps: UserOps[F]) {
  def login(email: String, pass: String): F[Either[Error, SessionKey]] = 
    userOps.findByEmail(email) flatMap { 
      case Some(user) => 
          if(user.pass == pass) {
            userOps.createSession(user) map ( Right(_) )
          } else ???(Left(PasswordsNotMatch))
      case _ => ???(Left(UserNotFound))
    }
  } 
}
```

---

# Cats to the rescue

```scala
import cats._

trait UserOps[F[_]] {
  def findByEmail(email: String): F[Option[User]]
  def createSession(user: User): F[SessionKey]
}

class UserService[F[_]: Monad](implicit userOps: UserOps[F]) {
  def login(email: String, pass: String): F[Either[Error, SessionKey]] = 
    userOps.findByEmail(email) flatMap { 
      case Some(user) => 
          if(user.pass == pass) {
            userOps.createSession(user) map ( Right(_) )
          } else Monad[F].pure(Left(PasswordsNotMatch))
      case _ => Monad[F].pure(Left(UserNotFound))
    }
}
```

```scala
trait Monad[F[_]] {
  def flatMap[A, B](fa: F[A])(f: (A) ⇒ F[B]): F[B]
  def pure[A](x: A): F[A] 
  def map[A, B](fa: F[A])(f: (A) ⇒ B): F[B] = 
    flatMap(fa) ( a => pure(f(a)) )
}
```

---

# Future implementation (interpreter)

```scala
trait FutureUserOpsImpl extends UserOps[Future] {
  override def findByEmail(user: User): Future[Option[User] = 
    Future.successful(Some(User(…))) //find user in database
  override def createSession(user: User): Future[SessionKey] = 
    Future.successful("") // create session in db and return the key
}

val loginResult: Future[Either[Error, SessionKey]] = 
  new UserService(new FutureUserOpsImpl{}).login("user", "password")
```

---

# Task implementation (interpreter)

```scala
trait TaskUserOpsImpl extends UserOps[Task] {
  override def findByEmail(user: User): Task[Option[User] = 
    Task.unit(Some(User(…))) //find user in database
  override def createSession(user: User): Task[SessionKey] = 
    Task.unit("") // create session in db and return the key
}

val loginResult: Task[Either[Error, SessionKey]] = 
  new UserService(new TaskUserOpsImpl{}).login("user", "password")
```

---

# Combine algebras 

```scala
trait UserOps[F[_]] {
  def findByEmail(email: String): F[Option[User]]
  def createSession(user: User): F[SessionKey]
}

trait LoggingOps[F[_]] {
  def info(message: String): F[Unit]
  def error(message: String, t: Throwable): F[Unit]
}

class UserService[F[_]: Monad](userOps: UserOps[F], logger: LoggingOps[F]) {
  def login(email: String, pass: String): F[Either[Error, SessionKey]] = 
    userOps.findByEmail flatMap { 
      case Some(user) => 
          if(user.pass == pass) {
            logger.info("Created session")
            userOps.createSession(user) map ( Right(_) )
          } else {
            logger.error("Sorry", new Exception("Password is bad"))
            Monad[F].pure(Left(PasswordsNotMatch))
          }
      case _ => {
        logger.error("Sorry again", new Exception("User not found"))
        Monad[F].pure(Left(UserNotFound))
      }
    }
  } 
}
```

---

More reads:
  - Expression problem
  - Object algebras
  - F-Bounded context
  - Free Monad
