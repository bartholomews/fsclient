package fsclient.http.effect

import cats.effect.Effect
import fsclient.entities.OAuthToken
import org.http4s.Request
import org.http4s.client.oauth1.{Consumer, signRequest}

trait OAuthSignature {

  private[http] def sign[F[_] : Effect](consumer: Consumer, oAuthToken: Option[OAuthToken] = None)
                                       (req: Request[F]): F[Request[F]] = {
    signRequest(
      req,
      consumer,
      callback = None,
      verifier = oAuthToken.flatMap(_.verifier),
      oAuthToken.map(_.token)
    )
  }
}
