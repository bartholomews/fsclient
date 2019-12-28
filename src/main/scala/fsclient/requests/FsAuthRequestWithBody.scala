package fsclient.requests

import cats.effect.Effect
import fs2.Pipe
import fsclient.client.effect.HttpEffectClient
import fsclient.codecs.RawDecoder
import fsclient.entities.{HttpResponse, OAuthEnabled, OAuthToken}
import org.http4s.Method.{DefaultMethodWithBody, SafeMethodWithBody}
import org.http4s.{EntityEncoder, Method}

trait FsAuthRequestWithBody[Body, Raw, Res] extends FsClientRequestWithBody[Body] {
  final def runWith[F[_]: Effect](
    client: HttpEffectClient[F]
  )(implicit token: OAuthToken,
    requestBodyEncoder: EntityEncoder[F, Body],
    rawDecode: RawDecoder[Raw],
    resDecode: Pipe[F, Raw, Res]): F[HttpResponse[Res]] =
    client.fetch(this.toHttpRequest[F](client.consumer), OAuthEnabled(token))
}

object FsAuthRequestWithBody {
  trait Get[Body, Raw, Res] extends FsAuthRequestWithBody[Body, Raw, Res] {
    override val method: SafeMethodWithBody = Method.GET
  }
  trait Post[Body, Raw, Res] extends FsAuthRequestWithBody[Body, Raw, Res] {
    override val method: DefaultMethodWithBody = Method.POST
  }
}
