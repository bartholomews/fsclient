package fsclient.client.io_client

import cats.effect.IO
import fs2.Pipe
import fsclient.config.{FsClientConfig, UserAgent}
import fsclient.entities.AuthVersion.V1
import fsclient.entities._
import fsclient.requests.{AccessTokenEndpointBase, AccessTokenRequestV1}

import scala.concurrent.ExecutionContext

class IOAuthClient[V <: AuthVersion](userAgent: UserAgent, signer: Signer)(implicit val ec: ExecutionContext)
    extends IOClient {

  // TODO: Double check with OAuth RFC
  final def accessTokenRequest(
    requestToken: V1.RequestToken,
    endpoint: AccessTokenEndpointBase
  )(implicit decode: Pipe[IO, String, V1.AccessToken]): IO[HttpResponse[V1.AccessToken]] = {
    import fsclient.implicits._
    val request = AccessTokenRequestV1(endpoint.uri, requestToken)(signer.consumer)
    super.fetch[String, V1.AccessToken](request.toHttpRequest[IO](appConfig.userAgent), AuthEnabled(request.token))
  }

  final override val appConfig: FsClientConfig[AuthInfo] = FsClientConfig(userAgent, AuthEnabled(signer))
}
