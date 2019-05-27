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

  private val USER_AGENT = Headers.of {
    Header("User-Agent", consumer.userAgent)
  }

  private def createRequest(uri: Uri): Request[F] = Request[F]()
    .withUri(uri)
    .withHeaders(USER_AGENT)

  private[http] def fetchPlainText[A, B](uri: Uri,
                                         method: Method,
                                         body: B,
                                         accessToken: Option[OAuthAccessToken])
                                        (implicit
                                         effect: Effect[F],
                                         consumer: Consumer,
                                         resource: Resource[F, Client[F]],
                                         bodyEntityEncoder: EntityEncoder[F, B],
                                         plainTextToEntityPipe: HttpPipe[F, String, A]): F[HttpResponse[A]] = {

    val request = createRequest(uri).withMethod(method).withEntity(body)
    resource.use(client => run(plainTextRequest[F, A](client)(request, accessToken)))
  }

  private[http] def fetchPlainText[A](uri: Uri,
                                      method: Method = Method.GET,
                                      accessToken: Option[OAuthAccessToken])
                                     (implicit
                                      effect: Effect[F],
                                      consumer: Consumer,
                                      resource: Resource[F, Client[F]],
                                      decoder: HttpPipe[F, String, A]): F[HttpResponse[A]] = {

    val request = createRequest(uri).withMethod(method)
    resource.use(client => run(plainTextRequest[F, A](client)(request, accessToken)))
  }

  private[http] def fetchJson[A, B](uri: Uri,
                                    method: Method,
                                    body: B,
                                    accessToken: Option[OAuthAccessToken])
                                   (implicit
                                    effect: Effect[F],
                                    consumer: Consumer,
                                    resource: Resource[F, Client[F]],
                                    bodyEntityEncoder: EntityEncoder[F, B],
                                    decode: Decoder[A]): F[HttpResponse[A]] = {

    val request = createRequest(uri).withMethod(method).withEntity(body)
    resource.use(client => run(jsonRequest(client)(request, accessToken)))
  }

  private[http] def fetchJson[A](uri: Uri,
                                 method: Method = Method.GET,
                                 accessToken: Option[OAuthAccessToken])
                                (implicit
                                 effect: Effect[F],
                                 consumer: Consumer,
                                 resource: Resource[F, Client[F]],
                                 decode: Decoder[A]): F[HttpResponse[A]] = {

    val request = createRequest(uri).withMethod(method)
    resource.use(client => run(jsonRequest(client)(request, accessToken)))
  }
}
