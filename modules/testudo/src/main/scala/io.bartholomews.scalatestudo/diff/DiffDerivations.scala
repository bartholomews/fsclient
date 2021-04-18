package io.bartholomews.scalatestudo.diff

import com.softwaremill.diffx.scalatest.DiffMatcher
import com.softwaremill.diffx.{Derived, Diff, DiffResult, DiffResultObject, DiffResultValue, FieldPath, Identical}
import io.bartholomews.fsclient.core.oauth.v1.OAuthV1.{Consumer, Token}
import io.bartholomews.fsclient.core.oauth.v1.TemporaryCredentials
import io.bartholomews.fsclient.core.oauth.v2.OAuthV2.{AccessToken, RefreshToken}
import io.bartholomews.fsclient.core.oauth.{
  AccessTokenSigner,
  NonRefreshableTokenSigner,
  ResourceOwnerAuthorizationUri,
  Scope
}
import sttp.model.Uri

trait DiffDerivations extends DiffMatcher {

  implicit val uriDiff: Diff[Uri] = Diff[String].contramap[Uri](_.toString)
  implicit val booleanDiff: Diff[Boolean] =
    (left: Boolean, right: Boolean, _: List[FieldPath]) =>
      if (left == right) Identical(left) else DiffResultValue(left, right)

  implicit val accessTokenDiff: Diff[AccessToken] = Diff.derived[AccessToken]
  implicit val consumerDiff: Diff[Consumer] = Diff.derived[Consumer]
  implicit val resourceOwnerAuthorizationUriDiff: Diff[ResourceOwnerAuthorizationUri] =
    Diff.derived[ResourceOwnerAuthorizationUri]
  implicit val temporaryCredentialsDiff: Diff[TemporaryCredentials] = Diff.derived[TemporaryCredentials]
  implicit val tokenDiff: Diff[Token] = Diff.derived[Token]
  implicit val refreshTokenDiff: Diff[RefreshToken] = Diff.derived[RefreshToken]
  implicit val scopeDiff: Diff[Scope] = Diff.derived[Scope]

  implicit val accessTokenSignerDiff: Diff[AccessTokenSigner] =
    Derived[Diff[AccessTokenSigner]](Diff.derived[AccessTokenSigner])
      .ignore[AccessTokenSigner, Long](_.generatedAt)

  implicit val nonRefreshableTokenSignerDiff: Diff[NonRefreshableTokenSigner] =
    Derived[Diff[NonRefreshableTokenSigner]](Diff.derived[NonRefreshableTokenSigner])
      .ignore[NonRefreshableTokenSigner, Long](_.generatedAt)

  /**
   * implicit val diffCaseClassWithPrivateConstructor: Diff[CaseClassWithPrivateConstructor] =
   *    (left: CaseClassWithPrivateConstructor, right: CaseClassWithPrivateConstructor, _) => {
   *      fromObject(
   *        left,
   *        DiffResultObject(
   *            name = "CaseClassWithPrivateConstructor",
   *            fields = Map(
   *                "fieldA" -> Diff[FieldA].apply(left.fieldA, right.fieldA),
   *                "fieldB" -> `Diff[Option[FieldB]]`.apply(left.fieldB, right.fieldB)
   *              )
   *       )
   *    )
   * }
   *
   * @param value the `T` value to test
   * @param diffResultObject the DiffResultObject implementation for the specific type T
   * @tparam T the type of the object to write the custom diff
   * @return a custom `DiffResult` for `T`
   */
  def fromObject[T](value: T, diffResultObject: DiffResultObject): DiffResult =
    diffResultObject.fields.values
      .collectFirst({ case result if !result.isIdentical => diffResultObject })
      .getOrElse(Identical(value))
}

object DiffDerivations extends DiffDerivations
