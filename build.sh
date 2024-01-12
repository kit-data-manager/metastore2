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
  echo "Script for creating $REPO_NAME service."
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
    if ! mkdir -p "$INSTALLATION_DIRECTORY"; then
      echo "Error creating directory '$INSTALLATION_DIRECTORY'!"
      echo "Please make sure that you have the correct access permissions for the specified directory."
      exit 1
    fi
  fi
  # Check if directory is empty
  if [ -n "$(ls -A "$INSTALLATION_DIRECTORY")" ]; then
     echo "Directory '$INSTALLATION_DIRECTORY' is not empty!"
     echo "Please enter an empty or a new directory!"
     exit 1
  fi
  # Convert variable of installation directory to an absolute path
  cd "$INSTALLATION_DIRECTORY" || { echo "Failure changing to directory $INSTALLATION_DIRECTORY"; exit 1; }
  INSTALLATION_DIRECTORY=$(pwd)
  cd "$ACTUAL_DIR" || { echo "Failure changing to directory $ACTUAL_DIR"; exit 1; }
}

################################################################################
function printInfo {
################################################################################
  echo "---------------------------------------------------------------------------"
  echo "$*"
  echo "---------------------------------------------------------------------------"
}

################################################################################
# END DECLARATION FUNCTIONS / START OF SCRIPT
################################################################################

################################################################################
# Test for commands used in this script
################################################################################
testForCommands="chmod cp dirname find java javac mkdir git"

for command in $testForCommands
do 
  if ! type "$command" >> /dev/null; then
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
REPO_NAME=$(./gradlew -q printProjectName)
# Use only last line
REPO_NAME=${REPO_NAME##*$'\n'}

################################################################################
# Check parameters
################################################################################
checkParameters "$*"

printInfo "Build microservice of $REPO_NAME at '$INSTALLATION_DIRECTORY'"

################################################################################
# Build service
################################################################################
echo Build service...
./gradlew -Dprofile=minimal clean build


echo "Copy configuration to '$INSTALLATION_DIRECTORY'..."
find ./settings -name application-default.properties -exec cp '{}' "$INSTALLATION_DIRECTORY"/application.properties.temp \;

################################################################################
# Replace constants
################################################################################
while IFS='' read -r line; do
    echo "${line//INSTALLATION_DIR/$INSTALLATION_DIRECTORY}"
done < "$INSTALLATION_DIRECTORY"/application.properties.temp > "$INSTALLATION_DIRECTORY"/application.properties
rm "$INSTALLATION_DIRECTORY"/application.properties.temp

echo "Copy jar file to '$INSTALLATION_DIRECTORY'..."
find build/libs -name "$REPO_NAME*.jar" -exec cp '{}' "$INSTALLATION_DIRECTORY" \;

echo "Create config directory"
mkdir "$INSTALLATION_DIRECTORY"/config
echo "To overwrite default properties place 'application.properties' into this directory." > "$INSTALLATION_DIRECTORY"/config/README.txt
echo "Only changed properties should be part of this file." >> "$INSTALLATION_DIRECTORY"/config/README.txt

echo "Create lib directory"
mkdir "$INSTALLATION_DIRECTORY"/lib

###############################################################################
# Create run script
################################################################################
printInfo "Create run script ..."

cd "$INSTALLATION_DIRECTORY" || { echo "Failure changing to directory $INSTALLATION_DIRECTORY"; exit 1; }

# Determine name of jar file.
jarFile=($(ls $REPO_NAME*.jar))
# Create soft link for jar file
ln -s ${jarFile[0]} $REPO_NAME.jar

{
  echo "#!/bin/bash"                                                                             
  echo "################################################################################"        
  echo "# Run microservice '$REPO_NAME'"                                                         
  echo "# /"                                                                                     
  echo "# |- application.properties    - Default configuration for microservice"                 
  echo "# |- '$REPO_NAME'*.jar         - Microservice"
  echo "# |- run.sh                    - Start script"                                       
  echo "# |- lib/                      - Directory for plugins (if supported)"                   
  echo "# |- config/"                                                                           
  echo "#    |- application.properties - Overwrites default configuration (optional)"            
  echo "################################################################################"        
  echo " "                                                                                       
  echo "################################################################################"        
  echo "# Define jar file"                                                                       
  echo "################################################################################"        
  echo "jarFile=${REPO_NAME}.jar"
  echo " "                                                                                       
  echo "################################################################################"        
  echo "# Determine directory of script."                                                        
  echo "################################################################################"        
  echo "ACTUAL_DIR=\"\$( cd \"\$( dirname \"\${BASH_SOURCE[0]}\" )\" >/dev/null 2>&1 && pwd )\""
  echo "cd \"\$ACTUAL_DIR\""                                                                       
  echo " "                                                                                       
  echo "################################################################################"        
  echo "# Start micro service"                                                                   
  echo "################################################################################"        
  echo "java -cp \".:\$jarFile\" -Dloader.path=\"file://\$ACTUAL_DIR/\$jarFile,./lib/,.\" -jar \$jarFile \$*"
} > run.sh

# make script executable
chmod 755 run.sh

echo .
printInfo "Now you can start the service by calling '$INSTALLATION_DIRECTORY/run.sh'"
