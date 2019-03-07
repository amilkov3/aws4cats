val http4s = "0.20.0-M6"
val confide = "0.0.2"
val circe = "0.11.1"

lazy val root = (project in file("."))
  .settings(
    Seq(
      name := "aws4cats",
      scalaVersion := "2.12.8",
      version := "0.0.1"
    )
  ).settings(
  credentials += Credentials(Path.userHome / ".sbt" / ".credentials"),
  resolvers ++= Seq("Sun Dev Releases" at "https://repo.artifacts.weather.com/sun-release-local")
).settings(
  libraryDependencies ++= Seq(
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "io.chrisdavenport" %% "log4cats-slf4j" % "0.3.0-M2",
    "io.circe" %% "circe-core" % circe,
    "io.circe" %% "circe-generic" % circe,
    "io.circe" %% "circe-java8" % circe,
    "io.circe" %% "circe-parser" % circe,
    "com.amazonaws" % "aws-java-sdk" % "1.11.513",
    //"com.amazonaws" % "aws-java-sdk-sqs" % "1.11.508",
    "org.http4s" %% "http4s-circe" % http4s,
    "org.typelevel" %% "cats-core" % "1.1.0",
    "org.typelevel" %% "cats-effect" % "1.0.0-RC2",

    "org.scalacheck" %% "scalacheck" % "1.14.0" % Test,
    "org.scalatest" %% "scalatest" % "3.0.5" % Test
  )
).settings(
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
).settings(
  scalacOptions ++= Seq(
    "-Xfatal-warnings",
    "-Ypartial-unification",
    //"-Ywarn-unused-import",
    "-feature",
    "-deprecation",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:experimental.macros"
  )
)
  .settings(
    mainClass in assembly := Some("com.weather.dngs.Service"),
    assemblyJarName in assembly := "dngs.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", _@_*) => MergeStrategy.discard
      case _ => MergeStrategy.first
    }
  )
  .settings(
    fork in Test := true,
    javaOptions in Test += "-Xmx2G"
  )
  .configs(IntegrationTest.extend(Test))
  .settings(Defaults.itSettings)
