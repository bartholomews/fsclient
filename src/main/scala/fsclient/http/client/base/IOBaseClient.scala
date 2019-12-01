package fsclient.http.client.base

import cats.effect.{ContextShift, IO, Resource}
import fsclient.config.AppConsumer
import fsclient.entities._
import fsclient.http.effect.HttpEffectClient
import fsclient.utils.HttpTypes.IOHttpPipe
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.oauth1.Consumer
import org.http4s.{Headers, Status}

import scala.concurrent.ExecutionContext

abstract private[http] class IOBaseClient(override val consumer: AppConsumer)(implicit ec: ExecutionContext)
    extends HttpEffectClient[IO] {

  implicit val stringDecoderPipe: IOHttpPipe[String, String] = _.map(identity)

  implicit val ioContextShift: ContextShift[IO] =
    IO.contextShift(ec)

  implicit val resource: Resource[IO, Client[IO]] =
    BlazeClientBuilder[IO](ec).resource

  final override def run[A]: fs2.Stream[IO, HttpResponse[A]] => IO[HttpResponse[A]] =
    _.compile.last
      .flatMap(
        _.toRight(ResponseError.apply(EmptyResponseException, Status.InternalServerError)).fold(
          error => IO.pure(HttpResponse(Headers.empty, Left(error))),
          value => IO.pure(value)
        )
      )

  implicit val httpConsumer: Consumer =
    Consumer(consumer.key, consumer.secret)
}
