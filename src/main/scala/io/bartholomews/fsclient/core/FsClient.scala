package io.bartholomews.fsclient.core

import io.bartholomews.fsclient.core.config.FsClientConfig
import io.bartholomews.fsclient.core.oauth.Signer
import sttp.client.SttpBackend

case class FsClient[F[_], S <: Signer](appConfig: FsClientConfig[S], sttpBackend: SttpBackend[F, Nothing, Nothing])
