package fsclient.client.io_client

import fsclient.config.AppConsumer
import fsclient.entities.{OAuthEnabled, OAuthToken}

import scala.concurrent.ExecutionContext

// TODO: Load secret and key from config / env var / etc. so you give a purpose to this class
//  instead of just using the simple client passing the token on auth defs
/**
 * Class which executes authenticated calls by default,
 * without the need to pass the `accessToken` on each request.
 */
class IOAuthClient(override val consumer: AppConsumer)(implicit val ec: ExecutionContext, token: OAuthToken)
    extends IOBaseClient(consumer) {

  val oAuthInfo: OAuthEnabled = OAuthEnabled(token)
}
