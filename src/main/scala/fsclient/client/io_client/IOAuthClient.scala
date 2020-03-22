package fsclient.client.io_client

import fsclient.config.{FsClientConfig, UserAgent}
import fsclient.entities._

import scala.concurrent.ExecutionContext

class IOAuthClient[V <: OAuthVersion](userAgent: UserAgent, signer: Signer)(implicit val ec: ExecutionContext)
    extends IOClient[OAuthEnabled] {

  final override val appConfig: FsClientConfig[OAuthEnabled] = FsClientConfig(userAgent, OAuthEnabled(signer))
}
