package fsclient.http.effect

import cats.effect.{Effect, Resource}
import fsclient.config.AppConsumer
import fsclient.requests.{HttpResponse, OAuthInfo}
import fsclient.utils.HttpTypes.HttpPipe
import io.circe.Decoder
import org.http4s._
import org.http4s.client.Client

trait HttpEffectClient[F[_]] extends RequestF {

  def consumer: AppConsumer

  implicit def resource: Resource[F, Client[F]]

  def run[A]: fs2.Stream[F, HttpResponse[A]] => F[HttpResponse[A]]

  private[fsclient] def fetchJson[R](request: Request[F], oAuthInfo: OAuthInfo)(
    implicit
    effect: Effect[F],
    responseDecoder: Decoder[R]
  ): F[HttpResponse[R]] =
    resource.use { client =>
      run(jsonRequest[F, R](client)(request, oAuthInfo))
    }

  private[fsclient] def fetchPlainText[R](request: Request[F], oAuthInfo: OAuthInfo)(
    implicit
    effect: Effect[F],
    responseDecoder: HttpPipe[F, String, R]
  ): F[HttpResponse[R]] =
    resource.use { client =>
      run(
        plainTextRequest[F, R](client)(request, oAuthInfo)
      )
    }
}
