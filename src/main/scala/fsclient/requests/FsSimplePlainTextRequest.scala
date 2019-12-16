package fsclient.requests

import cats.effect.Effect
import fsclient.http.effect.HttpEffectClient
import fsclient.utils.HttpTypes.HttpPipe
import org.http4s.Method
import org.http4s.Method.{DefaultMethodWithBody, SafeMethodWithBody}

trait FsSimplePlainTextRequest[Res] extends FsClientPlainRequest {
  final def runWith[F[_]: Effect](
    client: HttpEffectClient[F]
  )(implicit responseDecoder: HttpPipe[F, String, Res]): F[HttpResponse[Res]] =
    client.fetchPlainText(this.toHttpRequest[F](client.consumer), OAuthDisabled)
}

object FsSimplePlainTextRequest {

  trait Get[Res] extends FsSimplePlainTextRequest[Res] {
    override val method: SafeMethodWithBody = Method.GET
  }

  trait Post[Res] extends FsSimplePlainTextRequest[Res] {
    override val method: DefaultMethodWithBody = Method.POST
  }
}
