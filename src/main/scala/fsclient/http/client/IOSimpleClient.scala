package fsclient.http.client

import cats.data.EitherT
import cats.effect.IO
import fsclient.config.OAuthConsumer
import fsclient.entities._
import io.circe.{Decoder, Encoder}

import scala.concurrent.ExecutionContext

class IOSimpleClient(override val consumer: OAuthConsumer)(implicit val ec: ExecutionContext)
    extends IOClient(consumer) {

  def accessTokenRequest(
    request: AccessTokenRequest
  )(implicit responseDecoder: IOHttpPipe[String, AccessToken]): IO[HttpResponse[AccessToken]] =
    super.getPlainText(request, Some(request.token))

  def toOAuthClient(
    request: AccessTokenRequest
  )(implicit responseDecoder: IOHttpPipe[String, AccessToken]): IO[Either[ResponseError, IOAuthClient]] =
    (for {
      accessToken <- EitherT(accessTokenRequest(request).map(_.entity))
      res <- EitherT.pure[IO, ResponseError](new IOAuthClient(consumer)(implicitly, accessToken))
    } yield res).value

  //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  // TODO: decide on naming / different / overloading etc

  final def getJson[R](
    accessToken: Option[AccessToken]
  )(endpoint: FsClientPlainRequest.GET[R])(implicit responseDecoder: Decoder[R]): IOResponse[R] =
    super.getJson(endpoint.uri, accessToken.map(OAuthToken.apply))

  final def getPlainText[R](
    accessToken: Option[AccessToken]
  )(request: FsClientPlainRequest.GET[R])(implicit responseDecoder: IOHttpPipe[String, R]): IOResponse[R] =
    super.getPlainText(request, accessToken.map(OAuthToken.apply))

  final def fetchJson[B, R](accessToken: Option[AccessToken])(request: FsClientRequestWithBody[B, R])(
    implicit
    requestBodyEncoder: Encoder[B],
    responseDecoder: Decoder[R]
  ): IOResponse[R] =
    super.fetchJson(request, accessToken.map(OAuthToken.apply))

  final def fetchPlainText[B, R](accessToken: Option[AccessToken])(
    request: FsClientRequestWithBody[B, R]
  )(implicit requestBodyEncoder: Encoder[B], responseDecoder: HttpPipe[IO, String, R]): IOResponse[R] =
    super.fetchPlainText(request, accessToken.map(OAuthToken.apply))
}
