# Metastore 2 repository

[![Build Status](https://travis-ci.com/kit-data-manager/metastore2.svg?branch=master)](https://travis-ci.com/kit-data-manager/metastore2)
[![Coverage Status](https://coveralls.io/repos/github/kit-data-manager/metastore2/badge.svg?branch=master)](https://coveralls.io/github/kit-data-manager/metastore2?branch=master)
![License](https://img.shields.io/github/license/kit-data-manager/metastore2.svg)

General purpose metadata repository and schema registry service.

It allows you to 
- register an (XML/JSON) schema
- update an (XML/JSON) schema
- add metadata linked with a registered schema
- validate metadata against a registered schema
- update added metadata
 
## Installation
There are three ways to install metastore2 as a micorservice:
- [Using](#Installation-via-DockerHub) the image available via [DockerHub](https://hub.docker.com/r/kitdm/) (***recommended***)
- [Building](#Build-docker-container-locally) docker image locally
- [Building](#Build-and-run-locally) and running locally

## Installation via DockerHub
### Prerequisites
In order to run this microservice via docker you'll need:

* [Docker](https://www.docker.com/) 

### Installation
Typically, there is no need for locally building images as all version are accessible via DockerHub.
Have a look of available images and their tags [here](https://hub.docker.com/r/kitdm/metastore2) (***available soon***)
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
Now you'll have to create an image containing the micro service. This can be done via a script.
On default the created images will be tagged as follows:

*'latest tag'-'actual date(yyyy-mm-dd)'* (e.g.: 0.1.1-2020-10-05)

```
user@localhost:/home/user/metastore2$ bash docker/buildDocker.sh
---------------------------------------------------------------------------
Build docker container metastore2:0.1.1-2020-10-05
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
user@localhost:/home/user/metastore2$ docker images metastore2 --format {{.Tag}}
0.1.1-2020-10-05
user@localhost:/home/user/metastore2$ docker run -d -p8040:8040 --name metastore4docker metastore2:0.1.1-2020-10-05
57c973e7092bfc3778569f90632d60775dfecd12352f13a4fd2fdf4270865286
user@localhost:/home/user/metastore2$
```

#### Stop docker container
If you want to stop container just type
```
user@localhost:/home/user/metastore2$ docker stop metastore4docker
```

#### (Re)start docker container
If you want to start container just type
```
user@localhost:/home/user/metastore2$ docker start metastore4docker
```

## Build and run locally
### Prerequisites
In order to run this microservice via docker you'll need:

* [Java SE Development Kit 8 or higher](https://openjdk.java.net/) 
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
user@localhost:/home/user/metastore2$bash build.sh /path/to/empty/installation/directory
---------------------------------------------------------------------------
Build microservice of metastore2 at /path/to/empty/installation/directory
---------------------------------------------------------------------------
[...]
---------------------------------------------------------------------------
Now you can start the service by calling /path/to/empty/installation/directory/run.sh
---------------------------------------------------------------------------
user@localhost:/home/user/metastore2$
```

## First steps
As soon as the microservice is started, you can browse to 

http://localhost:8040/swagger-ui.html

in order to see available RESTful endpoints and their documentation. Furthermore, you can use this Web interface to test single API calls in order to get familiar with the 
service. A small documentation guiding you through the first steps of using the RESTful API you can find at

http://localhost:8040/static/docs/documentation.html


## Setup for production mode
:WARNING: If you want to use the service in production mode you have modify your configuration (application.properties).

1. Don't open port to public (as long as AAI is not implemented)
2. Use a productive database (e.g. postgres)
3. Setup directories for schemata and metadata to a reliable disc. (metastore.schema.schemaFolder, metastore.metadata.metadataFolder)

:information_source: If metastore should be used standalone (without KIT Data Manager) 
you have to setup a database before. (See ['Installation PostgreSQL'](installation_postgres.md)) 

#### Setup metastore2
Before you are able to start the repository microservice, you have to modify the file 'application.properties' according to your local setup. 
Therefor, copy the file 'settings/application-example.properties' to your project folder, rename it to 'application.properties' and customize it. Special attentioned should be payed to the database setup (spring.datasource.*),
and the paths of schemata (metastore.schema.schemaFolder) / metadata (metastore.schema.metadataFolder). If you changed the port you also have to adapt the 
url of the schema registry (metastore.metadata.schemaRegistries) 
to the repository base path. Also, the property 'repo.messaging.enabled' should be changed to 'true' in case you want to use the messaging feature of the repository.

#### Setup database
See [setup database](installation_postgres.md#setup-database) and [setup for metastore](installation_postgres.md#setup-metastore2-microservice).

### Start metastore2
As soon as you finished modifying 'application.properties', you may start the repository microservice by executing the following command inside the project folder, 
e.g. where the service has been built before:

## More Information

* [Information about KIT Data Manager 2](https://github.com/kit-data-manager/base-repo)
* [REST Documentation Metastore 2](restDocu.md) 

## License

The Metastore2 is licensed under the Apache License, Version 2.0.
