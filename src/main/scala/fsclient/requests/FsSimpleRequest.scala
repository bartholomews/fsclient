package fsclient.requests

import cats.effect.Effect
import fs2.Pipe
import fsclient.client.effect.HttpEffectClient
import fsclient.codecs.RawDecoder
import fsclient.entities.{HttpResponse, OAuthDisabled}
import org.http4s.Method
import org.http4s.Method.{DefaultMethodWithBody, SafeMethodWithBody}

sealed trait FsSimpleRequest[Raw, Res] extends FsClientPlainRequest {
  final def runWith[F[_]: Effect](
    client: HttpEffectClient[F]
  )(implicit
    rawDecoder: RawDecoder[Raw],
    decode: Pipe[F, Raw, Res]): F[HttpResponse[Res]] =
    client.fetch[Raw, Res](this.toHttpRequest[F](client.consumer), OAuthDisabled)
}

object FsSimpleRequest {
  trait Get[Raw, Res] extends FsSimpleRequest[Raw, Res] {
    override val method: SafeMethodWithBody = Method.GET
  }
  trait Post[Raw, Res] extends FsSimpleRequest[Raw, Res] {
    override val method: DefaultMethodWithBody = Method.POST
  }
}
