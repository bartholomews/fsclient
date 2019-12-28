package fsclient.client.io_client

import cats.effect.{ContextShift, IO, Resource}
import fsclient.client.effect.HttpEffectClient
import fsclient.config.AppConsumer
import fsclient.entities.{EmptyResponseException, HttpResponse, ResponseError}
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.oauth1.Consumer
import org.http4s.{Headers, Status}

import scala.concurrent.ExecutionContext

abstract private[client] class IOBaseClient(override val consumer: AppConsumer)(implicit ec: ExecutionContext)
    extends HttpEffectClient[IO] {

  implicit val ioContextShift: ContextShift[IO] =
    IO.contextShift(ec)

  implicit override val resource: Resource[IO, Client[IO]] =
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
