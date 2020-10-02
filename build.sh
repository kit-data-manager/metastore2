#!/bin/bash
################################################################################
# Build spring boot with starter script
# Usage:
# bash buildDocker.sh [/path/to/installation/dir]
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
  echo USAGE:
  echo   $0 [/path/to/installation/dir]
  echo IMPORTANT: Please enter an empty or new directory as installation directory.
  exit 1
}

################################################################################
function checkParameters {
################################################################################
  # Check no of parameters.
  if [ "$#" -ne 1 ]; then
    echo "Illegal number of parameters"
    usage
  fi

  # Check if argument is given
  if [ -z "$1" ]; then
    echo Please provide a directory where to install.
    usage
    exit 1
  fi
  
  INSTALLATION_DIRECTORY=$1

  # Check if directory exists
  if [ ! -d "$INSTALLATION_DIRECTORY" ]; then
    # Create directory if it doesn't exists.
    mkdir -p "$INSTALLATION_DIRECTORY"
    if [ $? -ne 0 ]; then
      echo "Error creating directory $INSTALLATION_DIRECTORY!"
      exit 1
    fi
  fi
  # Check if directory is empty
  if [ ! -z "$(ls -A "$INSTALLATION_DIRECTORY")" ]; then
     echo $INSTALLATION_DIRECTORY is not empty!
     echo "Please enter an empty or a new directory!"
     exit 1
  fi
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
testForCommands="dirname git date java javac"

for command in $testForCommands
do 
  $command --help >> /dev/null
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
# Determine repo name 
################################################################################
REPO_URL=`git remote get-url origin`
REPO_NAME=${REPO_URL##*/}

printInfo Build microservice of $REPO_NAME at $INSTALLATION_DIRECTORY


################################################################################
# Build service
################################################################################

echo Build service...
./gradlew -Prelease clean build


echo Copy configuration to $INSTALLATION_DIRECTORY...
find . -name application-default.properties -exec cp '{}' $INSTALLATION_DIRECTORY/application.properties \;

echo Copy jar file to $INSTALLATION_DIRECTORY...
find . -name "$REPO_NAME*.jar" -exec cp '{}' $INSTALLATION_DIRECTORY \;

echo Create config directory
mkdir $INSTALLATION_DIRECTORY/config

echo Create lib directory
mkdir $INSTALLATION_DIRECTORY/lib

###############################################################################
# Create run script
################################################################################
printInfo Create run script ...

# Determine name of jar file.
for file in $INSTALLATION_DIRECTORY/*.jar; do
  jarFile=${file##*/}
done

echo #!/bin/bash > $INSTALLATION_DIRECTORY/run.sh
echo cd $INSTALLATION_DIRECTORY                                                       >> $INSTALLATION_DIRECTORY/run.sh
echo ################################################################################ >> $INSTALLATION_DIRECTORY/run.sh
echo # Determine directory of script.                                                 >> $INSTALLATION_DIRECTORY/run.sh
echo ################################################################################ >> $INSTALLATION_DIRECTORY/run.sh
echo ACTUAL_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"   >> $INSTALLATION_DIRECTORY/run.sh
echo "java -cp \"$ACTUAL_DIR/.:$ACTUAL_DIR/$jarFile.jar\" -Dloader.path=file://$ACTUAL_DIR/$jarFile,$ACTUAL_DIR/lib/,$ACTUAL_DIR/. -jar $jarFile" >> $INSTALLATION_DIRECTORY/run.sh

echo .
printInfo Now you can start the service by calling "$INSTALLATION_DIRECTORY/run.sh"
