package fsclient.http.client

import cats.effect.IO
import fsclient.config.OAuthConsumer
import fsclient.entities.{AccessToken, HttpRequest, OAuthToken}
import io.circe.{Decoder, Encoder}

import scala.concurrent.ExecutionContext

// TODO: Load secret and key from config / env var / etc. so you give a purpose to this class
//  instead of just using the simple client passing the token on auth defs
class IOAuthClient(override val consumer: OAuthConsumer,
                   accessToken: AccessToken)(implicit val ec: ExecutionContext)
    extends IOClient(consumer) {

  private val token = Some(OAuthToken(accessToken))

  final def getJson[R](request: HttpRequest.GET[R])(
      implicit responseDecoder: Decoder[R]): IOResponse[R] =
    super.getJson(request.uri, token)

  final def getPlainText[R](request: HttpRequest.GET[R])(
      implicit responseDecoder: IOHttpPipe[String, R]): IOResponse[R] =
    super.getPlainText(request.uri, token)

  final def fetchJson[B, R](request: HttpRequest[B, R])(
      implicit requestBodyEncoder: Encoder[B],
      responseDecoder: Decoder[R]): IOResponse[R] =
    super.fetchJson(request.uri, request.method, request.body, token)

  final def fetchPlainText[B, R](request: HttpRequest[B, R])(
      implicit requestBodyEncoder: Encoder[B],
      responseDecoder: HttpPipe[IO, String, R]): IOResponse[R] =
    super.fetchPlainText(request.uri, request.method, request.body, token)
}
