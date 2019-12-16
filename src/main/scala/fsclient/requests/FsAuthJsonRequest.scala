package fsclient.requests

import cats.effect.Effect
import fsclient.http.effect.HttpEffectClient
import fsclient.oauth.OAuthToken
import io.circe.Decoder
import org.http4s.Method
import org.http4s.Method.{DefaultMethodWithBody, SafeMethodWithBody}

trait FsAuthJsonRequest[Res] extends FsClientPlainRequest {
  final def runWith[F[_]: Effect](
    client: HttpEffectClient[F]
  )(implicit token: OAuthToken, responseDecoder: Decoder[Res]): F[HttpResponse[Res]] =
    client.fetchJson(this.toHttpRequest[F](client.consumer), OAuthEnabled(token))
}

object FsAuthJsonRequest {
  trait Get[Res] extends FsAuthJsonRequest[Res] {
    override val method: SafeMethodWithBody = Method.GET
  }

  trait Post[Res] extends FsAuthJsonRequest[Res] {
    override val method: DefaultMethodWithBody = Method.POST
  }
}
