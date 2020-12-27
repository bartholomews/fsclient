//package io.bartholomews.fsclient.client
//
//import cats.effect.{ContextShift, IO}
//import io.bartholomews.fsclient.config.UserAgent
//import io.bartholomews.fsclient.entities.oauth.SignerV1
//import org.http4s.client.oauth1.Consumer
//
//import scala.concurrent.ExecutionContext
//
//object ClientData {
//
//  implicit val ec: ExecutionContext = ExecutionContext.global
//  implicit val cs: ContextShift[IO] = IO.contextShift(ec)
//
//  private val sampleUserAgent: UserAgent = UserAgent(
//    appName = "SAMPLE_APP_NAME",
//    appVersion = Some("SAMPLE_APP_VERSION"),
//    appUrl = Some("https://bartholomews.io/sample-app-url")
//  )
//
//  private val sampleConsumer = Consumer(key = "SAMPLE_CONSUMER_KEY", secret = "SAMPLE_CONSUMER_SECRET")
//
//  val clientNoAuth: FClientNoAuth[IO] = FClientNoAuth(sampleUserAgent)
//
//  val clientV1Basic: FsClient[IO, SignerV1] = FsClientV1.basic(sampleUserAgent, sampleConsumer)
//
//  // FIXME: There is not point in having a V2 Token signer attached to the client;
//  //  it needs to be refreshed so need to be dynamic at def level;
//  //  only other OAuth modes like `ClientCredentials` and `Implicit Grant` make sense here (in FsClient ADT),
//  //  so it would be nice to enforce that
////  val clientV2: IOClient[OAUthV2] = new IOClient[OAUthV2] {
////    override def appConfig: FsClientConfig[OAUthV2] = new FsClientConfig[OAUthV2](
////      sampleUserAgent, SignerV2(
////        sampleTokenEndpoint,
////        sampleClientPassword,
////        TestudoF
////      )
////    )
////  }
//
//}
