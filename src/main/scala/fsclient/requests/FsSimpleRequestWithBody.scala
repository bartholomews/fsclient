package fsclient.requests

import cats.effect.Effect
import fs2.Pipe
import fsclient.http.client.base.RawDecoder
import fsclient.http.effect.HttpEffectClient
import org.http4s.Method.{DefaultMethodWithBody, SafeMethodWithBody}
import org.http4s.{EntityEncoder, Method}

trait FsSimpleRequestWithBody[Body, Raw, Res] extends FsClientRequestWithBody[Body] {
  final def runWith[F[_]: Effect](
    client: HttpEffectClient[F]
  )(implicit
    requestBodyEncoder: EntityEncoder[F, Body],
    rawDecode: RawDecoder[Raw],
    resDecode: Pipe[F, Raw, Res]): F[HttpResponse[Res]] =
    client.fetch[Raw, Res](this.toHttpRequest[F](client.consumer), OAuthDisabled)
}

object FsSimpleRequestWithBody {
  trait Get[Body, Raw, Res] extends FsSimpleRequestWithBody[Body, Raw, Res] {
    override val method: SafeMethodWithBody = Method.GET
  }
  trait Post[Body, Raw, Res] extends FsSimpleRequestWithBody[Body, Raw, Res] {
    override val method: DefaultMethodWithBody = Method.POST
  }
}
