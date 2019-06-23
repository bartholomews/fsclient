package fsclient.entities

import org.http4s.Method

sealed trait HttpMethod {
  def value: Method
}

case class GET() extends HttpMethod {
  override val value: Method = Method.GET
}
case class POST() extends HttpMethod {
  override val value: Method = Method.POST
}