package io.bartholomews.fsclient.requests

import cats.effect.ConcurrentEffect
import fs2.Pipe
import io.bartholomews.fsclient.client.effect.HttpEffectClient
import io.bartholomews.fsclient.codecs.RawDecoder
import io.bartholomews.fsclient.entities.{OAuthVersion, Signer}
import io.bartholomews.fsclient.utils.HttpTypes.HttpResponse
import io.circe.Json
import org.http4s.Method.{DefaultMethodWithBody, SafeMethodWithBody}
import org.http4s._

sealed trait FsSimpleRequest[Body, Raw, Res] extends FsClientRequest[Body] {
  final def runWith[F[_]: ConcurrentEffect, V <: OAuthVersion, S <: Signer[V]](client: HttpEffectClient[F, V])(
    implicit
    requestBodyEncoder: EntityEncoder[F, Body],
    rawDecoder: RawDecoder[Raw],
    resDecoder: Pipe[F, Raw, Res]
  ): F[HttpResponse[V, Res]] =
    client.fetch(this.toHttpRequest[F](client.appConfig.userAgent), client.appConfig.signer)
}

object FsSimpleRequest {

  trait Get[Body, Raw, Res] extends FsSimpleRequest[Body, Raw, Res] {
    final override private[fsclient] def method: SafeMethodWithBody = Method.GET
  }

  trait Post[Body, Raw, Res] extends FsSimpleRequest[Body, Raw, Res] {
    final override private[fsclient] def method: DefaultMethodWithBody = Method.POST
  }
}

object JsonRequest {

  trait Get[Res] extends FsSimpleRequest.Get[Nothing, Json, Res] {
    final override private[fsclient] def body: Option[Nothing] = None
  }

  trait Post[Body, Res] extends FsSimpleRequest.Post[Body, Json, Res] {
    def entityBody: Body
    final override private[fsclient] def body: Option[Body] = Some(entityBody)
  }
}

object PlainTextRequest {

  trait Get[Res] extends FsSimpleRequest.Get[Nothing, String, Res] {
    final override private[fsclient] def body: Option[Nothing] = None
  }

  trait Post[Body, Res] extends FsSimpleRequest.Post[Body, String, Res] {
    def entityBody: Body
    final override private[fsclient] def body: Option[Body] = Some(entityBody)
  }
}
