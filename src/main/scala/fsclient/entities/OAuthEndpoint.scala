package fsclient.entities

trait OAuthEndpoint[T] extends HttpEndpoint[T] {
  def requestToken: RequestToken
}
