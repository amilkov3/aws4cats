import microsites.ExtraMdFileConfig

val http4s = "0.20.0-M6"
val confide = "0.0.2"
val circe = "0.11.1"
val awssdk = "2.5.11"

lazy val commonSettings = Seq(
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.9"),
  scalacOptions ++= Seq(
    "-Xfatal-warnings",
    "-Ypartial-unification",
    //"-Ywarn-unused-import",
    "-feature",
    "-deprecation",
    "-language:higherKinds",
    "-language:implicitConversions",
  )
)

lazy val root = (project in file("."))
  .settings(noPublishSettings)
  .settings(
    scalaVersion := "2.12.8"
  )
  .settings(
    fork in Test := true,
    javaOptions in Test += "-Xmx2G"
  )
  .aggregate(core, dynamodb, s3, sqs)
  /*.configs(IntegrationTest.extend(Test))
  .settings(Defaults.itSettings)*/


lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

lazy val core = project.in(file("core"))
  .settings(name := "aws4cats-core")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= deps ++ Seq(
      "software.amazon.awssdk" % "aws-core" % awssdk,
      "software.amazon.awssdk" % "netty-nio-client" % awssdk
    )
  )

lazy val docs = project.in(file("docs"))
  .enablePlugins(MicrositesPlugin)
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    micrositeName := "aws4cats",
    micrositeDescription := "Purely functional clients for AWS services",
    micrositeGithubOwner := "amilkov3",
    micrositeGithubRepo := "aws4cats",
    micrositeExtraMdFiles := Map(
      file("README.md") -> ExtraMdFileConfig(
        "index.md",
        "home",
        Map("section" -> "home", "position" -> "0")
      )
    )
  )
  .dependsOn(core)

lazy val dynamodb = project.in(file("dynamodb"))
  .settings(name := "aws4cats-dynamodb")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++=
      (deps :+ ("software.amazon.awssdk" % "dynamodb" % awssdk))
  )
  .dependsOn(core % "compile->compile")

lazy val s3 = project.in(file("s3"))
  .settings(name := "aws4cats-s3")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++=
      (deps :+ ("software.amazon.awssdk" % "s3" % awssdk))
  )
  .dependsOn(core % "compile->compile")

lazy val sqs = project.in(file("sqs"))
  .settings(name := "aws4cats-sqs")
  .settings(commonSettings)
  .settings(
    libraryDependencies ++=
      (deps :+ ("software.amazon.awssdk" % "sqs" % awssdk))
  )
  .dependsOn(core % "compile->compile")

lazy val deps = Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "io.chrisdavenport" %% "log4cats-slf4j" % "0.3.0-M2",
  /*"io.circe" %% "circe-core" % circe,
  "io.circe" %% "circe-generic" % circe,
  "io.circe" %% "circe-java8" % circe,
  "io.circe" %% "circe-parser" % circe,*/
  "org.http4s" %% "http4s-core" % http4s,
  "org.typelevel" %% "cats-core" % "1.1.0",
  "org.typelevel" %% "cats-effect" % "1.0.0-RC2",
  "uk.com.robust-it" % "cloning" % "1.9.12",

  "org.scalacheck" %% "scalacheck" % "1.14.0" % Test,
  "org.scalatest" %% "scalatest" % "3.0.5" % Test
)
