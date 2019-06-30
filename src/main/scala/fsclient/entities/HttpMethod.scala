package fsclient.entities

import org.http4s.Method
import org.http4s.Method.{DefaultMethodWithBody, SafeMethodWithBody}

sealed trait HttpMethod {
  def method: Method
}

object HttpMethod {

  trait GET extends HttpMethod {
    override val method: SafeMethodWithBody = Method.GET
  }

  trait POST extends HttpMethod {
    override val method: DefaultMethodWithBody = Method.POST
  }

}