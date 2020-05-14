package io.bartholomews.fsclient.client

import cats.effect.Effect
import cats.implicits._
import fs2.{Pipe, Stream}
import io.bartholomews.fsclient.codecs.RawDecoder
import io.bartholomews.fsclient.entities.{EmptyResponseException, ErrorBody, ErrorBodyString}
import io.bartholomews.fsclient.utils.HttpTypes._
import io.bartholomews.fsclient.utils.{FsHeaders, FsLogger}
import io.circe.Json
import io.circe.fs2.byteStreamParser
import org.apache.http.entity.ContentType
import org.http4s.headers.`Content-Type`
import org.http4s.{Response, Status}

private[client] object HttpPipes {

  import FsLogger._

  /**
   * Attempt to decode an Http Response with the provided decoder
   *
   * @param rawDecoder a Pipe from `Byte` to `Raw`
   * @param resDecoder a Pipe from `Raw` to `Res`
   * @tparam F the `Effect`
   * @tparam Raw the raw type of the response to decode (e.g. Json, PlainText string)
   * @tparam Res the type of expected decoded response entity
   * @return a Pipe transformed in an `Either[(Status, ErrorBody), Res]`
   */
  def decodeResponse[F[_]: Effect, Raw, Res](
    rawDecoder: RawDecoder[Raw],
    resDecoder: Pipe[F, Raw, Res]
  ): Pipe[F, Response[F], ErrorOr[Res]] =
    _.through(rawDecoder.decode)
      .through(resDecoder)
      .attempt
      .through(errorLogPipe)
      .map(_.leftMap { error =>
        Tuple2(
          Status.UnprocessableEntity,
          error match {
            case _: io.circe.Error =>
              ErrorBodyString("There was a problem decoding or parsing this response, please check the error logs")
            case throwable =>
              ErrorBodyString(throwable.getMessage)
          }
        )
      })

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
  def foldToResponseError[F[_]: Effect, A, E <: ErrorBody](
    response: Response[F],
    f: A => ErrorBody
  ): Pipe[F, Either[Throwable, A], ErrorOr[Nothing]] =
    _.through(errorLogPipe)
      .map(
        _.fold(
          err => Tuple2(response.status, ErrorBodyString(err.getMessage)).asLeft,
          res => Tuple2(response.status, f(res)).asLeft
        )
      )

  /**
   * Decode an Http Response into an `Either.left[ResponseError, Nothing]`.
   *
   * @tparam F the `Effect`
   * @return a Pipe transformed in an `Either.left[ResponseError, Nothing]`
   */
  def errorHandler[F[_]: Effect]: Pipe[F, Response[F], ErrorOr[Nothing]] =
    _.flatMap { response =>
      response.headers.get(`Content-Type`) match {
        case Some(contentType) if FsHeaders.is(contentType, ContentType.APPLICATION_JSON) =>
          response.body
            .through(byteStreamParser)
            .last
            .flatMap(_.fold[Stream[F, Json]](Stream.raiseError[F](EmptyResponseException()))(Stream.emit))
            .attempt
            .through(foldToResponseError(response, ErrorBody.apply))

        // could also intercept `text/html`
        case _ =>
          Stream
            .eval(response.as[String])
            .attempt
            .through(foldToResponseError(response, ErrorBody.apply))
      }
    }
}
