package fsclient.http.client

import cats.effect.IO
import fsclient.config.OAuthConsumer
import fsclient.entities.{AccessToken, FsClientPlainRequest, FsClientRequestWithBody, OAuthToken}
import io.circe.{Decoder, Encoder}

import scala.concurrent.ExecutionContext

// TODO: Load secret and key from config / env var / etc. so you give a purpose to this class
//  instead of just using the simple client passing the token on auth defs
class IOAuthClient(override val consumer: OAuthConsumer)(implicit val ec: ExecutionContext, accessToken: AccessToken)
    extends IOClient(consumer) {

  private val token = Some(OAuthToken(accessToken))

  final def getJson[R](request: FsClientPlainRequest.GET[R])(implicit responseDecoder: Decoder[R]): IOResponse[R] =
    effect.getJson(request, token)

  final def getPlainText[R](
    request: FsClientPlainRequest.GET[R]
  )(implicit responseDecoder: IOHttpPipe[String, R]): IOResponse[R] =
    effect.getPlainText(request, token)

  final def fetchJson[B, R](request: FsClientRequestWithBody[B, R])(implicit requestBodyEncoder: Encoder[B],
                                                                    responseDecoder: Decoder[R]): IOResponse[R] =
    effect.fetchJson(request, token)

  final def fetchPlainText[B, R](
    request: FsClientRequestWithBody[B, R]
  )(implicit requestBodyEncoder: Encoder[B], responseDecoder: HttpPipe[IO, String, R]): IOResponse[R] =
    effect.fetchPlainText(request, token)
}
