package fsclient.http.effect

import cats.effect.Effect
import cats.implicits._
import fs2.{Pipe, Stream}
import fsclient.entities.{EmptyResponseException, ResponseError}
import fsclient.utils.{HttpTypes, Logger}
import io.circe.fs2.{byteStreamParser, decoder}
import io.circe.{Decoder, Json}
import org.http4s.headers.`Content-Type`
import org.http4s.{Response, Status}

private[http] trait HttpPipes extends HttpTypes with Logger {

  def doNothing[F[_]: Effect, A]: Pipe[F, A, A] = _.map(identity)

  /**
   * Attempt to decode an Http Response with the provided decoder
   *
   * @param decode a decoder for type `A`
   * @tparam F the `Effect`
   * @tparam A the type of expected response entity
   * @return a Pipe transformed in an `Either[ResponseError, A]`
   */
  def decodeJsonResponse[F[_]: Effect, A](implicit decode: Decoder[A]): Pipe[F, Response[F], ErrorOr[A]] =
    _.flatMap(
      _.body
        .through(byteStreamParser)
        .through(decoder[F, A])
        .attempt
        .through(leftMapToResponseError(Status.UnprocessableEntity))
    )

  /**
   * Attempt to decode a PlainText Http Response with the provided decoder
   *
   * @param decoder a Pipe decoder from String to type `A`
   * @tparam F the `Effect`
   * @tparam A the type of expected response entity
   * @return a Pipe transformed in an `Either[ResponseError, A]`
   */
  def decodeTextPlainResponse[F[_]: Effect, A](
    implicit decoder: HttpPipe[F, String, A]
  ): Pipe[F, Response[F], ErrorOr[A]] =
    _.flatMap(res => {
      Stream
        .eval(res.as[String])
        .attempt
        .through(leftMapToResponseError[F, String](Status.UnprocessableEntity))
        .through(decoder)
    })

  /**
   * Map the left side of an `Either[Throwable, A]` into a `ResponseError`
   *
   * @param status the Status Code of the `ResponseError`
   * @tparam F the `Effect`
   * @tparam A the type of expected response entity
   * @return an `Either[ResponseError, A]`
   */
  def leftMapToResponseError[F[_]: Effect, A](status: Status): Pipe[F, Either[Throwable, A], ErrorOr[A]] =
    _.through(errorLogPipe).map(_.leftMap(ResponseError(_, status)))

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
