# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Security

### Added

### Changed

### Fixed

## [0.3.7] - 2022-01-11
### Added
- First version of GUI for MetaStore.

## [0.3.6] - 2021-12-18
### Security
- Fix for CWE-611, CWE-776, CWE-827, CWE-352

### Added
- Code and Security analysis
- Make CORS partly configurable

### Changed
- Update libraries:
  - Spring Boot 2.4.13
  - Spring Doc  1.5.13
  - Javers 6.5.3
  - DependencyCheck 6.5.0.1
  - json-schema-validator 1.0.64
  - netflix-eureka-client 3.0.5  
  - service-base 0.3.2
  - repo-core 0.9.2
 
## [0.3.5] - 2021-12-13
### Security
- Fix for CVE-2021-44228

### Fixed
- OAS 3.0 is not available (issue #58)
- Incorrect schema version in the metadata record (issue #59) 
- XML validation is not threadsafe. (issue #60)

### Changed
- Update to service-base version 0.3.1

## [0.3.4] - 2021-11-17
### Fixed
- Error handling versions for metadata documents (issue #55)
### Changed
- Building service all tests now enabled by default (use -Dprofile=minimal to execute only mandatory tests)
- Update to repo-core version 0.9.1

## [0.3.3] - 2021-11-12
### Fixed
- Error handling versions for (schema/metadata) records (issues #52, #53)

## [0.3.2] - 2021-10-25
### Fixed
- Error regarding persistent identifiers.

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

[Unreleased]: https://github.com/kit-data-manager/metastore2/compare/v0.3.6...HEAD
[0.3.6]: https://github.com/kit-data-manager/metastore2/compare/v0.3.5...v0.3.6
[0.3.5]: https://github.com/kit-data-manager/metastore2/compare/v0.3.4...v0.3.5
[0.3.4]: https://github.com/kit-data-manager/metastore2/compare/v0.3.3...v0.3.4
[0.3.3]: https://github.com/kit-data-manager/metastore2/compare/v0.3.2...v0.3.3
[0.3.2]: https://github.com/kit-data-manager/metastore2/compare/v0.3.1...v0.3.2
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

