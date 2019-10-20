import scala.sys.process._

ThisBuild / scalaVersion := "2.12.10"
Global / cancelable := true

val gCloudFunctionName   = settingKey[String]("Name of gCloud function")
val gCloudFunctionDeploy = taskKey[Unit]("Deploy function to gCloud")

val commonSettings = Seq(
  organization := "com.github.esgott"
)

def gCloudFunctionSettings(functionName: String) = commonSettings ++ Seq(
  scalaJSLinkerConfig ~= {
    _.withModuleKind(ModuleKind.CommonJSModule)
  },
  libraryDependencies += "io.scalajs.npm" %%% "express" % "0.4.2",
  gCloudFunctionName := functionName,
  gCloudFunctionDeploy := {
    val gcTarget     = target.value / "gcloud"
    val function     = gcTarget / "function.js"
    val functionName = gCloudFunctionName.value
    IO.copyFile((fastOptJS in Compile).value.data, function)
    s"gcloud functions deploy $functionName --source ${gcTarget.getAbsolutePath} --runtime nodejs8 --trigger-http" !!
  }
)

lazy val `full-pocket-ingestor` = (project in file("ingestor"))
  .enablePlugins(ScalaJSPlugin)
  .settings(gCloudFunctionSettings("ingestFunction"))

lazy val `full-pocket` = (project in file("."))
  .aggregate(
    `full-pocket-ingestor`
  )
