package io.bartholomews.fsclient.requests

import cats.effect.ConcurrentEffect
import fs2.Pipe
import io.bartholomews.fsclient.client.FsClient
import io.bartholomews.fsclient.codecs.RawDecoder
import io.bartholomews.fsclient.entities.oauth.Signer
import io.bartholomews.fsclient.utils.HttpTypes.HttpResponse
import io.circe.Json
import org.http4s.Method.{DefaultMethodWithBody, IdempotentMethodWithBody, SafeMethodWithBody}
import org.http4s.{EntityEncoder, Method}

sealed trait FsAuthRequest[Body, Raw, Res] extends FsClientRequest[Body] {
  final def runWith[F[_]: ConcurrentEffect](client: FsClient[F, _])(implicit
    signer: Signer,
    requestBodyEncoder: EntityEncoder[F, Body],
    rawDecoder: RawDecoder[Raw],
    resDecoder: Pipe[F, Raw, Res]
  ): F[HttpResponse[Res]] =
    client.fetch(this.toHttpRequest[F](client.appConfig.userAgent), signer)
}

private[fsclient] object FsAuthRequest {

  trait Get[Body, Raw, Res] extends FsAuthRequest[Body, Raw, Res] {
    final override private[fsclient] def method: SafeMethodWithBody = Method.GET
  }

  trait Put[Body, Raw, Res] extends FsAuthRequest[Body, Raw, Res] {
    final override private[fsclient] def method: IdempotentMethodWithBody = Method.PUT
  }

  trait Post[Body, Raw, Res] extends FsAuthRequest[Body, Raw, Res] {
    final override private[fsclient] def method: DefaultMethodWithBody = Method.POST
  }
}

/**
 * Requests which are not concerned with decoding a response, or expecting an empty response body.
 * If you don't need to sign / oAuth handling, you should use the `Simple` objects instead
 */
object FsAuth {

  trait PutEmpty extends FsAuthRequest.Put[Nothing, Unit, Unit] {
    final override private[fsclient] def body: Option[Nothing] = None
  }

  trait Put[Body] extends FsAuthRequest.Put[Body, Unit, Unit] {
    def entityBody: Body
    final override private[fsclient] def body: Option[Body] = Some(entityBody)
  }

  trait Post[Body] extends FsAuthRequest.Post[Body, Unit, Unit] {
    def entityBody: Body
    final override private[fsclient] def body: Option[Body] = Some(entityBody)
  }
}

/**
 * Requests which are decoding a Json response into a `Res` type
 */
object FsAuthJson {
  trait Get[Res] extends FsAuthRequest.Get[Nothing, Json, Res] {
    final override private[fsclient] def body: Option[Nothing] = None
  }

  trait Put[Body, Res] extends FsAuthRequest.Put[Body, Json, Res] {
    def entityBody: Body
    final override private[fsclient] def body: Option[Body] = Some(entityBody)
  }

  trait Post[Body, Res] extends FsAuthRequest.Post[Body, Json, Res] {
    def entityBody: Body
    final override private[fsclient] def body: Option[Body] = Some(entityBody)
  }
}

/**
 * Requests which are decoding a PlainText response into a `Res` type
 */
object FsAuthPlainText {
  trait Get[Res] extends FsAuthRequest.Get[Nothing, String, Res] {
    final override private[fsclient] def body: Option[Nothing] = None
  }

  trait Put[Body, Res] extends FsAuthRequest.Put[Body, String, Res] {
    def entityBody: Body
    final override private[fsclient] def body: Option[Body] = Some(entityBody)
  }

  trait Post[Body, Res] extends FsAuthRequest.Post[Body, String, Res] {
    def entityBody: Body
    final override private[fsclient] def body: Option[Body] = Some(entityBody)
  }
}