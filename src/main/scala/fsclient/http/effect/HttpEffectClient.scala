package fsclient.http.effect

import cats.effect.{Effect, Resource}
import fsclient.config.AppConsumer
import fsclient.entities.{HttpResponse, OAuthInfo}
import fsclient.utils.HttpTypes.HttpPipe
import io.circe.Decoder
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.oauth1.Consumer

trait HttpEffectClient[F[_]] extends RequestF {

  def consumer: AppConsumer

  def run[A]: fs2.Stream[F, HttpResponse[A]] => F[HttpResponse[A]]

  private[http] def fetchJson[R](request: Request[F], oAuthInfo: OAuthInfo)(
    implicit
    effect: Effect[F],
    resource: Resource[F, Client[F]],
    consumer: Consumer,
    responseDecoder: Decoder[R]
  ): F[HttpResponse[R]] =
    resource.use { client =>
      run(jsonRequest[F, R](client)(request, oAuthInfo))
    }

  private[http] def fetchPlainText[R](request: Request[F], oAuthInfo: OAuthInfo)(
    implicit
    effect: Effect[F],
    resource: Resource[F, Client[F]],
    consumer: Consumer,
    responseDecoder: HttpPipe[F, String, R]
  ): F[HttpResponse[R]] =
    resource.use { client =>
      run(
        plainTextRequest[F, R](client)(request, oAuthInfo)
      )
    }
}
