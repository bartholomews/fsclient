# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- `FsJsonRequest[A]` trait which imports an `EntityEncoder`
- `PUT` / `DELETE` requests and `Unit` decoders
### Removed
- Logger name from config (to avoid concurrency error when using multiple clients)
- Removed implicit attribute to `deriveJsonPipe` in `CodecSyntax` to avoid diverging expansion 
- `FsSimpleRequest` and `FsAuthRequest` objects are not public anymore:
   The accessible objects to create requests are
   - `SimpleRequest`, `JsonRequest`, `PlainTextRequest`
   - `AuthRequest`, `AuthJsonRequest`, `AuthPlainTextRequest`
### Changed
- Updated [Readme](https://github.com/bartholomews/fsclient/compare/v0.0.2...HEAD#diff-04c6e90faac2675aa89e2176d2eec7d8) scala snippets
- Decode < 400 as `success` status instead of just 200
- Renamed `FsRequest` objects to `FsSimple/Auth`
### Fixed

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

[Unreleased]: https://github.com/bartholomews/fsclient/compare/v0.0.2...HEAD
[0.0.2]: https://github.com/bartholomews/fsclient/compare/v0.0.1...v0.0.2
[0.0.1]: https://github.com/bartholomews/fsclient/releases/tag/v0.0.1