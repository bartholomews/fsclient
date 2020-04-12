package io.bartholomews.fsclient.entities

import org.http4s.Status

case class EmptyResponseException() extends Throwable {
  val status: Status = Status.NoContent
  override val getMessage: String = "Response body was empty"
}
