#!/bin/bash
#========================================================================================
#
# Usage: release.sh community|enterprise RELEASE_VERSION DEVELOPMENT_VERSION RELEASE_BRANCH GIT_USERNAME test|production
#
# Description: 
# Add description here
#
# Options: 
# Add option description here
#========================================================================================

#include the utility library
source `dirname $0`/rhq_bash.lib


#========================================================================================
# Description: Display an error message and abort the script.
#========================================================================================
abort()
{
   echo >&2
   for ARG in "$@"; do
      echo "$ARG" >&2
      echo "">&2
   done
   exit 1
}


#========================================================================================
# Description: Display usage information then abort the script.
#========================================================================================
usage() 
{
   USAGE=$(
cat << EOF
USAGE:   release.sh OPTIONS
   --release-type=community|enterprise    [REQUIRED]
      Type of release.

   --release-version=version
      The release version to be tagged by this script.

   --development-version=version          [REQUIRED]
      The version under which development will continue after tagging.

   --release-branch=git_branch            [REQUIRED]
      Git branch to be used as base for tagging and/or branching.

   --git-username=username                [REQUIRED]
      Git username for authentication. Please make sure that the user is authenticated prior to using this script.

   --branch                               [OPTIONAL]
      Branch from release branch before tagging the release version. And updated development version on original branch.

   --test-mode                            [OPTIONAL]
      Run this script in test mode. Create a test branch from release branch and perform tagging and version updates on this test branch.

   --tag-only                             [DEFAULT]
      Use the release branch to tag the release version. And update development version on the same branch.
EOF
)

   EXAMPLE="release.sh --release-type=\"enterprise\" --release-version=\"5.0.0.GA\" --development-version=\"5.0.1-SNAPSHOT\" --release-branch=\"stefan/release_test\" --git-username=\"user\" --branch"

   abort "$@" "$USAGE" "$EXAMPLE"
}


#========================================================================================
# Description: Validate and parse input arguments
#========================================================================================
parse_validate_options()
{
   RELEASE_VERSION=
   DEVELOPMENT_VERSION=
   RELEASE_BRANCH=
   RELEASE_TYPE=
   GIT_USERNAME=
   MODE="production"
   SCM_STRATEGY="tag"

   short_options="h"
   long_options="help,release-version:,development-version:,release-branch:,git-username:,release-type:,test-mode,branch,tag-only"

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
		   --development-version)
			   shift
            DEVELOPMENT_VERSION=$1
            shift
			   ;;
         --release-branch)
            shift
            RELEASE_BRANCH=$1
            shift
            ;;
         --release-type)
            shift
            RELEASE_TYPE=$1
            shift
            ;;
         --git-username)
            shift
            GIT_USERNAME=$1
            shift
            ;;
         --test-mode)
            MODE="test"
            shift
            ;;
         --tag-only)
            SCM_STRATEGY="tag"
            shift
            ;;
         --branch)
            SCM_STRATEGY="branch"
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

   if [ -z $RELEASE_VERSION ];
   then
      usage "Release version not specified!"
   fi

   if [ -z $DEVELOPMENT_VERSION ];
   then
      usage "Development version not specified!"
   fi

   if [ -z $RELEASE_BRANCH ];
   then
      usage "Release branch not specified!"
   fi

   if [ "$RELEASE_TYPE" != "community" ] && [ "$RELEASE_TYPE" != "enterprise" ]; then
      usage "Invalid release type: $RELEASE_TYPE (valid release types are 'community' or 'enterprise')"
   fi

   if [ -z $GIT_USERNAME ];
   then
      usage "Git username not specified!"
   fi

   print_centered "Script Options"
   script_options=( "RELEASE_VERSION" "DEVELOPMENT_VERSION" "RELEASE_BRANCH" "RELEASE_TYPE" "GIT_USERNAME" \
                     "MODE" "SCM_STRATEGY")
   print_variables "${script_options[@]}"

   #if [ "$MODE" = "production" ]; then
   #   if [ -z "$JBOSS_ORG_USERNAME" ] || [ -z "$JBOSS_ORG_PASSWORD" ]; then
   #      usage "In production mode, jboss.org credentials must be specified via the JBOSS_ORG_USERNAME and JBOSS_ORG_PASSWORD environment variables."
   #   fi
   #fi
}

