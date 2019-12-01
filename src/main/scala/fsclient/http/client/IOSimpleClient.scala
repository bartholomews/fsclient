package fsclient.http.client

import cats.effect.IO
import fsclient.config.OAuthConsumer
import fsclient.entities._
import fsclient.http.client.base.{IOBaseCalls, IOBaseClient}
import fsclient.utils.HttpTypes.{IOHttpPipe, IOResponse}
import io.circe.Decoder
import org.http4s.EntityEncoder

import scala.concurrent.ExecutionContext

/**
 * Class which executes unauthenticated calls.
 */
class IOSimpleClient(consumer: OAuthConsumer)(implicit val ec: ExecutionContext)
    extends IOBaseClient(consumer)
    with IOBaseCalls {

  final override def fetchJson[R](request: FsClientPlainRequest)(implicit responseDecoder: Decoder[R]): IOResponse[R] =
    super.fetchJson(request.toHttpRequest[IO], OAuthDisabled)

  final override def fetchPlainText[R](
    request: FsClientPlainRequest
  )(implicit responseDecoder: IOHttpPipe[String, R]): IOResponse[R] =
    super.fetchPlainText(request.toHttpRequest[IO], OAuthDisabled)

  final override def fetchJsonWithBody[B, R](
    request: FsClientRequestWithBody[B]
  )(implicit requestBodyEncoder: EntityEncoder[IO, B], responseDecoder: Decoder[R]): IOResponse[R] =
    super.fetchJson(request.toHttpRequest[IO], OAuthDisabled)

  final override def fetchPlainTextWithBody[B, R](
    request: FsClientRequestWithBody[B]
  )(implicit requestBodyEncoder: EntityEncoder[IO, B], responseDecoder: IOHttpPipe[String, R]): IOResponse[R] =
    super.fetchPlainText(request.toHttpRequest[IO], OAuthDisabled)
}
