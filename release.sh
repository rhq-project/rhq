#!/bin/sh

source `dirname $0`/rhq_bash.lib

# Functions

abort()
{
   echo >&2
   for ARG in "$@"; do
      echo "$ARG" >&2
   done
   exit 1
}

usage() 
{   
   abort "$@" "Usage:   $EXE community|enterprise RELEASE_VERSION DEVELOPMENT_VERSION RELEASE_BRANCH GIT_USERNAME test|production" "Example: $EXE enterprise 3.0.0.GA 3.0.0-SNAPSHOT release-3.0.0 ips test"
}

set_variables()
{
   # Constants

   PROJECT_NAME="rhq"
   PROJECT_DISPLAY_NAME="RHQ"
   PROJECT_GIT_WEB_URL="http://git.fedorahosted.org/git/?p=rhq/rhq.git"
   TAG_PREFIX="RHQ"
   MINIMUM_MAVEN_VERSION="2.1.0"


   # Process command line args.

   EXE=`basename $0`
   if [ "$#" -ne 6 ]; then
      usage
   fi  
   RELEASE_TYPE="$1"
   if [ "$RELEASE_TYPE" != "community" ] && [ "$RELEASE_TYPE" != "enterprise" ]; then
      usage "Invalid release type: $RELEASE_TYPE (valid release types are 'community' or 'enterprise')"
   fi
   RELEASE_VERSION="$2"
   DEVELOPMENT_VERSION="$3"
   RELEASE_BRANCH="$4"
   GIT_USERNAME="$5"
   MODE="$6"
   if [ "$MODE" != "test" ] && [ "$MODE" != "production" ]; then
      usage "Invalid mode: $MODE (valid modes are 'test' or 'production')"
   fi

   if [ "$MODE" = "production" ]; then
      if [ -z "$JBOSS_ORG_USERNAME" ] || [ -z "$JBOSS_ORG_PASSWORD" ]; then
         usage "In production mode, jboss.org credentials must be specified via the JBOSS_ORG_USERNAME and JBOSS_ORG_PASSWORD environment variables."
      fi    
   fi

   # Set various environment variables.

   MAVEN_OPTS="-Xms512M -Xmx1024M -XX:PermSize=128M -XX:MaxPermSize=256M"
   export MAVEN_OPTS


   # Set various local variables.

   if [ -n "$HUDSON_URL" ] && [ -n "$WORKSPACE" ]; then
      echo "We appear to be running in a Hudson job." 
      WORKING_DIR="$WORKSPACE"
      MAVEN_LOCAL_REPO_DIR="$HOME/.m2/hudson-release-$RELEASE_TYPE-repository"
      MAVEN_SETTINGS_FILE="$HOME/.m2/hudson-$JOB_NAME-settings.xml"
   elif [ -z "$WORKING_DIR" ]; then
      WORKING_DIR="$HOME/release/rhq"
   #   MAVEN_LOCAL_REPO_DIR="$HOME/release/m2-repository"
      MAVEN_LOCAL_REPO_DIR="$HOME/.m2/repository"
      MAVEN_SETTINGS_FILE="$HOME/release/m2-settings.xml"
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

if [ -n "$RELEASE_DEBUG" ]; then
   echo "Debug output is enabled."
   set -x
fi

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
                  "MAVEN_SETTINGS_FILE" "MAVEN_ARGS" "MAVEN_RELEASE_PERFORM_GOAL" "JBOSS_ORG_USERNAME" \
                  "RELEASE_VERSION" "RELEASE_TAG")
print_variables "${local_variables[@]}"



echo "============================= Program Versions ================================"
program_versions=("git --version" "java -version" "mvn --version")
print_program_versions "${program_versions[@]}"

echo "==============================================================================="

# Clone and/or checkout the source from git.

if [ -d "$WORKING_DIR" ]; then
   cd "$WORKING_DIR"
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
fi
if [ ! -d "$WORKING_DIR" ]; then
   echo "Cloning the $PROJECT_NAME git repo (this will take about 10-15 minutes)..."
   git clone "$PROJECT_GIT_URL" "$WORKING_DIR"
   [ "$?" -ne 0 ] && abort "Failed to clone $PROJECT_NAME git repo ($PROJECT_GIT_URL)."
   cd "$CLONE_DIR"
   if [ "$RELEASE_BRANCH" != "master" ]; then
       git checkout --track -b $RELEASE_BRANCH "origin/$RELEASE_BRANCH"
   fi
   [ "$?" -ne 0 ] && abort "Failed to checkout release branch ($RELEASE_BRANCH)."
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
    if [ -n "$EXISTING_BUILD_BRANCH" ]; then
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
             
             
# We should now have the build_branch checked out
echo "Current Branch is $BUILD_BRANCH"

# If the specified tag already exists remotely and we're in production mode, then abort. If it exists and 
# we're in test mode, delete it