#========================================================================================
# Description: Set all the local and environment variables required by the script.
#========================================================================================
set_variables()
{
   # Constants

   PROJECT_NAME="rhq"
   PROJECT_DISPLAY_NAME="RHQ"
   PROJECT_GIT_WEB_URL="http://git.fedorahosted.org/git/?p=rhq/rhq.git"
   TAG_PREFIX="RHQ"
   MINIMUM_MAVEN_VERSION="2.1.0"

   # Set various environment variables.

   MAVEN_OPTS="-Xms512M -Xmx1024M -XX:PermSize=128M -XX:MaxPermSize=256M"
   export MAVEN_OPTS

   # Set various local variables.

   if [ -n "$WORKSPACE" ]; then
      echo "We appear to be running in a Hudson job." 
      MAVEN_LOCAL_REPO_DIR="$WORKSPACE/.m2/repository"
      MAVEN_SETTINGS_FILE="$WORKSPACE/.m2/settings.xml"
   else
      MAVEN_LOCAL_REPO_DIR="$HOME/.m2/repository"
      MAVEN_SETTINGS_FILE="$HOME/.m2/settings.xml"
   fi

   PROJECT_GIT_URL="ssh://${GIT_USERNAME}@git.fedorahosted.org/git/rhq/rhq.git"

   MAVEN_ARGS="--settings $MAVEN_SETTINGS_FILE --batch-mode --errors -Penterprise,dist,release"
   # TODO: We may eventually want to reenable tests for production releases.
   #if [ "$MODE" = "test" ]; then
   #   MAVEN_ARGS="$MAVEN_ARGS -DskipTests=true"
   #fi
   MAVEN_ARGS="$MAVEN_ARGS -DskipTests=true"
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

   # TODO: We may eventually want to reenable publishing of enterprise artifacts.
   #if [ "$MODE" = "production" ] && [ "$RELEASE_TYPE" = "community" ]; then
   #   MAVEN_RELEASE_PERFORM_GOAL="deploy"
   #else   
      MAVEN_RELEASE_PERFORM_GOAL="install"
   #fi

   TAG_VERSION=`echo $RELEASE_VERSION | sed 's/\./_/g'`
   RELEASE_TAG="${TAG_PREFIX}_${TAG_VERSION}"

   # Set the system character encoding to ISO-8859-1 to ensure i18log reads its 
   # messages and writes its resource bundle properties files in that encoding, 
   # since that is how the German and French I18NMessage annotation values are
   # encoded and the encoding used by i18nlog to read in resource bundle
   # property files.
   LANG=en_US.iso8859
   export LANG
}

