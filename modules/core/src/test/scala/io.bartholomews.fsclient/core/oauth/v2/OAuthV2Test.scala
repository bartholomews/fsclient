package io.bartholomews.fsclient.core.oauth.v2

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalTo, post, stubFor, urlMatching}
import io.bartholomews.fsclient.client.ClientData.{
  sampleAuthorizationCode,
  sampleClientPassword,
  sampleRedirectUri,
  sampleRefreshToken,
  sampleUserAgent
}
import io.bartholomews.fsclient.client.IdentityClient
import io.bartholomews.fsclient.core.FsClient
import io.bartholomews.fsclient.core.http.SttpResponses.ResponseHandler
import io.bartholomews.fsclient.core.oauth.v2.OAuthV2.{
  AccessToken,
  AuthorizationCodeGrant,
  ClientCredentialsGrant,
  ImplicitGrant
}
import io.bartholomews.fsclient.core.oauth.{AccessTokenSigner, ClientPasswordAuthentication, NonRefreshableTokenSigner}
import io.bartholomews.fsclient.wiremock.WiremockServer
import org.scalatest.Inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sttp.client3.{Identity, UriContext}
import sttp.model.Uri
import sttp.model.Uri.QuerySegment.KeyValue
import sttp.model.Uri.{QuerySegment, QuerySegmentEncoding}

