#!/bin/bash
#========================================================================================
# Usage:
# See usage function.
#
# Description: 
# RHQ publish script. Supports publishing of artifacts to maven and sourceforge.
#
# Options: 
# See usage function.
#========================================================================================

#include the utility library
source `dirname $0`/rhq_bash.lib


#========================================================================================
# Description: Display usage information then abort the script.
#========================================================================================
usage() 
{   
      USAGE=$(
cat << EOF
USAGE:   publish.sh OPTIONS

   --release-version=version              [REQUIRED]
      The release version to be tagged by this script.

   --release-tag=git_tag                  [REQUIRED]
      Git branch to be used as base for tagging and/or branching.

   --release-type=community|enterprise    [REQUIRED]
      Type of release.

   --test-mode                            [OPTIONAL, DEFAULT]
      Run this script in test mode. Create a test branch from release branch and perform tagging and version updates on this test branch.

   --production-mode                      [OPTIONAL]
      Run this script in production mode. Follow the official branching and tagging model.

   --mode=test|production                 [OPTIONAL]
      Used to directly set the script mode.

   --extra-profile=extra_profile          [OPTIONAL]
      An extra maven profile to be used for all the maven commands.

   --debug=[true|false]                   [OPTIONAL]
      Set maven in debug mode. Default is false; true if option specified without argument.

   --workspace=workspace_to_use           [OPTIONAL]
      Override the workspace used by default by the script.
EOF
)

   EXAMPLE="publish.sh --release-type=\"enterprise\" --release-version=\"5.0.0.GA\" --release_tag=\"release_test_tag\""

   abort "$@" "$USAGE" "$EXAMPLE"
}


#========================================================================================
# Description: Validate and parse input arguments
#========================================================================================
parse_and_validate_options()
{
   print_function_information $FUNCNAME

   RELEASE_VERSION=
   RELEASE_TAG=
   RELEASE_TYPE="community"
   MODE="test"
   EXTRA_MAVEN_PROFILE=
   DEBUG_MODE=false

   SOURCEFORGE=false

   short_options="h"
   long_options="help,release-version:,release-tag:,release-type:,test-mode,production-mode,mode:,branch,tag,extra-profile:,debug::,workspace:,sourceforge"

   PROGNAME=${0##*/}
   ARGS=$(getopt -s bash --options $short_options --longoptions $long_options --name $PROGNAME -- "$@" )
   eval set -- "$ARGS"

   while true; do
	   case $1 in
         -h|--help)
            usage
            ;;
         --release-version)
            shift
            RELEASE_VERSION="$1"
            shift
            ;;
         --release-tag)
            shift
            RELEASE_TAG="$1"
            shift
            ;;
         --release-type)
            shift
            RELEASE_TYPE="$1"
            shift
            ;;
         --test-mode)
            MODE="test"
            shift
            ;;
         --production-mode)
            MODE="production"
            shift
            ;;
         --mode)
            shift
            MODE="$1"
            shift
            ;;
         --extra-profile)
            shift
            EXTRA_MAVEN_PROFILE="$1"
            shift
            ;;
         --debug)
            shift
            case "$1" in
               true)
                  DEBUG_MODE=true
                  shift
                  ;;
               false)
                  DEBUG_MODE=false
                  shift
                  ;;
               "")
                  DEBUG_MODE=true
                  shift
                  ;;
               *)
                  DEBUG_MODE=false
                  shift
                  ;;
            esac
            ;;
         --workspace)
            shift
            WORKSPACE=$1
            shift
            ;;
         --sourceforge)
            SOURCEFORGE=true
            shift
            ;;
         --)
            shift
            break
            ;;
         *)
            usage
            ;;
	   esac
   done

   if [ "$MODE" = "production" ]; then
      if [ -z "$JBOSS_ORG_USERNAME" ] || [ -z "$JBOSS_ORG_PASSWORD" ]; then
         usage "In production mode, jboss.org credentials must be specified via the JBOSS_ORG_USERNAME and JBOSS_ORG_PASSWORD environment variables."
      fi
   fi


   if [ -z "$RELEASE_VERSION" ];
   then
      usage "Release version not specified!"
   fi

   if [ -z "$DEVELOPMENT_VERSION" ];
   then
      usage "Development version not specified!"
   fi

   if [ -z "$RELEASE_TAG" ];
   then
      usage "Release tag not specified!"
   fi

   if [ "$RELEASE_TYPE" != "community" ] && [ "$RELEASE_TYPE" != "enterprise" ];
   then
      usage "Invalid release type: $RELEASE_TYPE (valid release types are 'community' or 'enterprise')"
   fi

   if [ "$MODE" != "test" ] && [ "$MODE" != "production" ];
   then
      usage "Invalid script mode: $MODE (valid modes are 'test' or 'production')"
   fi

   print_centered "Script Options"
   script_options=( "RELEASE_VERSION" "RELEASE_TAG" "RELEASE_TYPE" \
                     "MODE" )
   print_variables "${script_options[@]}"
}


