# MetaStore 2 repository

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.7685007.svg)](https://doi.org/10.5281/zenodo.7685007)
[![Build Status](https://github.com/kit-data-manager/metastore2/actions/workflows/gradle.yml/badge.svg)](https://github.com/kit-data-manager/metastore2/actions/workflows/gradle.yml)
[![Codecov](https://codecov.io/gh/kit-data-manager/metastore2/graph/badge.svg)](https://codecov.io/gh/kit-data-manager/metastore2)
[![CodeQL](https://github.com/kit-data-manager/metastore2/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/kit-data-manager/metastore2/actions/workflows/codeql-analysis.yml)
![License](https://img.shields.io/github/license/kit-data-manager/metastore2.svg)
![current Version](https://img.shields.io/github/v/release/kit-data-manager/metastore2)

MetaStore is a research data repository software for storing metadata documents and schemas.
Quality and consistency are ensured by associating and validating each document against a schema.
It supports JSON and XML.

It allows you to 
- register an (XML/JSON) schema
- update an (XML/JSON) schema
- add metadata linked with a registered schema
- validate metadata against a registered schema
- update added metadata
 
## Installation
There are three ways to install metaStore2 as a microservice:
- [Using](#Installation-via-GitHub-Packages) the image available via [GitHub Packages](https://github.com/orgs/kit-data-manager/packages?repo_name=metastore2) (***recommended***)
- [Building](#Build-docker-container-locally) docker image locally
- [Building](#Build-and-run-locally) and running locally

## Installation via GitHub Packages
### Prerequisites
In order to run this microservice via docker you'll need:

* [Docker](https://www.docker.com/) 

### Installation
Typically, there is no need for locally building images as all version are accessible via GitHub Packages.
Have a look of available images and their tags [here](https://github.com/orgs/kit-data-manager/packages?repo_name=metastore2) 
Just follow instructions [below](#Build-docker-container).

## Build docker container locally
### Prerequisites
In order to run this microservice via docker you'll need:

* [Docker](https://www.docker.com/) 
* [git](https://git-scm.com/) 

### Installation
#### Clone repository
First of all you'll have to clone this repository:
```
user@localhost:/home/user/$ git clone https://github.com/kit-data-manager/metastore2.git
Clone to 'metastore2'
[...]
user@localhost:/home/user/$ cd metastore2
user@localhost:/home/user/metastore2$
```

#### Create image
Now you'll have to create an image containing the microservice. This can be done via a script.
On default the created images will be tagged as follows:

*'latest tag'-'actual date(yyyy-mm-dd)'* (e.g.: 1.2.0-2023-06-27)

```
user@localhost:/home/user/metastore2$ bash docker/buildDocker.sh
---------------------------------------------------------------------------
Build docker container ghcr.io/kit-data-manager/metastore2:1.2.0-2023-06-27
---------------------------------------------------------------------------
[...]
---------------------------------------------------------------------------
Now you can create and start the container by calling ...
---------------------------------------------------------------------------
user@localhost:/home/user/metastore2$
```

#### Build docker container
After building image you have to create (and start) a container for executing microservice:
```
# If you want to use a specific image you may list all possible tags first.
user@localhost:/home/user/metastore2$ docker images ghcr.io/kit-data-manager/metastore2 --format {{.Tag}}
1.2.0-2023-06-27
user@localhost:/home/user/metastore2$ docker run -d -p8040:8040 --name metastore4docker ghcr.io/kit-data-manager/metastore2:1.2.0-2023-06-27
57c973e7092bfc3778569f90632d60775dfecd12352f13a4fd2fdf4270865286
user@localhost:/home/user/metastore2$
```

#### Customize settings
If you want to overwrite default configuration of your docker container you have to
'mount' a config directory containing 'application.properties' with your adapted settings.
Therefor you have to provide an additional flag to the command mentioned before:
```
# Overwriting default settings
# Create config folder
user@localhost:/home/user/metastore2$ mkdir config
# Place your own 'application.properties' inside the config directory
# Create/run container
user@localhost:/home/user/metastore2$ docker run -d -p8040:8040 -v `pwd`/config:/spring/metastore2/config --name metastore4docker ghcr.io/kit-data-manager/metastore2:1.2.0-2023-06-27
57c973e7092bfc3778569f90632d60775dfecd12352f13a4fd2fdf4270865286
user@localhost:/home/user/metastore2$
```

#### Stop docker container
If you want to stop container just type
```
user@localhost:/home/user/metastore2$ docker stop metastore4docker
```

#### (Re)start docker container
If you want to (re)start container just type
```
user@localhost:/home/user/metastore2$ docker start metastore4docker
```

## Build and run locally
### Prerequisites
In order to run this microservice via docker you'll need:

* [Java SE Development Kit >= 17](https://openjdk.java.net/) 
* [git](https://git-scm.com/) 

### Installation
#### Clone repository
First of all you'll have to clone this repository:
```
user@localhost:/home/user/$ git clone https://github.com/kit-data-manager/metastore2.git
Clone to 'metastore2'
[...]
user@localhost:/home/user/$ cd metastore2
user@localhost:/home/user/metastore2$
```
#### Build service 
To build service just execute the build.sh script:
```
user@localhost:/home/user/metastore2$bash build.sh /PATH/TO/EMPTY/INSTALLATION/DIRECTORY
---------------------------------------------------------------------------
Build microservice of metastore2 at /PATH/TO/EMPTY/INSTALLATION/DIRECTORY
---------------------------------------------------------------------------
[...]
---------------------------------------------------------------------------
Now you can start the service by calling /PATH/TO/EMPTY/INSTALLATION/DIRECTORY/run.sh
---------------------------------------------------------------------------
user@localhost:/home/user/metastore2$
```
#### Customize settings
If you want to overwrite default configuration of your docker container you have to
add a file named 'application.properties' to the 'config' directory inside your installation
path (/PATH/TO/EMPTY/INSTALLATION/DIRECTORY)selected before. The added file should
only contain your adapted settings. e.g. in case you want to change only the port to '1234' your
'application.properties' should look like this:
```
# Overwriting default settings from ../application.properties
# Server settings
server.port: 1234
```
## Build framework using docker 
### Prerequisites
In order to run this framework via docker you'll need:
* [Docker](https://www.docker.com/)
### Preparation
You have to add the following line to your /etc/hosts file:
```
127.0.0.1 metastore.docker
```
```yaml
Note for Windows users:
You have to add the line to your C:\Windows\System32\drivers\etc\hosts file.
```
Based on Docker Compose, the entire framework, including elasticsearch and the UI, can now be installed with a single command.
```
user@localhost:/home/user/metastore2$ docker compose up -d
```
As soon all services are running you can browse to http://metastore.docker/index.html

```yaml
Note: The framework is only for testing purposes and not intended for production use.
If you want to use the framework in production mode you have to modify your 
configuration (application.properties) and setup a database (e.g. postgres).
``` 

### First steps using framework
If you want to use the (external) web frontend please visit:

https://kit-data-manager.github.io/webpage/metastore/documentation/frontend/index.html

A small documentation guiding you through the first steps of using the RESTful API you can find at

https://kit-data-manager.github.io/webpage/metastore/documentation/REST/APIv2/index.html

https://kit-data-manager.github.io/webpage/metastore/documentation/REST/APIv1/index.html (deprecated)

If you want to use a script for doing so please refer to 

http://metastore.docker:8040/metastore/swagger-ui.html

in order to see available RESTful endpoints and their documentation. Furthermore, 
you can use this Web interface to test single API calls in order to get familiar with the 
service. 

### Managing framework using docker
To stop and start all services do the following:
```
user@localhost:/home/user/metastore2$ docker compose stop
[+] Running 6/6
 ⠿ Container frontend.docker            Stopped                            1.5s
 ⠿ Container metastore.docker           St...                             13.1s
 ⠿ Container indexing.docker            Stopped                            0.3s
 ⠿ Container metastore2-dps-1           Stopped                           13.2s
 ⠿ Container rabbitmq.docker            S...                               6.5s
 ⠿ Container elastic.docker             St...                              0.8s
user@localhost:/home/user/metastore2$ docker compose start
[+] Running 6/6
 ⠿ Container frontend.docker     Started                                                                                                                                 0.3s
 ⠿ Container metastore2-dps-1    Started                                                                                                                                10.5s
 ⠿ Container elastic.docker      Started                                                                                                                                 0.2s
 ⠿ Container rabbitmq.docker     Started                                                                                                                                 0.2s
 ⠿ Container metastore.docker    Started                                                                                                                                 0.2s
 ⠿ Container indexing.docker     Started                                                                                                                                 0.2s
user@localhost:/home/user/metastore2$
```

## First steps using MetaStore standalone
If you're using MetaStore without the whole framework the service is reachable via
http://localhost:8040/metastore/....

## Setup for production mode
:WARNING: If you want to use the service in production mode you have modify your configuration (application.properties).

1. Don't open port to public (as long as AAI is not implemented)
2. Use a productive database (e.g. postgres)
3. Setup directories for schemata and metadata to a reliable disc. (metastore.schema.schemaFolder, metastore.metadata.metadataFolder)
4. Check all settings in application.properties. (e.g. CSRF)

:information_source: If MetaStore should be used standalone (without KIT Data Manager) 
you have to install a database before. (See ['Installation PostgreSQL'](installation_postgres.md)) 

#### Setup MetaStore2
Before you are able to start the repository microservice, you have to modify the file 'application.properties' according to your local setup. 
Therefor, copy the file 'settings/application-example.properties' to your project folder, rename it to 'application.properties' and customize it. Special attentioned should be payed to the database setup (spring.datasource.*),
and the paths of schemata (metastore.schema.schemaFolder) / metadata (metastore.schema.metadataFolder). 
to the repository base path. Also, the property 'repo.messaging.enabled' should be changed to 'true' in case you want to use the messaging feature of the repository.

#### Setup database
See [setup database](installation_postgres.md#setup-database) and [setup for MetaStore2](installation_postgres.md#setup-metastore2-microservice).

### Start MetaStore2
As soon as you finished modifying 'application.properties', you may start the repository microservice by executing the following command inside the installation folder:
```
user@localhost:/home/user/metastore2$bash /PATH/TO/EMPTY/INSTALLATION/DIRECTORY/run.sh
```

## More Information

* [Information about KIT Data Manager 2](https://github.com/kit-data-manager/base-repo)
* [REST Documentation MetaStore2 API V1](restDocu.md) 
* [REST Documentation MetaStore2 API V2](restDocuV2.md) 

## License

The MetaStore2 is licensed under the Apache License, Version 2.0.

## Acknowledgements

This work has been supported by the research program [‘Engineering Digital Futures’](https://www.helmholtz.de/en/research/research-fields/information/engineering-digital-futures/) of the [Helmholtz Association of German Research Centers](https://www.helmholtz.de/en) and the [Helmholtz Metadata Collaboration Platform (HMC)](https://helmholtz-metadaten.de/), with additional support by [NFDI4ING](https://nfdi4ing.de/) (DFG project number 442146713) and [NFDI-MatWerk](https://nfdi-matwerk.de/) (DFG project number 460247524).
