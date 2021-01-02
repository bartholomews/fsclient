package io.bartholomews.fsclient.client

import io.bartholomews.fsclient.client.ClientData.{sampleConsumer, sampleUserAgent}
import io.bartholomews.fsclient.core.FsClient
import io.bartholomews.fsclient.core.oauth.{AuthDisabled, SignerV1}
import sttp.client.{HttpURLConnectionBackend, Identity, NothingT, SttpBackend}

trait IdentityClient {

  implicit val sttpIdentityBackend: SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()

  val clientNoAuth: FsClient[Identity, AuthDisabled.type] =
    FsClient(sampleUserAgent, AuthDisabled, sttpIdentityBackend)

  val clientCredentials: FsClient[Identity, SignerV1] =
    FsClient.v1.clientCredentials(sampleUserAgent, sampleConsumer)
}
