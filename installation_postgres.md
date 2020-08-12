# Installation PostgreSQL

## Prerequisites
- Docker version 18.06 or higher

## Build and start docker container with postgreSQL
First time you have to build docker container. Port 5555 is used to avoid overlap.
```bash=bash
# Create directory for database dumps
user@localhost:/home/user/$mkdir -p /path/for/backups/postgres
user@localhost:/home/user/$docker run -p 5555:5432 --name postgres4kitdm -e POSTGRES_PASSWORD=YOUR_ADMIN_PASSWORD -v /path/for/backups/postgres:/dump -d postgres
123.....
```
Now postgreSQL is available on localhost via port 5555.

## Setup Database
```bash=bash
user@localhost:/home/user/$docker exec -ti postgres4kitdm sh -c "psql postgres -h localhost -d postgres"
psql (12.3 (Debian 12.3-1.pgdg100+1))
Type "help" for help.

postgres=# CREATE DATABASE kitdm20;
CREATE DATABASE
postgres=# CREATE USER kitdm_admin WITH ENCRYPTED PASSWORD 'KITDM_ADMIN_PASSWORD';
CREATE ROLE
postgres=# GRANT ALL PRIVILEGES ON DATABASE kitdm20 TO kitdm_admin;
GRANT
postgres=# \q
user@localhost:/home/user/$
```
Now postgreSQL is setup for metastore2.

To start/stop docker container afterwards use
```bash=bash
user@localhost:/home/user/$docker stop postgres4kitdm
user@localhost:/home/user/$docker start postgres4kitdm
```
## Setup Metastore2 Microservice
For using this database the settings in 'application.properties' should look like this:
```
### application.properties for postgres
#spring datasource settings
spring.datasource.platform: postgres
spring.datasource.url: jdbc:postgresql://localhost:5555/kitdm20
spring.datasource.username: kitdm_admin
spring.datasource.password: KITDM_ADMIN_PASSWORD
spring.datasource.driverClassName: org.postgresql.Driver
spring.jpa.database: POSTGRESQL
spring.jpa.database-platform: org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto: update
```

## Backup PostgreSQL
```bash=bash
# Backup kitdm20
user@localhost:/home/user/$docker exec -ti postgres4kitdm sh -c "pg_dump -U postgres -h 127.0.0.1 kitdm20 > /dump/database_dump_kitdm20_`date +%Y_%m_%dt%H_%M`.sql"
# Backup kitdm20 Authentication
user@localhost:/home/user/$docker exec -ti postgres4kitdm sh -c "pg_dump -U postgres -h 127.0.0.1 kitdm20_auth > /dump/database_dump_kitdm20_auth_`date +%Y_%m_%dt%H_%M`.sql"
```


## More Information

* [Docker](https://www.docker.com/)
* [PostgreSQL - Add user to database](https://medium.com/coding-blocks/creating-user-database-and-adding-access-on-postgresql-8bfcd2f4a91e)
