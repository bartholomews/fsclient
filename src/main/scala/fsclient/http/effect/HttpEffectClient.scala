package fsclient.http.effect

import cats.effect.{Effect, Resource}
import fsclient.config.OAuthConsumer
import fsclient.entities.{FsClientPlainRequest, FsClientRequestWithBody, HttpResponse, OAuthToken}
import io.circe.{Decoder, Encoder}
import org.http4s.circe.jsonEncoderOf
import org.http4s.client.Client
import org.http4s.client.oauth1.Consumer
import org.http4s.{Header, Headers, Request, Uri}

private[http] trait HttpEffectClient[F[_]] extends RequestF {

  def consumer: OAuthConsumer

  def run[R]: fs2.Stream[F, HttpResponse[R]] => F[HttpResponse[R]]

  private val USER_AGENT = Headers.of {
    Header("User-Agent", consumer.userAgent)
  }

  private def createRequest(uri: Uri): Request[F] =
    Request[F]()
      .withUri(uri)
      .withHeaders(USER_AGENT)

  object effect {
    def fetchPlainText[B, R](clientRequest: FsClientRequestWithBody[B, R], oAuthToken: Option[OAuthToken])(
      implicit
      effect: Effect[F],
      consumer: Consumer,
      resource: Resource[F, Client[F]],
      requestBodyEncoder: Encoder[B],
      responseDecoder: HttpPipe[F, String, R]
    ): F[HttpResponse[R]] = {

      val request = createRequest(clientRequest.uri)
        .withMethod(clientRequest.method)
        .withEntity(clientRequest.body)(jsonEncoderOf[F, B])

      resource.use(client => run(plainTextRequest[F, R](client)(request, oAuthToken)))
    }

    def getPlainText[R](clientRequest: FsClientPlainRequest[R], oAuthToken: Option[OAuthToken])(
      implicit
      effect: Effect[F],
      consumer: Consumer,
      resource: Resource[F, Client[F]],
      responseDecoder: HttpPipe[F, String, R]
    ): F[HttpResponse[R]] = {

      val request = createRequest(clientRequest.uri).withMethod(clientRequest.method)
      resource.use(client => run(plainTextRequest[F, R](client)(request, oAuthToken)))
    }

    def fetchJson[B, R](clientRequest: FsClientRequestWithBody[B, R], oAuthToken: Option[OAuthToken])(
      implicit
      effect: Effect[F],
      consumer: Consumer,
      resource: Resource[F, Client[F]],
      requestBodyEncoder: Encoder[B],
      responseDecoder: Decoder[R]
    ): F[HttpResponse[R]] = {

      val request = createRequest(clientRequest.uri)
        .withMethod(clientRequest.method)
        .withEntity(clientRequest.body)(jsonEncoderOf[F, B])

      resource.use(client => run(jsonRequest(client)(request, oAuthToken)))
    }

    def getJson[R](clientRequest: FsClientPlainRequest[R], oAuthToken: Option[OAuthToken])(
      implicit
      effect: Effect[F],
      consumer: Consumer,
      resource: Resource[F, Client[F]],
      responseDecoder: Decoder[R]
    ): F[HttpResponse[R]] = {

      val request = createRequest(clientRequest.uri).withMethod(clientRequest.method)
      resource.use(client => run(jsonRequest(client)(request, oAuthToken)))
    }
  }
}
