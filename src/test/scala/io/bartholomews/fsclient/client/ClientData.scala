package io.bartholomews.fsclient.client

import cats.effect.{ContextShift, IO}
import io.bartholomews.fsclient.entities.OAuthVersion.OAuthV1
import io.bartholomews.testudo.data.TestudoClientData

import scala.concurrent.ExecutionContext

object ClientData extends TestudoClientData {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)

  val clientNoAuth: FClientNoAuth[IO] = FClientNoAuth(sampleUserAgent)

  val clientV1Basic: FsClient[IO, OAuthV1] = FsClientV1.basic(sampleUserAgent, sampleConsumer)

  // FIXME: There is not point in having a V2 Token signer attached to the client;
  //  it needs to be refreshed so need to be dynamic at def level;
  //  only other OAuth modes like `ClientCredentials` and `Implicit Grant` make sense here (in FsClient ADT),
  //  so it would be nice to enforce that
//  val clientV2: IOClient[OAUthV2] = new IOClient[OAUthV2] {
//    override def appConfig: FsClientConfig[OAUthV2] = new FsClientConfig[OAUthV2](
//      sampleUserAgent, SignerV2(
//        sampleTokenEndpoint,
//        sampleClientPassword,
//        TestudoF
//      )
//    )
//  }

}
