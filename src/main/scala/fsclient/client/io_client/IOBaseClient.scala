package fsclient.client.io_client

import cats.effect.{ContextShift, IO, Resource}
import fsclient.client.effect.HttpEffectClient
import fsclient.config.AppConsumer
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext

abstract private[client] class IOBaseClient(override val consumer: AppConsumer)(implicit ec: ExecutionContext)
    extends HttpEffectClient[IO] {

  implicit val ioContextShift: ContextShift[IO] =
    IO.contextShift(ec)

  // FIXME: Can/Should be moved to HttpEffectClient ?
  implicit override val resource: Resource[IO, Client[IO]] =
    BlazeClientBuilder[IO](ec).resource
}
