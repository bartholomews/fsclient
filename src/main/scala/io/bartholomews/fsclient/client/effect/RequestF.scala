package io.bartholomews.fsclient.client.effect

import cats.Applicative
import cats.effect.Effect
import fs2.{Pipe, Pure, Stream}
import io.bartholomews.fsclient.codecs.RawDecoder
import io.bartholomews.fsclient.entities._
import io.bartholomews.fsclient.requests.OAuthV2AuthorizationFramework.AuthorizationCodeGrant.RefreshTokenRequest
import io.bartholomews.fsclient.requests.OAuthV2AuthorizationFramework.{ClientPassword, RefreshToken}
import io.bartholomews.fsclient.utils.HttpTypes.HttpResponse
import io.bartholomews.fsclient.utils.{FsHeaders, Logger}
import org.http4s.client.Client
import org.http4s.{Headers, Request, Status, Uri}

private[client] trait RequestF {

  import HttpPipes._
  import Logger._
  import io.bartholomews.fsclient.implicits.rawJsonPipe

  def signAndProcessRequest[F[_]: Effect, V <: OAuthVersion, Raw, Res](
    request: Request[F],
    effectClient: HttpEffectClient[F, _],
    client: Client[F],
    signer: Signer[V]
  )(
    implicit rawDecoder: RawDecoder[Raw],
    resDecoder: Pipe[F, Raw, Res]
  ): Stream[F, HttpResponse[V, Res]] =
    signRequest[F, V, Raw, Res](request, effectClient, signer).flatMap({
      case (request, signer) => processRequest[F, V, Raw, Res](client, request, signer)
    })

  private def signRequest[F[_]: Effect: Applicative, V <: OAuthVersion, Raw, Res](
    request: Request[F],
    effectClient: HttpEffectClient[F, _],
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
        val accessTokenResponse = v2.accessTokenResponse
        val signWithAccessToken =
          Stream[F, Request[F]](request.putHeaders(FsHeaders.authorizationBearer(accessTokenResponse.accessToken)))
            .map(Tuple2(_, signer))

        if (accessTokenResponse.isExpired.getOrElse(false)) {
          logger.debug("Refreshing token...")
          accessTokenResponse.refreshToken.fold({
            logger.warn("Refresh token not present in access token response, it won't be refreshed...")
            signWithAccessToken
          }) { token =>
            implicit val signerNoRefresh: SignerV2 =
              v2.copy(accessTokenResponse = v2.accessTokenResponse.copy(expiresIn = None))

            Stream
              .eval(
                new RefreshTokenRequest {
                  override val refreshToken: RefreshToken = token
                  override val clientPassword: ClientPassword = v2.clientPassword
                  override val scopes: List[String] = List.empty
                  override val uri: Uri = v2.tokenEndpoint
                }.runWith(effectClient)
              )
              .flatMap {
                case FsResponseSuccess(_, _, _, refreshedAccessToken) =>
                  Stream[F, Request[F]](
                    request.putHeaders(FsHeaders.authorizationBearer(refreshedAccessToken.accessToken))
                  ).map(
                    Tuple2(
                      _,
                      v2.copy(accessTokenResponse = refreshedAccessToken)
                        .asInstanceOf[Signer[V]] // FIXME
                    )
                  )

                case error: FsResponseError[_, _] =>
                  Stream
                    .raiseError[F](error)
                    .covaryOutput[Request[F]]
                    .map(Tuple2(_, signer))
              }
          }
        } else signWithAccessToken
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
            case _ @ FsResponseErrorString(_, headers, status, error) =>
              Stream.emit(FsResponseErrorString(signer, headers, status, error))
            case err =>
              Stream.emit(FsResponseErrorString(signer, Headers.empty, Status.InternalServerError, err.getMessage))
          }
      }
}
