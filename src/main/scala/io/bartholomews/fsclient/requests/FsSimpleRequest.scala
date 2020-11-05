package io.bartholomews.fsclient.requests

import cats.effect.ConcurrentEffect
import fs2.Pipe
import io.bartholomews.fsclient.client.FsClient
import io.bartholomews.fsclient.codecs.RawDecoder
import io.bartholomews.fsclient.entities.oauth.Signer
import io.bartholomews.fsclient.utils.HttpTypes.HttpResponse
import io.circe.Json
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
    final override private[fsclient] def method = Method.GET
  }

  trait Put[Body, Raw, Res] extends FsSimpleRequest[Body, Raw, Res] {
    final override private[fsclient] def method = Method.PUT
  }

  trait Post[Body, Raw, Res] extends FsSimpleRequest[Body, Raw, Res] {
    final override private[fsclient] def method = Method.POST
  }

  trait Delete[Body, Raw, Res] extends FsSimpleRequest[Body, Raw, Res] {
    final override private[fsclient] def method = Method.DELETE
  }
}

/**
 * Requests which are not concerned with decoding a response, or expecting an empty response body.
 * If you need to sign / oAuth handling, you should use the `Auth` objects instead
 */
object FsSimple {

  trait PutEmpty extends FsSimpleRequest.Put[Nothing, Unit, Unit] {
    final override private[fsclient] def body: Option[Nothing] = None
  }

  trait PostEmpty extends FsSimpleRequest.Post[Nothing, Unit, Unit] {
    final override private[fsclient] def body: Option[Nothing] = None
  }

  trait DeleteEmpty extends FsSimpleRequest.Delete[Nothing, Unit, Unit] {
    final override private[fsclient] def body: Option[Nothing] = None
  }

  trait Put[Body] extends FsSimpleRequest.Put[Body, Unit, Unit] {
    def entityBody: Body
    final override private[fsclient] def body: Option[Body] = Some(entityBody)
  }

  trait Post[Body] extends FsSimpleRequest.Post[Body, Unit, Unit] {
    def entityBody: Body
    final override private[fsclient] def body: Option[Body] = Some(entityBody)
  }

  trait Delete[Body] extends FsSimpleRequest.Delete[Body, Unit, Unit] {
    def entityBody: Body
    final override private[fsclient] def body: Option[Body] = Some(entityBody)
  }
}

/**
 * Requests which are decoding a Json response into a `Res` type
 */
object FsSimpleJson {

  trait Get[Res] extends FsSimpleRequest.Get[Nothing, Json, Res] {
    final override private[fsclient] def body: Option[Nothing] = None
  }

  trait PutEmpty[Res] extends FsSimpleRequest.Put[Nothing, Json, Res] {
    final override private[fsclient] def body: Option[Nothing] = None
  }

  trait PostEmpty[Res] extends FsSimpleRequest.Post[Nothing, Json, Res] {
    final override private[fsclient] def body: Option[Nothing] = None
  }

  trait DeleteEmpty[Res] extends FsSimpleRequest.Delete[Nothing, Json, Res] {
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

  trait Delete[Body, Res] extends FsSimpleRequest.Delete[Body, Json, Res] {
    def entityBody: Body
    final override private[fsclient] def body: Option[Body] = Some(entityBody)
  }
}

/**
 * Requests which are decoding a PlainText response into a `Res` type
 */
object FsSimplePlainText {

  trait Get[Res] extends FsSimpleRequest.Get[Nothing, String, Res] {
    final override private[fsclient] def body: Option[Nothing] = None
  }

  trait PutEmpty[Res] extends FsSimpleRequest.Put[Nothing, String, Res] {
    final override private[fsclient] def body: Option[Nothing] = None
  }

  trait PostEmpty[Res] extends FsSimpleRequest.Post[Nothing, String, Res] {
    final override private[fsclient] def body: Option[Nothing] = None
  }

  trait DeleteEmpty[Res] extends FsSimpleRequest.Delete[Nothing, String, Res] {
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

  trait Delete[Body, Res] extends FsSimpleRequest.Delete[Body, String, Res] {
    def entityBody: Body
    final override private[fsclient] def body: Option[Body] = Some(entityBody)
  }
}
