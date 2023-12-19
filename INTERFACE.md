---
filename: INTERFACES.md
description: MetaStore
idea: One such file per repository (services, plugins, frontends, ...) as a proposal for DEM developers.
...

# Interfaces Overview for MetaStore

This document aims to answer questions on how to configure external dependencies and which public interfaces are offered by MetaStore in a comprehensive way. It is meant to be used for getting an overview and guidance in addition to the official documentation, which is available at the official [MetaStore Web page](https://kit-data-manager.github.io/webpage/metastore/).How can this software be connected with other software? This is the question this document aims to answer.

> ℹ️ **Note:**
> This document applies to the MetaStore version it is shipped with. If you have a specific version running, please refer to `INTERFACE.md` of this particular release.

## TOC

- [Interfaces Overview for MetaStore](#interfaces-overview-for-metastore)
   * [External Dependencies](#external-dependencies-) 📤
      + [Relational Database (mandatory)](#relational-database-mandatory-) ⛁
      + [Local Filesystem (mandatory)](#local-filesystem-mandatory-) 📂
      + [Messaging (optional)](#messaging-optional-) 💬
      + [Enhanced Search (optional)](#enhanced-search-optional-) 🔍
      + [Access Control (optional)](#access-control-optional-) 🔐
   * [Public Interfaces](#public-interfaces-) 📥
      + [HTTP / REST](#http--rest)
      + [Elasticsearch Proxy](#elasticsearch-proxy-) 🔍
      + [OAI-PMH](#oai-pmh)
      + [Digital Object Interface Protocol (DOIP)](#digital-object-interface-protocol-doip)
## External Dependencies 📤

External dependencies are third-party services that are required for MetaStore to work properly or that can be added optionally to provide additional functionality. Typically, external dependencies require additional software to be installed and configured, before they can be included in the MetaStore configuration, which is typically done via the main configuration file `application.properties`. If you do not want to lose your default settings, we recommend that you make a copy of "application.properties" and move it to the "config" subfolder. Remove all properties you want to keep from the new file. **All properties in "config/application.properties" override the settings in "application.properties".**


## External Dependencies 📤

External dependencies are third-party services that are required for MetaStore to work properly or that can be added optionally to provide additional functionality. Typically, external dependencies require
additional software to be installed and configured, before they can be included in the MetaStore configuration, which is should be done via the  configuration file `config/application.properties`.

### Relational Database (mandatory) ⛁
A relational database is required by MetaStore to store administrative metadata for metadata documents/schemas. If not configured properly, MetaStore will fail to start.

#### Configuration ⚙️
 - H2 In-Memory (driver included, used for **testing only**, **not** recommended for **production**) [Example](https://github.com/kit-data-manager/metastore2/blob/v1.3.0/src/test/resources/test-config/application-test.properties#L38-L44)
 - H2 File-Based (driver included, used for basic Docker setup or for 'quick and dirty' tests, **not** recommended for **production**) [Example](https://github.com/kit-data-manager/metastore2/blob/v1.3.0/settings/application-docker.properties#L106-L113)
 - Postgres (driver included, **requires a running PostgreSQL server**, **recommended for production**) [PostgreSQL](https://www.postgresql.org/), [Example](https://github.com/kit-data-manager/metastore2/blob/v1.3.0/settings/application-postgres.properties#L106-L116)

> ℹ️ **Note:** 
> Other relational databases, such as MariaDB, SQLite, or Oracle, may also work, but require additional steps. To allow MetaStore to connect, the source code repository must be cloned, an appropriate JDBC driver has to be added to `build.gradle` and MetaStore has to be compiled. Proper JDBC drivers are typically provided on the database's web page. Afterwards, the database can be configured in `config/application.properties` similar to PostgreSQL but with database-specific property naming. Please refer to the driver documentation for details.

### Local Filesystem (mandatory) 📂
MetaStore requires access to the local file system in order to store and manage uploaded metadata (schema) documents. MetaStore requires access to two folders, which can be located on the local hard drive or mounted via NFS. By default, two subfolders named 'metadata' and 'schema' are created in the installation directory.


#### Configuration ⚙️
 - see `application.properties` 
     - [Configure folders](https://github.com/kit-data-manager/metastore2/blob/v1.3.0/settings/application-default.properties#L18-L22)
     - [Configure folder structure for metadata documents](https://github.com/kit-data-manager/metastore2/blob/v1.3.0/settings/application-default.properties#L24-L42)

> ℹ️ **Note:** 
> The file path to the folders have to start with three '/'. If you overwrite the default settings we recommend to create or edit 'config/application.properties'. e.g.: 
> ``` title= config/application.properties
> metastore.schema.schemaFolder:file:///data/metastore/schema
> metastore.metadata.metadataFolder:file:///data/metastore/metadata
>```
> **If the folders do not exist, they will be created.**

### Messaging (optional) 💬
AMQP-based messaging is an optional feature of MetaStore, which allows MetaStore to emit messages about creation, modification, and deletion events related to **metadata documents**. These messages can be received by registered consumers and processed in an asynchronous way.
#### Installation
- [Installing RabbitMQ](https://kit-data-manager.github.io/webpage/metastore/documentation/installation/framework/setup-rabbitMq.html)
#### Configuration ⚙️
 - [RabbitMQ](https://www.rabbitmq.com/) (dependencies included, serves as messaging distributor, requires a running RabbitMQ server) 
     - [Introduction Messaging for MetaStore](https://kit-data-manager.github.io/webpage/metastore/documentation/installation/messaging/messaging-introduction.html)
     - [Configuration Messaging for MetaStore](https://kit-data-manager.github.io/webpage/metastore/documentation/installation/messaging/messaging-configuration.html)
     - [Example](https://kit-data-manager.github.io/webpage/metastore/documentation/installation/setup-metastore.html#rabbitmq) 

### Enhanced Search (optional) 🔍
By default, MetaStore offers basic search based on administrative metadata via RESTful API by certain query parameters. Optionally, enhanced search via a search index can be enabled and used for fine-grained and facetted search operations.Therefor all metadata documents were indexed using an [elasticsearch instance](https://www.elastic.co/de/elasticsearch/). (JSON metadata documents are provided by default. XML documents should be transformed to JSON)
#### Requirements
- [Messaging](#messaging-optional-)
- Indexing Service
    - [Installation](https://kit-data-manager.github.io/webpage/metastore/documentation/installation/framework/setup-indexing-service.html)
    - [Configuration](https://github.com/kit-data-manager/indexing-service/blob/main/settings/application-default.properties#L12-L21)
- [Elasticsearch](https://kit-data-manager.github.io/webpage/metastore/documentation/installation/framework/setup-elasticsearch.html)
Messaging is used to inform indexing service about any updates regarding metadata documents. Indexing service will fetch the document, transform it (if mapping available) and send it to elasticsearch for indexing.
#### Configuration ⚙️
- [Configuration](https://kit-data-manager.github.io/webpage/metastore/documentation/installation/setup-metastore.html#elasticsearch)
 
### Access Control (optional) 🔐
By default, MetaStore itself is open for all kinds of operations, i.e., read and write, where write access should be restricted on the user interface level, e.g., by a password-protected area for critical operations. Optionally, authentication and authorization via JSON Web Tokens (JWT) issued by a Keycloak instance, can be configured, which allows a fine-granulated access management on document level.

#### Requirements
- [Keycloak](https://www.keycloak.org/)
#### Configuration ⚙️
- [Setup MetaStore](https://kit-data-manager.github.io/webpage/metastore/documentation/installation/setup-metastore.html#keycloak)

## Public Interfaces 📥
Public Interfaces are used to access MetaStore in order to obtain its contents, typically this happens via HTTP/REST. Depending on the interface, special clients or protocols must be used to access a specific public interface.


Interfaces which trigger functionality or return information **on request**.

### HTTP / REST
A REST interface which allows to access the service functionality, like creating/registering, updating and validating metadata documents/schemas on request.
#### Documentation 📖
 - [OpenAPI](https://kit-data-manager.github.io/webpage/metastore/documentation/api-docs.html) / or via running instance, as described in the [Readme](https://github.com/kit-data-manager/metastore2#first-steps-using-framework).
 - [Usage with Examples](https://kit-data-manager.github.io/webpage/metastore/documentation/REST/index.html)
    
#### Application Examples 📋 
- [Frontend Collection](https://github.com/kit-data-manager/frontend-collection) 
- [Metadata Hub](https://git.rwth-aachen.de/nfdi4ing/s-3/s-3-3/metadatahub)

### Elasticsearch Proxy 🔍
If [Enhanced Search](#enhanced-search-optional-) is enabled, an additional REST endpoint becomes available, which allows to tunnel search queries to the underlying Elasticsearch instance. The advantage for proxying Elasticsearch access is, that access restrictions enabled via [Access Control](#access-control-optional-) are included in the query such that only results accessible by the caller are returned.

#### Documentation 📖
 - [Search Configuration](https://kit-data-manager.github.io/webpage/metastore/documentation/installation/search-configuration.html)
    
#### Application Examples 📋
- [Frontend Collection](https://github.com/kit-data-manager/frontend-collection) 
- [Metadata Hub](https://git.rwth-aachen.de/nfdi4ing/s-3/s-3-3/metadatahub)

### OAI-PMH
[OAI-PMH](https://www.openarchives.org/pmh/) is a standardized harvesting protocol that allows to build up external search indices that can be kept up to data by regular harvesting changes from an OAI-PMH source. For MetaStore only XML metadata documents are supported.
#### Documentation 📖
 - [Configuration](https://kit-data-manager.github.io/webpage/metastore/documentation/installation/setup-metastore.html#oai-pmh)
    
### ~~Digital Object Interface Protocol (DOIP)~~
[DOIP](https://www.dona.net/sites/default/files/2018-11/DOIPv2Spec_1.pdf) is a novel protocol to provide generic access to digital resources. Instead of using HTTP-based communication, the protocol acts directly on top of TCP/IP and is therefore mainly relevant for special ecosystems.
**Not finalized yet!**

#### Documentation 📖
Not yet available!
