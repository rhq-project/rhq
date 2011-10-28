#!/bin/sh

source `dirname $0`/rhq_bash.lib

# Functions

usage() 
{   
   EXE=`basename $0`

   abort "$@" "Usage:   $EXE community|enterprise RELEASE_BRANCH test|production" "Example: $EXE enterprise release-3.0.0 test"
}

validate_arguments()
{
   if [ "$#" -ne 3 ]; then
      usage
   fi
   RELEASE_TYPE="$1"
   if [ "$RELEASE_TYPE" != "community" ] && [ "$RELEASE_TYPE" != "enterprise" ]; then
      usage "Invalid release type: $RELEASE_TYPE (valid release types are 'community' or 'enterprise')"
   fi
   RELEASE_BRANCH="$2"
   MODE="$3"

   if [ "$MODE" != "test" ] && [ "$MODE" != "production" ]; then
      usage "Invalid mode: $MODE (valid modes are 'test' or 'production')"
   fi

   if [ "$MODE" = "production" ]; then
      if [ -z "$JBOSS_ORG_USERNAME" ] || [ -z "$JBOSS_ORG_PASSWORD" ]; then
         usage "In production mode, jboss.org credentials must be specified via the JBOSS_ORG_USERNAME and JBOSS_ORG_PASSWORD environment variables."
      fi
   fi
}

set_variables()
{
   PROJECT_NAME="rhq"
   PROJECT_DISPLAY_NAME="RHQ"
   PROJECT_GIT_WEB_URL="http://git.fedorahosted.org/git/?p=rhq/rhq.git"
   TAG_PREFIX="RHQ"
   MINIMUM_MAVEN_VERSION="2.1.0"

   # Set various environment variables.
   MAVEN_OPTS="-Xms512M -Xmx1024M -XX:PermSize=128M -XX:MaxPermSize=256M"
   export MAVEN_OPTS


   # Set various local variables.

   if [ -n "$HUDSON_URL" ] && [ -n "$WORKSPACE" ]; then
      echo "We appear to be running in a Hudson job." 
      WORKING_DIR="$WORKSPACE"
      MAVEN_LOCAL_REPO_DIR="$WORKSPACE/.m2/repository"
      #MAVEN_SETTINGS_FILE="$HOME/.m2/hudson-$JOB_NAME-settings.xml"
   elif [ -z "$WORKING_DIR" ]; then
      WORKING_DIR="$HOME/release/rhq"
      MAVEN_LOCAL_REPO_DIR="$HOME/release/m2-repository"
      #MAVEN_LOCAL_REPO_DIR="$HOME/.m2/repository"
      MAVEN_SETTINGS_FILE="$HOME/release/m2-settings.xml"
   fi

   MAVEN_SETTINGS_FILE="$WORKSPACE/settings.xml"

   PROJECT_GIT_URL="git://git.fedorahosted.org/rhq/rhq.git"

   MAVEN_ARGS="--settings $MAVEN_SETTINGS_FILE --batch-mode --errors -Prhq-publish-release,enterprise,dist -Dmaven.repo.local=$MAVEN_LOCAL_REPO_DIR"

   if [ "$MODE" = "test" ]; then
      MAVEN_ARGS="$MAVEN_ARGS -DskipTests=true"
   fi

   if [ "$RELEASE_TYPE" = "enterprise" ]; then
      MAVEN_ARGS="$MAVEN_ARGS -Dexclude-webdav "
      #MAVEN_ARGS="$MAVEN_ARGS -Dexclude-webdav -Djava5.home=$JAVA5_HOME/jre"
   fi
   if [ -n "$RELEASE_DEBUG" ]; then
      MAVEN_ARGS="$MAVEN_ARGS --debug"
   fi
   if [ -n "$RELEASE_ADDITIONAL_MAVEN_ARGS" ]; then
      MAVEN_ARGS="$MAVEN_ARGS $RELEASE_ADDITIONAL_MAVEN_ARGS"
   fi

   if [ "$MODE" = "production" ] && [ "$RELEASE_TYPE" = "community" ]; then
      MAVEN_RELEASE_PERFORM_GOAL="deploy"
   else
      MAVEN_RELEASE_PERFORM_GOAL="install"
   fi

   # Set the system character encoding to ISO-8859-1 to ensure i18log reads its 
   # messages and writes its resource bundle properties files in that encoding, 
   # since that is how the German and French I18NMessage annotation values are
   # encoded and the encoding used by i18nlog to read in resource bundle
   # property files.
   LANG=en_US.iso8859
   export LANG
}


