package fsclient.http.effect

import cats.effect.{Effect, Resource}
import fsclient.config.OAuthConsumer
import fsclient.entities.{HttpResponse, OAuthAccessToken}
import io.circe.Decoder
import org.http4s.client.Client
import org.http4s.client.oauth1.Consumer
import org.http4s.{EntityEncoder, Header, Headers, Method, Request, Uri}

private[http] trait HttpEffectClient[F[_]] extends RequestF {

  private[http] def consumer: OAuthConsumer

  private[http] def run[A]: fs2.Stream[F, HttpResponse[A]] => F[HttpResponse[A]]

  val USER_AGENT = Headers {
    Header("User-Agent", consumer.userAgent)
  }
  //  import org.http4s.circe.CirceEntityEncoder._
  //  import org.http4s.circe._
  //
  //  import org.http4s.circe.CirceEntityEncoder
  //  import org.http4s.circe.CirceEntityDecoder._
  //  import org.http4s.circe.CirceEntityCodec._
  //  import org.http4s.circe.CirceInstances._
  //
  //  import io.circe.generic.auto._

  //  implicit val body = jsonEncoderOf[F, Json]

  //  implicit def circeJsonDecoder[A](implicit decoder: Decoder[A],
  //                                   applicative: Applicative[F]): EntityEncoder[F, A] =
  //    org.http4s.circe.jsonEncoderOf[F, A]

  private def request(uri: Uri): Request[F] = Request[F]()
    .withUri(uri)
    .withHeaders(USER_AGENT)

  //  private def POST[A](uri: Uri)
  //                     (implicit entityEncoder: EntityEncoder[F, Json]): Request[F] =
  //    request(uri).withMethod(Method.POST).withEntity[Json]("dsd".asJson)

  private[http] def fetchPlainTextWithBody[A, B](uri: Uri,
                                                 method: Method = Method.POST,
                                                 body: B,
                                                 accessToken: Option[OAuthAccessToken] = None)
                                                (implicit
                                                 effect: Effect[F],
                                                 consumer: Consumer,
                                                 resource: Resource[F, Client[F]],
                                                 bodyEntityEncoder: EntityEncoder[F, B],
                                                 plainTextToEntityPipe: HttpPipe[F, String, A]): F[HttpResponse[A]] =

    resource.use(client => run(
      plainTextRequest[F, A](client)(request(uri)
        .withMethod(method).withEntity(body), accessToken)
    ))

  private[http] def fetchPlainText[A](uri: Uri,
                                      method: Method = Method.GET,
                                      accessToken: Option[OAuthAccessToken] = None)
                                     (implicit
                                      effect: Effect[F],
                                      consumer: Consumer,
                                      resource: Resource[F, Client[F]],
                                      decoder: HttpPipe[F, String, A]): F[HttpResponse[A]] =

    resource.use(client => run(
      plainTextRequest[F, A](client)(request(uri)
        .withMethod(method), accessToken)
    ))

  private[http] def fetchJsonWithBody[A, B](uri: Uri,
                                            method: Method = Method.POST,
                                            body: Option[B] = None,
                                            accessToken: Option[OAuthAccessToken] = None)
                                           (implicit
                                            effect: Effect[F],
                                            consumer: Consumer,
                                            resource: Resource[F, Client[F]],
                                            bodyEntityEncoder: EntityEncoder[F, B],
                                            decode: Decoder[A]): F[HttpResponse[A]] =

    resource.use(client => run(
      jsonRequest(client)(request(uri)
        .withMethod(method)
        .withEntity(body.get), accessToken)
    ))

  private[http] def fetchJson[A](uri: Uri,
                                 method: Method = Method.GET,
                                 accessToken: Option[OAuthAccessToken] = None)
                                (implicit
                                 effect: Effect[F],
                                 consumer: Consumer,
                                 resource: Resource[F, Client[F]],
                                 decode: Decoder[A]): F[HttpResponse[A]] =

    resource.use(client => run(
      jsonRequest(client)(request(uri)
        .withMethod(method), accessToken)))
}
