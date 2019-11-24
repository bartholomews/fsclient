package fsclient.http.client

import cats.data.EitherT
import cats.effect.IO
import fsclient.config.OAuthConsumer
import fsclient.entities._
import fsclient.http.client.base.{IOAuthCalls, IOBaseCalls, IOBaseClient}
import fsclient.oauth.OAuthVersion
import fsclient.utils.HttpTypes.{IOHttpPipe, IOResponse}
import io.circe.Decoder
import org.http4s.EntityEncoder

import scala.concurrent.ExecutionContext

/**
 * Class able to execute both unauthenticated calls (via the `simple` object)
 * and per-method authenticated call (via the `auth` object).
 * In both cases requests are signed with OAuth (if `oAuthVersion` is defined).
 */
class IOClient(val consumer: OAuthConsumer, oAuthVersion: OAuthVersion)(implicit val ec: ExecutionContext) {

  object auth extends IOBaseClient(consumer) with IOAuthCalls {

    final override def accessTokenRequest(
      request: AccessTokenRequest
    )(implicit responseDecoder: IOHttpPipe[String, AccessToken]): IO[HttpResponse[AccessToken]] =
      super.fetchPlainText(request.toHttpRequest[IO], OAuthTokenInfo(Some(oAuthVersion), request.token))

    final override def toOAuthClient(
      request: AccessTokenRequest
    )(implicit responseDecoder: IOHttpPipe[String, AccessToken]): IO[Either[ResponseError, IOAuthClient]] =
      (for {
        accessToken <- EitherT(accessTokenRequest(request).map(_.entity))
        res <- EitherT.pure[IO, ResponseError](new IOAuthClient(consumer, oAuthVersion)(implicitly, accessToken))
      } yield res).value

    final override def fetchJson[R](
      accessToken: AccessToken
    )(request: FsClientPlainRequest)(implicit responseDecoder: Decoder[R]): IOResponse[R] =
      super.fetchJson(request.toHttpRequest[IO], OAuthTokenInfo(accessToken, Some(oAuthVersion)))

    final override def fetchPlainText[R](
      accessToken: AccessToken
    )(request: FsClientPlainRequest)(implicit responseDecoder: IOHttpPipe[String, R]): IOResponse[R] =
      super.fetchPlainText(request.toHttpRequest[IO], OAuthTokenInfo(accessToken, Some(oAuthVersion)))

    final override def fetchJsonWithBody[B, R](
      accessToken: AccessToken
    )(request: FsClientRequestWithBody[B])(implicit requestBodyEncoder: EntityEncoder[IO, B],
                                           responseDecoder: Decoder[R]): IOResponse[R] =
      super.fetchJson(request.toHttpRequest[IO], OAuthTokenInfo(accessToken, Some(oAuthVersion)))

    final override def fetchPlainTextWithBody[B, R](
      accessToken: AccessToken
    )(request: FsClientRequestWithBody[B])(implicit requestBodyEncoder: EntityEncoder[IO, B],
                                           responseDecoder: IOHttpPipe[String, R]): IOResponse[R] =
      super.fetchPlainText(request.toHttpRequest[IO], OAuthTokenInfo(accessToken, Some(oAuthVersion)))
  }

  object simple extends IOBaseClient(consumer) with IOBaseCalls {

    final override def fetchJson[R](
      request: FsClientPlainRequest
    )(implicit responseDecoder: Decoder[R]): IOResponse[R] =
      super.fetchJson(request.toHttpRequest[IO], Basic(Some(oAuthVersion)))

    final override def fetchPlainText[R](
      request: FsClientPlainRequest
    )(implicit responseDecoder: IOHttpPipe[String, R]): IOResponse[R] =
      super.fetchPlainText(request.toHttpRequest[IO], Basic(Some(oAuthVersion)))

    final override def fetchJsonWithBody[B, R](
      request: FsClientRequestWithBody[B]
    )(implicit requestBodyEncoder: EntityEncoder[IO, B], responseDecoder: Decoder[R]): IOResponse[R] =
      super.fetchJson(request.toHttpRequest[IO], oAuthInfo = Basic(Some(oAuthVersion)))

    final override def fetchPlainTextWithBody[B, R](
      request: FsClientRequestWithBody[B]
    )(implicit requestBodyEncoder: EntityEncoder[IO, B], responseDecoder: IOHttpPipe[String, R]): IOResponse[R] =
      super.fetchPlainText(request.toHttpRequest[IO], oAuthInfo = Basic(Some(oAuthVersion)))
  }
}