abstract class OAuthV2Test[Encoder[_], Decoder[_], DE]
    extends AnyWordSpec
    with IdentityClient
    with WiremockServer
    with Matchers
    with Inside {

  implicit def responseHandler[T](implicit decoder: Decoder[T]): ResponseHandler[DE, T]

  implicit def accessTokenSignerDecoder: Decoder[AccessTokenSigner]
  implicit def nonRefreshableTokenSignerDecoder: Decoder[NonRefreshableTokenSigner]

  "OAuthV2" when {

    val signer = ClientPasswordAuthentication(sampleClientPassword)

    val client: FsClient[Identity, ClientPasswordAuthentication] = FsClient.v2.clientPassword(
      sampleUserAgent,
      signer
    )(backend)

    "AuthorizationCodeGrant" should {

      val authorizationCodeRequest = AuthorizationCodeRequest(
        clientId = client.signer.clientPassword.clientId,
        redirectUri = sampleRedirectUri,
        state = None,
        scopes = List.empty
      )

      val authorizationRequestUri: Uri =
        AuthorizationCodeGrant.authorizationRequestUri(
          authorizationCodeRequest,
          serverUri = uri"$wiremockBaseUri/oauth/code"
        )

      "set the correct host and path" in {
        authorizationRequestUri.copy(querySegments = List.empty) shouldBe uri"$wiremockBaseUri/oauth/code"
      }

      "set the correct query params" in {
        authorizationRequestUri.querySegments.toList shouldBe List(
          KeyValue("client_id", sampleClientPassword.clientId.value),
          KeyValue("response_type", "code"),
          KeyValue("redirect_uri", sampleRedirectUri.value.toString)
        )
      }

      "return an error if user denies permissions" in {

        val uriRedirect = sampleRedirectUri.value.addQuerySegment(
          QuerySegment.KeyValue("error", "temporarily_unavailable")
        )

        AuthorizationCodeGrant.authorizationResponse(
          request = authorizationCodeRequest,
          redirectionUriResponse = uriRedirect
        ) shouldBe Left("temporarily_unavailable")
      }

      "return the authorization code if user accepts permissions" in {

        val uriRedirect = sampleRedirectUri.value.addQuerySegment(
          QuerySegment.KeyValue("code", sampleAuthorizationCode.value)
        )

        AuthorizationCodeGrant.authorizationResponse(
          request = authorizationCodeRequest,
          redirectionUriResponse = uriRedirect
        ) shouldBe Right(sampleAuthorizationCode)
      }

      "return an access token" in {

        import java.net.URLEncoder.encode

        val expectedRequestBody = List(
          "grant_type=authorization_code",
          s"code=${sampleAuthorizationCode.value}",
          s"redirect_uri=${encode(sampleRedirectUri.value.toString, "UTF-8")}"
        ).mkString("&")

        stubFor(
          post(urlMatching("/oauth/authorize"))
            .withRequestBody(equalTo(expectedRequestBody))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withBodyFile("auth/authorization_access.json")
            )
        )

        val response = backend.send(
          AuthorizationCodeGrant
            .accessTokenRequest[DE](
              serverUri = uri"$wiremockBaseUri/oauth/authorize",
              sampleAuthorizationCode,
              Some(sampleRedirectUri),
              sampleClientPassword
            )
        )

        inside(response.body) { case Right(accessTokenSigner) =>
          accessTokenSigner.accessToken shouldBe AccessToken("some-access-token")
        }
      }

      "return a refresh token" in {

        val expectedRequestBody = List(
          "grant_type=refresh_token",
          s"refresh_token=${sampleRefreshToken.value}"
        ).mkString("&")

        stubFor(
          post(urlMatching("/oauth/refresh"))
            .withRequestBody(equalTo(expectedRequestBody))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withBodyFile("auth/authorization_access.json")
            )
        )

        val response = backend.send(
          AuthorizationCodeGrant
            .refreshTokenRequest[DE](
              serverUri = uri"$wiremockBaseUri/oauth/refresh",
              sampleRefreshToken,
              scopes = List.empty,
              sampleClientPassword
            )
        )

        inside(response.body) { case Right(accessTokenSigner) =>
          accessTokenSigner.accessToken shouldBe AccessToken("some-access-token")
        }
      }
    }

    "ImplicitGrant" should {

      val authorizationTokenRequest = AuthorizationTokenRequest(
        clientId = client.signer.clientPassword.clientId,
        redirectUri = sampleRedirectUri,
        state = None,
        scopes = List.empty
      )

      val authorizationRequestUri: Uri =
        ImplicitGrant.authorizationRequestUri(
          authorizationTokenRequest,
          serverUri = uri"$wiremockBaseUri/oauth/token"
        )

      "set the correct host and path" in {
        authorizationRequestUri.copy(querySegments = List.empty) shouldBe
          uri"$wiremockBaseUri/oauth/token"
      }

      "set the correct query params" in {
        authorizationRequestUri.querySegments.toList shouldEqual List(
          KeyValue("client_id", sampleClientPassword.clientId.value),
          KeyValue("response_type", "token", QuerySegmentEncoding.Standard),
          KeyValue("redirect_uri", sampleRedirectUri.value.toString)
        )
      }

      "return an error if user denies permissions" in {

        val uriRedirect = sampleRedirectUri.value.addQuerySegment(
          QuerySegment.KeyValue("error", "temporarily_unavailable")
        )

        ImplicitGrant.accessTokenResponse(
          request = authorizationTokenRequest,
          redirectionUriResponse = uriRedirect
        ) shouldBe Left("temporarily_unavailable")
      }

      "return the authorization code if user accepts permissions" in {

        val uriRedirect = sampleRedirectUri.value.addParams(
          ("access_token", "implicit-grant-access-token"),
          ("token_type", "bearer"),
          ("expires_in", "3600")
        )

        val response = ImplicitGrant.accessTokenResponse(
          request = authorizationTokenRequest,
          redirectionUriResponse = uriRedirect
        )

        inside(response) { case Right(nonRefreshableTokenSigner) =>
          nonRefreshableTokenSigner.accessToken shouldBe AccessToken("implicit-grant-access-token")
        }
      }
    }

    "ClientCredentialsGrant" should {

      val accessTokenRequest =
        ClientCredentialsGrant.accessTokenRequest[DE](
          serverUri = uri"$wiremockBaseUri/oauth/token",
          sampleClientPassword
        )

      "return an access token" in {
        stubFor(
          post(urlMatching("/oauth/token"))
            .withRequestBody(equalTo("grant_type=client_credentials"))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withBodyFile("auth/client_credentials.json")
            )
        )

        inside(backend.send(accessTokenRequest).body) { case Right(nonRefreshableTokenSigner) =>
          nonRefreshableTokenSigner.accessToken shouldBe AccessToken("some-access-token")
        }
      }
    }
  }
}
