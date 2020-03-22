package fsclient.client.io_client

import fsclient.config.{FsClientConfig, UserAgent}
import fsclient.entities._

import scala.concurrent.ExecutionContext

class IOAuthClient[V <: OAuthVersion](userAgent: UserAgent, signer: Signer[V])(implicit val ec: ExecutionContext)
    extends IOClient[OAuthEnabled[V]] {

  final override val appConfig: FsClientConfig[OAuthEnabled[V]] = FsClientConfig(userAgent, OAuthEnabled(signer))
}
