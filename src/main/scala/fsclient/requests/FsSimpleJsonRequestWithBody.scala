package fsclient.requests

import cats.effect.Effect
import fsclient.http.effect.HttpEffectClient
import io.circe.{Decoder, Encoder}
import org.http4s.Method.{DefaultMethodWithBody, SafeMethodWithBody}
import org.http4s.{EntityEncoder, Method}

trait FsSimpleJsonRequestWithBody[Body, Res] extends FsClientRequestWithBody[Body] {
  final def runWith[F[_]: Effect](
    client: HttpEffectClient[F]
  )(implicit requestBodyEncoder: EntityEncoder[F, Body], responseDecoder: Decoder[Res]): F[HttpResponse[Res]] =
    client.fetchJson(this.toHttpRequest[F](client.consumer), OAuthDisabled)
}

object FsSimpleJsonRequestWithBody {
  trait Get[Body, Res] extends FsSimpleJsonRequestWithBody[Body, Res] {
    override val method: SafeMethodWithBody = Method.GET
  }
  trait Post[Body, Res] extends FsSimpleJsonRequestWithBody[Body, Res] {
    override val method: DefaultMethodWithBody = Method.POST
  }
  trait PostJson[Body, Res] extends FsSimpleJsonRequestWithBody[Body, Res] {
    override val method: DefaultMethodWithBody = Method.POST
    import org.http4s.circe._
    implicit def entityJsonEncoder[F[_]: Effect](implicit encoder: Encoder[Body]): EntityEncoder[F, Body] =
      jsonEncoderOf[F, Body]
  }
}
