package fsclient.http.client

import cats.effect.IO
import fsclient.config.OAuthConsumer
import fsclient.entities._
import fsclient.http.client.base.{IOBaseCalls, IOBaseClient}
import fsclient.oauth.OAuthVersion
import fsclient.utils.HttpTypes.{HttpPipe, IOResponse}
import io.circe.Decoder
import org.http4s.EntityEncoder

import scala.concurrent.ExecutionContext

// TODO: Load secret and key from config / env var / etc. so you give a purpose to this class
//  instead of just using the simple client passing the token on auth defs
/**
 * Class which executes authenticated calls by default,
 * without the need to pass the `accessToken` on each request.
 */
class IOAuthClient(override val consumer: OAuthConsumer, version: OAuthVersion)(implicit val ec: ExecutionContext,
                                                                                accessToken: AccessToken)
    extends IOBaseClient(consumer)
    with IOBaseCalls {

  private val oAuthTokenInfo = OAuthTokenInfo(accessToken, Some(version))

  final override def fetchJson[R](request: FsClientPlainRequest)(implicit responseDecoder: Decoder[R]): IOResponse[R] =
    super.fetchJson[R](request.toHttpRequest[IO], oAuthTokenInfo)

  final override def fetchPlainText[R](
    request: FsClientPlainRequest
  )(implicit responseDecoder: HttpPipe[IO, String, R]): IOResponse[R] =
    super.fetchPlainText(request.toHttpRequest[IO], oAuthTokenInfo)

  final override def fetchJsonWithBody[B, R](
    request: FsClientRequestWithBody[B]
  )(implicit requestBodyEncoder: EntityEncoder[IO, B], responseDecoder: Decoder[R]): IOResponse[R] =
    super.fetchJson(request.toHttpRequest[IO], oAuthTokenInfo)

  final override def fetchPlainTextWithBody[B, R](
    request: FsClientRequestWithBody[B]
  )(implicit requestBodyEncoder: EntityEncoder[IO, B], responseDecoder: HttpPipe[IO, String, R]): IOResponse[R] =
    super.fetchPlainText(request.toHttpRequest[IO], oAuthTokenInfo)
}
