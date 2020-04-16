package io.bartholomews.fsclient.client.effect

import cats.effect.{ConcurrentEffect, Resource}
import cats.implicits._
import fs2.Pipe
import io.bartholomews.fsclient.codecs.RawDecoder
import io.bartholomews.fsclient.config.FsClientConfig
import io.bartholomews.fsclient.entities._
import io.bartholomews.fsclient.utils.HttpTypes.HttpResponse
import org.http4s._
import org.http4s.client.Client

private[fsclient] trait HttpEffectClient[F[_], OAuth <: OAuthVersion] extends RequestF {

  def appConfig: FsClientConfig[OAuth]

  implicit def resource: Resource[F, Client[F]]

  private def execute[V <: OAuthVersion, A](
    signer: Signer[V]
  )(implicit f: ConcurrentEffect[F]): fs2.Stream[F, HttpResponse[V, A]] => F[HttpResponse[V, A]] =
    _.compile.last.flatMap(maybeResponse =>
      f.pure(maybeResponse.getOrElse(FsResponse(signer, EmptyResponseException())))
    )

  private[fsclient] def fetch[V <: OAuthVersion, Raw, Res](request: Request[F], signer: Signer[V])(
    implicit
    f: ConcurrentEffect[F],
    rawDecoder: RawDecoder[Raw],
    decode: Pipe[F, Raw, Res]
  ): F[FsResponse[V, HttpError, Res]] =
    resource.use { client =>
      execute(signer)(f)(signAndProcessRequest[F, V, Raw, Res](request, effectClient = this, client, signer))
    }
}
