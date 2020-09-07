package io.bartholomews.fsclient.entities

import io.bartholomews.fsclient.utils.HttpTypes.ErrorOr
import io.circe.Json
import org.http4s.{Headers, Response, Status}

final case class FsResponse[E <: ErrorBody, +A](headers: Headers, status: Status, entity: Either[E, A]) {
  def foldBody[C](fa: E => C, fb: A => C): C = entity.fold(fa, fb)
}

object FsResponse {
  private[fsclient] def apply[F[_], A](response: Response[F], entity: ErrorOr[A]): FsResponse[ErrorBody, A] =
    entity.fold(
      { case (status, errorBody) => FsResponse(response.headers, status, Left(errorBody)) },
      entityBody => FsResponse(response.headers, response.status, Right(entityBody))
    )

  private[fsclient] def apply(error: EmptyResponseException): FsResponse[ErrorBody, Nothing] =
    new FsResponse(
      Headers.empty,
      error.status,
      Left(ErrorBodyString(error.getMessage))
    )
}

sealed trait ErrorBody
private[fsclient] object ErrorBody {
  def apply(error: String): ErrorBodyString = ErrorBodyString(error)
  def apply(error: Json): ErrorBodyJson = ErrorBodyJson(error)
}

final case class ErrorBodyString(value: String) extends ErrorBody
final case class ErrorBodyJson(value: Json) extends ErrorBody
