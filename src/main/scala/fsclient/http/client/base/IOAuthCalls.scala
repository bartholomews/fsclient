package fsclient.http.client.base

import cats.effect.IO
import fsclient.entities._
import fsclient.http.client.IOAuthClient
import fsclient.utils.HttpTypes.{IOHttpPipe, IOResponse}
import io.circe.Decoder
import org.http4s.EntityEncoder

trait IOAuthCalls {

  def accessTokenRequest(
    request: AccessTokenRequest
  )(implicit responseDecoder: IOHttpPipe[String, AccessToken]): IO[HttpResponse[AccessToken]]

  def toOAuthClient(
    request: AccessTokenRequest
  )(implicit responseDecoder: IOHttpPipe[String, AccessToken]): IO[Either[ResponseError, IOAuthClient]]

  def fetchJson[R](
    accessToken: AccessToken
  )(request: FsClientPlainRequest)(implicit responseDecoder: Decoder[R]): IOResponse[R]

  def fetchPlainText[R](
    accessToken: AccessToken
  )(request: FsClientPlainRequest)(implicit responseDecoder: IOHttpPipe[String, R]): IOResponse[R]

  def fetchJsonWithBody[B, R](
    accessToken: AccessToken
  )(request: FsClientRequestWithBody[B])(implicit requestBodyEncoder: EntityEncoder[IO, B],
                                         responseDecoder: Decoder[R]): IOResponse[R]

  def fetchPlainTextWithBody[B, R](
    accessToken: AccessToken
  )(request: FsClientRequestWithBody[B])(implicit requestBodyEncoder: EntityEncoder[IO, B],
                                         responseDecoder: IOHttpPipe[String, R]): IOResponse[R]
}
