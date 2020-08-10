# Metastore 2 repository

[![Build Status](https://api.travis-ci.com/kit-data-manager/metastore2.svg)]
[![Coverage Status](https://coveralls.io/repos/github/kit-data-manager/metastore2/badge.svg?branch=ci)](https://coveralls.io/github/kit-data-manager/metastore2?branch=ci)
![License](https://img.shields.io/github/license/kit-data-manager/metastore2.svg)

General purpose metadata repository and schema registry service

## How to build
In order to build this microservice you'll need:

* Java SE Development Kit 8 or higher

After obtaining the sources change to the folder where the sources are located perform the following steps:

```
user@localhost:/home/user/metastore2$ ./gradlew -Prelease build
Building metastore2 version: 0.1
Using release profile for building metastore2
[...]
user@localhost:/home/user/metastore2$
```

The Gradle wrapper will now take care of downloading the configured version of Gradle, checking out all required libraries, build these
libraries and finally build the metastore2 microservice itself. As a result, a fat jar containing the entire service is created at 'build/libs/metastore2-0.1.jar'.

## How to start

### Prerequisites

* PostgreSQL 9.1 or higher
* KIT data manager 2 (in case you want to use it in conjunction with a data repository)
  * RabbitMQ 3.7.3 or higher (in case you want to use the messaging feature, which is recommended)

### Setup
Before you are able to start the repository microservice, you have to modify the file 'application.properties' according to your local setup. 
Therefor, copy the file 'settings/application-example.properties' to your project folder, rename it to 'application.properties' and customize it. Special attentioned should be payed to the database setup (spring.datasource.*),
and the paths of schemata (metastore.schema.schemaFolder) / metadata (metastore.schema.metadataFolder). If you changed the port you also have to adapt the 
url of the schema registry (metastore.metadata.schemaRegistries) 
to the repository base path. Also, the property 'repo.messaging.enabled' should be changed to 'true' in case you want to use the messaging feature of the repository.

As soon as you finished modifying 'application.properties', you may start the repository microservice by executing the following command inside the project folder, 
e.g. where the service has been built before:

```
user@localhost:/home/user/metastore2$ ./build/libs/metastore2-0.1.jar

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v2.2.2.RELEASE)
2020-08-03 10:02:46.146  INFO 9893 --- [           main] e.k.datamanager.metastore2.Application   : Starting Application on ...
[...]

```

If your 'application.properties' is not located inside the project folder you can provide it using the command line argument --spring.config.location=<PATH_TO_APPLICATION.PROPERTIES>

As soon as the microservice is started, you can browse to 

http://localhost:8040/swagger-ui.html

in order to see available RESTful endpoints and their documentation. Furthermore, you can use this Web interface to test single API calls in order to get familiar with the 
service. A small documentation guiding you through the first steps of using the RESTful API you can find at

http://localhost:8040/static/docs/documentation.html


## More Information

* [Information about KIT Data Manager 2](https://github.com/kit-data-manager/base-repo)
* [REST Documentation Metastore 2](restDocu.adoc) (online only)

## License

The Metastore2 is licensed under the Apache License, Version 2.0.
