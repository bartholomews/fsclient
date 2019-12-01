package fsclient.http.client.base

import cats.effect.IO
import fsclient.entities._
import fsclient.http.client.IOAuthClient
import fsclient.oauth.OAuthToken
import fsclient.oauth.OAuthVersion.OAuthV1.{AccessTokenRequestV1, AccessTokenV1}
import fsclient.utils.HttpTypes.{IOHttpPipe, IOResponse}
import io.circe.Decoder
import org.http4s.EntityEncoder

trait IOAuthCalls {

  def accessTokenRequest(
    request: AccessTokenRequestV1
  )(implicit responseDecoder: IOHttpPipe[String, AccessTokenV1]): IO[HttpResponse[AccessTokenV1]]

  def toOAuthClientV1(
    request: AccessTokenRequestV1
  )(implicit responseDecoder: IOHttpPipe[String, AccessTokenV1]): IO[Either[ResponseError, IOAuthClient]]

  def fetchJson[R](
    token: OAuthToken
  )(request: FsClientPlainRequest)(implicit responseDecoder: Decoder[R]): IOResponse[R]

  def fetchPlainText[R](
    token: OAuthToken
  )(request: FsClientPlainRequest)(implicit responseDecoder: IOHttpPipe[String, R]): IOResponse[R]

  def fetchJsonWithBody[B, R](
    token: OAuthToken
  )(request: FsClientRequestWithBody[B])(implicit requestBodyEncoder: EntityEncoder[IO, B],
                                         responseDecoder: Decoder[R]): IOResponse[R]

  def fetchPlainTextWithBody[B, R](
    token: OAuthToken
  )(request: FsClientRequestWithBody[B])(implicit requestBodyEncoder: EntityEncoder[IO, B],
                                         responseDecoder: IOHttpPipe[String, R]): IOResponse[R]
}
