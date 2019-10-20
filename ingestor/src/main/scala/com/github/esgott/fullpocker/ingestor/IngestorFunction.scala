package com.github.esgott.fullpocker.ingestor

import io.scalajs.npm.express.{Request, Response}

import scala.scalajs.js.annotation.JSExportTopLevel

object IngestorFunction {

  @JSExportTopLevel("ingestFunction")
  def ingestFunction(req: Request, res: Response): Unit = {
    res.sendStatus(200)
  }

}