#========================================================================================
# Description: Set all the local and environment variables required by the script.
#========================================================================================
set_local_and_environment_variables()
{
   print_function_information $FUNCNAME

   # Set various environment variables.
   MAVEN_OPTS="-Xms512M -Xmx1024M"
   [[ $("$JAVA_HOME/bin/java" -version 2>&1 | awk -F '"' '/version/ {print $2}') > "1.8" ]] || MAVEN_OPTS="$MAVEN_OPTS -XX:PermSize=128M -XX:MaxPermSize=256M"
   export MAVEN_OPTS

   # Set various local variables
   if [ -n "$WORKSPACE" ]; then
      echo "Running script in a Hudson job."
      MAVEN_LOCAL_REPO_DIR="$WORKSPACE/.m2/repository"
      if [ ! -d "$MAVEN_LOCAL_REPO_DIR" ]; then
         mkdir -p "$MAVEN_LOCAL_REPO_DIR"
      fi
      MAVEN_SETTINGS_FILE="$WORKSPACE/.m2/settings.xml"
   else
      MAVEN_LOCAL_REPO_DIR="$HOME/.m2/repository"
      MAVEN_SETTINGS_FILE="$HOME/.m2/settings.xml"
   fi

   MAVEN_ARGS="--settings $MAVEN_SETTINGS_FILE -Dmaven.repo.local=$MAVEN_LOCAL_REPO_DIR --batch-mode --errors"


   if [ -n "$EXTRA_MAVEN_PROFILE" ];
   then
      MAVEN_ARGS="$MAVEN_ARGS --activate-profiles $EXTRA_MAVEN_PROFILE,enterprise,dist"
   else
      MAVEN_ARGS="$MAVEN_ARGS --activate-profiles enterprise,dist"
   fi

   if [ "$MODE" = "test" ]; then
      MAVEN_ARGS="$MAVEN_ARGS -DskipTests=true"
   fi

   if [ "$RELEASE_TYPE" = "enterprise" ]; then
      MAVEN_ARGS="$MAVEN_ARGS -Dexclude-webdav "
   fi

   if [ -n "$DEBUG_MODE" ]; then
      echo "Maven debug enabled"
      MAVEN_ARGS="$MAVEN_ARGS --debug"
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

   # Print out a summary of the environment.
   print_centered "Environment Variables"
   environment_variables=("JAVA_HOME" "M2_HOME" "MAVEN_OPTS" "PATH" "LANG")
   print_variables "${environment_variables[@]}"

   print_centered "Local Variables"
   local_variables=("WORKING_DIR" "RELEASE_TYPE" \
                     "RELEASE_TAG" "MODE" "MAVEN_LOCAL_REPO_DIR" \
                     "MAVEN_SETTINGS_FILE" "MAVEN_ARGS" "MAVEN_RELEASE_PERFORM_GOAL" )
   print_variables "${local_variables[@]}"
}


#========================================================================================
# Description: Run the validation process for all the system utilities needed by
#              the script. At the end print the version of each utility.
#========================================================================================
validate_system_utilities()
{
   print_function_information $FUNCNAME

   # TODO: Check that JDK version is < 1.7.

   validate_java_6

   validate_java_5

   validate_maven

   validate_git

   print_centered "Program Versions"
   program_versions=("git --version" "java -version" "mvn --version")
   print_program_versions "${program_versions[@]}"
}


#========================================================================================
# Description: Checkout release branch.
#========================================================================================
checkout_release_tag()
{
   print_function_information $FUNCNAME

   # Checkout the source from git, assume that the git repo is already cloned
   git status >/dev/null 2>&1
   GIT_STATUS_EXIT_CODE=$?
   # Note, git 1.6 and earlier returns an exit code of 1, rather than 0, if there are any uncommitted changes,
   # and git 1.7 returns 0, so we check if the exit code is less than or equal to 1 to determine if current folder
   # is truly a git working copy.
   if [ "$GIT_STATUS_EXIT_CODE" -le 1 ];
   then
       echo "Checking out a clean copy of the release branch ($RELEASE_TAG)..."
       git fetch origin "refs/tags/$RELEASE_TAG"
       [ "$?" -ne 0 ] && abort "Failed to fetch release branch ($RELEASE_TAG)."

       git checkout "refs/tags/$RELEASE_TAG"
       [ "$?" -ne 0 ] && abort "Failed to checkout release branch ($RELEASE_TAG)." 
   else
       echo "Current folder does not appear to be a git working directory ('git status' returned $GIT_STATUS_EXIT_CODE) - removing it so we can freshly clone the repo..."
   fi
}


#========================================================================================
# Description: Build source code.
#========================================================================================
build_from_source()
{
   echo "Building release from tag"
   mvn clean install $MAVEN_ARGS -Ddbreset
   [ "$?" -ne 0 ] && abort "Release build failed. Please see above Maven output for details, fix any issues, then try again."
   echo
   echo "Release build succeeded!"
}


#========================================================================================
# Description: Publish artifacts to external maven repostory.
#========================================================================================
publish_external_maven_repository()
{
   #mvn -Ddbsetup-do-not-check-schema=true -Dmaven.test.skip=true -P publish $MAVEN_RELEASE_PERFORM_GOAL
   #[ "$?" -ne 0 ] && abort "Release build failed. Please see above Maven output for details, fix any issues, then try again."
}


#========================================================================================
# Description: Publish artifacts to RHQ website.
#========================================================================================
publish_sourceforge()
{
   if [ -n "SOURCEFORGE"];
   then
      $FILE_PATH="modules/enterprise/server/container/target/rhq-server*.zip"
      scp $FILE_PATH frs.sourceforge.net:/home/frs/project/r/rh/rhq/rhq_4.3
   fi
}



############ MAIN SCRIPT ############

parse_and_validate_options $@

set_script_debug_mode $DEBUG_MODE

validate_system_utilities

set_local_and_environment_variables

checkout_release_tag

build_from_source

publish_external_maven_repository

publish_sourceforge

unset_script_debug_mode $DEBUG_MODE
