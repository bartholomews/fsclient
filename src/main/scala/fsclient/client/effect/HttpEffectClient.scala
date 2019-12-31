package fsclient.client.effect

import cats.effect.{Effect, Resource}
import cats.implicits._
import fs2.Pipe
import fsclient.codecs.RawDecoder
import fsclient.config.AppConsumer
import fsclient.entities.{EmptyResponseException, HttpResponse, OAuthInfo, ResponseError}
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.oauth1.Consumer

trait HttpEffectClient[F[_]] extends RequestF {

  def consumer: AppConsumer

  implicit def resource: Resource[F, Client[F]]

  implicit val httpConsumer: Consumer =
    Consumer(consumer.key, consumer.secret)

  private def run[A](implicit f: Effect[F]): fs2.Stream[F, HttpResponse[A]] => F[HttpResponse[A]] =
    _.compile.last.flatMap(
      _.toRight(ResponseError.apply(EmptyResponseException, Status.UnprocessableEntity)).fold(
        error => f.pure(HttpResponse(Headers.empty, Left(error))),
        value => f.pure(value)
      )
    )

  private[fsclient] def fetch[Raw, Res](request: Request[F], oAuthInfo: OAuthInfo)(
    implicit
    f: Effect[F],
    rawDecoder: RawDecoder[Raw],
    decode: Pipe[F, Raw, Res]
  ): F[HttpResponse[Res]] =
    resource.use { client =>
      run(f)(processHttpRequest[F, Raw, Res](client)(request, oAuthInfo, rawDecoder, decode))
    }
}
