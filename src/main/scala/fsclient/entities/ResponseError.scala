package fsclient.entities

import org.http4s.Status
import fsclient.utils.Logger

trait ResponseError extends Throwable {
  val status: Status
}

object ResponseError extends Logger {

  private case class ResponseErrorImpl(status: Status,
                                       throwable: Throwable,
                                       override val getMessage: String)
      extends ResponseError

  def apply(throwable: Throwable,
            status: Status = Status.InternalServerError): ResponseError = {
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
}

case object EmptyResponseException extends Exception {
  override val getMessage: String =
    "Response was empty. Please check request logs"
}

case object GenericResponseError extends Exception {
  override val getMessage: String =
    "There was a problem with the response. Please check error logs"
}
