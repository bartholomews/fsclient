package fsclient.requests

import cats.effect.Effect
import fs2.Pipe
import fsclient.client.effect.HttpEffectClient
import fsclient.codecs.RawDecoder
import fsclient.entities._
import io.circe.Json
import org.http4s.Method.{DefaultMethodWithBody, SafeMethodWithBody}
import org.http4s.{EntityEncoder, Method}

sealed trait FsAuthRequest[Body, Raw, Res] extends FsClientRequest[Body] {
  final def runWith[F[_]: Effect, V <: OAuthVersion](client: HttpEffectClient[F, _])(
    implicit
    signer: Signer[V],
    requestBodyEncoder: EntityEncoder[F, Body],
    rawDecoder: RawDecoder[Raw],
    resDecoder: Pipe[F, Raw, Res]
  ): F[FsResponse.Of[Res]] =
    client.fetch(this.toHttpRequest[F](client.appConfig.userAgent), OAuthEnabled(signer))
}

object FsAuthRequest {

  trait Get[Body, Raw, Res] extends FsAuthRequest[Body, Raw, Res] {
    final override private[fsclient] def method: SafeMethodWithBody = Method.GET
  }

  trait Post[Body, Raw, Res] extends FsAuthRequest[Body, Raw, Res] {
    final override private[fsclient] def method: DefaultMethodWithBody = Method.POST
  }
}

object AuthJsonRequest {
  trait Get[Res] extends FsAuthRequest.Get[Nothing, Json, Res] {
    final override private[fsclient] def body: Option[Nothing] = None
  }

  trait Post[Body, Res] extends FsAuthRequest.Post[Body, Json, Res] {
    def entityBody: Body
    final override private[fsclient] def body: Option[Body] = Some(entityBody)
  }
}

object AuthPlainTextRequest {
  trait Get[Res] extends FsAuthRequest.Get[Nothing, String, Res] {
    final override private[fsclient] def body: Option[Nothing] = None
  }
  trait Post[Body, Res] extends FsAuthRequest.Post[Body, String, Res] {
    def entityBody: Body
    final override private[fsclient] def body: Option[Body] = Some(entityBody)
  }
}