#### THE RELEASE SCRIPT ######

if [ -n "$RELEASE_DEBUG" ]; then
   echo "Debug output is enabled."
   set -x
fi

# Process command line args.
validate_arguments $@

# TODO: Check that JDK version is < 1.7.

validate_java_6

validate_java_5

validate_maven

validate_git

set_variables

# Print out a summary of the environment.
echo "========================== Environment Variables =============================="
environment_variables=("JAVA_HOME" "M2_HOME" "MAVEN_OPTS" "PATH" "LANG" "RELEASE_TYPE")
print_variables "${environment_variables[@]}"


echo "============================= Local Variables ================================="
local_variables=("WORKING_DIR" "PROJECT_NAME" "PROJECT_GIT_URL" "RELEASE_TYPE" "DEVELOPMENT_VERSION" \
                  "RELEASE_BRANCH" "MODE" "MAVEN_LOCAL_REPO_DIR" \
                  "MAVEN_SETTINGS_FILE" "MAVEN_ARGS" "MAVEN_RELEASE_PERFORM_GOAL" "JBOSS_ORG_USERNAME")
print_variables "${local_variables[@]}"


echo "============================= Program Versions ================================"
program_versions=("git --version" "java -version" "mvn --version")
print_program_versions "${program_versions[@]}"

echo "==============================================================================="

# Checkout the source from git.
git status >/dev/null 2>&1
GIT_STATUS_EXIT_CODE=$?
# Note, git 1.6 and earlier returns an exit code of 1, rather than 0, if there are any uncommitted changes,
# and git 1.7 returns 0, so we check if the exit code is less than or equal to 1 to determine if $WORKING_DIR
# is truly a git working copy.
if [ "$GIT_STATUS_EXIT_CODE" -le 1 ]; then       
   echo "Checking out a clean copy of the release branch ($RELEASE_BRANCH)..."
   git fetch origin "$RELEASE_BRANCH"
   [ "$?" -ne 0 ] && abort "Failed to fetch release branch ($RELEASE_BRANCH)."
   git checkout "$RELEASE_BRANCH" 2>/dev/null
   if [ "$?" -ne 0 ]; then
       git checkout --track -b "$RELEASE_BRANCH" "origin/$RELEASE_BRANCH"
   fi
   [ "$?" -ne 0 ] && abort "Failed to checkout release branch ($RELEASE_BRANCH)."
   git reset --hard "origin/$RELEASE_BRANCH"
   [ "$?" -ne 0 ] && abort "Failed to reset release branch ($RELEASE_BRANCH)."
   git clean -dxf
   [ "$?" -ne 0 ] && abort "Failed to clean release branch ($RELEASE_BRANCH)."
   git pull
   [ "$?" -ne 0 ] && abort "Failed to update release branch ($RELEASE_BRANCH)."
else
   echo "$WORKING_DIR does not appear to be a git working directory ('git status' returned $GIT_STATUS_EXIT_CODE) - removing it so we can freshly clone the repo..."
   cd ..
   rm -rf "$WORKING_DIR"
   [ "$?" -ne 0 ] && abort "Failed to remove bogus working directory ($WORKING_DIR)."
fi


echo "Building release from tag and publishing Maven artifacts (this will take about 10-15 minutes)..."
mvn $MAVEN_RELEASE_PERFORM_GOAL $MAVEN_ARGS -Ddbreset
[ "$?" -ne 0 ] && abort "Release build failed. Please see above Maven output for details, fix any issues, then try again."
echo
echo "Release build succeeded!"
