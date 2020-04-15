package io.bartholomews.fsclient.entities

import cats.implicits._
import io.bartholomews.fsclient.utils.HttpTypes.ErrorOr
import io.circe.Json
import org.http4s.{Headers, Response, Status}

// FIXME: Consider having [+O <: OAuthVersion] -> signer: Signer[O] / Nothing / V1 / V2
//  TBH it is useful only to get the refreshed token, so you might as well have an Option[AccessTokenV2]
//  but it looks bad having it for AuthDisabled / AuthV1 responses
sealed trait FsResponse[V <: OAuthVersion, +E <: HttpError, +A] {
  def signer: Signer[V]
  def headers: Headers
  def status: Status
  def entity: Either[E#BodyType, A]
  def fold[C](fa: FsResponseError[V, _] => C, fb: A => C): C = mapError(fa).fold(identity, fb)
  def mapError[EE](f: FsResponseError[V, _] => EE): Either[EE, A] = this match {
    case FsResponseSuccess(_, _, _, body) => body.asRight[EE]
    case err: FsResponseError[V, _] => f(err).asLeft[A]
  }
}

case class FsResponseSuccess[V <: OAuthVersion, E <: HttpError, A](signer: Signer[V], headers: Headers, status: Status, body: A) extends FsResponse[V, E, A] {
  final override val entity = Right(body)
}

object FsResponseSuccess {
  def apply[V <: OAuthVersion, A](signer: Signer[V], body: A): FsResponseSuccess[V, Nothing, A] = FsResponseSuccess(signer, Headers.empty, Status.Ok, body)
}

sealed trait FsResponseError[V <: OAuthVersion, E <: HttpError] extends Throwable with FsResponse[V, E, Nothing] {
  def error: E#BodyType
}

case class FsResponseErrorJson[V <: OAuthVersion](signer: Signer[V], headers: Headers, status: Status, error: Json) extends FsResponseError[V, HttpErrorJson] {
  final override val entity = Left(error)
}

case class FsResponseErrorString[V <: OAuthVersion](signer: Signer[V], headers: Headers, status: Status, error: String) extends FsResponseError[V, HttpErrorString] {
  final override val entity = Left(error)
}

object FsResponse {
  def apply[F[_], V <: OAuthVersion, A](signer: Signer[V], response: Response[F], entity: ErrorOr[A]): FsResponse[V, HttpError, A] =
    entity.fold(
      (err: HttpError) => err match {
        case HttpErrorString(status, body) => FsResponseErrorString(signer, response.headers, status, body)
        case HttpErrorJson(status, body) => FsResponseErrorJson(signer, response.headers, status, body)
      },
      (body: A) => FsResponseSuccess[V, Nothing, A](signer, response.headers, response.status, body)
    )

  def apply[V <: OAuthVersion](signer: Signer[V], error: EmptyResponseException): FsResponseErrorString[V] = FsResponseErrorString(
    signer,
    Headers.empty,
    error.status,
    error.getMessage
  )
}
