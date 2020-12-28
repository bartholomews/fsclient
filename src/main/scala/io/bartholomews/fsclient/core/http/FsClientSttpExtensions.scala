package io.bartholomews.fsclient.core.http

import io.bartholomews.fsclient.core.FsClient
import io.bartholomews.fsclient.core.config.UserAgent
import io.bartholomews.fsclient.core.oauth.{AuthDisabled, CustomAuthorizationHeader, Signer, SignerV1, SignerV2}
import sttp.client.{
  asStringAlways,
  emptyRequest,
  fromMetadata,
  ignore,
  Empty,
  HttpError,
  Request,
  RequestT,
  ResponseAs,
  ResponseError,
  ResponseMetadata
}
import sttp.model.Uri.{PathSegment, QuerySegment}
import sttp.model.{Header, HeaderNames, MediaType, Uri}

trait FsClientSttpExtensions {

  type FsResponseAs[T] = ResponseAs[Either[ResponseError[Exception], T], Nothing]

  def baseRequest[F[_], S <: Signer](client: FsClient[F, S]): RequestT[Empty, Either[String, String], Nothing] =
    emptyRequest
      .userAgent(client.appConfig.userAgent)

  def asUnit: ResponseAs[Either[HttpError, Unit], Nothing] =
    fromMetadata[Either[HttpError, Unit], Nothing] { meta: ResponseMetadata =>
      if (meta.isSuccess) ignore.map(Right(_))
      else asStringAlways.map(body => Left(HttpError(body, meta.code)))
    }

  def mapInto[A, B](implicit
                    responseMapping: ResponseMapping[A, B]
  ): ResponseAs[Either[ResponseError[Exception], B], Nothing] = responseMapping.responseAs

  implicit class UriExtensions(uri: Uri) {
    def /(path: String): Uri =
      uri.pathSegments(uri.pathSegments :+ PathSegment(path))

    def withQueryParam(key: String, value: String): Uri =
      uri.querySegment(QuerySegment.KeyValue(key, value))

    def withOptionQueryParam(key: String, maybeValue: Option[String]): Uri =
      maybeValue
        .map(value => uri.querySegment(QuerySegment.KeyValue(key, value)))
        .getOrElse(uri)
  }

  implicit class PartialRequestExtensions[U[_], T, +S](request: RequestT[U, T, S]) {
    def userAgent(userAgent: UserAgent, replaceExisting: Boolean = false): RequestT[U, T, S] =
      request.header(Header(HeaderNames.UserAgent, userAgent.value), replaceExisting)

    def hasContentType(mt: MediaType): Boolean = request.headers.contains(
      Header(HeaderNames.ContentType, mt.toString)
    )

    def as(implicit
      responseAs: ResponseAs[Either[ResponseError[Exception], T], Nothing]
    ): RequestT[U, Either[ResponseError[Exception], T], S] =
      request
        .response(responseAs)
  }

  implicit class RequestExtensions[T, +S](request: Request[T, S]) {
    def sign(implicit signer: Signer): Request[T, S] = signer match {
      case AuthDisabled                      => request
      case signer: CustomAuthorizationHeader => signer.sign(request)
      // TODO: https://www.thepolyglotdeveloper.com/2014/11/understanding-request-signing-oauth-1-0a-providers/
      case signerV1: SignerV1 => signerV1.sign(request.contentType(MediaType.ApplicationXWwwFormUrlencoded))
      case signerV2: SignerV2 => signerV2.sign(request)
    }

    def sign[F[_], Auth <: Signer](client: FsClient[F, Auth]): Request[T, S] =
      sign(client.appConfig.signer)
  }
}

object FsClientSttpExtensions extends FsClientSttpExtensions {}
