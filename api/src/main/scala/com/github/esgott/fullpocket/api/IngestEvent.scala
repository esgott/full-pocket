package com.github.esgott.fullpocket.api

import java.time.Instant

case class IngestEvent(
  time: Instant,
  sourceId: String,
  sourceAddress: Option[String],
  content: String
)
