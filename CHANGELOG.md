# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

### Changed

### Fixed

## [0.3.1] - 2021-10-20
### Fixed
- Error starting docker due to missing bash.

## [0.3.0] - 2021-10-16
- The code is now completely relying on the library of the kit-data-manager.
### Added
- Authentication based on JWT powered by KIT Data Manager. (issue #4)
- Metadata (schema) records now also versioned.
- OAI PMH protocol added (issue #6)
- Customization is enabled even if framework will be started via docker (issue #41)
### Changed
- Metadata are now linked to specific version of a schema. (issue #30)
- Attribute 'locked' from MetadataSchemaRecord changed to 'doNotSync' (issue #37)
- Change in related schema and data (add identifier type to identifier)
- Store all identifiers as global identifiers (type INTERNAL -> type URL)
- For registering a schema mimetype is no longer mandatory.
- Switch to gradle version 7.2
- Update to Spring Boot 2.4.10
- Update to service-base version 0.3.0
### Fixed
- Filtering metadata documents by resourceId, schemaId
- Filtering schema documents by mimetype
- Error while updating json schema documents without schema
- Speedup guessing type for schema. 
- Updating document without changes will no longer create new version (issue #27)
- Update schema via POST (issue #28)
- Add hash of schema documents to record. (issue #38)
- Drop tables at startup (default).

## [0.2.4] - date 2020-12-16
### Added
- Support for messaging
### Changed
- Update to service-base version 0.2.0

## [0.2.3] - date 2020-12-07
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

[Unreleased]: https://github.com/kit-data-manager/metastore2/compare/v0.3.1...HEAD
[0.3.1]: https://github.com/kit-data-manager/metastore2/compare/v0.3.0...v0.3.1
[0.3.0]: https://github.com/kit-data-manager/metastore2/compare/v0.2.4...v0.3.0
[0.2.4]: https://github.com/kit-data-manager/metastore2/compare/v0.2.3...v0.2.4
[0.2.3]: https://github.com/kit-data-manager/metastore2/compare/v0.2.2...v0.2.3
[0.2.2]: https://github.com/kit-data-manager/metastore2/compare/v0.2.1...v0.2.2
[0.2.1]: https://github.com/kit-data-manager/metastore2/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/kit-data-manager/metastore2/compare/v0.1.2...v0.2.0
[0.1.2]: https://github.com/kit-data-manager/metastore2/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/kit-data-manager/metastore2/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/kit-data-manager/metastore2/releases/tag/v0.1.0

