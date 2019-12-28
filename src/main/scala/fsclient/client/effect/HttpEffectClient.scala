package fsclient.client.effect

import cats.effect.{Effect, Resource}
import fs2.Pipe
import fsclient.codecs.RawDecoder
import fsclient.config.AppConsumer
import fsclient.entities.{HttpResponse, OAuthInfo}
import org.http4s._
import org.http4s.client.Client

trait HttpEffectClient[F[_]] extends RequestF {

  def consumer: AppConsumer

  implicit def resource: Resource[F, Client[F]]

  def run[A]: fs2.Stream[F, HttpResponse[A]] => F[HttpResponse[A]]

  private[fsclient] def fetch[Raw, Res](request: Request[F], oAuthInfo: OAuthInfo)(
    implicit
    effect: Effect[F],
    rawDecoder: RawDecoder[Raw],
    decode: Pipe[F, Raw, Res]
  ): F[HttpResponse[Res]] =
    resource.use { client =>
      run(processHttpRequest[F, Raw, Res](client)(request, oAuthInfo, rawDecoder, decode))
    }
}
