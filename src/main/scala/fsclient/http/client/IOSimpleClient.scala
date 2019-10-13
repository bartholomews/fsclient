package fsclient.http.client

import cats.data.EitherT
import cats.effect.IO
import fsclient.config.OAuthConsumer
import fsclient.entities._
import io.circe.{Decoder, Encoder}

import scala.concurrent.ExecutionContext

class IOSimpleClient(val consumer: OAuthConsumer)(implicit val ec: ExecutionContext) {

  // TODO maybe also a real SIMPLE-Client with no Consumer constructor?

  object auth extends IOClient(consumer) {

    final def accessTokenRequest(
      request: AccessTokenRequest
    )(implicit responseDecoder: IOHttpPipe[String, AccessToken]): IO[HttpResponse[AccessToken]] =
      effect.getPlainText(request, Some(request.token))

    final def toOAuthClient(
      request: AccessTokenRequest
    )(implicit responseDecoder: IOHttpPipe[String, AccessToken]): IO[Either[ResponseError, IOAuthClient]] =
      (for {
        accessToken <- EitherT(accessTokenRequest(request).map(_.entity))
        res <- EitherT.pure[IO, ResponseError](new IOAuthClient(consumer)(implicitly, accessToken))
      } yield res).value

    final def getJson[R](
      accessToken: AccessToken
    )(request: FsClientPlainRequest.GET[R])(implicit responseDecoder: Decoder[R]): IOResponse[R] =
      effect.getJson(request, Some(OAuthToken.apply(accessToken)))

    final def getPlainText[R](
      accessToken: AccessToken
    )(request: FsClientPlainRequest.GET[R])(implicit responseDecoder: IOHttpPipe[String, R]): IOResponse[R] =
      effect.getPlainText(request, Some(OAuthToken.apply(accessToken)))

    final def fetchJson[B, R](accessToken: AccessToken)(request: FsClientRequestWithBody[B, R])(
      implicit
      requestBodyEncoder: Encoder[B],
      responseDecoder: Decoder[R]
    ): IOResponse[R] =
      effect.fetchJson(request, Some(OAuthToken.apply(accessToken)))

    final def fetchPlainText[B, R](accessToken: AccessToken)(
      request: FsClientRequestWithBody[B, R]
    )(implicit requestBodyEncoder: Encoder[B], responseDecoder: HttpPipe[IO, String, R]): IOResponse[R] =
      effect.fetchPlainText(request, Some(OAuthToken.apply(accessToken)))
  }

  object simple extends IOClient(consumer) {
    final def getJson[R](request: FsClientPlainRequest.GET[R])(
      implicit responseDecoder: Decoder[R]
    ): IOResponse[R] = effect.getJson(request, oAuthToken = None)

    final def getPlainText[R](request: FsClientPlainRequest.GET[R])(
      implicit responseDecoder: IOHttpPipe[String, R]
    ): IOResponse[R] = effect.getPlainText(request, oAuthToken = None)

    final def fetchJson[B, R](request: FsClientRequestWithBody[B, R])(
      implicit requestBodyEncoder: Encoder[B],
      responseDecoder: Decoder[R]
    ): IOResponse[R] = effect.fetchJson(request, oAuthToken = None)

    final def fetchPlainText[B, R](request: FsClientRequestWithBody[B, R])(
      implicit requestBodyEncoder: Encoder[B],
      responseDecoder: HttpPipe[IO, String, R]
    ): IOResponse[R] = effect.fetchPlainText(request, oAuthToken = None)
  }
}
