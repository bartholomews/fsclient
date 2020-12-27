package io.bartholomews.fsclient.core

import io.bartholomews.fsclient.core.http.FsClientSttpExtensions
import io.bartholomews.fsclient.core.oauth.Signer
import sttp.client.SttpBackend

abstract case class FsApiClient[F[_], S <: Signer](client: FsClient[F, S]) extends FsClientSttpExtensions {
  implicit val backend: SttpBackend[F, Nothing, Nothing] = client.sttpBackend
}
