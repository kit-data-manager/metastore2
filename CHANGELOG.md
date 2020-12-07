# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Fixed
- New JSON schema will be validated by linked meta schema.
### Changed
- Metadata will be stored in a hierarchical structure 

## [0.2.2] - date 2020-12-04
### Fixed
- Docker container loses all data when stopping and restarting the container
- Update of the schema when the service runs in a docker container is now possible.

## [0.2.1] - date 2020-11-30
### Fixed
- Error serializing dates of metadata record
- Input for REST-API should be a file 
### Changed
- Update to service-base version 0.1.3

## [0.2.0] - date 2020-10-20
### Added
- Support for JSON

## [0.1.2] - date 2020-10-16
### Added
- Release management
### Fixed
- Build script no longer depends on git.

## [0.1.1] - date 2020-10-14
### Added
- Dockerization

### Fixed
- Typos in documentation
- Swagger-UI (POST methods now handled correct but still broken) 

## [0.1.0] - date 2020-08-07
### Added
- Registry for XSD files and support for XML metadata

[Unreleased]: https://github.com/kit-data-manager/metastore2/compare/v0.2.2...HEAD
[0.2.2]: https://github.com/kit-data-manager/metastore2/compare/v0.2.1...v0.2.2
[0.2.1]: https://github.com/kit-data-manager/metastore2/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/kit-data-manager/metastore2/compare/v0.1.2...v0.2.0
[0.1.2]: https://github.com/kit-data-manager/metastore2/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/kit-data-manager/metastore2/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/kit-data-manager/metastore2/releases/tag/v0.1.0

