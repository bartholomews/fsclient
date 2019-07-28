package fsclient.http.client

import cats.data.EitherT
import cats.effect.IO
import fsclient.config.OAuthConsumer
import fsclient.entities._

import scala.concurrent.ExecutionContext

class IOSimpleClient(override val consumer: OAuthConsumer)(implicit val ec: ExecutionContext) extends IOClient(consumer, None) {

  def toOAuthClient(request: OAuthEndpoint[AccessToken])
                   (implicit responseDecoder: IOHttpPipe[String, AccessToken]): IO[Either[ResponseError, IOAuthClient]] = {

    def accessTokenRequest: IO[HttpResponse[AccessToken]] =
      callPlainText(request.uri, request.method, Some(request.requestToken))

    (for {
      accessToken <- EitherT(accessTokenRequest.map(_.entity))
      res <- EitherT.right[ResponseError](IO.pure(new IOAuthClient(consumer, accessToken)))
    } yield res).value
  }
}