#========================================================================================
# Description: Perform version update process and test the outcome by building 
#              from source.
#========================================================================================
run_tag_version_process()
{
   # 1) Cleanup before doing anything
   echo "Cleaning up module target dirs"
   mvn clean $MAVEN_ARGS
   [ "$?" -ne 0 ] && abort "Failed to cleanup snbapshot jars from module target dirs. Please see above Maven output for details, fix any issues, then try again."

   # 2) Perform a test build before changing version
   mvn install $MAVEN_ARGS -Ddbreset
   [ "$?" -ne 0 ] && abort "Test build failed. Please see output for details, fix any issues, then try again."

   # 3) Run another cleanup
   echo "Cleaning up module target dirs..."
   mvn clean $MAVEN_ARGS
   [ "$?" -ne 0 ] && abort "Failed to cleanup snbapshot jars produced by test build from module target dirs. Please see above Maven output for details, fix any issues, then try again."

   # 4) Increment version on all poms
   mvn versions:set versions:use-releases -DnewVersion=$RELEASE_VERSION  -DallowSnapshots=false -DgenerateBackupPoms=false
   [ "$?" -ne 0 ] && abort "Version set failed. Please see output for details, fix any issues, then try again."

   # 5) Perform a test build with the new version   
   mvn install $MAVEN_ARGS -DskipTests=true -Ddbsetup-do-not-check-schema=true
   [ "$?" -ne 0 ] && abort "Maven build for new version failed. Please see output for details, fix any issues, then try again."

   # 6) Publish release artifacts
   #echo "Building release from tag and publishing Maven artifacts (this will take about 10-15 minutes)..."
   #mvn $MAVEN_RELEASE_PERFORM_GOAL $MAVEN_ARGS -Dmaven.test.skip=true -Ddbsetup-do-not-check-schema=true
   #[ "$?" -ne 0 ] && abort "Release build failed. Please see above Maven output for details, fix any issues, then try again."

   # 7) Cleanup after this test build
   echo "Cleaning up module target dirs..."
   mvn clean $MAVEN_ARGS
   [ "$?" -ne 0 ] && abort "Failed to cleanup snbapshot jars produced by test build from module target dirs. Please see above Maven output for details, fix any issues, then try again."

   # 8) Commit the change in version (if everything went well so far then this is a good tag
   git add -u
   git commit -m "tag $RELEASE_TAG"
   
   # 9) Tag the current source
   git tag "$RELEASE_TAG"

   # 10) Set version to the current development version
   mvn versions:set versions:use-releases -DnewVersion=$DEVELOPMENT_VERSION  -DallowSnapshots=false -DgenerateBackupPoms=false
   [ "$?" -ne 0 ] && abort "Version set failed. Please see output for details, fix any issues, then try again."

   # 11) Commit the change in version (if everything went well so far then this is a good tag
   git add -u
   git commit -m "development RHQ_$DEVELOPMENT_VERSION"

   # 12) If everything went well so far than means all the changes can be pushed!!!
   git push origin "$BUILD_BRANCH"
   git push origin "$RELEASE_TAG"
}

if [ -n "$RELEASE_DEBUG" ];
then
   echo "Debug output is enabled."
   set -x
fi

# TODO: Check that JDK version is < 1.7.

parse_validate_options $@

validate_java_6

validate_java_5

validate_maven

validate_git

set_variables

# Print out a summary of the environment.
print_centered "Environment Variables"
environment_variables=("JAVA_HOME" "M2_HOME" "MAVEN_OPTS" "PATH" "LANG" "RELEASE_TYPE")
print_variables "${environment_variables[@]}"


print_centered "Local Variables"
local_variables=( "PROJECT_NAME" "PROJECT_GIT_URL" "RELEASE_TYPE" "DEVELOPMENT_VERSION" \
                  "RELEASE_BRANCH" "MODE" "MAVEN_LOCAL_REPO_DIR" \
                  "MAVEN_SETTINGS_FILE" "MAVEN_ARGS" "MAVEN_RELEASE_PERFORM_GOAL" "JBOSS_ORG_USERNAME" \
                  "RELEASE_VERSION" "RELEASE_TAG")
print_variables "${local_variables[@]}"


print_centered "Program Versions"
program_versions=("git --version" "java -version" "mvn --version")
print_program_versions "${program_versions[@]}"

print_centered "="

