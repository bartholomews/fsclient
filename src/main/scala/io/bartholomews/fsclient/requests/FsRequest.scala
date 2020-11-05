package io.bartholomews.fsclient.requests

import cats.effect.ConcurrentEffect
import io.bartholomews.fsclient.client.FsClient
import io.bartholomews.fsclient.codecs.StringPipe
import io.bartholomews.fsclient.entities.oauth.Signer
import io.bartholomews.fsclient.utils.HttpTypes.HttpResponse
import io.circe.{Decoder, Encoder, Json}
import org.http4s.{Method, UrlForm}

sealed private[fsclient] trait FsRequest[Body, Raw, Res]
    extends FsClientRequest[Body]
    with RequestBodyInfo[Body]
    with HasDecoder[Raw, Res] {
  def runWith[F[_]: ConcurrentEffect, S <: Signer](client: FsClient[F, S], signer: Option[S]): F[HttpResponse[Res]] =
    client.fetch(this.toHttpRequest[F](client.appConfig.userAgent), signer.getOrElse(client.appConfig.signer))(
      implicitly[ConcurrentEffect[F]],
      rawDecoder,
      pipeDecoder
    )
}

sealed trait FsSimpleRequest[Body, Raw, Res] extends FsRequest[Body, Raw, Res] {
  def runWith[F[_]: ConcurrentEffect, S <: Signer](client: FsClient[F, S]): F[HttpResponse[Res]] =
    super.runWith(client, signer = None)
}

sealed private[fsclient] trait FsAuthRequest[Body, Raw, Res] extends FsRequest[Body, Raw, Res] {
  def runWith[F[_]: ConcurrentEffect, S <: Signer](client: FsClient[F, S])(implicit signer: S): F[HttpResponse[Res]] =
    super.runWith(client, Some(signer))
}

// *************************************************************************************************
// Auth (signed) request expecting Json response content type
// *************************************************************************************************

object FsAuthJson {
  abstract class Get[Res](implicit decoder: Decoder[Res])
      extends FsAuthRequest[Nothing, Json, Res]
      with HasJsonDecoder[Res]
      with EmptyRequestBody {
    final override private[fsclient] def method: Method = Method.GET
    final override def resDecoder: Decoder[Res] = decoder
  }

  abstract class Put[Body, Res](implicit encoder: Encoder[Body], decoder: Decoder[Res])
      extends FsAuthRequest[Body, Json, Res]
      with HasJsonDecoder[Res]
      with HasRequestBodyAsJson[Body] {
    final override private[fsclient] def method: Method = Method.PUT
    final override def bodyEncoder: Encoder[Body] = encoder
    final override def resDecoder: Decoder[Res] = decoder
  }

  abstract class PutEmpty[Res](implicit decoder: Decoder[Res])
      extends FsAuthRequest[Nothing, Json, Res]
      with HasJsonDecoder[Res]
      with EmptyRequestBody {
    final override private[fsclient] def method: Method = Method.PUT
    final override def resDecoder: Decoder[Res] = decoder
  }

  abstract class Post[Body, Res](implicit encoder: Encoder[Body], decoder: Decoder[Res])
      extends FsAuthRequest[Body, Json, Res]
      with HasJsonDecoder[Res]
      with HasRequestBodyAsJson[Body] {
    final override private[fsclient] def method: Method = Method.POST
    final override def bodyEncoder: Encoder[Body] = encoder
    final override def resDecoder: Decoder[Res] = decoder
  }

  abstract class PostEmpty[Res](implicit decoder: Decoder[Res])
      extends FsAuthRequest[Nothing, Json, Res]
      with HasJsonDecoder[Res]
      with EmptyRequestBody {
    final override private[fsclient] def method: Method = Method.POST
    final override def resDecoder: Decoder[Res] = decoder
  }

  abstract class Delete[Body, Res](implicit encoder: Encoder[Body], decoder: Decoder[Res])
      extends FsAuthRequest[Body, Json, Res]
      with HasJsonDecoder[Res]
      with HasRequestBodyAsJson[Body] {
    final override private[fsclient] def method: Method = Method.DELETE
    final override def bodyEncoder: Encoder[Body] = encoder
    final override def resDecoder: Decoder[Res] = decoder
  }

