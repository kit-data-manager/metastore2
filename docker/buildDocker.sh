#!/bin/bash
################################################################################
# Build docker locally
# Usage:
# bash buildDocker.sh [TAG_NAME]
################################################################################

################################################################################
# Define default values for variables
################################################################################
TAG_NAME=local

################################################################################
# START DECLARATION FUNCTIONS
################################################################################

################################################################################
function usage {
################################################################################
  echo USAGE:
  echo   $0 [TAG_NAME]
  exit 1
}

################################################################################
function printInfo {
################################################################################
echo ---------------------------------------------------------------------------
echo $*
echo ---------------------------------------------------------------------------
}

################################################################################
# END DECLARATION FUNCTIONS / START OF SCRIPT
################################################################################

################################################################################
# Test for commands used in this script
################################################################################
testForCommands="dirname date docker git"

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
# Determine repo name 
################################################################################
REPO_NAME=`$ACTUAL_DIR/../gradlew -q printProjectName`
# Use only last line
REPO_NAME=${REPO_NAME##*$'\n'}

################################################################################
# Determine tag 
# 1. use tagname given as argument
# 2. determine last tag of git
#    a) if no tag defined -> local
#    b) concat actual date to tag
################################################################################
if [ "$1" != "" ]; then
  TAG_NAME=$1
  # Check for invalid flags
  if [ "${TAG_NAME:0:1}" = "-" ]; then
    usage
  fi
else
  LAST_TAG=`git describe --abbrev=0 --tags` >> /dev/null
  if [ "$LAST_TAG" = "" ]; then
    LAST_TAG=$TAG_NAME
  fi
  TAG_NAME=$LAST_TAG-`date -u +%Y-%m-%d`
fi

cd $ACTUAL_DIR/..

################################################################################
# Build local docker
################################################################################
printInfo Build docker container $REPO_NAME:$TAG_NAME 

docker build -t $REPO_NAME:$TAG_NAME .

if [ $? -ne 0 ]; then
  echo .
  printInfo "ERROR while building docker container!"
  usage
else 
  echo .
  printInfo Now you can create and start the container by calling docker "run -d -p8040:8040 --name metastore4docker $REPO_NAME:$TAG_NAME"
fi