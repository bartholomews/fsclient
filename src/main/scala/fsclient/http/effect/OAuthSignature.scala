package fsclient.http.effect

import cats.effect.Effect
import fsclient.entities.OAuthAccessToken
import org.http4s.Request
import org.http4s.client.oauth1.{Consumer, signRequest}

trait OAuthSignature {

  private[http] def sign[F[_] : Effect](consumer: Consumer, accessToken: Option[OAuthAccessToken] = None)
                                       (req: Request[F]): F[Request[F]] = {
    signRequest(
      req,
      consumer,
      callback = None,
      verifier = accessToken.flatMap(_.verifier),
      accessToken.map(_.token)
    )
  }
}
