import scala.sys.process._
import sbtcrossproject.CrossPlugin.autoImport.{CrossType, crossProject}

ThisBuild / scalaVersion := "2.12.10"
Global / cancelable := true

val gCloudFunctionName   = settingKey[String]("Name of gCloud function")
val gCloudRegion         = settingKey[String]("Google Cloud region")
val gCloudApiDeploy      = taskKey[Unit]("Deploy OpenAPI to gCloud")
val gCloudFunctionDeploy = taskKey[Unit]("Deploy function to gCloud")

lazy val macroParadise = "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full

val commonSettings = Seq(
  organization := "com.github.esgott",
  addCompilerPlugin(macroParadise),
  wartremoverErrors ++= Warts.unsafe,
  scalacOptions ++= Seq(
    "-encoding",
    "UTF-8",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:postfixOps",
    "-unchecked",
    "-deprecation",
    "-feature",
    "-Ywarn-unused-import",
    "-Xlint:-unused",
    "-Ywarn-dead-code",
    "-Ywarn-value-discard",
    "-Ywarn-inaccessible",
    "-Ypartial-unification"
  )
)

val gcloudApiSettings = commonSettings ++ Seq(
  gCloudApiDeploy := {
    val openApiYaml = baseDirectory.value / "openapi.yaml"
    val project     = "--project grounded-cider-254518"
    s"gcloud endpoints services deploy ${openApiYaml.getAbsolutePath} $project" !!
  }
)

def gCloudFunctionSettings(functionName: String) = commonSettings ++ Seq(
  scalaJSLinkerConfig ~= {
    _.withModuleKind(ModuleKind.CommonJSModule)
  },
  libraryDependencies += "io.scalajs.npm" %%% "express" % "0.4.2",
  gCloudFunctionName := functionName,
  gCloudRegion := "europe-west1",
  gCloudFunctionDeploy := {
    val gcTarget = target.value / "gcloud"
    IO.copyFile((fastOptJS in Compile).value.data, gcTarget / "function.js")
    val functionName = gCloudFunctionName.value
    val source       = s"--source ${gcTarget.getAbsolutePath}"
    val runtime      = "--runtime nodejs8"
    val triggerHttp  = "--trigger-http"
    val region       = s"--region ${gCloudRegion.value}"
    s"gcloud functions deploy $functionName $source $runtime $triggerHttp $region" !!
  }
)

lazy val circeVersion = "0.12.2"
lazy val tapirVersion = "0.11.7"

lazy val `full-pocket-api` = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("api"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core"           % circeVersion,
      "io.circe" %%% "circe-generic"        % circeVersion,
      "io.circe" %%% "circe-generic-extras" % circeVersion
    )
  )

lazy val `full-pocket-api-jvm` = `full-pocket-api`.jvm
lazy val `full-pocket-api-js`  = `full-pocket-api`.js

lazy val `full-pocket-api-generator` = (project in file("api-generator"))
  .dependsOn(`full-pocket-api-jvm`)
  .settings(gcloudApiSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.tapir" %% "tapir-core"               % tapirVersion,
      "com.softwaremill.tapir" %% "tapir-json-circe"         % tapirVersion,
      "com.softwaremill.tapir" %% "tapir-openapi-docs"       % tapirVersion,
      "com.softwaremill.tapir" %% "tapir-openapi-circe-yaml" % tapirVersion
    )
  )

lazy val `full-pocket-ingestor` = (project in file("ingestor"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(`full-pocket-api-js`)
  .settings(gCloudFunctionSettings("ingestFunction"))
  .settings(
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-parser" % circeVersion
    )
  )

lazy val `full-pocket` = (project in file("."))
  .aggregate(
    `full-pocket-api-jvm`,
    `full-pocket-api-js`,
    `full-pocket-api-generator`,
    `full-pocket-ingestor`
  )
