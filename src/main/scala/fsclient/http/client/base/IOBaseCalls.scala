package fsclient.http.client.base

import cats.effect.IO
import fsclient.entities.{FsClientPlainRequest, FsClientRequestWithBody}
import fsclient.utils.HttpTypes.{IOHttpPipe, IOResponse}
import io.circe.Decoder
import org.http4s.EntityEncoder

private[http] trait IOBaseCalls {

  def fetchJson[R](request: FsClientPlainRequest)(implicit responseDecoder: Decoder[R]): IOResponse[R]

  def fetchPlainText[R](request: FsClientPlainRequest)(implicit responseDecoder: IOHttpPipe[String, R]): IOResponse[R]

  def fetchJsonWithBody[B, R](request: FsClientRequestWithBody[B])(implicit requestBodyEncoder: EntityEncoder[IO, B],
                                                                   responseDecoder: Decoder[R]): IOResponse[R]

  def fetchPlainTextWithBody[B, R](
    request: FsClientRequestWithBody[B]
  )(implicit requestBodyEncoder: EntityEncoder[IO, B], responseDecoder: IOHttpPipe[String, R]): IOResponse[R]
}
