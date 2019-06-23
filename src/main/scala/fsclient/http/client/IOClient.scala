package fsclient.http.client

import cats.effect.{ContextShift, IO, Resource}
import fsclient.config.OAuthConsumer
import fsclient.entities._
import fsclient.http.effect.HttpEffectClient
import io.circe.{Decoder, Encoder}
import org.http4s.Headers
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.oauth1.Consumer

import scala.concurrent.ExecutionContext

class IOClient(override val consumer: OAuthConsumer,
               private[http] val accessToken: Option[OAuthAccessToken] = None)
              (implicit ec: ExecutionContext) extends HttpEffectClient[IO] {

  type IOHttpPipe[A, B] = HttpPipe[IO, A, B]

  private[http] implicit val ioContextShift: ContextShift[IO] = IO.contextShift(ec)
  private[http] implicit val httpConsumer: Consumer = Consumer(this.consumer.key, this.consumer.secret)
  private[http] implicit val resource: Resource[IO, Client[IO]] = BlazeClientBuilder[IO](ec).resource

  override private[http] def run[A]: fs2.Stream[IO, HttpResponse[A]] => IO[HttpResponse[A]] = _
    .compile
    .last
    .flatMap(_.toRight(ResponseError.apply(GenericResponseError)).fold(
      error => IO.pure(HttpResponse(Headers.empty, Left(error))),
      value => IO.pure(value)
    ))

  def getAndDecodeJsonAs[T](endpoint: HttpEndpoint[T, GET])(implicit responseDecoder: Decoder[T]): IOResponse[T] =
    callJson(endpoint.uri, endpoint.method.value, accessToken)

  def getAndDecodePlainTextAs[T](endpoint: HttpEndpoint[T, GET])
                                (implicit responseDecoder: IOHttpPipe[String, T]): IOResponse[T] =
    callPlainText(endpoint.uri, endpoint.method.value, accessToken)

  def postAndDecodeJsonAs[T, B](endpoint: HttpEndpoint[T, POST], body: B)
                               (implicit
                                requestBodyEncoder: Encoder[B],
                                responseDecoder: Decoder[T]): IOResponse[T] =
    callJson(endpoint.uri, endpoint.method.value, body, accessToken)

  def postAndDecodePlainTextAs[A, B](endpoint: HttpEndpoint[A, POST], body: B)
                                    (implicit
                                     requestBodyEncoder: Encoder[B],
                                     responseDecoder: HttpPipe[IO, String, A]): IOResponse[A] =
    callPlainText(endpoint.uri, endpoint.method.value, body, accessToken)
}
