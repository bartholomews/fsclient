package io.bartholomews.fsclient.entities

import io.circe.Json
import org.http4s.Status

sealed trait HttpError {
  type BodyType
  def status: Status
  def body: BodyType
}

case class HttpErrorString(status: Status, body: String) extends HttpError {
  override type BodyType = String
}

object HttpErrorString {

  def apply(empty: EmptyResponseException): HttpErrorString = HttpErrorString(empty.status, empty.getMessage)

  def apply(status: Status)(throwable: Throwable): HttpErrorString =
    throwable match {
      case _: io.circe.Error =>
        new HttpErrorString(
          Status.UnprocessableEntity,
          "There was a problem decoding or parsing this response, please check the error logs"
        )
      case _ => new HttpErrorString(status, throwable.getMessage)
    }
}

case class HttpErrorJson(status: Status, body: Json) extends HttpError {
  override type BodyType = Json
}
