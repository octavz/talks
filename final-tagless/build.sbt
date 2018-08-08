lazy val root = (project in file(".")).
  aggregate(app).
  settings(inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.11.12"
    )),
    name := "final-tagless-root"
  )

lazy val app = (project in file("app")).
  settings(
    name := "final-tagless"
  )

val catsVersion = "1.2.0"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % catsVersion
)
