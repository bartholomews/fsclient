package fsclient.requests

import cats.effect.Effect
import fsclient.http.effect.HttpEffectClient
import io.circe.Decoder
import org.http4s.Method
import org.http4s.Method.{DefaultMethodWithBody, SafeMethodWithBody}

trait FsSimpleJsonRequest[Res] extends FsClientPlainRequest {
  final def runWith[F[_]: Effect](
    client: HttpEffectClient[F]
  )(implicit responseDecoder: Decoder[Res]): F[HttpResponse[Res]] =
    client.fetchJson(this.toHttpRequest[F](client.consumer), OAuthDisabled)
}

object FsSimpleJsonRequest {
  trait Get[Res] extends FsSimpleJsonRequest[Res] {
    override val method: SafeMethodWithBody = Method.GET
  }
  trait Post[Res] extends FsSimpleJsonRequest[Res] {
    override val method: DefaultMethodWithBody = Method.POST
  }
}
