package fsclient.client.io_client

import fsclient.config.{FsClientConfig, UserAgent}
import fsclient.entities._

import scala.concurrent.ExecutionContext

class IOAuthClient(userAgent: UserAgent, signer: Signer)(implicit val ec: ExecutionContext) extends IOClient {

  final override val appConfig: FsClientConfig[AuthInfo] = FsClientConfig(userAgent, AuthEnabled(signer))
}
