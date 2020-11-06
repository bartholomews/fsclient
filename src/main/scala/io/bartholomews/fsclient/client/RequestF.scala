package io.bartholomews.fsclient.client

import cats.Applicative
import cats.effect.{ConcurrentEffect, Effect}
import fs2.{Pure, Stream}
import io.bartholomews.fsclient.codecs.{RawDecoder, ResDecoder}
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
  )(implicit rawDecoder: RawDecoder[Raw], resDecoder: ResDecoder[Raw, Res]): Stream[F, HttpResponse[Res]] =
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

      case temporaryCredentials: TemporaryCredentialsRequest =>
        logger.debug("Signing request with OAuth 1.0 (Temporary credentials)...")
        Stream.eval(temporaryCredentials.sign(request))

      case tokenCredentials: TokenCredentials =>
        logger.debug("Signing request with OAuth v1...")
        Stream.eval(tokenCredentials.sign(request))

      case ClientPasswordBasicAuthenticationV2(clientPassword) =>
        logger.debug("Signing request with OAuth v2 Basic...")
        Stream[F, Request[F]](request.putHeaders(clientPassword.authorizationBasic))

      case v2: AccessTokenSignerV2 =>
        Stream[F, Request[F]](
          v2.tokenType.toUpperCase match {
            case "BEARER" =>
              logger.debug("Signing request with OAuth v2 Bearer...")
              request.putHeaders(FsHeaders.authorizationBearer(v2.accessToken))
            case other =>
              logger.warn(s"Unknown token type [$other]: The request will not be signed")
              request
          }
        )
    }

  private def processRequest[F[_]: Effect, Raw, Res](client: Client[F])(
    request: Request[F]
  )(implicit rawDecoder: RawDecoder[Raw], resDecoder: ResDecoder[Raw, Res]): Stream[F, HttpResponse[Res]] =
    client
      .stream(request)
      .through(responseHeadersLogPipe)
      .attempt
      .flatMap {
        case Right(response) =>
          (response.status match {

            case status if status.isSuccess =>
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
          Stream.emit(
            FsResponse(Headers.empty, Status.InternalServerError, Left(ErrorBodyString(throwable.getMessage)))
          )
      }
}
