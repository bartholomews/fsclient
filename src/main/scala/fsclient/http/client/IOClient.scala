package fsclient.http.client

import cats.effect.{ContextShift, IO, Resource}
import fsclient.config.OAuthConsumer
import fsclient.entities.{AccessToken, GenericResponseError, HttpEndpoint, HttpResponse, ResponseError}
import fsclient.http.effect.HttpEffectClient
import io.circe.{Decoder, Encoder}
import org.http4s.Headers
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.oauth1.Consumer

import scala.concurrent.ExecutionContext

private[http] class IOClient(override val consumer: OAuthConsumer, accessToken: Option[AccessToken])
                            (implicit ec: ExecutionContext) extends HttpEffectClient[IO] {
  type IOHttpPipe[A, B] = HttpPipe[IO, A, B]

  private[http] implicit val ioContextShift: ContextShift[IO] = IO.contextShift(ec)
  private[http] implicit val resource: Resource[IO, Client[IO]] = BlazeClientBuilder[IO](ec).resource

  override final def run[A]: fs2.Stream[IO, HttpResponse[A]] => IO[HttpResponse[A]] = _
    .compile
    .last
    .flatMap(_.toRight(ResponseError.apply(GenericResponseError)).fold(
      error => IO.pure(HttpResponse(Headers.empty, Left(error))),
      value => IO.pure(value)
    ))

  private[http] implicit val httpConsumer: Consumer = Consumer(consumer.key, consumer.secret)

  final def fetchJson[T](endpoint: HttpEndpoint[T])
                  (implicit responseDecoder: Decoder[T]): IOResponse[T] =
    callJson(endpoint.uri, endpoint.method, accessToken)

  final def fetchPlainText[T](endpoint: HttpEndpoint[T])
                       (implicit responseDecoder: IOHttpPipe[String, T]): IOResponse[T] =
    callPlainText(endpoint.uri, endpoint.method, accessToken)

  final def fetchJson[T, B](endpoint: HttpEndpoint[T], body: B)
                           (implicit requestBodyEncoder: Encoder[B], responseDecoder: Decoder[T]): IOResponse[T] =
    callJson(endpoint.uri, endpoint.method, body, accessToken)

  final def fetchPlainText[A, B](endpoint: HttpEndpoint[A], body: B)
                                (implicit requestBodyEncoder: Encoder[B], responseDecoder: HttpPipe[IO, String, A]): IOResponse[A] =
    callPlainText(endpoint.uri, endpoint.method, body, accessToken)
}