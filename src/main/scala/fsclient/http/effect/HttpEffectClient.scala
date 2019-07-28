package fsclient.http.effect

import cats.effect.{Effect, Resource}
import fsclient.config.OAuthConsumer
import fsclient.entities.{HttpResponse, OAuthToken}
import io.circe.{Decoder, Encoder}
import org.http4s.circe.jsonEncoderOf
import org.http4s.client.Client
import org.http4s.client.oauth1.Consumer
import org.http4s.{Header, Headers, Method, Request, Uri}

private[http] trait HttpEffectClient[F[_]] extends RequestF {

  def consumer: OAuthConsumer

  def run[A]: fs2.Stream[F, HttpResponse[A]] => F[HttpResponse[A]]

  private val USER_AGENT = Headers.of {
    Header("User-Agent", consumer.userAgent)
  }

  private def createRequest(uri: Uri): Request[F] = Request[F]()
    .withUri(uri)
    .withHeaders(USER_AGENT)

  def callPlainText[A, B](uri: Uri,
                          method: Method,
                          body: B,
                          oAuthToken: Option[OAuthToken])
                         (implicit
                          effect: Effect[F],
                          consumer: Consumer,
                          resource: Resource[F, Client[F]],
                          requestBodyEncoder: Encoder[B],
                          responseDecoder: HttpPipe[F, String, A]): F[HttpResponse[A]] = {

    val request = createRequest(uri).withMethod(method).withEntity(body)(jsonEncoderOf[F, B])
    resource.use(client => run(plainTextRequest[F, A](client)(request, oAuthToken)))
  }

  def callPlainText[A](uri: Uri,
                       method: Method = Method.GET,
                       oAuthToken: Option[OAuthToken])
                      (implicit
                       effect: Effect[F],
                       consumer: Consumer,
                       resource: Resource[F, Client[F]],
                       responseDecoder: HttpPipe[F, String, A]): F[HttpResponse[A]] = {

    val request = createRequest(uri).withMethod(method)
    resource.use(client => run(plainTextRequest[F, A](client)(request, oAuthToken)))
  }

  def callJson[A, B](uri: Uri,
                     method: Method,
                     body: B,
                     oAuthToken: Option[OAuthToken])
                    (implicit
                     effect: Effect[F],
                     consumer: Consumer,
                     resource: Resource[F, Client[F]],
                     requestBodyEncoder: Encoder[B],
                     responseDecoder: Decoder[A]): F[HttpResponse[A]] = {

    val request = createRequest(uri).withMethod(method).withEntity(body)(jsonEncoderOf[F, B])
    resource.use(client => run(jsonRequest(client)(request, oAuthToken)))
  }

  def callJson[A](uri: Uri,
                  method: Method = Method.GET,
                  oAuthToken: Option[OAuthToken])
                 (implicit
                  effect: Effect[F],
                  consumer: Consumer,
                  resource: Resource[F, Client[F]],
                  responseDecoder: Decoder[A]): F[HttpResponse[A]] = {

    val request = createRequest(uri).withMethod(method)
    resource.use(client => run(jsonRequest(client)(request, oAuthToken)))
  }
}
