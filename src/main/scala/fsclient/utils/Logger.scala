package fsclient.utils

import cats.effect.Effect
import cats.implicits._
import fs2.Pipe
import fsclient.config.FsClientConfig
import fsclient.utils.HttpTypes.ErrorOr
import io.circe.Json
import org.http4s.{Request, Response}
import org.log4s.getLogger
import pureconfig.ConfigSource

object Logger {

  import pureconfig.generic.auto._

  private val loggerName: String = ConfigSource.default
    .load[FsClientConfig.LoggerConfig]
    .map(_.logger.name)
    .getOrElse("fsclient-logger")

  private[fsclient] val logger = getLogger(loggerName).logger

  logger.info(s"$logger started.")

  private[fsclient] def logRequest[F[_]: Effect, B](request: Request[F]): Request[F] = {
    logger.info(s"Request: ${request.method.name} [${request.uri}]")
    logger.debug(s"Request headers - {\n${request.headers.toList.mkString("\t", "\n\t", "\n")}}")
    request
  }

  private[fsclient] def logRequestBody[F[_]: Effect, B](body: B): B = {
    logger.debug(s"Request body - {\n\t$body\n}")
    body
  }

  private[fsclient] def responseHeadersLogPipe[F[_]: Effect, T]: Pipe[F, Response[F], Response[F]] =
    _.map { res =>
      logger.debug(s"Response status: [${res.status}]")
      logger.debug(s"Response headers - {\n${res.headers.toList.mkString("\t", "\n\t", "\n")}}")
      res
    }

  private[fsclient] def rawJsonResponseLogPipe[F[_]: Effect, A]: Pipe[F, Json, Json] =
    _.map { json =>
      logger.debug(s"Json response - ${json.spaces2}")
      json
    }

  private[fsclient] def rawPlainTextResponseLogPipe[F[_]: Effect, A]: Pipe[F, String, String] =
    _.map { text =>
      logger.debug(s"PlainText response - [\n\t$text\n]")
      text
    }

  private[fsclient] def responseLogPipe[F[_]: Effect, A]: Pipe[F, ErrorOr[A], ErrorOr[A]] =
    _.map(_.map { entity =>
      logger.debug(s"Response entity - {\n\t$entity\n}")
      // TODO: figure out logger shape (entity, sumologic etc)
      //  logger.debug(s"Response entity", s"{\n\t$entity\n}")
      entity
    })

  private[fsclient] def errorLogPipe[F[_]: Effect, A]: Pipe[F, Either[Throwable, A], Either[Throwable, A]] =
    _.map(_.leftMap { throwable =>
      logger.error("There was a problem with the request", throwable)
      throwable
    })
}
