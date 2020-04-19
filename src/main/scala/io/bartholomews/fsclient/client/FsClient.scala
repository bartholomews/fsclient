package io.bartholomews.fsclient.client

import cats.effect.{ConcurrentEffect, ContextShift, Resource}
import cats.implicits._
import fs2.Pipe
import io.bartholomews.fsclient.codecs.RawDecoder
import io.bartholomews.fsclient.config.{FsClientConfig, UserAgent}
import io.bartholomews.fsclient.entities._
import io.bartholomews.fsclient.entities.oauth.OAuthVersion.OAuthV1
import io.bartholomews.fsclient.entities.oauth.v2.OAuthV2AuthorizationFramework.ClientPassword
import io.bartholomews.fsclient.entities.oauth.{NonRefreshableToken, OAuthVersion, Signer}
import io.bartholomews.fsclient.utils.HttpTypes.HttpResponse
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.oauth1.Consumer

import scala.concurrent.ExecutionContext

sealed trait FsClient[F[_]] extends RequestF {

  def appConfig: FsClientConfig

  implicit def resource: Resource[F, Client[F]]

  private def execute[A](implicit f: ConcurrentEffect[F]): fs2.Stream[F, HttpResponse[A]] => F[HttpResponse[A]] =
    _.compile.last.flatMap(maybeResponse => f.pure(maybeResponse.getOrElse(FsResponse(EmptyResponseException()))))

  private[fsclient] def fetch[V <: OAuthVersion, Raw, Res](request: Request[F], signer: Signer)(
    implicit
    f: ConcurrentEffect[F],
    rawDecoder: RawDecoder[Raw],
    decode: Pipe[F, Raw, Res]
  ): F[FsResponse[HttpError, Res]] =
    resource.use { client =>
      execute(f)(signAndProcessRequest[F, Raw, Res](request, client, signer))
    }
}

case class FClientNoAuth[F[_]: ConcurrentEffect](userAgent: UserAgent)(
  implicit val ec: ExecutionContext,
  val contextShift: ContextShift[F]
) extends FsClient[F] {
  final override val appConfig: FsClientConfig = FsClientConfig.disabled(userAgent)
  implicit override val resource: Resource[F, Client[F]] = BlazeClientBuilder[F](ec).resource
}

abstract class BlazeClient[F[_]: ConcurrentEffect]() extends FsClient[F] {
  def ec: ExecutionContext
  implicit override val resource: Resource[F, Client[F]] = BlazeClientBuilder[F](ec).resource
}

case class FsClientV1[F[_]: ConcurrentEffect](appConfig: FsClientConfig)(
  implicit val ec: ExecutionContext,
  val contextShift: ContextShift[F]
) extends BlazeClient[F]

object FsClientV1 {
  def apply[F[_]: ConcurrentEffect, V <: OAuthV1](userAgent: UserAgent, signer: Signer)(
    implicit ec: ExecutionContext,
    contextShift: ContextShift[F]
  ): FsClientV1[F] = FsClientV1(FsClientConfig(userAgent, signer))

  def unsafeFromConfigBasic[F[_]: ConcurrentEffect](
    implicit ec: ExecutionContext,
    contextShift: ContextShift[F]
  ): FsClientV1[F] = FsClientV1(FsClientConfig.v1.basic().orThrow)

  def basic[F[_]: ConcurrentEffect](userAgent: UserAgent, consumer: Consumer)(
    implicit ec: ExecutionContext,
    contextShift: ContextShift[F]
  ): FsClientV1[F] = FsClientV1(FsClientConfig.v1.basic(userAgent, consumer))
}

case class FsClientV2[F[_]: ConcurrentEffect](appConfig: FsClientConfig, clientPassword: ClientPassword)(
  implicit val ec: ExecutionContext,
  val contextShift: ContextShift[F]
) extends BlazeClient[F]

object FsClientV2 {
  def apply[F[_]: ConcurrentEffect](userAgent: UserAgent, clientPassword: ClientPassword, signer: NonRefreshableToken)(
    implicit ec: ExecutionContext,
    contextShift: ContextShift[F]
  ): FsClientV2[F] = new FsClientV2(FsClientConfig(userAgent, signer), clientPassword)
}
