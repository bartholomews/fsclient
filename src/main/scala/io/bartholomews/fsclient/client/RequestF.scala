package io.bartholomews.fsclient.client

import cats.Applicative
import cats.effect.{ConcurrentEffect, Effect}
import fs2.{Pipe, Pure, Stream}
import io.bartholomews.fsclient.codecs.RawDecoder
import io.bartholomews.fsclient.entities._
import io.bartholomews.fsclient.entities.oauth._
import io.bartholomews.fsclient.utils.HttpTypes.HttpResponse
import io.bartholomews.fsclient.utils.{FsHeaders, FsLogger}
import org.http4s.client.Client
import org.http4s.{Headers, Request, Status}

private[client] trait RequestF {

  import FsLogger._
  import io.bartholomews.fsclient.client.HttpPipes._

  def signAndProcessRequest[F[_]: ConcurrentEffect, Raw, Res](
    request: Request[F],
    client: Client[F],
    signer: Signer
  )(implicit rawDecoder: RawDecoder[Raw], resDecoder: Pipe[F, Raw, Res]): Stream[F, HttpResponse[Res]] =
    signRequest[F, Raw, Res](request, signer).flatMap(processRequest[F, Raw, Res](client))

  private def signRequest[F[_]: ConcurrentEffect: Applicative, Raw, Res](
    request: Request[F],
    signer: Signer
  ): Stream[F, Request[F]] =
    signer match {

      case AuthDisabled =>
        logger.warn("No OAuth version selected, the request will not be signed.")
        Stream[Pure, Request[F]](request)

      case basicSignature: ClientCredentials =>
        logger.debug("Signing request with OAuth 1.0 (Consumer info only)...")
        Stream.eval(basicSignature.sign(request))

      case tokenCredentials: TokenCredentials =>
        logger.debug("Signing request with OAuth v1...")
        Stream.eval(tokenCredentials.sign(request))

      case v2: SignerV2 =>
        logger.debug("Signing request with OAuth v2...")
        Stream[F, Request[F]](
          v2.tokenType.toUpperCase match {
            case "BEARER" => request.putHeaders(FsHeaders.authorizationBearer(v2.accessToken))
            case other =>
              logger.warn(s"Unknown token type [$other]: The request will not be signed")
              request
          }
        )
    }

  private def processRequest[F[_]: Effect, Raw, Res](client: Client[F])(
    request: Request[F]
  )(implicit rawDecoder: RawDecoder[Raw], resDecoder: Pipe[F, Raw, Res]): Stream[F, HttpResponse[Res]] =
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

          }).map(entity => FsResponse(response, entity))

        case Left(throwable) =>
          logger.error(throwable.getMessage)
          throwable match {
            case _ @FsResponseErrorString(headers, status, error) =>
              Stream.emit(FsResponseErrorString(headers, status, error))
            case err =>
              Stream.emit(FsResponseErrorString(Headers.empty, Status.InternalServerError, err.getMessage))
          }
      }
}
