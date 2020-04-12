package io.bartholomews.fsclient.client.io_client

import io.bartholomews.fsclient.config.{FsClientConfig, UserAgent}
import io.bartholomews.fsclient.entities._

import scala.concurrent.ExecutionContext

class IOAuthClient[V <: OAuthVersion](userAgent: UserAgent, signer: Signer[V])(implicit val ec: ExecutionContext)
    extends IOClient[OAuthEnabled[V]] {

  final override val appConfig: FsClientConfig[OAuthEnabled[V]] = FsClientConfig(userAgent, OAuthEnabled(signer))
}