# Checkout the source from git, assume that the git repo is already cloned
git status >/dev/null 2>&1
GIT_STATUS_EXIT_CODE=$?
# Note, git 1.6 and earlier returns an exit code of 1, rather than 0, if there are any uncommitted changes,
# and git 1.7 returns 0, so we check if the exit code is less than or equal to 1 to determine if current folder
# is truly a git working copy.
if [ "$GIT_STATUS_EXIT_CODE" -le 1 ]; 
then
    echo "Checking out a clean copy of the release branch ($RELEASE_BRANCH)..."
    git fetch origin "$RELEASE_BRANCH"
    [ "$?" -ne 0 ] && abort "Failed to fetch release branch ($RELEASE_BRANCH)."

    git checkout "$RELEASE_BRANCH" 2>/dev/null
    if [ "$?" -ne 0 ]; 
    then
        git checkout --track -b "$RELEASE_BRANCH" "origin/$RELEASE_BRANCH"
    fi

    [ "$?" -ne 0 ] && abort "Failed to checkout release branch ($RELEASE_BRANCH)."
    git reset --hard "origin/$RELEASE_BRANCH"
    [ "$?" -ne 0 ] && abort "Failed to reset release branch ($RELEASE_BRANCH)."
    git clean -dxf
    [ "$?" -ne 0 ] && abort "Failed to clean release branch ($RELEASE_BRANCH)."
    git pull origin $RELEASE_BRANCH
    [ "$?" -ne 0 ] && abort "Failed to update release branch ($RELEASE_BRANCH)."
else
    echo "Current folder does not appear to be a git working directory ('git status' returned $GIT_STATUS_EXIT_CODE) - removing it so we can freshly clone the repo..."
fi

# if this is a test build then create a temporary build branch off of RELEASE_BRANCH.  This allows checkins to
# continue in RELEASE_BRANCH without affecting the release plugin work, which will fail if the branch contents
# change before it completes.
if [ "$MODE" = "production" ]; then  
    BUILD_BRANCH="${RELEASE_BRANCH}"
else
    BUILD_BRANCH="${RELEASE_BRANCH}-test-build"
#   delete the branch if it exists, so we can recreate it fresh     
    EXISTING_BUILD_BRANCH=`git ls-remote --heads origin "$BUILD_BRANCH"`
    if [ -n "$EXISTING_BUILD_BRANCH" ]; 
    then
        echo "Deleting remote branch origin/$BUILD_BRANCH"    
        git branch -D -r "origin/$BUILD_BRANCH"
        echo "Deleting local branch $BUILD_BRANCH"        
        git branch -D "$BUILD_BRANCH"
    fi
    echo "Creating and checking out local branch $BUILD_BRANCH from $RELEASE_BRANCH"    
    git checkout -b "$BUILD_BRANCH"
    echo "Creating remote branch $BUILD_BRANCH"  
    git pull origin "$BUILD_BRANCH"
    git push origin "$BUILD_BRANCH"    
fi

# If the specified tag already exists remotely and we're in production mode, then abort. If it exists and 
# we're in test mode, delete it
EXISTING_REMOTE_TAG=`git ls-remote --tags origin "$RELEASE_TAG"`
if [ -n "$EXISTING_REMOTE_TAG" ];
then
   abort "A remote tag named $RELEASE_TAG already exists - aborting" 
fi   

# See if the specified tag already exists locally - if so, delete it (even if in production mode).
# If the tag is just local then there were errors during the last run; no harm in removing it.
EXISTING_LOCAL_TAG=`git tag -l "$RELEASE_TAG"`
if [ -n "$EXISTING_LOCAL_TAG" ]; 
then
   echo "A local tag named $RELEASE_TAG already exists - deleting it..."
   git tag -d "$RELEASE_TAG"
   [ "$?" -ne 0 ] && abort "Failed to delete local tag ($RELEASE_TAG)."
fi 

run_tag_version_process

echo
echo "=============================== Release Info =================================="
echo "Version: $RELEASE_VERSION"
echo "Branch URL: $PROJECT_GIT_WEB_URL;a=shortlog;h=refs/heads/$RELEASE_BRANCH"
echo "Tag URL: $PROJECT_GIT_WEB_URL;a=shortlog;h=refs/tags/$RELEASE_TAG"
echo "==============================================================================="
