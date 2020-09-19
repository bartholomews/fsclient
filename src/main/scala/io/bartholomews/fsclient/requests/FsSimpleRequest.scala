package io.bartholomews.fsclient.requests

import cats.effect.ConcurrentEffect
import fs2.Pipe
import io.bartholomews.fsclient.client.FsClient
import io.bartholomews.fsclient.codecs.RawDecoder
import io.bartholomews.fsclient.entities.oauth.Signer
import io.bartholomews.fsclient.utils.HttpTypes.HttpResponse
import io.circe.Json
import org.http4s.Method.{DefaultMethodWithBody, IdempotentMethodWithBody, SafeMethodWithBody}
import org.http4s._

sealed trait FsSimpleRequest[Body, Raw, Res] extends FsClientRequest[Body] {
  final def runWith[F[_]: ConcurrentEffect, S <: Signer](client: FsClient[F, S])(implicit
    requestBodyEncoder: EntityEncoder[F, Body],
    rawDecoder: RawDecoder[Raw],
    resDecoder: Pipe[F, Raw, Res]
  ): F[HttpResponse[Res]] =
    client.fetch(this.toHttpRequest[F](client.appConfig.userAgent), client.appConfig.signer)
}

private[fsclient] object FsSimpleRequest {
  trait Get[Body, Raw, Res] extends FsSimpleRequest[Body, Raw, Res] {
    final override private[fsclient] def method: SafeMethodWithBody = Method.GET
  }

  trait Put[Body, Raw, Res] extends FsSimpleRequest[Body, Raw, Res] {
    final override private[fsclient] def method: IdempotentMethodWithBody = Method.PUT
  }

  trait Post[Body, Raw, Res] extends FsSimpleRequest[Body, Raw, Res] {
    final override private[fsclient] def method: DefaultMethodWithBody = Method.POST
  }
}

object SimpleRequest {

  trait PutEmpty extends FsSimpleRequest.Put[Nothing, Unit, Unit]

  trait Put[Body] extends FsSimpleRequest.Put[Body, Unit, Unit] {
    def entityBody: Body
    final override private[fsclient] def body: Option[Body] = Some(entityBody)
  }

  trait Post[Body] extends FsSimpleRequest.Post[Body, Unit, Unit] {
    def entityBody: Body
    final override private[fsclient] def body: Option[Body] = Some(entityBody)
  }
}

object JsonRequest {

  trait Get[Res] extends FsSimpleRequest.Get[Nothing, Json, Res] {
    final override private[fsclient] def body: Option[Nothing] = None
  }

  trait Put[Body, Res] extends FsSimpleRequest.Put[Body, Json, Res] {
    def entityBody: Body
    final override private[fsclient] def body: Option[Body] = Some(entityBody)
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

  trait Put[Body, Res] extends FsSimpleRequest.Put[Body, String, Res] {
    def entityBody: Body
    final override private[fsclient] def body: Option[Body] = Some(entityBody)
  }

  trait Post[Body, Res] extends FsSimpleRequest.Post[Body, String, Res] {
    def entityBody: Body
    final override private[fsclient] def body: Option[Body] = Some(entityBody)
  }
}
