package io.bartholomews.fsclient.client

import cats.effect.{ConcurrentEffect, ContextShift, Resource}
import io.bartholomews.fsclient.client.effect.HttpEffectClient
import io.bartholomews.fsclient.config.{FsClientConfig, UserAgent}
import io.bartholomews.fsclient.entities.oauth.OAuthVersion.{OAuthV1, OAuthV2}
import io.bartholomews.fsclient.entities.oauth.{NonRefreshableToken, OAuthVersion, Signer}
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.oauth1.Consumer

import scala.concurrent.ExecutionContext

private[fsclient] sealed trait FsClient[F[_], V <: OAuthVersion] extends HttpEffectClient[F, V] {
  implicit def contextShift: ContextShift[F]
  implicit def resource: Resource[F, Client[F]]
}

case class FClientNoAuth[F[_]: ConcurrentEffect](userAgent: UserAgent)(
  implicit ec: ExecutionContext,
  val contextShift: ContextShift[F]
) extends FsClient[F, Nothing] {
  final override val appConfig: FsClientConfig[Nothing] = FsClientConfig.disabled(userAgent)
  implicit override def resource: Resource[F, Client[F]] = BlazeClientBuilder[F](ec).resource
}

case class FsClientV1[F[_]: ConcurrentEffect](appConfig: FsClientConfig[OAuthV1])(
  implicit ec: ExecutionContext,
  val contextShift: ContextShift[F]
) extends FsClient[F, OAuthV1] {
  implicit override def resource: Resource[F, Client[F]] = BlazeClientBuilder[F](ec).resource
}
object FsClientV1 {
  def apply[F[_]: ConcurrentEffect, V <: OAuthV1](userAgent: UserAgent, signer: Signer[OAuthV1])(
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

case class FsClientV2[F[_]: ConcurrentEffect](appConfig: FsClientConfig[OAuthV2])(
  implicit ec: ExecutionContext,
  val contextShift: ContextShift[F]
) extends FsClient[F, OAuthV2] {
  implicit override def resource: Resource[F, Client[F]] = BlazeClientBuilder[F](ec).resource
}

object FsClientV2 {
  def apply[F[_]: ConcurrentEffect](userAgent: UserAgent, signer: NonRefreshableToken)(
    implicit ec: ExecutionContext,
    contextShift: ContextShift[F]
  ): FsClientV2[F] = FsClientV2(FsClientConfig(userAgent, signer))
}
