# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
### Removed
- `FsApiClient`
### Changed
- `AccessTokenSigner` using `currentTimeMillis` instead of `nanoTime`, and has `refreshToken` required
- `baseRequest` taking an `userAgent` instead of `client`
### Fixed

## [0.1.1] - 2021-01-30
### Added
- `play` module
- some entity codecs
### Changed
- Migrated to sttp v3
- Removed implicit sttp backend, it needs to be passed explicitly now
- `AccessTokenSigner` using `nanoTime` instead of `currentTimeMillis`
### Fixed
- `AuthorizationRequest` preserving given query parameters

## [0.1.0] - 2021-01-02
### Changed
- Switched to multi-project setup: `fsclient-core` and `fsclient-circe`

## [0.1.0] - 2020-12-29
### Removed
- Most of the implementation, which has been migrated to sttp
### Changed
- Major refactor, this is now mostly a wrapper around sttp with oauth and other utils on top of it

## [0.0.3] - 2020-11-06
### Added
- `PUT` / `DELETE` requests and `Unit` decoders
### Removed
- Removed `Logger` name from config (to avoid concurrency error when using multiple clients)
- Removed implicit attribute to `deriveJsonPipe` in `CodecSyntax` to avoid diverging expansion 
### Changed
- Updated [Readme](https://github.com/bartholomews/fsclient/compare/v0.0.2...HEAD#diff-04c6e90faac2675aa89e2176d2eec7d8) scala snippets
- Decode < 400 as `success` status instead of just 200
- Renamed `FsRequest` objects to `FsSimple/Auth` and providing some implicit codecs
- Updated various dependencies

## [0.0.2] - 2020-05-23
### Added
- Various breaking changes including:

    - Additional type parameter in `client: FsClient[F[_], S <: Signer]`
    - `TemporaryCredentialsRequest` for handling the first phase of OAuth v1 authentication/callback
    - `ClientPasswordBasicAuthenticationV2` signer for OAuth v2 basic signature
    - `generatedAt` is now part of `SignerV2` entities, so they can be serialized into a session cookie easily

## [0.0.1] - 2020-05-03
### Added
- This is the first release of `fsclient`.

[Unreleased]: https://github.com/bartholomews/fsclient/compare/v0.1.1...HEAD
[0.1.1]: https://github.com/bartholomews/fsclient/compare/v0.0.3...v0.1.1
[0.1.0]: https://github.com/bartholomews/fsclient/compare/v0.0.3...v0.1.0
[0.0.3]: https://github.com/bartholomews/fsclient/compare/v0.0.2...v0.0.3
[0.0.2]: https://github.com/bartholomews/fsclient/compare/v0.0.1...v0.0.2
[0.0.1]: https://github.com/bartholomews/fsclient/releases/tag/v0.0.1