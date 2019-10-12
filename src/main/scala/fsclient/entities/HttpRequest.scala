package fsclient.entities

import org.http4s.Method.{DefaultMethodWithBody, SafeMethodWithBody}
import org.http4s.{Method, Uri}

trait HttpRequest[Body, Response] {
  def uri: Uri

  def method: Method

  def body: Body
}

object HttpRequest {

  case class GET[Response](uri: Uri) extends HttpRequest[None.type, Response] {
    override val method: SafeMethodWithBody = Method.GET
    override val body: None.type = None
  }

  case class POST[Body, Response](uri: Uri, body: Body)
      extends HttpRequest[Body, Response] {
    override val method: DefaultMethodWithBody = Method.POST
  }

}