EXISTING_REMOTE_TAG=`git ls-remote --tags origin "$RELEASE_TAG"`
if [ -n "$EXISTING_REMOTE_TAG" ] && [ "$MODE" = "production" ]; then
   abort "A remote tag named $RELEASE_TAG already exists - aborting, since we are in production mode..." 
fi   

if [ -n "$EXISTING_REMOTE_TAG" ] && [ "$MODE" = "test" ]; then
   echo "A remote tag named $RELEASE_TAG already exists - deleting it, since we are in test mode..."      
   git push origin ":refs/tags/$RELEASE_TAG"
   [ "$?" -ne 0 ] && abort "Failed to delete remote tag ($RELEASE_TAG)."
fi   


# See if the specified tag already exists locally - if so, delete it (even if in production mode).

EXISTING_LOCAL_TAG=`git tag -l "$RELEASE_TAG"`
if [ -n "$EXISTING_LOCAL_TAG" ]; then
   echo "A local tag named $RELEASE_TAG already exists - deleting it..."      
   git tag -d "$RELEASE_TAG"
   [ "$?" -ne 0 ] && abort "Failed to delete local tag ($RELEASE_TAG)."
fi 
 
# Run a test build before tagging. This will publish the snapshot artifacts to the local repo to "bootstrap" the repo.

#echo "Building project to ensure tests pass and to bootstrap local Maven repo (this will take about 15-30 minutes)..."
# NOTE: There is no need to do a mvn clean below, since we just did either a clone or clean checkout above.
#mvn install $MAVEN_ARGS -Ddbreset
#[ "$?" -ne 0 ] && abort "Test build failed. Please see above Maven output for details, fix any issues, then try again."
#echo
#echo "Test build succeeded!"


# Clean up the snapshot jars produced by the test build from module target dirs.

echo "Cleaning up snapshot jars produced by test build from module target dirs..."
mvn clean $MAVEN_ARGS
[ "$?" -ne 0 ] && abort "Failed to cleanup snbapshot jars produced by test build from module target dirs. Please see above Maven output for details, fix any issues, then try again."


# If this is a production build perform a dry run of tagging the release. Skip this for test builds to reduce the
# build time 

if [ "$MODE" = "todo" ]; then
    echo "Doing a dry run of tagging the release..."
    mvn release:prepare $MAVEN_ARGS -DreleaseVersion=$RELEASE_VERSION -DdevelopmentVersion=$DEVELOPMENT_VERSION -Dresume=false -Dtag=$RELEASE_TAG "-DpreparationGoals=install $MAVEN_ARGS -DskipTests=true -Ddbsetup-do-not-check-schema=true" -DdryRun=true
    [ "$?" -ne 0 ] && abort "Tagging dry run failed. Please see above Maven output for details, fix any issues, then try again."
    mvn release:clean $MAVEN_ARGS
    [ "$?" -ne 0 ] && abort "Failed to cleanup release plugin working files from tagging dry run. Please see above Maven output for details, fix any issues, then try again."
    echo
    echo "Tagging dry run succeeded!"
fi


# If the dry run was skipped or succeeded, tag it for real.

echo "Tagging the release..."
mvn release:prepare $MAVEN_ARGS -DreleaseVersion=$RELEASE_VERSION -DdevelopmentVersion=$DEVELOPMENT_VERSION -Dresume=false -Dtag=$RELEASE_TAG "-DpreparationGoals=install $MAVEN_ARGS -DskipTests=true -Ddbsetup-do-not-check-schema=true" -DdryRun=false -Dusername=$GIT_USERNAME
[ "$?" -ne 0 ] && abort "Tagging failed. Please see above Maven output for details, fix any issues, then try again."
echo
echo "Tagging succeeded!"


# Checkout the tag and build it. If in production mode, publish the Maven artifacts.

#echo "Checking out release tag $RELEASE_TAG..."
#git checkout "$RELEASE_TAG"
#[ "$?" -ne 0 ] && abort "Checkout of release tag ($RELEASE_TAG) failed. Please see above git output for details, fix any issues, then try again."
#git clean -dxf
#[ "$?" -ne 0 ] && abort "Failed to cleanup unversioned files. Please see above git output for details, fix any issues, then try again."
#echo "Building release from tag and publishing Maven artifacts (this will take about 10-15 minutes)..."
#mvn $MAVEN_RELEASE_PERFORM_GOAL $MAVEN_ARGS -Dmaven.test.skip=true -Ddbsetup-do-not-check-schema=true
#[ "$?" -ne 0 ] && abort "Release build failed. Please see above Maven output for details, fix any issues, then try again."
#echo
#echo "Release build succeeded!"


echo
echo "=============================== Release Info =================================="
echo "Version: $RELEASE_VERSION"
echo "Branch URL: $PROJECT_GIT_WEB_URL;a=shortlog;h=refs/heads/$RELEASE_BRANCH"
echo "Tag URL: $PROJECT_GIT_WEB_URL;a=shortlog;h=refs/tags/$RELEASE_TAG"
echo "==============================================================================="

