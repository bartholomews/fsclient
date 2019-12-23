package fsclient.http.client

import cats.data.EitherT
import cats.effect.IO
import fs2.Pipe
import fsclient.config.AppConsumer
import fsclient.http.client.base.IOBaseClient
import fsclient.oauth.OAuthVersion.OAuthV1.{AccessTokenRequestV1, AccessTokenV1}
import fsclient.requests._

import scala.concurrent.ExecutionContext

/**
 * Class able to execute both unauthenticated calls (via the `simple` object)
 * and per-method authenticated call (via the `auth` object).
 * In both cases requests are signed with OAuth (if `oAuthVersion` is defined).
 */
class IOClient(override val consumer: AppConsumer)(implicit val ec: ExecutionContext) extends IOBaseClient(consumer) {

  // FIXME: Double check these:
  final def accessTokenRequest(
    request: AccessTokenRequestV1
  )(implicit decode: Pipe[IO, String, AccessTokenV1]): IO[HttpResponse[AccessTokenV1]] = {
    import fsclient.implicits.plainTextPipe
    super.fetch[String, AccessTokenV1](request.toHttpRequest[IO](consumer), OAuthEnabled(request.token))
  }

  // FIXME: Double check these:
  final def toOAuthClientV1(
    request: AccessTokenRequestV1
  )(implicit decode: Pipe[IO, String, AccessTokenV1]): IO[Either[ResponseError, IOAuthClient]] =
    (for {
      accessToken <- EitherT(accessTokenRequest(request).map(_.entity))
      res <- EitherT.pure[IO, ResponseError](
        new IOAuthClient(consumer)(implicitly, accessToken)
      )
    } yield res).value
}
