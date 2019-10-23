package com.github.esgott.fullpocket.apigenerator

import com.github.esgott.fullpocket.api.{ApiError, IngestEvent}
import io.circe.generic.auto._
import tapir._
import tapir.docs.openapi._
import tapir.json.circe._
import tapir.openapi.circe.yaml._

@SuppressWarnings(Array("org.wartremover.warts.Any"))
object Api {

  val ingest: Endpoint[IngestEvent, ApiError, String, Nothing] =
    endpoint.post
      .in("ingest")
      .in(jsonBody[IngestEvent].description("Event to ingest"))
      .errorOut(jsonBody[ApiError].description("Possible error"))
      .out(stringBody.description("OK if event ingestion successful"))

  def generateOpenapi: String = ingest.toOpenAPI("Full Pocket", "1.0.0").toYaml

}
