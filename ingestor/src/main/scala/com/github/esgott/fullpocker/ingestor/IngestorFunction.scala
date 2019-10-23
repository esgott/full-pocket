package com.github.esgott.fullpocker.ingestor

import com.github.esgott.fullpocket.api.ApiError
import com.github.esgott.fullpocket.api.ApiError.{DeserializationFailed, Unauthorized}
import io.circe.generic.auto._
import io.circe.parser.parse
import io.scalajs.npm.express.{Request, Response}

import scala.scalajs.js.annotation.JSExportTopLevel

object IngestorFunction {

  @JSExportTopLevel("ingestFunction")
  def ingestFunction(req: Request, res: Response): Unit = {
    val result: Either[ApiError, UserInfo] = for {
      token <- req.headers
                .get("X-Endpoint-API-UserInfo")
                .toRight[ApiError](
                  Unauthorized(
                    s"No UserInfo header in \n${req.headers.map { case (key, value) => s"$key=$value" }.mkString("\n")}"
                  )
                )

      tokenJson <- parse(token).left.map(_.message).left.map(DeserializationFailed.apply)
      userInfo  <- tokenJson.as[UserInfo].left.map(_.message).left.map(DeserializationFailed.apply)
    } yield userInfo

    result match {
      case Right(userInfo) => res.send(userInfo.email)
      case Left(error)     => res.send(error.toString)
    }
  }

}
