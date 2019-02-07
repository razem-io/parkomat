name := """parkomat"""
maintainer := "julian+parkomat@razem.io"

lazy val commonSettings = Seq(
  version := "0.0.2-SNAPSHOT",
  scalaVersion := "2.12.3",
  organization := "io.razem"
)

val upickleV = "0.4.4"
lazy val server = (project in file("server")).settings(commonSettings).settings(
  scalaJSProjects := Seq(client),
  pipelineStages in Assets := Seq(scalaJSPipeline),
  pipelineStages := Seq(digest, gzip),
  buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
  packageName := "parkomat",
  // triggers scalaJSPipeline when using compile or continuous compilation
  compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
  libraryDependencies ++= Seq(
    "com.vmunier" %% "scalajs-scripts" % "1.1.1",
    "io.monix" %% "monix" % "3.0.0-RC2",
    "org.webjars.bower" % "bulma" % "0.6.1",
    "org.webjars" % "font-awesome" % "4.7.0",
    "org.webjars" % "jquery" % "3.2.1",
    "com.lihaoyi" %%% "upickle" % upickleV,
    guice,
    "org.scalatest" %% "scalatest" % "3.0.4" % Test,
    "org.scalamock" %% "scalamock" % "4.0.0" % Test,
    specs2 % Test,
    ws
  )
).enablePlugins(PlayScala, BuildInfoPlugin).
  dependsOn(sharedJvm)

lazy val client = (project in file("client")).settings(commonSettings).settings(
  scalaJSUseMainModuleInitializer := true,
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.9.3",
    "com.lihaoyi" %%% "upickle" % upickleV
  )
).enablePlugins(ScalaJSPlugin, ScalaJSWeb).
  dependsOn(sharedJs)

lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared")).settings(commonSettings)
lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

// loads the server project at sbt startup
onLoad in Global := (onLoad in Global).value andThen {s: State => "project server" :: s}
