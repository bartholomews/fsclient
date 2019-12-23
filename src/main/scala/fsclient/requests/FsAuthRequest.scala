package fsclient.requests

import cats.effect.Effect
import fs2.Pipe
import fsclient.http.client.base.RawDecoder
import fsclient.http.effect.HttpEffectClient
import fsclient.oauth.OAuthToken
import org.http4s.Method
import org.http4s.Method.{DefaultMethodWithBody, SafeMethodWithBody}

trait FsAuthRequest[Raw, Res] extends FsClientPlainRequest {
  final def runWith[F[_]: Effect](
    client: HttpEffectClient[F]
  )(implicit token: OAuthToken, rawDecoder: RawDecoder[Raw], decode: Pipe[F, Raw, Res]): F[HttpResponse[Res]] =
    client.fetch(this.toHttpRequest[F](client.consumer), OAuthEnabled(token))
}

object FsAuthRequest {
  trait Get[Raw, Res] extends FsAuthRequest[Raw, Res] {
    override val method: SafeMethodWithBody = Method.GET
  }
  trait Post[Raw, Res] extends FsAuthRequest[Raw, Res] {
    override val method: DefaultMethodWithBody = Method.POST
  }
}
