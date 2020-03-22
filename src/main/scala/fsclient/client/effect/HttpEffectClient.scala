package fsclient.client.effect

import cats.effect.{Effect, Resource}
import cats.implicits._
import fs2.Pipe
import fsclient.codecs.RawDecoder
import fsclient.config.FsClientConfig
import fsclient.entities._
import org.http4s._
import org.http4s.client.Client

trait HttpEffectClient[F[_], OAuth <: OAuthInfo] extends RequestF {

  def appConfig: FsClientConfig[OAuth]

  implicit def resource: Resource[F, Client[F]]

  private def execute[A](implicit f: Effect[F]): fs2.Stream[F, HttpResponse[A]] => F[HttpResponse[A]] =
    _.compile.last.flatMap(
      _.toRight(ResponseError.apply(EmptyResponseException, Status.UnprocessableEntity)).fold(
        error => f.pure(HttpResponse(Headers.empty, Left(error))),
        value => f.pure(value)
      )
    )

  private[fsclient] def fetch[Raw, Res](request: Request[F], authInfo: OAuthInfo)(
    implicit
    f: Effect[F],
    rawDecoder: RawDecoder[Raw],
    decode: Pipe[F, Raw, Res]
  ): F[HttpResponse[Res]] =
    resource.use { client =>
      execute(f)(processHttpRequest[F, Raw, Res](client)(request, authInfo, rawDecoder, decode))
    }
}
