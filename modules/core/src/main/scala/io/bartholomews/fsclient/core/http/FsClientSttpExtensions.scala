package io.bartholomews.fsclient.core.http

import io.bartholomews.fsclient.core.FsClient
import io.bartholomews.fsclient.core.config.UserAgent
import io.bartholomews.fsclient.core.oauth.{AuthDisabled, CustomAuthorizationHeader, Signer, SignerV1, SignerV2}
import sttp.client3.{
  asStringAlways,
  emptyRequest,
  ignore,
  Empty,
  HttpError,
  Request,
  RequestT,
  ResponseAs,
  ResponseException
}
import sttp.model.Uri.QuerySegment
import sttp.model.{Header, HeaderNames, MediaType, Uri}

trait FsClientSttpExtensions {
  def baseRequest[F[_], S <: Signer](client: FsClient[F, S]): RequestT[Empty, Either[String, String], Any] =
    emptyRequest
      .userAgent(client.userAgent)

  import sttp.client3.asEither
  def asUnit[R]: ResponseAs[Either[ResponseException[String, Nothing], Unit], R] =
    asEither[ResponseException[String, Nothing], Unit, R](
      onError = asStringAlways.mapWithMetadata({ case (body, meta) => HttpError(body, meta.code) }),
      onSuccess = ignore
    )

  def mapInto[A, DE, B](implicit
    responseMapping: ResponseMapping[A, DE, B]
  ): ResponseAs[Either[ResponseException[String, DE], B], Any] = responseMapping.responseAs

  implicit class UriExtensions(uri: Uri) {
    def /(path: String): Uri =
      uri.addPath(path)

    def withQueryParam(key: String, value: String): Uri =
      uri.addQuerySegment(QuerySegment.KeyValue(key, value))

    def withOptionQueryParam(key: String, maybeValue: Option[String]): Uri =
      maybeValue
        .map(value => uri.addQuerySegment(QuerySegment.KeyValue(key, value)))
        .getOrElse(uri)
  }

  implicit class PartialRequestExtensions[U[_], T, R](request: RequestT[U, T, R]) {
    def userAgent(userAgent: UserAgent, replaceExisting: Boolean = false): RequestT[U, T, R] =
      request.header(Header(HeaderNames.UserAgent, userAgent.value), replaceExisting)

    def hasContentType(mt: MediaType): Boolean = request.headers.contains(
      Header(HeaderNames.ContentType, mt.toString)
    )

    def as(implicit
      responseAs: ResponseAs[Either[ResponseException[String, Exception], T], R]
    ): RequestT[U, Either[ResponseException[String, Exception], T], R] =
      request
        .response(responseAs)
  }

  implicit class RequestExtensions[T, -R](request: Request[T, R]) {
    def sign(implicit signer: Signer): Request[T, R] = signer match {
      case AuthDisabled                      => request
      case signer: CustomAuthorizationHeader => signer.sign(request)
      // TODO: https://www.thepolyglotdeveloper.com/2014/11/understanding-request-signing-oauth-1-0a-providers/
      case signerV1: SignerV1 => signerV1.sign(request.contentType(MediaType.ApplicationXWwwFormUrlencoded))
      case signerV2: SignerV2 => signerV2.sign(request)
    }

    def sign[F[_], Auth <: Signer](client: FsClient[F, Auth]): Request[T, R] =
      sign(client.signer)
  }
}

object FsClientSttpExtensions extends FsClientSttpExtensions {}
