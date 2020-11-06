package io.bartholomews.fsclient.client

import cats.effect.{ConcurrentEffect, ContextShift, Resource}
import cats.implicits._
import io.bartholomews.fsclient.codecs.{RawDecoder, ResDecoder}
import io.bartholomews.fsclient.config.{FsClientConfig, UserAgent}
import io.bartholomews.fsclient.entities._
import io.bartholomews.fsclient.entities.oauth._
import io.bartholomews.fsclient.entities.oauth.v2.OAuthV2AuthorizationFramework.ClientPassword
import io.bartholomews.fsclient.utils.HttpTypes.HttpResponse
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.oauth1.Consumer

import scala.concurrent.ExecutionContext

sealed trait FsClient[F[_], S <: Signer] extends RequestF {

  def appConfig: FsClientConfig[S]

  implicit def resource: Resource[F, Client[F]]

  private def execute[A](implicit f: ConcurrentEffect[F]): fs2.Stream[F, HttpResponse[A]] => F[HttpResponse[A]] =
    _.compile.last.flatMap(maybeResponse => f.pure(maybeResponse.getOrElse(FsResponse(EmptyResponseException()))))

  private[fsclient] def fetch[V <: OAuthVersion, Raw, Res](request: Request[F], signer: Signer)(implicit
    f: ConcurrentEffect[F],
    rawDecoder: RawDecoder[Raw],
    resDecoder: ResDecoder[Raw, Res]
  ): F[FsResponse[ErrorBody, Res]] =
    resource.use { client =>
      execute(f)(signAndProcessRequest[F, Raw, Res](request, client, signer))
    }
}

case class FClientNoAuth[F[_]: ConcurrentEffect](userAgent: UserAgent)(implicit
  val ec: ExecutionContext,
  val contextShift: ContextShift[F]
) extends FsClient[F, AuthDisabled.type] {
  final override val appConfig: FsClientConfig[AuthDisabled.type] = FsClientConfig.disabled(userAgent)
  implicit override val resource: Resource[F, Client[F]] = BlazeClientBuilder[F](ec).resource
}

abstract class BlazeClient[F[_]: ConcurrentEffect, S <: Signer]() extends FsClient[F, S] {
  def ec: ExecutionContext
  implicit override val resource: Resource[F, Client[F]] = BlazeClientBuilder[F](ec).resource
}

case class FsClientV1[F[_]: ConcurrentEffect, S <: Signer](appConfig: FsClientConfig[S])(implicit
  val ec: ExecutionContext,
  val contextShift: ContextShift[F]
) extends BlazeClient[F, S]

object FsClientV1 {
  def apply[F[_]: ConcurrentEffect](userAgent: UserAgent, signer: SignerV1)(implicit
    ec: ExecutionContext,
    contextShift: ContextShift[F]
  ): FsClientV1[F, SignerV1] = FsClientV1(FsClientConfig(userAgent, signer))

  def unsafeFromConfigBasic[F[_]: ConcurrentEffect](consumerNamespace: String)(implicit
    ec: ExecutionContext,
    contextShift: ContextShift[F]
  ): FsClientV1[F, SignerV1] = FsClientV1(FsClientConfig.v1.basic(consumerNamespace).orThrow)

  def basic[F[_]: ConcurrentEffect](userAgent: UserAgent, consumer: Consumer)(implicit
    ec: ExecutionContext,
    contextShift: ContextShift[F]
  ): FsClientV1[F, SignerV1] = FsClientV1(FsClientConfig.v1.basic(userAgent, consumer))
}

case class FsClientV2[F[_]: ConcurrentEffect, S <: SignerV2](
  appConfig: FsClientConfig[S],
  clientPassword: ClientPassword
)(implicit
  val ec: ExecutionContext,
  val contextShift: ContextShift[F]
) extends BlazeClient[F, S]

object FsClientV2 {
  def apply[F[_]: ConcurrentEffect](userAgent: UserAgent, clientPassword: ClientPassword, signer: NonRefreshableToken)(
    implicit
    ec: ExecutionContext,
    contextShift: ContextShift[F]
  ): FsClientV2[F, SignerV2] = new FsClientV2(FsClientConfig(userAgent, signer), clientPassword)
}
