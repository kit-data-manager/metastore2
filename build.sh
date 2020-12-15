#!/bin/bash
################################################################################
# Build spring boot with starter script
# Usage:
# bash build.sh [/path/to/installation/dir]
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
  echo "Script for creating metastore service."
  echo "USAGE:"
  echo "  $0 [/path/to/installation/dir]"
  echo "IMPORTANT: Please enter an empty or new directory as installation directory."
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

  # Check if argument is given
  if [ -z "$1" ]; then
    echo "Please provide a directory where to install."
    usage
    exit 1
  fi
  
  # Check for invalid flags
  if [ "${1:0:1}" = "-" ]; then
    usage
  fi

  INSTALLATION_DIRECTORY=$1

  # Check if directory exists
  if [ ! -d "$INSTALLATION_DIRECTORY" ]; then
    # Create directory if it doesn't exists.
    mkdir -p "$INSTALLATION_DIRECTORY"
    if [ $? -ne 0 ]; then
      echo "Error creating directory '$INSTALLATION_DIRECTORY'!"
      echo "Please make sure that you have the correct access permissions for the specified directory."
      exit 1
    fi
  fi
  # Check if directory is empty
  if [ ! -z "$(ls -A "$INSTALLATION_DIRECTORY")" ]; then
     echo "Directory '$INSTALLATION_DIRECTORY' is not empty!"
     echo "Please enter an empty or a new directory!"
     exit 1
  fi
  # Convert variable of installation directory to an absolute path
    cd "$INSTALLATION_DIRECTORY"
    INSTALLATION_DIRECTORY=`pwd`
    cd "$ACTUAL_DIR"
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
testForCommands="chmod cp dirname find java javac mkdir"

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
# Determine repo name 
################################################################################
REPO_NAME=`./gradlew -q printProjectName`
# Use only last line
REPO_NAME=${REPO_NAME##*$'\n'}

printInfo "Build microservice of $REPO_NAME at '$INSTALLATION_DIRECTORY'"


################################################################################
# Build service
################################################################################

echo Build service...
./gradlew -Prelease clean build


echo "Copy configuration to '$INSTALLATION_DIRECTORY'..."
find . -name application-default.properties -exec cp '{}' "$INSTALLATION_DIRECTORY"/application.properties \;

echo "Copy jar file to '$INSTALLATION_DIRECTORY'..."
find . -name "$REPO_NAME*.jar" -exec cp '{}' "$INSTALLATION_DIRECTORY" \;

echo "Create config directory"
mkdir "$INSTALLATION_DIRECTORY"/config

echo "Create lib directory"
mkdir "$INSTALLATION_DIRECTORY"/lib

###############################################################################
# Create run script
################################################################################
printInfo "Create run script ..."

cd "$INSTALLATION_DIRECTORY"

# Determine name of jar file.
jarFile=(`ls $REPO_NAME*.jar`)

echo "#!/bin/bash"                                                                              >  run.sh
echo "################################################################################"         >> run.sh
echo "# Run microservice '$REPO_NAME'"                                                          >> run.sh
echo "# /"                                                                                      >> run.sh
echo "# |- application.properties    - Default configuration for microservice"                  >> run.sh
echo "# |- '$REPO_NAME'*.jar"        - Microservice                                             >> run.sh
echo "# |- run.sh                    - Start script    "                                        >> run.sh
echo "# |- lib/                      - Directory for plugins (if supported)"                    >> run.sh
echo "# |- config/ "                                                                            >> run.sh
echo "#    |- application.properties - Overwrites default configuration (optional)"             >> run.sh
echo "################################################################################"         >> run.sh
echo " "                                                                                        >> run.sh
echo "################################################################################"         >> run.sh
echo "# Define jar file"                                                                        >> run.sh
echo "################################################################################"         >> run.sh
echo jarFile=$jarFile                                                                           >> run.sh
echo " "                                                                                        >> run.sh
echo "################################################################################"         >> run.sh
echo "# Determine directory of script."                                                         >> run.sh
echo "################################################################################"         >> run.sh
echo 'ACTUAL_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"'           >> run.sh
echo 'cd "$ACTUAL_DIR"'                                                                         >> run.sh
echo " "                                                                                        >> run.sh
echo "################################################################################"         >> run.sh
echo "# Start micro service"                                                                    >> run.sh
echo "################################################################################"         >> run.sh
echo 'java -cp ".:$jarFile" -Dloader.path="file://$ACTUAL_DIR/$jarFile,./lib/,." -jar $jarFile' >> run.sh

# make script executable
chmod 755 run.sh

echo .
printInfo "Now you can start the service by calling '$INSTALLATION_DIRECTORY/run.sh'"