  abstract class DeleteEmpty[Res](implicit decoder: Decoder[Res])
      extends FsAuthRequest[Nothing, Json, Res]
      with HasJsonDecoder[Res]
      with EmptyRequestBody {
    final override private[fsclient] def method: Method = Method.DELETE
    final override def resDecoder: Decoder[Res] = decoder
  }
}

// *************************************************************************************************
// Auth (signed) request expecting PlainText response content type
// *************************************************************************************************

object FsAuthPlainText {

  trait GetEmpty[Res] extends FsAuthRequest[Nothing, String, Res] with EmptyRequestBody with HasPlainTextDecoder[Res] {
    final override private[fsclient] def method: Method = Method.GET
  }

  abstract class Put[Body, Res](implicit encoder: Encoder[Body])
      extends FsAuthRequest[Body, String, Res]
      with HasRequestBodyAsJson[Body]
      with HasPlainTextDecoder[Res] {
    final override private[fsclient] def method: Method = Method.PUT
    final override def bodyEncoder: Encoder[Body] = encoder
  }

  trait PutEmpty[Res] extends FsAuthRequest[Nothing, String, Res] with EmptyRequestBody with HasPlainTextDecoder[Res] {
    final override private[fsclient] def method: Method = Method.PUT
  }

  abstract class Post[Body, Res](implicit encoder: Encoder[Body])
      extends FsAuthRequest[Body, String, Res]
      with HasRequestBodyAsJson[Body]
      with HasPlainTextDecoder[Res] {
    final override private[fsclient] def method: Method = Method.POST
    final override def bodyEncoder: Encoder[Body] = encoder
  }

  trait PostEmpty[Res] extends FsAuthRequest[Nothing, String, Res] with EmptyRequestBody with HasPlainTextDecoder[Res] {
    final override private[fsclient] def method: Method = Method.POST
  }

  abstract class Delete[Body, Res](implicit encoder: Encoder[Body])
      extends FsAuthRequest[Body, String, Res]
      with HasRequestBodyAsJson[Body]
      with HasPlainTextDecoder[Res] {
    final override private[fsclient] def method: Method = Method.DELETE
    final override def bodyEncoder: Encoder[Body] = encoder
  }

  trait DeleteEmpty[Res]
      extends FsAuthRequest[Nothing, String, Res]
      with EmptyRequestBody
      with HasPlainTextDecoder[Res] {
    final override private[fsclient] def method: Method = Method.DELETE
  }
}

// *************************************************************************************************
// Simple (unsigned) request expecting Json response content type
// *************************************************************************************************

object FsSimpleJson {
  abstract class Get[Res](implicit decoder: Decoder[Res])
      extends FsSimpleRequest[Nothing, Json, Res]
      with HasJsonDecoder[Res]
      with EmptyRequestBody {
    final override private[fsclient] def method: Method = Method.GET
    final override def resDecoder: Decoder[Res] = decoder
  }

  abstract class Put[Body, Res](implicit encoder: Encoder[Body], decoder: Decoder[Res])
      extends FsSimpleRequest[Body, Json, Res]
      with HasJsonDecoder[Res]
      with HasRequestBodyAsJson[Body] {
    final override private[fsclient] def method: Method = Method.PUT
    final override def bodyEncoder: Encoder[Body] = encoder
    final override def resDecoder: Decoder[Res] = decoder
  }

  abstract class PutEmpty[Res](implicit decoder: Decoder[Res])
      extends FsSimpleRequest[Nothing, Json, Res]
      with HasJsonDecoder[Res]
      with EmptyRequestBody {
    final override private[fsclient] def method: Method = Method.PUT
    final override def resDecoder: Decoder[Res] = decoder
  }

  abstract class Post[Body, Res](implicit encoder: Encoder[Body], decoder: Decoder[Res])
      extends FsSimpleRequest[Body, Json, Res]
      with HasJsonDecoder[Res]
      with HasRequestBodyAsJson[Body] {
    final override private[fsclient] def method: Method = Method.POST
    final override def bodyEncoder: Encoder[Body] = encoder
    final override def resDecoder: Decoder[Res] = decoder
  }

