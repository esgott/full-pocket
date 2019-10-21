package com.github.esgott.fullpocket.api

sealed trait ApiError

object ApiError {

  case class DeserializationFailed(error: String) extends ApiError

}
