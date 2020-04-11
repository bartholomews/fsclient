package fsclient.entities

import fsclient.utils.HttpTypes.ErrorOr
import io.circe.Json
import org.http4s.{Headers, Response, Status}

sealed trait FsResponse[+E <: HttpError, +A] {
  def headers: Headers
  def status: Status
  protected def entity: Either[E#BodyType, A]
}

case class FsResponseSuccess[E <: HttpError, A](headers: Headers, status: Status, body: A) extends FsResponse[E, A] {
  protected final override val entity = Right(body)
}

sealed trait FsResponseError[E <: HttpError] extends FsResponse[E, Nothing] {
  def error: E#BodyType
}

case class FsResponseErrorJson(headers: Headers, status: Status, error: Json) extends FsResponseError[HttpErrorJson] {
  protected final override val entity = Left(error)
}

case class FsResponseErrorString(headers: Headers, status: Status, error: String) extends FsResponseError[HttpErrorString] {
  protected final override val entity = Left(error)
}

object FsResponse {
  def apply[F[_], A](response: Response[F], entity: ErrorOr[A]): FsResponse[HttpError, A] =
    entity.fold(
      (err: HttpError) => err match {
        case HttpErrorString(status, body) => FsResponseErrorString(response.headers, status, body)
        case HttpErrorJson(status, body) => FsResponseErrorJson(response.headers, status, body)
      },
      (body: A) => FsResponseSuccess[Nothing, A](response.headers, response.status, body)
    )

  def apply(error: EmptyResponseException): FsResponseErrorString = FsResponseErrorString(
    Headers.empty,
    error.status,
    error.getMessage
  )
}
