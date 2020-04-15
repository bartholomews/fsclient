package io.bartholomews.fsclient.entities

import cats.implicits._
import io.bartholomews.fsclient.utils.HttpTypes.ErrorOr
import io.circe.Json
import org.http4s.{Headers, Response, Status}

// FIXME: Consider having [+O <: OAuthVersion] -> signer: Signer[O] / Nothing / V1 / V2
//  TBH it is useful only to get the refreshed token, so you might as well have an Option[AccessTokenV2]
//  but it looks bad having it for AuthDisabled / AuthV1 responses
sealed trait FsResponse[+E <: HttpError, +A] {
  def signer: OAuthInfo
  def headers: Headers
  def status: Status
  def entity: Either[E#BodyType, A]
  def fold[C](fa: FsResponseError[_] => C, fb: A => C): C = mapError(fa).fold(identity, fb)
  def mapError[EE](f: FsResponseError[_] => EE): Either[EE, A] = this match {
    case FsResponseSuccess(_, _, _, body) => body.asRight[EE]
    case err: FsResponseError[_] => f(err).asLeft[A]
  }
}

case class FsResponseSuccess[E <: HttpError, A](signer: OAuthInfo, headers: Headers, status: Status, body: A) extends FsResponse[E, A] {
  final override val entity = Right(body)
}

object FsResponseSuccess {
  def apply[A](signer: OAuthInfo, body: A): FsResponseSuccess[Nothing, A] = FsResponseSuccess(signer, Headers.empty, Status.Ok, body)
}

sealed trait FsResponseError[E <: HttpError] extends Throwable with FsResponse[E, Nothing] {
  def error: E#BodyType
}

case class FsResponseErrorJson(signer: OAuthInfo, headers: Headers, status: Status, error: Json) extends FsResponseError[HttpErrorJson] {
  final override val entity = Left(error)
}

case class FsResponseErrorString(signer: OAuthInfo, headers: Headers, status: Status, error: String) extends FsResponseError[HttpErrorString] {
  final override val entity = Left(error)
}

object FsResponse {
  def apply[F[_], A](signer: OAuthInfo, response: Response[F], entity: ErrorOr[A]): FsResponse[HttpError, A] =
    entity.fold(
      (err: HttpError) => err match {
        case HttpErrorString(status, body) => FsResponseErrorString(signer, response.headers, status, body)
        case HttpErrorJson(status, body) => FsResponseErrorJson(signer, response.headers, status, body)
      },
      (body: A) => FsResponseSuccess[Nothing, A](signer, response.headers, response.status, body)
    )

  def apply(signer: OAuthInfo, error: EmptyResponseException): FsResponseErrorString = FsResponseErrorString(
    signer,
    Headers.empty,
    error.status,
    error.getMessage
  )
}
