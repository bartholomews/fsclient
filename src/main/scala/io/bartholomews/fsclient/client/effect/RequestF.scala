package io.bartholomews.fsclient.client.effect

import cats.Applicative
import cats.effect.Effect
import fs2.{Pipe, Pure, Stream}
import io.bartholomews.fsclient.client.effect.HttpPipes._
import io.bartholomews.fsclient.codecs.RawDecoder
import io.bartholomews.fsclient.entities.OAuthVersion.Version1
import io.bartholomews.fsclient.entities.OAuthVersion.Version1.{BasicSignature, TokenV1}
import io.bartholomews.fsclient.entities._
import io.bartholomews.fsclient.requests.OAuthV2AuthorizationFramework.AuthorizationCodeGrant.RefreshTokenRequest
import io.bartholomews.fsclient.requests.OAuthV2AuthorizationFramework.{ClientPassword, RefreshToken}
import io.bartholomews.fsclient.utils.HttpTypes.HttpResponse
import io.bartholomews.fsclient.utils.{FsHeaders, Logger}
import org.http4s.client.Client
import org.http4s.{Headers, Request, Status, Uri}

private[client] trait RequestF {

  import Logger._
  import io.bartholomews.fsclient.implicits.rawJsonPipe

  def signAndProcessRequest[F[_]: Effect, Raw, Res](effectClient: HttpEffectClient[F, _], client: Client[F])(
    implicit request: Request[F],
    oAuthInfo: OAuthInfo,
    rawDecoder: RawDecoder[Raw],
    resDecoder: Pipe[F, Raw, Res]
  ): Stream[F, HttpResponse[Res]] =
    signRequest[F, Raw, Res](effectClient).flatMap({
      case (request, signer) => processRequest[F, Raw, Res](client, request, signer)
    })

  private def signRequest[F[_]: Effect: Applicative, Raw, Res](
    effectClient: HttpEffectClient[F, _]
  )(implicit request: Request[F], oAuthInfo: OAuthInfo): Stream[F, (Request[F], OAuthInfo)] =
    oAuthInfo match {
      case OAuthDisabled =>
        logger.warn("No OAuth version selected, the request will not be signed.")
        Stream[Pure, Request[F]](request).map(Tuple2(_, oAuthInfo))

      case _ @OAuthEnabled(signer: BasicSignature) =>
        logger.debug("Signing request with OAuth 1.0 (Consumer info only)...")
        Stream.eval(Version1.sign(signer)(request)).map(Tuple2(_, oAuthInfo))

      case _ @OAuthEnabled(signer: TokenV1) =>
        logger.debug("Signing request with OAuth v1...")
        Stream.eval(Version1.sign(signer)(request)).map(Tuple2(_, oAuthInfo))

      case _ @OAuthEnabled(signer2: SignerV2) =>
        logger.debug("Signing request with OAuth v2...")
        val accessTokenResponse = signer2.accessTokenResponse
        val signWithAccessToken =
          Stream[F, Request[F]](request.putHeaders(FsHeaders.authorizationBearer(accessTokenResponse.accessToken)))
            .map(Tuple2(_, oAuthInfo))

        if (accessTokenResponse.isExpired.getOrElse(false)) {
          logger.debug("Refreshing token...")
          accessTokenResponse.refreshToken.fold({
            logger.warn("Refresh token not present in access token response, it won't be refreshed...")
            signWithAccessToken
          }) { token =>
            implicit val signerNoRefresh: SignerV2 =
              signer2.copy(accessTokenResponse = signer2.accessTokenResponse.copy(expiresIn = None))

            Stream
              .eval(
                new RefreshTokenRequest {
                  override val refreshToken: RefreshToken = token
                  override val clientPassword: ClientPassword = signer2.clientPassword
                  override val scopes: List[String] = List.empty
                  override val uri: Uri = signer2.tokenEndpoint
                }.runWith(effectClient)
              )
              .flatMap {
                case FsResponseSuccess(_, _, _, refreshedAccessToken) =>
                  Stream[F, Request[F]](
                    request.putHeaders(FsHeaders.authorizationBearer(refreshedAccessToken.accessToken))
                  ).map(
                    Tuple2(
                      _,
                      OAuthEnabled(SignerV2(signer2.tokenEndpoint, signer2.clientPassword, refreshedAccessToken))
                    )
                  )

                // FIXME: Need to give also this new `accessTokenV2` to the response

                case error: FsResponseError[_] =>
                  Stream
                    .raiseError[F](error)
                    .covaryOutput[Request[F]]
                    .map(Tuple2(_, oAuthInfo))
              }
          }
        } else signWithAccessToken
    }

  private def processRequest[F[_]: Effect, Raw, Res](
    client: Client[F],
    request: Request[F],
    signer: OAuthInfo
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

          }).map(entity => FsResponse(signer, response, entity))

        case Left(throwable) =>
          logger.error(throwable.getMessage)
          throwable match {
            case err: FsResponseError[_] =>
              Stream.emit(err)
            case err =>
              Stream.emit(FsResponseErrorString(signer, Headers.empty, Status.InternalServerError, err.getMessage))
          }
      }
}
