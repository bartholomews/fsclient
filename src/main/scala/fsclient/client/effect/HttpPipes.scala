package fsclient.client.effect

import cats.effect.Effect
import cats.implicits._
import fs2.{Pipe, Stream}
import fsclient.codecs.RawDecoder
import fsclient.entities.{EmptyResponseException, ResponseError}
import fsclient.utils.HttpTypes._
import fsclient.utils.Logger
import io.circe.Json
import io.circe.fs2.byteStreamParser
import org.http4s.headers.`Content-Type`
import org.http4s.{Response, Status}

private[client] object HttpPipes {

  import Logger._

  def doNothing[F[_]: Effect, A]: Pipe[F, A, A] = _.map(identity)

  /**
   * Attempt to decode an Http Response with the provided decoder
   *
   * @param rawDecoder a Pipe from `Byte` to `Raw`
   * @param resDecoder a Pipe from `Raw` to `Res`
   * @tparam F the `Effect`
   * @tparam Raw the raw type of the response to decode (e.g. Json, PlainText string)
   * @tparam Res the type of expected decoded response entity
   * @return a Pipe transformed in an `Either[ResponseError, Res]`
   */
  def decodeResponse[F[_]: Effect, Raw, Res](
    rawDecoder: RawDecoder[Raw],
    resDecoder: Pipe[F, Raw, Res]
  ): Pipe[F, Response[F], ErrorOr[Res]] =
    _.through(rawDecoder.decode)
      .through(resDecoder)
      .attempt
      .through(errorLogPipe)
      .map(_.leftMap(ResponseError(_, Status.UnprocessableEntity)))

  /**
   *
   * Fold both sides of an `Either[Throwable, A]` into an `Either.left[ResponseError]`
   *
   * @param response the `Response`
   * @param f      function to map the `A` into the error message
   * @tparam F the `Effect`
   * @tparam A the type of expected response entity, which will be folded to the left
   * @return a Pipe transformed in an `Either.left[ResponseError, Nothing]`
   */
  def foldToResponseError[F[_]: Effect, A](
    response: Response[F],
    f: A => String
  ): Pipe[F, Either[Throwable, A], ErrorOr[Nothing]] =
    _.through(errorLogPipe)
      .map(
        _.fold(
          err => ResponseError(err, response.status).asLeft,
          res => ResponseError(new Exception(f(res)), response.status).asLeft
        )
      )

  /**
   * Decode an Http Response into an `Either.left[ResponseError, Nothing]`.
   *
   * @tparam F the `Effect`
   * @return a Pipe transformed in an `Either.left[ResponseError, Nothing]`
   */
  def errorHandler[F[_]: Effect]: Pipe[F, Response[F], ErrorOr[Nothing]] =
    _.flatMap(
      response => {

        response.headers.get(`Content-Type`).map(_.value) match {

          case Some("application/json") =>
            response.body
              .through(byteStreamParser)
              .last
              .flatMap(_.fold[Stream[F, Json]](Stream.raiseError[F](EmptyResponseException))(Stream.emit))
              .attempt
              // FIXME: could try to parse a { "message": "[value]" } instead of _.spaces2
              .through(foldToResponseError(response, _.spaces2))

          case _ =>
            Stream
              .eval(response.as[String])
              .attempt
              .through(foldToResponseError(response, res => res))
        }
      }
    )

}
