package fsclient.http.client

import cats.data.EitherT
import cats.effect.IO
import fsclient.config.OAuthConsumer
import fsclient.entities._
import fsclient.http.client.base.{IOAuthCalls, IOBaseCalls, IOBaseClient}
import fsclient.oauth.OAuthToken
import fsclient.oauth.OAuthVersion.OAuthV1.{AccessTokenRequestV1, AccessTokenV1}
import fsclient.utils.HttpTypes.{IOHttpPipe, IOResponse}
import io.circe.Decoder
import org.http4s.EntityEncoder

import scala.concurrent.ExecutionContext

/**
 * Class able to execute both unauthenticated calls (via the `simple` object)
 * and per-method authenticated call (via the `auth` object).
 * In both cases requests are signed with OAuth (if `oAuthVersion` is defined).
 */
class IOClient(val consumer: OAuthConsumer)(implicit val ec: ExecutionContext) {

  object auth extends IOBaseClient(consumer) with IOAuthCalls {

    // FIXME: Double check these:
    final override def accessTokenRequest(
      request: AccessTokenRequestV1
    )(implicit responseDecoder: IOHttpPipe[String, AccessTokenV1]): IO[HttpResponse[AccessTokenV1]] =
      super.fetchPlainText(request.toHttpRequest[IO], OAuthEnabled(request.token))

    // FIXME: Double check these:
    final override def toOAuthClientV1(
      request: AccessTokenRequestV1
    )(implicit responseDecoder: IOHttpPipe[String, AccessTokenV1]): IO[Either[ResponseError, IOAuthClient]] =
      (for {
        accessToken <- EitherT(accessTokenRequest(request).map(_.entity))
        res <- EitherT.pure[IO, ResponseError](
          new IOAuthClient(consumer)(implicitly, accessToken)
        )
      } yield res).value

    final override def fetchJson[R](
      token: OAuthToken
    )(request: FsClientPlainRequest)(implicit responseDecoder: Decoder[R]): IOResponse[R] =
      super.fetchJson(request.toHttpRequest[IO], OAuthEnabled(token))

    final override def fetchPlainText[R](
      token: OAuthToken
    )(request: FsClientPlainRequest)(implicit responseDecoder: IOHttpPipe[String, R]): IOResponse[R] =
      super.fetchPlainText(request.toHttpRequest[IO], OAuthEnabled(token))

    final override def fetchJsonWithBody[B, R](
      token: OAuthToken
    )(request: FsClientRequestWithBody[B])(implicit requestBodyEncoder: EntityEncoder[IO, B],
                                           responseDecoder: Decoder[R]): IOResponse[R] =
      super.fetchJson(request.toHttpRequest[IO], OAuthEnabled(token))

    final override def fetchPlainTextWithBody[B, R](
      token: OAuthToken
    )(request: FsClientRequestWithBody[B])(implicit requestBodyEncoder: EntityEncoder[IO, B],
                                           responseDecoder: IOHttpPipe[String, R]): IOResponse[R] =
      super.fetchPlainText(request.toHttpRequest[IO], OAuthEnabled(token))
  }

  object simple extends IOBaseClient(consumer) with IOBaseCalls {

    final override def fetchJson[R](
      request: FsClientPlainRequest
    )(implicit responseDecoder: Decoder[R]): IOResponse[R] =
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
}
