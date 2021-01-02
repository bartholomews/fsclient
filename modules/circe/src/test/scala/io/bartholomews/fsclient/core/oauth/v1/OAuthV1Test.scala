package io.bartholomews.fsclient.core.oauth.v1

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlMatching}
import io.bartholomews.fsclient.client.ClientData.{sampleConsumer, sampleRedirectUri, sampleUserAgent}
import io.bartholomews.fsclient.client.IdentityClient
import io.bartholomews.fsclient.core.oauth.v1.OAuthV1.{SignatureMethod, Token}
import io.bartholomews.fsclient.core.oauth.{
  RequestTokenCredentials,
  ResourceOwnerAuthorizationUri,
  TemporaryCredentialsRequest
}
import io.bartholomews.fsclient.wiremock.WiremockServer
import org.scalatest.Inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sttp.client3.{Identity, Response, ResponseException, UriContext}
import sttp.model.Method

class OAuthV1Test extends AnyWordSpec with IdentityClient with WiremockServer with Matchers with Inside {

  "OAuthV1" should {

    val temporaryCredentialsRequest = TemporaryCredentialsRequest(
      sampleConsumer,
      sampleRedirectUri,
      SignatureMethod.SHA1
    )

    // 1. Retrieve a request token
    def maybeTemporaryCredentials
      : Identity[Response[Either[ResponseException[String, Exception], TemporaryCredentials]]] =
      temporaryCredentialsRequest.send(
        Method.POST,
        serverUri = uri"$wiremockBaseUri/oauth/request-token",
        sampleUserAgent,
        ResourceOwnerAuthorizationUri(uri"https://some-server/oauth/authorize")
      )

    "get temporary credentials" in {

      val responseBody =
        "oauth_token=AAA&oauth_token_secret=BBB&oauth_callback_confirmed=true"

      stubFor(
        post(urlMatching("/oauth/request-token"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(responseBody)
          )
      )

      inside(maybeTemporaryCredentials.body) { case Right(temporaryCredentials) =>
        temporaryCredentials shouldBe TemporaryCredentials(
          consumer = sampleConsumer,
          token = Token("AAA", "BBB"),
          callbackConfirmed = true,
          ResourceOwnerAuthorizationUri(uri"https://some-server/oauth/authorize")
        )
      }
    }

    "get request token credentials" in {

      val temporaryCredentials =
        TemporaryCredentials(
          consumer = sampleConsumer,
          token = Token("AAA", "BBB"),
          callbackConfirmed = true,
          ResourceOwnerAuthorizationUri(uri"https://some-server/oauth/authorize")
        )

      val maybeRequestTokenCredentials = RequestTokenCredentials.fetchRequestTokenCredentials(
        sampleRedirectUri.value.withParams(
          ("oauth_token", "AAA"),
          ("oauth_verifier", "ZZZ")
        ),
        temporaryCredentials,
        SignatureMethod.PLAINTEXT
      )

      maybeRequestTokenCredentials shouldBe Right(
        RequestTokenCredentials(
          temporaryCredentials.token,
          verifier = "ZZZ",
          sampleConsumer,
          SignatureMethod.PLAINTEXT
        )
      )
    }
  }
}
