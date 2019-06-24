import microsites.ExtraMdFileConfig
import ReleaseTransformations._
import xerial.sbt.Sonatype.GitHubHosting

val http4s = "0.21.0-M1"
val confide = "0.0.2"
val circe = "0.12.0-M3"
val awssdk = "2.5.11"
val cats = "2.0.0-M4"

lazy val commonSettings = Seq(
  isTravisBuild := true,
  scalacOptions ++= {
    if (is13OrAfter(scalaVersion.value)) "-Ymacro-annotations" :: Nil
    else "-Ypartial-unification" :: Nil
  },
  libraryDependencies ++= {
    if (is13OrAfter(scalaVersion.value)) Nil
    else compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full) :: Nil
  },
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"),
  organization := "ml.milkov",
  scalacOptions ++= Seq(
    //"-Xfatal-warnings",
    "-feature",
    "-deprecation",
    "-language:higherKinds",
    "-language:implicitConversions",
  ),
  scalacOptions in Test ++= Seq("-Yrangepos"),
  fork in Test := true,
  updateOptions := updateOptions.value.withGigahorse(false),
  javaOptions in Test ++= Seq("-Xmx2G", "-XX:MaxMetaspaceSize=1024M")
)


def is13OrAfter(scalaVersion: String): Boolean =
  CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, n)) if n >= 13 => true
      case _ => false
    }

lazy val root = (project in file("."))
  .settings(noPublishSettings)
  .aggregate(core, dynamodb, s3, sqs)
  /*.configs(IntegrationTest.extend(Test))
  .settings(Defaults.itSettings)*/

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

lazy val releasePublishSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
    pushChanges
  ),
  homepage := Some(url("https://github.com/amilkov3/aws4cats")),
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  sonatypeProfileName := "ml.milkov",
  sonatypeProjectHosting := Some(GitHubHosting("amilkov3", "aws4cats", "amilkov3@gmail.com")),
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/amilkov3/aws4cats"),
      "scm:git@github.com/amilkov3/aws4cats.git"
    )
  ),
  developers := List(
    Developer("amilkov3",  "Alex Milkov", "amilkov3@gmail.com", url("https://milkov.ml"))
  )
)

lazy val core = project.in(file("core"))
  .settings(name := "aws4cats-core")
  .settings(commonSettings)
  .settings(releasePublishSettings)
  .settings(
    libraryDependencies ++= deps ++ Seq(
      "software.amazon.awssdk" % "aws-core" % awssdk,
      "software.amazon.awssdk" % "netty-nio-client" % awssdk
    )
  )

lazy val docs = project.in(file("docs"))
  .enablePlugins(MicrositesPlugin)
  .enablePlugins(GhpagesPlugin)
  .enablePlugins(ScalaUnidocPlugin)
  .settings(
    git.remoteRepo := "git@github.com:amilkov3/aws4cats.git",
    ghpagesNoJekyll := true,
    ghpagesRepository := file("git@github.com:amilkov3/aws4cats.git"),
    excludeFilter in ghpagesCleanSite :=
      new FileFilter{
        def accept(f: File): Boolean = (ghpagesRepository.value / "CNAME").getCanonicalPath == f.getCanonicalPath
      } || "versions.html"
  )
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    micrositeName := "aws4cats",
    micrositeBaseUrl := "",
    micrositeDescription := "Purely functional clients for AWS services",
    micrositeDocumentationUrl := "/api",
    micrositeGithubOwner := "amilkov3",
    micrositeGithubRepo := "aws4cats",
    micrositeHomepage := "https://aws4cats.milkov.ml",
    micrositeExtraMdFiles := Map(
      file("README.md") -> ExtraMdFileConfig(
        "index.md",
        "home",
        Map("section" -> "home", "position" -> "0")
      )
    )
  ).settings(
  crossScalaVersions := crossScalaVersions.value.filterNot(_.startsWith("2.13")),
  addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), micrositeDocumentationUrl),
    unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(dynamodb, s3)
  )
  .dependsOn(core, sqs)

lazy val dynamodb = project.in(file("dynamodb"))
  .settings(name := "aws4cats-dynamodb")
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    libraryDependencies ++=
      (deps :+ ("software.amazon.awssdk" % "dynamodb" % awssdk))
  )
  .dependsOn(core % "compile->compile;test->test")

lazy val s3 = project.in(file("s3"))
  .settings(name := "aws4cats-s3")
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    libraryDependencies ++=
      (deps :+ ("software.amazon.awssdk" % "s3" % awssdk))
  )
  .dependsOn(core % "compile->compile;test->test")

lazy val sqs = project.in(file("sqs"))
  .settings(name := "aws4cats-sqs")
  .settings(commonSettings)
  .settings(releasePublishSettings)
  /*.settings(
    scalacOptions += "-Ywarn-unused-import"
  )*/
  .settings(
    libraryDependencies ++=
      (deps :+ ("software.amazon.awssdk" % "sqs" % awssdk))
  )
  .dependsOn(core % "compile->compile;test->test")

lazy val deps = Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "eu.timepit" %% "refined" % "0.9.8",
  "io.chrisdavenport" %% "log4cats-slf4j" % "0.4.0-M1",
  /*"io.circe" %% "circe-core" % circe,
  "io.circe" %% "circe-java8" % circe,
  "io.circe" %% "circe-parser" % circe,*/
  "org.http4s" %% "http4s-core" % http4s,
  "org.typelevel" %% "cats-core" % cats,
  "org.typelevel" %% "cats-effect" % cats,
  "uk.com.robust-it" % "cloning" % "1.9.12",

  "io.circe" %% "circe-generic" % circe % Test,
  "org.http4s" %% "http4s-circe" % http4s % Test,
  "org.scalacheck" %% "scalacheck" % "1.14.0" % Test,
  "org.specs2" %% "specs2-core" % "4.5.1" % Test
)

