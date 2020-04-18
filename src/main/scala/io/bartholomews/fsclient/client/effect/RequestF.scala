package io.bartholomews.fsclient.client.effect

import cats.Applicative
import cats.effect.{ConcurrentEffect, Effect}
import fs2.{Pipe, Pure, Stream}
import io.bartholomews.fsclient.codecs.RawDecoder
import io.bartholomews.fsclient.entities._
import io.bartholomews.fsclient.utils.HttpTypes.HttpResponse
import io.bartholomews.fsclient.utils.{FsHeaders, Logger}
import org.http4s.client.Client
import org.http4s.{Headers, Request, Status}

private[client] trait RequestF {

  import HttpPipes._
  import Logger._

  def signAndProcessRequest[F[_]: ConcurrentEffect, V <: OAuthVersion, Raw, Res](
    request: Request[F],
    client: Client[F],
    signer: Signer[V]
  )(implicit rawDecoder: RawDecoder[Raw], resDecoder: Pipe[F, Raw, Res]): Stream[F, HttpResponse[V, Res]] =
    signRequest[F, V, Raw, Res](request, signer).flatMap({
      case (request, signer) => processRequest[F, V, Raw, Res](client, request, signer)
    })

  private def signRequest[F[_]: ConcurrentEffect: Applicative, V <: OAuthVersion, Raw, Res](
    request: Request[F],
    signer: Signer[V]
  ): Stream[F, (Request[F], Signer[V])] =
    signer match {

      case AuthDisabled =>
        logger.warn("No OAuth version selected, the request will not be signed.")
        Stream[Pure, Request[F]](request).map(Tuple2(_, signer))

      case basicSignature: BasicSignature =>
        logger.debug("Signing request with OAuth 1.0 (Consumer info only)...")
        Stream.eval(basicSignature.sign(request)).map(Tuple2(_, signer))

      case token1: TokenV1 =>
        logger.debug("Signing request with OAuth v1...")
        Stream.eval(token1.sign(request)).map(Tuple2(_, signer))

      case v2: SignerV2 =>
        logger.debug("Signing request with OAuth v2...")
        Stream[F, Request[F]](request.putHeaders(FsHeaders.authorizationBearer(v2.accessTokenResponse.accessToken)))
          .map(Tuple2(_, signer))
    }

  private def processRequest[F[_]: Effect, V <: OAuthVersion, Raw, Res](
    client: Client[F],
    request: Request[F],
    signer: Signer[V]
  )(implicit rawDecoder: RawDecoder[Raw], resDecoder: Pipe[F, Raw, Res]): Stream[F, HttpResponse[V, Res]] =
    client
      .stream(request)
      .through(responseHeadersLogPipe)
      .attempt
      .flatMap {
        case Right(response) =>
          (response.status match {

            case Status.Ok =>
              Stream
                .emit(response)
                .through(decodeResponse(rawDecoder, resDecoder))
                .through(responseLogPipe)

            case _ =>
              Stream
                .emit(response)
                .through(errorHandler)
                .through(responseLogPipe)

          }).map(entity => FsResponse(signer, response, entity))

        case Left(throwable) =>
          logger.error(throwable.getMessage)
          throwable match {
            case _ @FsResponseErrorString(_, headers, status, error) =>
              Stream.emit(FsResponseErrorString(signer, headers, status, error))
            case err =>
              Stream.emit(FsResponseErrorString(signer, Headers.empty, Status.InternalServerError, err.getMessage))
          }
      }
}
