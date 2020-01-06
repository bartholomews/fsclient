package fsclient.requests

import cats.effect.Effect
import fs2.Pipe
import fsclient.client.effect.HttpEffectClient
import fsclient.codecs.RawDecoder
import fsclient.entities.{HttpResponse, OAuthDisabled}
import io.circe.Json
import org.http4s.Method.{DefaultMethodWithBody, SafeMethodWithBody}
import org.http4s._

sealed trait FsSimpleRequest[Body, Raw, Res] extends FsClientRequest[Body] {
  final def runWith[F[_]: Effect](
    client: HttpEffectClient[F]
  )(implicit
    requestBodyEncoder: EntityEncoder[F, Body],
    rawDecoder: RawDecoder[Raw],
    resDecoder: Pipe[F, Raw, Res]): F[HttpResponse[Res]] =
    client.fetch[Raw, Res](this.toHttpRequest[F](client.consumer), OAuthDisabled)
}

object FsSimpleRequest {

  trait Get[Body, Raw, Res] extends FsSimpleRequest[Body, Raw, Res] {
    override val method: SafeMethodWithBody = Method.GET
  }

  trait Post[Body, Raw, Res] extends FsSimpleRequest[Body, Raw, Res] {
    override val method: DefaultMethodWithBody = Method.POST
  }
}

object JsonRequest {
  trait Get[Res] extends FsSimpleRequest.Get[Nothing, Json, Res]
  trait Post[Body, Res] extends FsSimpleRequest.Post[Body, Json, Res]
}

object PlainTextRequest {
  trait Get[Res] extends FsSimpleRequest.Get[Nothing, String, Res]
  trait Post[Body, Res] extends FsSimpleRequest.Post[Body, String, Res]
}
