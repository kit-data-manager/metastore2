# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Security

### Added
- Add landing page redirecting to configurable site.

### Changed
- Bump edu.kit.datamanager:repo-core from 1.1.2 to 1.2.0
- Bump edu.kit.datamanager:service-base from 1.1.1 to 1.2.0 
- Bump javersVersion from 6.14.0 to 7.0.0
- Bump org.springframework.boot from 2.7.10 to 3.1.0
- Bump org.springframework.cloud:spring-cloud-starter-config from 3.1.7 to 4.0.3
- Bump org.springframework.cloud:spring-cloud-starter-netflix-eureka-client from 3.1.6 to 4.0.2 
- Bump org.springframework.data:spring-data-elasticsearch from 4.4.13 to 5.1.0
- Bump org.springframework.restdocs:spring-restdocs-mockmvc from 2.0.7.RELEASE to 3.0.0
- Bump springDocVersion from 1.7.0 to 2.1.0
 
### Fixed

## [1.2.3] - 2023-04-13
### Added
- Add CITATION.cff
- Allow cli arguments for start script.
- Enable configuration to organize internal storage of metadata documents (https://github.com/kit-data-manager/metastore2/issues/241)

### Changed
- Update several badges
- Bump com.networknt:json-schema-validator from 1.0.78 to 1.0.79
- Bump edu.kit.datamanager:repo-core from 1.1.1 to 1.1.2
- Bump edu.kit.datamanager:service-base from 1.1.0 to 1.1.1 
- Bump gradle from 7.6 to 7.6.1.
- Bump io.freefair.maven-publish-java from 6.6.3 to 8.0.1 
- Bump io.freefair.lombok from 6.6.3 to 8.0.1 
- Bump org.mockito:mockito-core from 5.1.1 to 5.3.0 
- Bump org.owasp.dependencycheck from 8.1.0 to 8.2.1
- Bump org.postgresql:postgresql from 42.5.4 to 42.6.0 
- Bump org.springframework.boot from 2.7.9 to 2.7.10
- Bump org.springframework.cloud:spring-cloud-starter-config from 3.1.5 to 3.1.6.
- Bump org.springframework.data:spring-data-elasticsearch from 4.4.8 to 4.4.10.
- Bump org.springframework:spring-messaging from 5.3.25 to 5.3.26
- Bump springDocVersion from 1.6.14 to 1.7.0

### Fixed
- Providing (invalid) version number while updating schema document may break old versions. (https://github.com/kit-data-manager/metastore2/issues/245)
- Trace log may slow down service. (https://github.com/kit-data-manager/metastore2/issues/233)
- Calling REST-Endpoint for UI fails if no page information is provided. (https://github.com/kit-data-manager/metastore2/issues/264)
- Problem running MetaStore standalone in a docker container. (https://github.com/kit-data-manager/metastore2/issues/270)

## [1.2.2] - 2023-02-28
### Fixed
- Add configuration to use the service behind a proxy. (https://github.com/kit-data-manager/metastore2/issues/218)
- Typos in README

### Changed
- Improve documentation on setting up the MetaStore framework using Docker.
- Document directories are now set up as subfolders of the installation directory by default.
- Bump io.freefair.lombok from 6.6.1 to 6.6.3
- Bump io.freefair.maven-publish-java from 6.6.1 to 6.6.3
- Bump javersVersion from 6.9.1 to 6.11.0
- Bump json-schema-validator from 1.0.76 to 1.0.77
- Bump org.postgresql:postgresql from 42.5.3 to 42.5.4
- Bump org.springframework.boot from 2.7.8 to 2.7.9
- Bump spring-cloud-gateway-mvc from 3.1.5 to 3.1.6
- Bump spring-cloud-starter-netflix-eureka-client from 3.1.4 to 3.1.5
- Bump spring-data-elasticsearch from 4.4.7 to 4.4.8

## [1.2.1] - 2023-02-13
### Added
- Add docker compose file for building whole framework (MetaStore, elasticsearch and UI)

### Fixed
- Fix wrong document format for indexing metadata documents. (https://github.com/kit-data-manager/metastore2/issues/208)

### Changed
- Bump javersVersion from 6.8.2 to 6.9.1
- Bump org.apache.tika:tika-core from 2.6.0 to 2.7.0 
- Bump org.mockito:mockito-core from 5.1.0 to 5.1.1 
- Bump org.owasp.dependencycheck from 8.0.2 to 8.1.0
- Bump org.postgresql:postgresql from 42.5.1 to 42.5.3

## [1.2.0] - 2023-02-03
### Security

### Added
- Add proxy for authenticated search via elasticsearch
- Add commandline parser for reindexing elasticsearch
- Add actuator endpoints for info and health (https://github.com/kit-data-manager/metastore2/issues/184)
- Add spring-data-elasticsearch 4.4.7

### Changed
- Bump commons-text from 1.9 to 1.10.0
- Bump gradle from 7.5.1 to 7.6
- Bump httpclient from 4.5.13 to 4.5.14 
- Bump io.freefair.lombok from 6.5.1 to 6.6.1
- Bump io.freefair.maven-publish-java from 6.5.1 to 6.6.1 
- Bump io.spring.dependency-management from 1.0.14.RELEASE to 1.1.0 
- Bump javersVersion from 6.6.5 to 6.8.2 
- Bump json-schema-validator from 1.0.73 to 1.0.76 
- Bump mockito-core from 4.8.0 to 5.1.0 
- Bump org.owasp.dependencycheck from 7.2.1 to 8.0.2 
- Bump org.springframework.boot from 2.7.4 to 2.7.8
- Bump postgresql from 42.5.0 to 42.5.1
- Bump repo-core from 1.0.4 to 1.1.1 
- Bump service-base from 1.0.7 to 1.1.0 
- Bump springDocVersion from 1.6.11 to 1.6.14
- Bump spring-boot from 2.7.4 to 2.7.6
- Bump spring-cloud-starter-config from 3.1.4 to 3.1.5
- Bump spring-cloud-gateway-mvc from 3.1.4 to 3.1.5
- Bump spring-messaging from 5.3.23 to 5.3.25 
- Bump spring-restdocs-mockmvc:from 2.0.6.RELEASE to 2.0.7.RELEASE
- Bump spring-security-config from 5.5.2 to 5.7.5
- Bump spring-security-web from 5.7.2 to 5.7.5
- Bump tika-core from 1.2.7 to 2.6.0 
- Bump xercesImpl from 2.12.1 to 2.12.2 

### Fixed
- Invalid input for resource identifier causes NPE (https://github.com/kit-data-manager/metastore2/issues/198)
- Hibernate validation was not enabled by default. (https://github.com/kit-data-manager/metastore2/issues/191)
- Check metadata directory for valid entry during startup (https://github.com/kit-data-manager/metastore2/issues/185)

## [1.1.0] - 2022-10-17
### Security
- Switch to 'eclipse-temurin' for docker due to end of support for 'openjdk'.

### Added
- More specific messages for creating/updating metadata documents.
- Add ACL info for services to enable authorization.
- Support for Keycloak tokens.

### Changed
- Update to service-base 1.0.7
- Update to repo-core 1.0.4
- Update to javers 6.6.5
- Update to io.freefair.lombok 6.5.1
- Update to org.owasp.dependencycheck 7.2.1
- Update to spring-boot 2.7.4
- Update to spring-doc 1.6.11
- Update to spring-cloud 3.1.4
- Update to spring-messaging 5.3.23
- Update to postgresql 42.5.0
- Update to h2 2.1.214
- Update to gradle version 7.5.1
- Get rid of powermock
- Support for Java 17 (tests)
- Remove jwt libraries (already part of service-base).

### Fixed

## [1.0.1] - 2022-06-17
### Security
- Update to h2 2.1.212:
  - Please migrate your database if you want to update MetaStore while using h2!
    See: https://h2database.com/html/migration-to-v2.html 

### Changed
- Update to service-base 1.0.3
- Changed some labels in GUI

### Fixed
- Docker: Add support for M1 chip architecture (https://github.com/kit-data-manager/metastore2/issues/107)
- Access public documents of other users is broken.(https://github.com/kit-data-manager/metastore2/issues/100)
- Fix bug ignoring type of related resource. (https://github.com/kit-data-manager/metastore2/issues/105)
- Fix bug not hiding revoked resources in listings.
- Fix bugs using Swagger UI for REST calls.
- Fix typos in documentation.

## [1.0.0] - 2022-03-29
### Added
- Finalized version of MetaStoreGui
  - Fix https://github.com/kit-data-manager/metastore2/issues/69
- Allow also IDs for metadata documents (https://github.com/kit-data-manager/metastore2/issues/76)
- Access filter for monitoring.

### Changed
- Update to repo-core 1.0.2
- Update to service-base 1.0.1
- Update to postgresql 42.2.25
- Downgrade library due to some issues regarding validation
  - json-schema-validator 1.0.64. -> 1.0.59 (https://github.com/kit-data-manager/metastore2/issues/77)

### Fixed
- Fix bug listing resources without proper authorization (https://github.com/kit-data-manager/metastore2/issues/71)
- Fix bug listing all metadata documents related to a specific schema
- Fix a bug that can cause the metadata document/schema to become invalid due to an update. (https://github.com/kit-data-manager/metastore2/issues/78)
- Fix bug with path in Windows. (https://github.com/kit-data-manager/metastore2/issues/88)
- CSRF is now disabled by default. (https://github.com/kit-data-manager/metastore2/issues/70) 
- Check ACLs while creating/updating records (https://github.com/kit-data-manager/metastore2/issues/39)
- Added missing spaces to swagger-ui.

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
- OAS 3.0 is not available (https://github.com/kit-data-manager/metastore2/issues/58)
- Incorrect schema version in the metadata record (https://github.com/kit-data-manager/metastore2/issues/59) 
- XML validation is not threadsafe. (https://github.com/kit-data-manager/metastore2/issues/60)

### Changed
- Update to service-base version 0.3.1

## [0.3.4] - 2021-11-17
### Fixed
- Error handling versions for metadata documents (https://github.com/kit-data-manager/metastore2/issues/55)
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
- Authentication based on JWT powered by KIT Data Manager. (https://github.com/kit-data-manager/metastore2/issues/4)
- Metadata (schema) records now also versioned.
- OAI PMH protocol added (https://github.com/kit-data-manager/metastore2/issues/6)
- Customization is enabled even if framework will be started via docker (https://github.com/kit-data-manager/metastore2/issues/41)
### Changed
- Metadata are now linked to specific version of a schema. (https://github.com/kit-data-manager/metastore2/issues/30)
- Attribute 'locked' from MetadataSchemaRecord changed to 'doNotSync' (https://github.com/kit-data-manager/metastore2/issues/37)
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
- Updating document without changes will no longer create new version (https://github.com/kit-data-manager/metastore2/issues/27)
- Update schema via POST (https://github.com/kit-data-manager/metastore2/issues/28)
- Add hash of schema documents to record. (https://github.com/kit-data-manager/metastore2/issues/38)
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

[Unreleased]: https://github.com/kit-data-manager/metastore2/compare/v1.2.3...HEAD
[1.2.3]: https://github.com/kit-data-manager/metastore2/compare/v1.2.2...v1.2.3
[1.2.2]: https://github.com/kit-data-manager/metastore2/compare/v1.2.1...v1.2.2
[1.2.1]: https://github.com/kit-data-manager/metastore2/compare/v1.2.0...v1.2.1
[1.2.0]: https://github.com/kit-data-manager/metastore2/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/kit-data-manager/metastore2/compare/v1.0.1...v1.1.0
[1.0.1]: https://github.com/kit-data-manager/metastore2/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/kit-data-manager/metastore2/compare/v0.3.7...v1.0.0
[0.3.7]: https://github.com/kit-data-manager/metastore2/compare/v0.3.6...v0.3.7
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

