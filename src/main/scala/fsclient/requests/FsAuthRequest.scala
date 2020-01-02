package fsclient.requests

import cats.effect.Effect
import fs2.Pipe
import fsclient.client.effect.HttpEffectClient
import fsclient.codecs.RawDecoder
import fsclient.entities.{HttpResponse, OAuthEnabled, OAuthToken}
import org.http4s.Method.{DefaultMethodWithBody, SafeMethodWithBody}
import org.http4s.{EntityEncoder, Method}

sealed trait FsAuthRequest[Body, Raw, Res] extends FsClientRequest[Body] {
  final def runWith[F[_]: Effect](
    client: HttpEffectClient[F]
  )(implicit
    token: OAuthToken,
    requestBodyEncoder: EntityEncoder[F, Body],
    rawDecoder: RawDecoder[Raw],
    resDecoder: Pipe[F, Raw, Res]): F[HttpResponse[Res]] =
    client.fetch(this.toHttpRequest[F](client.consumer), OAuthEnabled(token))
}

object FsAuthRequest {

  trait Get[Body, Raw, Res] extends FsAuthRequest[Body, Raw, Res] {
    override val method: SafeMethodWithBody = Method.GET
  }

  trait Post[Body, Raw, Res] extends FsAuthRequest[Body, Raw, Res] {
    override val method: DefaultMethodWithBody = Method.POST
  }
}
