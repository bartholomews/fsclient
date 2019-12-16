package fsclient.requests

import cats.effect.Effect
import fsclient.http.effect.HttpEffectClient
import fsclient.oauth.OAuthToken
import fsclient.utils.HttpTypes.HttpPipe
import org.http4s.Method
import org.http4s.Method.{DefaultMethodWithBody, SafeMethodWithBody}

trait FsAuthPlainTextRequest[Res] extends FsClientPlainRequest {
  final def runWith[F[_]: Effect](
    client: HttpEffectClient[F]
  )(implicit token: OAuthToken, responseDecoder: HttpPipe[F, String, Res]): F[HttpResponse[Res]] =
    client.fetchPlainText(this.toHttpRequest[F](client.consumer), OAuthEnabled(token))
}

object FsAuthPlainTextRequest {

  trait Get[Res] extends FsAuthPlainTextRequest[Res] {
    override val method: SafeMethodWithBody = Method.GET
  }

  trait Post[Res] extends FsAuthPlainTextRequest[Res] {
    override val method: DefaultMethodWithBody = Method.POST
  }
}
