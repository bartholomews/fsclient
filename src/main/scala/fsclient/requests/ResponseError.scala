package fsclient.requests

import org.http4s.Status

trait ResponseError extends Throwable {
  val status: Status
}

object ResponseError {

  private case class ResponseErrorImpl(status: Status, throwable: Throwable, override val getMessage: String)
      extends ResponseError

  def apply(throwable: Throwable, status: Status): ResponseError =
    throwable match {
      case circeError: io.circe.Error =>
        ResponseErrorImpl(
          Status.InternalServerError,
          circeError,
          "There was a problem decoding or parsing this response, please check the error logs"
        )
      case _ => ResponseErrorImpl(status, throwable, throwable.getMessage)
    }
}

case object EmptyResponseException extends Exception {
  override val getMessage: String = "Response body was empty"
}
