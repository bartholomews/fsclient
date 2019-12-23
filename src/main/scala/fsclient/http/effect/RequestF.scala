package fsclient.http.effect

import cats.effect.Effect
import fs2.{Pipe, Pure, Stream}
import fsclient.http.client.base.RawDecoder
import fsclient.http.effect.HttpPipes._
import fsclient.oauth.OAuthVersion.OAuthV1
import fsclient.oauth.OAuthVersion.OAuthV2.AccessTokenV2
import fsclient.oauth.{FsHeaders, OAuthTokenV1, OAuthTokenV2}
import fsclient.requests._
import fsclient.utils.Logger
import org.http4s.client.Client
import org.http4s.{Request, Status}

private[http] trait RequestF {

  import Logger._

  def processHttpRequest[F[_]: Effect, Raw, Res](client: Client[F])(
    request: Request[F],
    oAuthInfo: OAuthInfo,
    rawDecoder: RawDecoder[Raw],
    resDecoder: Pipe[F, Raw, Res]
  ): Stream[F, HttpResponse[Res]] = {

    val signed =
      oAuthInfo match {
        case OAuthDisabled =>
          logger.warn("No OAuth version selected, the request will not be signed.")
          Stream[Pure, Request[F]](request)

        case _ @OAuthEnabled(v1: OAuthTokenV1) =>
          logger.debug("Signing request with OAuth 1.0...")
          Stream.eval(OAuthV1.sign(v1)(request))

        case _ @OAuthEnabled(v2: OAuthTokenV2) =>
          logger.warn("OAuth 2.0 is currently not supported by `fsclient`, you have to implement it yourself.")
          v2 match {
            case accessToken: AccessTokenV2 =>
              logger.warn("Adding `Authorization: Bearer` header for now, but need to double check RFC...")
              Stream[Pure, Request[F]](request.putHeaders(FsHeaders.authorizationBearer(accessToken)))
            // TODO: Double check other non-`AccessTokenV2` calls like refresh etc, Request token...")
          }
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
            .through(decodeResponse(rawDecoder, resDecoder))
            .through(responseLogPipe)

        case _ =>
          Stream
            .emit(response)
            .through(errorHandler)
            //            .through(decodeLeft) // FIXME: WTF IS THIS? Does a Pipe[F, ErrorOr[Nothing], ErrorOr[Res]] make sense?
            .through(responseLogPipe)

      }).map(HttpResponse(response.headers, _))

    } yield httpRes
  }
}
