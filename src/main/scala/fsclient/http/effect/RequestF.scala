package fsclient.http.effect

import cats.effect.Effect
import fs2.{Pipe, Stream}
import fsclient.entities.{HttpResponse, OAuthToken}
import fsclient.utils.{HttpTypes, Logger}
import io.circe.Decoder
import org.http4s.client.Client
import org.http4s.client.oauth1.Consumer
import org.http4s.{Request, Response, Status}

private[http] trait RequestF extends HttpPipes with HttpTypes with OAuthSignature with Logger {

  def jsonRequest[F[_]: Effect, A](client: Client[F])(request: Request[F], oAuthToken: Option[OAuthToken] = None)(
    implicit
    consumer: Consumer,
    decoder: Decoder[A]
  ): Stream[F, HttpResponse[A]] =
    processHttpRequest(client)(request, oAuthToken, decodeJsonResponse, doNothing)

  def plainTextRequest[F[_]: Effect, A](client: Client[F])(
    request: Request[F],
    oAuthToken: Option[OAuthToken] = None
  )(implicit consumer: Consumer, decoder: HttpPipe[F, String, A]): Stream[F, HttpResponse[A]] =
    processHttpRequest(client)(request, oAuthToken, decodeTextPlainResponse, decoder)

  private def processHttpRequest[F[_]: Effect, A](client: Client[F])(
    request: Request[F],
    oAuthToken: Option[OAuthToken],
    decodeRight: Pipe[F, Response[F], ErrorOr[A]],
    decodeLeft: Pipe[F, ErrorOr[Nothing], ErrorOr[A]]
  )(implicit consumer: Consumer): Stream[F, HttpResponse[A]] = {

    val signed: Stream[F, Request[F]] = Stream.eval(sign(consumer, oAuthToken)(request))
    //    val pure: Stream[Pure, Request[F]] = Stream(request)

    for {

      request <- signed.through(requestHeadersLogPipe)

      responseStream <- client
        .stream(request)
        .through(responseHeadersLogPipe)
        .attempt
        .map {
          case Right(value) => Stream(value)
          case Left(throwable) =>
            logger.error(throwable.getMessage)
            Stream.empty
        }

      response <- responseStream

      httpRes <- (response.status match {

        case Status.Ok =>
          Stream
            .emit(response)
            .through(decodeRight)
            .through(responseLogPipe)

        case _ =>
          Stream
            .emit(response)
            .through(errorHandler)
            .through(decodeLeft) // FIXME: WTF IS THIS
            .through(responseLogPipe)

      }).map(HttpResponse(response.headers, _))

    } yield httpRes
  }
}
