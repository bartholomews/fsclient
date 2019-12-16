package fsclient.requests

import cats.effect.Effect
import fsclient.http.effect.HttpEffectClient
import fsclient.utils.HttpTypes.HttpPipe
import io.circe.Encoder
import org.http4s.Method.{DefaultMethodWithBody, SafeMethodWithBody}
import org.http4s.{EntityEncoder, Method}

trait FsSimplePlainTextRequestWithBody[Body, Res] extends FsClientRequestWithBody[Body] {
  final def runWith[F[_]: Effect](
    client: HttpEffectClient[F]
  )(implicit requestBodyEncoder: EntityEncoder[F, Body],
    responseDecoder: HttpPipe[F, String, Res]): F[HttpResponse[Res]] =
    client.fetchPlainText(this.toHttpRequest[F](client.consumer), OAuthDisabled)
}

object FsSimplePlainTextRequestWithBody {
  trait Get[Body, Res] extends FsSimplePlainTextRequestWithBody[Body, Res] {
    override val method: SafeMethodWithBody = Method.GET
  }
  trait Post[Body, Res] extends FsSimplePlainTextRequestWithBody[Body, Res] {
    override val method: DefaultMethodWithBody = Method.POST
  }
  trait PostJson[Body, Res] extends FsSimplePlainTextRequestWithBody[Body, Res] {
    override val method: DefaultMethodWithBody = Method.POST
    import org.http4s.circe._
    implicit def entityJsonEncoder[F[_]: Effect](implicit encoder: Encoder[Body]): EntityEncoder[F, Body] =
      jsonEncoderOf[F, Body]
  }
}
