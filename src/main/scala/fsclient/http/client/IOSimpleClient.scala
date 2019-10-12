package fsclient.http.client

import cats.data.EitherT
import cats.effect.IO
import fsclient.config.OAuthConsumer
import fsclient.entities._
import io.circe.{Decoder, Encoder}

import scala.concurrent.ExecutionContext

class IOSimpleClient(override val consumer: OAuthConsumer)(
    implicit val ec: ExecutionContext)
    extends IOClient(consumer) {

  def toOAuthClient(request: AccessTokenRequest)(
      implicit responseDecoder: IOHttpPipe[String, AccessToken])
    : IO[Either[ResponseError, IOAuthClient]] = {

    def accessTokenRequest: IO[HttpResponse[AccessToken]] =
      super.fetchPlainText(request.uri,
                           request.method,
                           request.body,
                           Some(request.token))

    (for {
      accessToken <- EitherT(accessTokenRequest.map(_.entity))
      res <- EitherT.right[ResponseError](
        IO.pure(new IOAuthClient(consumer, accessToken)))
    } yield res).value
  }

  //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  // TODO: decide on naming / different / overloading etc

  final def getJson[R](accessToken: Option[AccessToken])(
      endpoint: HttpRequest.GET[R])(
      implicit responseDecoder: Decoder[R]): IOResponse[R] =
    super.getJson(endpoint.uri, accessToken.map(OAuthToken.apply))

  final def getPlainText[R](accessToken: Option[AccessToken])(
      endpoint: HttpRequest.GET[R])(
      implicit responseDecoder: IOHttpPipe[String, R]): IOResponse[R] =
    super.getPlainText(endpoint.uri, accessToken.map(OAuthToken.apply))

  final def fetchJson[B, R](accessToken: Option[AccessToken])(
      request: HttpRequest[B, R])(implicit
                                  requestBodyEncoder: Encoder[B],
                                  responseDecoder: Decoder[R]): IOResponse[R] =
    super.fetchJson(request.uri,
                    request.method,
                    request.body,
                    accessToken.map(OAuthToken.apply))

  final def fetchPlainText[B, R](accessToken: Option[AccessToken])(
      request: HttpRequest[B, R])(
      implicit requestBodyEncoder: Encoder[B],
      responseDecoder: HttpPipe[IO, String, R]): IOResponse[R] =
    super.fetchPlainText(request.uri,
                         request.method,
                         request.body,
                         accessToken.map(OAuthToken.apply))
}