  abstract class PostEmpty[Res](implicit decoder: Decoder[Res])
      extends FsSimpleRequest[Nothing, Json, Res]
      with HasJsonDecoder[Res]
      with EmptyRequestBody {
    final override private[fsclient] def method: Method = Method.POST
    final override def resDecoder: Decoder[Res] = decoder
  }

  abstract class Delete[Body, Res](implicit encoder: Encoder[Body], decoder: Decoder[Res])
      extends FsSimpleRequest[Body, Json, Res]
      with HasJsonDecoder[Res]
      with HasRequestBodyAsJson[Body] {
    final override private[fsclient] def method: Method = Method.DELETE
    final override def bodyEncoder: Encoder[Body] = encoder
    final override def resDecoder: Decoder[Res] = decoder
  }

  abstract class DeleteEmpty[Res](implicit decoder: Decoder[Res])
      extends FsSimpleRequest[Nothing, Json, Res]
      with HasJsonDecoder[Res]
      with EmptyRequestBody {
    final override private[fsclient] def method: Method = Method.DELETE
    final override def resDecoder: Decoder[Res] = decoder
  }
}

// *************************************************************************************************
// Simple (unsigned) request expecting PlainText response content type
// *************************************************************************************************

object FsSimplePlainText {

  abstract class Get[Res](implicit decoder: StringPipe[Res])
      extends FsSimpleRequest[Nothing, String, Res]
      with EmptyRequestBody
      with HasPlainTextDecoder[Res] {
    final override private[fsclient] def method: Method = Method.GET
    final override def resDecoder: StringPipe[Res] = decoder
  }

  abstract class Put[Body, Res](implicit encoder: Encoder[Body], decoder: StringPipe[Res])
      extends FsSimpleRequest[Body, String, Res]
      with HasRequestBodyAsJson[Body]
      with HasPlainTextDecoder[Res] {
    final override private[fsclient] def method: Method = Method.PUT
    final override def bodyEncoder: Encoder[Body] = encoder
    final override def resDecoder: StringPipe[Res] = decoder
  }

  abstract class PutEmpty[Res](implicit decoder: StringPipe[Res])
      extends FsSimpleRequest[Nothing, String, Res]
      with EmptyRequestBody
      with HasPlainTextDecoder[Res] {
    final override private[fsclient] def method: Method = Method.PUT
    final override def resDecoder: StringPipe[Res] = decoder
  }

  abstract class Post[Body, Res](implicit encoder: Encoder[Body], decoder: StringPipe[Res])
      extends FsSimpleRequest[Body, String, Res]
      with HasRequestBodyAsJson[Body]
      with HasPlainTextDecoder[Res] {
    final override private[fsclient] def method: Method = Method.POST
    final override def bodyEncoder: Encoder[Body] = encoder
    final override def resDecoder: StringPipe[Res] = decoder
  }

  abstract class PostEmpty[Res](implicit decoder: StringPipe[Res])
      extends FsSimpleRequest[Nothing, String, Res]
      with EmptyRequestBody
      with HasPlainTextDecoder[Res] {
    final override private[fsclient] def method: Method = Method.POST
    final override def resDecoder: StringPipe[Res] = decoder
  }

  abstract class Delete[Body, Res](implicit encoder: Encoder[Body], decoder: StringPipe[Res])
      extends FsSimpleRequest[Body, String, Res]
      with HasRequestBodyAsJson[Body]
      with HasPlainTextDecoder[Res] {
    final override private[fsclient] def method: Method = Method.DELETE
    final override def bodyEncoder: Encoder[Body] = encoder
    final override def resDecoder: StringPipe[Res] = decoder
  }

  abstract class DeleteEmpty[Res](implicit decoder: StringPipe[Res])
      extends FsSimpleRequest[Nothing, String, Res]
      with EmptyRequestBody
      with HasPlainTextDecoder[Res] {
    final override private[fsclient] def method: Method = Method.DELETE
    final override def resDecoder: StringPipe[Res] = decoder
  }
}

object UrlFormRequest {
  trait Post[Res] extends FsAuthRequest[UrlForm, Json, Res] with HasRequestBodyAsUrlForm {
    final override private[fsclient] def method: Method = Method.POST
  }
}
