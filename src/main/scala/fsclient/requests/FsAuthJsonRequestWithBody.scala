package fsclient.requests

import cats.effect.Effect
import fsclient.http.effect.HttpEffectClient
import fsclient.oauth.OAuthToken
import io.circe.{Decoder, Encoder}
import org.http4s.{EntityEncoder, Method}
import org.http4s.Method.{DefaultMethodWithBody, SafeMethodWithBody}

trait FsAuthJsonRequestWithBody[Body, Res] extends FsClientRequestWithBody[Body] {
  final def runWith[F[_]: Effect](
    client: HttpEffectClient[F]
  )(implicit token: OAuthToken,
    requestBodyEncoder: EntityEncoder[F, Body],
    responseDecoder: Decoder[Res]): F[HttpResponse[Res]] =
    client.fetchJson(this.toHttpRequest[F](client.consumer), OAuthEnabled(token))
}

object FsAuthJsonRequestWithBody {
  trait Get[Body, Res] extends FsAuthJsonRequestWithBody[Body, Res] {
    override val method: SafeMethodWithBody = Method.GET
  }
  trait Post[Body, Res] extends FsAuthJsonRequestWithBody[Body, Res] {
    override val method: DefaultMethodWithBody = Method.POST
  }
  trait PostJson[Body, Res] extends FsAuthJsonRequestWithBody[Body, Res] {
    override val method: DefaultMethodWithBody = Method.POST
    import org.http4s.circe._
    implicit def entityJsonEncoder[F[_]: Effect](implicit encoder: Encoder[Body]): EntityEncoder[F, Body] =
      jsonEncoderOf[F, Body]
  }
}
