package fsclient.conf

import org.http4s.Uri

case class BaseUri(value: Uri) extends AnyVal
case class ApiUri(value: Uri) extends AnyVal
