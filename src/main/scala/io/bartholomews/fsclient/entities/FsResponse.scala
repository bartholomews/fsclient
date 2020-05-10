package io.bartholomews.fsclient.entities

import io.bartholomews.fsclient.utils.HttpTypes.ErrorOr
import io.circe.Json
import org.http4s.{Headers, Response, Status}

case class FsResponse[E <: ErrorBody, +A](headers: Headers, status: Status, entity: Either[E, A]) {
  final def fold[C](fa: E => C, fb: A => C): C = entity.fold(fa, fb)
}

object FsResponse {
  def apply[F[_], A](response: Response[F], entity: ErrorOr[A]): FsResponse[ErrorBody, A] =
    FsResponse(response.headers, response.status, entity.left.map(ErrorBody.apply))

  def apply(error: EmptyResponseException): FsResponse[ErrorBody, Nothing] = new FsResponse(
    Headers.empty,
    error.status,
    Left(ErrorBodyString(error.getMessage))
  )
}

sealed trait ErrorBody
object ErrorBody {
  def apply(httpError: HttpError): ErrorBody = httpError match {
    case err: HttpErrorString => ErrorBodyString(err.body)
    case err: HttpErrorJson   => ErrorBodyJson(err.body)
  }
}

case class ErrorBodyString(value: String) extends ErrorBody
case class ErrorBodyJson(value: Json) extends ErrorBody
