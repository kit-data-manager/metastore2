# How to Setup a complete Framework with Docker

## Step 1
  Create network for docker:
```
docker network create network4datamanager
```

## Step 2
  Start RabbitMQ server:
```
docker run -d --hostname rabbitmq --net network4datamanager --name rabbitmq4docker rabbitmq:3-management
```

## Step 3 
  Start metastore2:
```
docker run -d -p8040:8040 --net network4datamanager --name metastore4docker kitdm/metastore2:latest
```

## Step 4
  Start elasticsearch server:
```
docker run -d --net network4datamanager --name elasticsearch4metastore  -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" elasticsearch:7.9.3
```

## Step 5
  Start Indexing-Service:
```
docker run -d --net network4datamanager --name indexing4metastore  -p 8050:8050 indexing-service:latest
```

## Step 6
  Register schema (for metastore2)
```
```

## Step 7
  Register mapping (for elasticsearch):
```
```

## Step 8
  You're ready to ingest your metadata to metastore:
```
```

## Step 9
  All metadata is now available via elasticsearch.
```
```
# Stop Docker Framework


## Step 1
  Stop Indexing-Service:
```
docker stop indexing4docker
```

## Step 2
  Stop elasticsearch server:
```
docker stop elasticsearch4metastore 
```

## Step 3 
  Stop metastore2:
```
docker stop metastore4docker 
```

## Step 4
  Stop RabbitMQ server:
```
docker stop rabbitmq4docker 
```

# Start Docker Framework
## Step 1
  Start RabbitMQ server:
```
docker start rabbitmq4docker 
```

## Step 2 
  Start metastore2:
```
docker start metastore4docker 
```
## Step 3
  Start elasticsearch server:
```
docker start elasticsearch4metastore 
```

## Step 4
  Start Indexing-Service:
```
docker start indexing4docker
```
