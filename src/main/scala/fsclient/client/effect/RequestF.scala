package fsclient.client.effect

import cats.effect.Effect
import fs2.{Pipe, Pure, Stream}
import fsclient.client.effect.HttpPipes._
import fsclient.codecs.RawDecoder
import fsclient.entities.OAuthVersion.{Version1, Version2}
import fsclient.entities._
import fsclient.utils.{FsHeaders, Logger}
import org.http4s.client.Client
import org.http4s.{Request, Status}

private[client] trait RequestF {

  import Logger._

  def processHttpRequest[F[_]: Effect, Raw, Res](client: Client[F])(
    request: Request[F],
    oAuthInfo: OAuthInfo,
    rawDecoder: RawDecoder[Raw],
    resDecoder: Pipe[F, Raw, Res]
  ): Stream[F, HttpResponse[Res]] = {

    val signed = oAuthInfo match {
      case OAuthDisabled =>
        logger.warn("No OAuth version selected, the request will not be signed.")
        Stream[Pure, Request[F]](request)

      case _ @OAuthEnabled(signer: Version1.BasicSignature) =>
        logger.debug("Signing request with OAuth 1.0 (Consumer info only)...")
        Stream.eval(Version1.sign(signer)(request))

      case _ @OAuthEnabled(signer: Version1.TokenResponse) =>
        logger.debug("Signing request with OAuth 1.0...")
        Stream.eval(Version1.sign(signer)(request))

      case _ @OAuthEnabled(v2: SignerV2) =>
        logger.warn("OAuth 2.0 is not fully supported by `fsclient`, you might have to implement it yourself.")
        v2 match {
          case accessTokenResponse: Version2.AccessTokenResponse =>
            // TODO: What if it's expired? Should support self refreshing?
            //  But then it should return the new token with the response...
            logger.warn("Adding `Authorization: Bearer` header...")
            Stream[Pure, Request[F]](
              request.putHeaders(FsHeaders.authorizationBearer(accessTokenResponse.accessToken))
            )
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
            //            .through(decodeLeft) // FIXME: Does a Pipe[F, ErrorOr[Nothing], ErrorOr[Res]] make sense? Probably not
            .through(responseLogPipe)

      }).map(HttpResponse(response.headers, _))

    } yield httpRes
  }
}
