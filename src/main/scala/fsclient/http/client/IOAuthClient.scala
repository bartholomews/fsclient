package fsclient.http.client

import fsclient.config.OAuthConsumer
import fsclient.entities.AccessToken

import scala.concurrent.ExecutionContext

class IOAuthClient(override val consumer: OAuthConsumer, accessToken: AccessToken)
                  (implicit val ec: ExecutionContext) extends IOClient(consumer, Some(accessToken)) {}