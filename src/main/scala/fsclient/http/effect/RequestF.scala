package fsclient.http.effect

import cats.effect.Effect
import fs2.{Pipe, Pure, Stream}
import fsclient.entities._
import fsclient.http.effect.HttpPipes._
import fsclient.oauth.FsHeaders
import fsclient.oauth.OAuthVersion.OAuthV1
import fsclient.oauth.OAuthVersion.OAuthV2.AccessTokenV2
import fsclient.utils.HttpTypes.{ErrorOr, HttpPipe}
import fsclient.utils.Logger
import io.circe.Decoder
import org.http4s.client.Client
import org.http4s.client.oauth1.Consumer
import org.http4s.{Request, Response, Status}

private[http] trait RequestF {

  import Logger._

  def jsonRequest[F[_]: Effect, A](client: Client[F])(request: Request[F], oAuthInfo: OAuthInfo)(
    implicit
    consumer: Consumer,
    decoder: Decoder[A]
  ): Stream[F, HttpResponse[A]] =
    processHttpRequest(client)(request, oAuthInfo, decodeJsonResponse, doNothing)

  def plainTextRequest[F[_]: Effect, A](client: Client[F])(
    request: Request[F],
    oAuthInfo: OAuthInfo
  )(implicit consumer: Consumer, decoder: HttpPipe[F, String, A]): Stream[F, HttpResponse[A]] =
    processHttpRequest(client)(request, oAuthInfo, decodeTextPlainResponse, decoder)

  private def processHttpRequest[F[_]: Effect, A](client: Client[F])(
    request: Request[F],
    oAuthInfo: OAuthInfo,
    decodeRight: Pipe[F, Response[F], ErrorOr[A]],
    decodeLeft: Pipe[F, ErrorOr[Nothing], ErrorOr[A]]
  )(implicit consumer: Consumer): Stream[F, HttpResponse[A]] = {

    val signed =
      oAuthInfo match {
        case _ @OAuthVersion1(v1) =>
          logger.debug("Signing request with OAuth 1.0...")
          Stream.eval(OAuthV1.sign(consumer, v1)(request))

        case _ @OAuthVersion2(token: AccessTokenV2) =>
          logger.warn("OAuth 2.0 is currently not supported by `fsclient`, you have to implement it yourself.")
          logger.warn("I am adding `Authorization: Bearer` header for now, but need to double check RFC...")
          Stream[Pure, Request[F]](
            request.putHeaders(
              FsHeaders.authorizationBearer(token)
            )
          )

        case _ @OAuthVersion2(_) =>
          logger.warn("OAuth 2.0 is currently not supported by `fsclient`, you have to implement it yourself.")
          logger.warn("TODO: Double check other non-`AccessTokenV2` calls like refresh etc, Request token...")
          Stream[Pure, Request[F]](request)

        case OAuthDisabled =>
          logger.warn("No OAuth version selected, the request will not be signed.")
          Stream[Pure, Request[F]](request)
      }

    for {

      request <- signed

      responseStream <- client
        .stream(request)
        .through(responseHeadersLogPipe)
        .attempt
        .map {
          case Right(value) => Stream(value)
          case Left(throwable) =>
            logger.error(throwable.getMessage)
            Stream.empty
        }

      response <- responseStream

      httpRes <- (response.status match {

        case Status.Ok =>
          Stream
            .emit(response)
            .through(decodeRight)
            .through(responseLogPipe)

        case _ =>
          Stream
            .emit(response)
            .through(errorHandler)
            .through(decodeLeft) // FIXME: WTF IS THIS
            .through(responseLogPipe)

      }).map(HttpResponse(response.headers, _))

    } yield httpRes
  }
}
