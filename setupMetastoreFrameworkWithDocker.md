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
```

## Step 4
  Start elasticsearch server:
```
```

## Step 4
  Start elasticsearch server:
```
```

## Step 4
  Start elasticsearch server:
```
```

