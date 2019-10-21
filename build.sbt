import scala.sys.process._

ThisBuild / scalaVersion := "2.12.10"
Global / cancelable := true

val gCloudFunctionName   = settingKey[String]("Name of gCloud function")
val gCloudRegion         = settingKey[String]("Google Cloud region")
val gCloudApiDeploy      = taskKey[Unit]("Deploy OpenAPI to gCloud")
val gCloudFunctionDeploy = taskKey[Unit]("Deploy function to gCloud")

def scalaDep(group: String, prefix: String, version: String): String => ModuleID = artifact => {
  group %% depName(prefix, artifact) % version
}

def depName(prefix: String, artifact: String): String = {
  val suffix = if (artifact.isEmpty) artifact else s"-$artifact"
  s"$prefix$suffix"
}

lazy val circe = scalaDep("io.circe", "circe", "0.12.2")
lazy val tapir = scalaDep("com.softwaremill.tapir", "tapir", "0.11.7")

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
    val project = "--project grounded-cider-254518"
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

lazy val `full-pocket-api` = (project in file("api"))
  .settings(gcloudApiSettings)
  .settings(
    libraryDependencies ++= Seq(
      circe("core"),
      circe("generic"),
      circe("generic-extras"),
      tapir("core"),
      tapir("json-circe"),
      tapir("openapi-docs"),
      tapir("openapi-circe-yaml")
    )
  )

lazy val `full-pocket-ingestor` = (project in file("ingestor"))
  .enablePlugins(ScalaJSPlugin)
  .settings(gCloudFunctionSettings("ingestFunction"))

lazy val `full-pocket` = (project in file("."))
  .aggregate(
    `full-pocket-api`,
    `full-pocket-ingestor`
  )
