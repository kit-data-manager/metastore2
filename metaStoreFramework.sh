#!/bin/bash
################################################################################
# Management for metastore framework
# - Managing the following instances 
#    - elasticsearch
#    - rabbitMQ
#    - indexing-service
#    - metastore2
# Usage:
# bash metastoreFramework.sh [init|start|stop]
################################################################################

################################################################################
# Define default values for variables
################################################################################
# no defaults yet!

################################################################################
# START DECLARATION FUNCTIONS
################################################################################

################################################################################
function usage {
################################################################################
  echo "Script for managing metastore service."
  echo "USAGE:"
  echo "  $0 [init|start|stop]"
  echo " "
  echo "  init - Initialize/Reset the whole framework"
  echo "  start - Start stopped framework"
  echo "  stop - Stop framework"
  exit 1
}

################################################################################
function checkParameters {
################################################################################
  # Check no of parameters.
  if [ "$#" -ne 1 ]; then
    echo "Illegal number of parameters!"
    usage
  fi
}

################################################################################
function initFramework {
################################################################################
printInfo "Setup Framework"

echo "Setup configuration directories for metaStore and indexing-Service"
mkdir -p "$ACTUAL_DIR/settings/metastore"
mkdir -p "$ACTUAL_DIR/settings/indexing"

echo "Setup network for docker..."
docker network create network4datamanager

echo "Start RabbitMQ server..."
deleteDockerContainer rabbitmq4docker
docker run -d --hostname rabbitmq --net network4datamanager --name rabbitmq4docker -p 5672:5672 -p 15672:15672 rabbitmq:3-management

echo "Start metaStore2..."
deleteDockerContainer metastore4docker
docker run -d -v "$ACTUAL_DIR/settings/metastore":/spring/metastore2/config --net network4datamanager --name metastore4docker -p8040:8040 kitdm/metastore2:latest

echo "Start elasticsearch server..."
deleteDockerContainer elasticsearch4metastore
docker run -d --net network4datamanager --name elasticsearch4metastore  -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" elasticsearch:7.9.3

echo "Start Indexing-Service..."
deleteDockerContainer indexing4metastore
docker run -d -v "$ACTUAL_DIR/settings/metastore":/spring/indexing-service/config --net network4datamanager --name indexing4metastore  -p 8050:8050 indexing-service:latest

printInfo "Ready to use metastore"
}

################################################################################
function startFramework {
################################################################################
printInfo "(Re)start metastore and all linked services..."

echo "Start RabbitMQ server..."
docker start rabbitmq4docker 

echo "Start metastore2..."
docker start metastore4docker 

echo "Start elasticsearch server..."
docker start elasticsearch4metastore 

echo "Start Indexing-Service..."
docker start indexing4docker

printInfo "Framework started!"
}

################################################################################
function stopFramework {
################################################################################
printInfo "Shutdown metastore and all linked services..."

echo "Stop Indexing-Service..."
docker stop indexing4docker

echo "Stop elasticsearch server..."
docker stop elasticsearch4metastore 

echo "Stop metastore2..."
docker stop metastore4docker 

echo "Stop RabbitMQ server..."
docker stop rabbitmq4docker 

printInfo "Framework stopped!"
}

################################################################################
function deleteDockerContainer {
################################################################################
printInfo "Delete docker image '$1'"

docker ps | grep -q $1

if [ $? -eq 0 ]; then
    echo "Docker container '$1' still running -> Stop docker container"
    docker stop $1
fi

docker ps -a | grep -q $1
if [ $? -eq 0 ]; then
    echo "Docker container '$1' exists -> Remove docker container"
    docker rm $1
fi
}

################################################################################
function printInfo {
################################################################################
echo "---------------------------------------------------------------------------"
echo $*
echo "---------------------------------------------------------------------------"
}

################################################################################
# END DECLARATION FUNCTIONS / START OF SCRIPT
################################################################################

################################################################################
# Test for commands used in this script
################################################################################
testForCommands="type echo grep mkdir docker"

for command in $testForCommands
do 
  type $command >> /dev/null
  if [ $? -ne 0 ]; then
    echo "Error: command '$command' is not installed!"
    exit 1
  fi
done

################################################################################
# Determine directory of script. 
################################################################################
ACTUAL_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

################################################################################
# Check parameters
################################################################################
checkParameters $*

################################################################################
# Manage framework
################################################################################

case "$1" in
  init) initFramework
     ;;
  start) startFramework
     ;;
  stop) stopFramework
      ;;
  *) usage
     ;;
esac
