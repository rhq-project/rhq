#!/bin/sh

# Constants

PROJECT_NAME="rhq"
PROJECT_DISPLAY_NAME="RHQ"
PROJECT_GIT_URL="ssh://git.fedorahosted.org/git/rhq/rhq.git"
PROJECT_GIT_WEB_URL="http://git.fedorahosted.org/git/?p=rhq/rhq.git"
TAG_PREFIX="RHQ"
MINIMUM_MAVEN_VERSION="2.1.0"


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
   abort "$@" "Usage:   $EXE community|enterprise RELEASE_VERSION DEVELOPMENT_VERSION" "Example: $EXE 3.0.0.GA 3.0.0-SNAPSHOT"   
}


# Process command line args.

EXE=`basename $0`
if [ "$#" -ne 3 ]; then
   usage
fi  
RELEASE_TYPE="$1"
if [ "$RELEASE_TYPE" != "community" ] && [ "$RELEASE_TYPE" != "enterprise" ]; then
   usage "Invalid release type: $RELEASE_TYPE"
fi
RELEASE_VERSION="$2"
TAG_VERSION=`echo $RELEASE_VERSION | sed 's/\./_/g'`
RELEASE_TAG="${TAG_PREFIX}_${TAG_VERSION}"
DEVELOPMENT_VERSION="$3"
BRANCH="master"
RELEASE_BRANCH="release-$RELEASE_VERSION"


# Make sure JAVA_HOME points to a valid JDK 1.6+ install.

if [ -z "$JAVA_HOME" ]; then
   abort "JAVA_HOME environment variable is not set - JAVA_HOME must point to a JDK (not JRE) 6 install dir."
fi

if [ ! -d "$JAVA_HOME" ]; then
   abort "JAVA_HOME ($JAVA_HOME) does not exist or is not a directory - JAVA_HOME must point to a JDK (not JRE) 6 install dir."
fi

echo "Prepending $JAVA_HOME/bin to PATH..."
PATH="$JAVA_HOME/bin:$PATH"

if ! which java >/dev/null 2>&1; then
   abort "java not found in PATH ($PATH) - JAVA_HOME must point to a JDK (not JRE) 6 install dir."
fi

if ! which javac >/dev/null 2>&1; then
   abort "javac not found in PATH ($PATH) - JAVA_HOME must point to a JDK (not JRE) 6 install dir."
fi

if ! javap java.util.Deque >/dev/null 2>&1; then
   abort "java.util.Deque not found - Java version appears to be less than 1.6 - Jave version must be 1.6 or later."
fi


# Make sure JAVA5_HOME points to a valid JDK 1.5 install. 
# We need this to validate only Java 5 or earlier APIs are used in all modules, except the CLI, which requires Java 6.

if [ -z "$JAVA5_HOME" ]; then
   abort "JAVA5_HOME environment variable is not set - JAVA5_HOME must point to a JDK (not JRE) 1.5 install dir."
fi

if [ ! -d "$JAVA5_HOME" ]; then
   abort "JAVA5_HOME ($JAVA5_HOME) does not exist or is not a directory - JAVA5_HOME must point to a JDK (not JRE) 1.5 install dir."
fi

if [ ! -x "$JAVA5_HOME/bin/java" ]; then
   abort "$JAVA5_HOME/bin/java does not exist or is not executable - JAVA5_HOME must point to a JDK (not JRE) 1.5 install dir."
fi

if [ ! -x "$JAVA5_HOME/bin/javac" ]; then
   abort "$JAVA5_HOME/bin/javac does not exist or is not executable - JAVA5_HOME must point to a JDK (not JRE) 1.5 install dir."
fi

if ! "$JAVA5_HOME/bin/javap" java.lang.Enum >/dev/null 2>&1; then
   abort "java.lang.Enum not found - JAVA5_HOME ($JAVA5_HOME) version appears to be less than 1.5 - version must be 1.5.x."
fi

if "$JAVA5_HOME/bin/javap" java.util.Deque >/dev/null 2>&1; then
   abort "java.util.Deque found - JAVA5_HOME ($JAVA5_HOME) version appears to be greater than or equal to 1.6 - version must be 1.5.x."
fi


# Make sure M2_HOME points to a valid Maven 2 install.

if [ -z "$M2_HOME" ]; then
   abort "M2_HOME environment variable is not set - M2_HOME must point to a Maven, $MINIMUM_MAVEN_VERSION or later, install dir."
fi

if [ ! -d "$M2_HOME" ]; then
   abort "M2_HOME ($M2_HOME) does not exist or is not a directory - M2_HOME must point to a Maven, $MINIMUM_MAVEN_VERSION or later, install dir."
fi

echo "Prepending $M2_HOME/bin to PATH..."
PATH="$M2_HOME/bin:$PATH"

if ! which mvn >/dev/null 2>&1; then
   abort "mvn not found in PATH ($PATH) - M2_HOME must point to a Maven, $MINIMUM_MAVEN_VERSION or later, install dir."
fi


# Make sure git is in the PATH.

if ! which git >/dev/null 2>&1; then
   abort "git not found in PATH ($PATH)."
fi

# TODO: Check for a minimum git version?


# Set various local variables.
if [ -n "$HUDSON_URL" ] && [ -n "$WORKSPACE" ]; then
   echo "We appear to be running in a Hudson job." 
   WORK_DIR="$WORKSPACE"
elif [ -z "$WORK_DIR" ]; then
   WORK_DIR="$HOME/release"
fi
cd "$WORK_DIR"


MAVEN_LOCAL_REPO_DIR="$WORK_DIR/m2-repository"
MAVEN_SETTINGS_FILE="$WORK_DIR/m2-settings.xml"
MAVEN_ARGS="--settings $MAVEN_SETTINGS_FILE --errors -Penterprise,dist,release"
if [ "$RELEASE_TYPE" = "enterprise" ]; then
   MAVEN_ARGS="$MAVEN_ARGS -Dexclude-webdav"
fi
if [ -z "$RHQ_RELEASE_QUIET" ]; then
   MAVEN_ARGS="$MAVEN_ARGS --debug"
fi
if [ -z "$MAVEN_LOCAL_REPO_PURGE_INTERVAL_HOURS" ]; then
   MAVEN_LOCAL_REPO_PURGE_INTERVAL_HOURS="12"
fi


# Print out a summary of the environment.

echo
echo "========================== Environment Variables =============================="
echo "JAVA_HOME=$JAVA_HOME"
echo "JAVA5_HOME=$JAVA5_HOME"
echo "M2_HOME=$M2_HOME"
echo "MAVEN_OPTS=$MAVEN_OPTS"
echo "PATH=$PATH"
echo "============================= Local Variables ================================="
echo "WORK_DIR=$WORK_DIR"
echo "PROJECT_NAME=$PROJECT_NAME"
echo "RELEASE_TYPE=$RELEASE_TYPE"
echo "RELEASE_VERSION=$RELEASE_VERSION"
echo "DEVELOPMENT_VERSION=$DEVELOPMENT_VERSION"
echo "RELEASE_BRANCH=$RELEASE_BRANCH"
echo "RELEASE_TAG=$RELEASE_TAG"
echo "MAVEN_LOCAL_REPO_DIR=$MAVEN_LOCAL_REPO_DIR"
echo "MAVEN_LOCAL_REPO_PURGE_INTERVAL_HOURS=$MAVEN_LOCAL_REPO_PURGE_INTERVAL_HOURS"
echo "MAVEN_SETTINGS_FILE=$MAVEN_SETTINGS_FILE"
echo "MAVEN_ARGS=$MAVEN_ARGS"
echo "============================= Program Versions ================================"
git --version
echo
java -version
echo
mvn --version | head -1
echo "==============================================================================="
echo


# Clean the Maven local repo if it hasn't been purged recently.
if [ -f "$MAVEN_LOCAL_REPO_DIR" ]; then
   OUTPUT=`find "$MAVEN_LOCAL_REPO_DIR" -maxdepth 0 -mtime $MAVEN_LOCAL_REPO_PURGE_INTERVAL_HOURS`
   if [ -n "$OUTPUT" ]; then       
      echo "MAVEN_LOCAL_REPO_DIR ($MAVEN_LOCAL_REPO_DIR) has existed for more than $MAVEN_LOCAL_REPO_PURGE_INTERVAL_HOURS hours - purging it for a clean-clean build..."
      rm -rf "$MAVEN_LOCAL_REPO_DIR"
   fi
fi
mkdir -p "$MAVEN_LOCAL_REPO_DIR"


# Create the Maven settings file.
cat <<EOF >"${MAVEN_SETTINGS_FILE}"
<settings>
   <localRepository>$MAVEN_LOCAL_REPO_DIR</localRepository>
   
   <profiles>

      <profile>
         <id>release</id>
         <properties>
            <rhq.test.ds.connection-url>jdbc:postgresql://jon03.qa.atl2.redhat.com:5432/rhq_release</rhq.test.ds.connection-url>
            <rhq.test.ds.user-name>rhqadmin</rhq.test.ds.user-name>
            <rhq.test.ds.password>rhqadmin</rhq.test.ds.password>
            <rhq.test.ds.type-mapping>PostgreSQL</rhq.test.ds.type-mapping>
            <rhq.test.ds.driver-class>org.postgresql.Driver</rhq.test.ds.driver-class>
            <rhq.test.ds.xa-datasource-class>org.postgresql.xa.PGXADataSource</rhq.test.ds.xa-datasource-class>
            <rhq.test.ds.server-name>jon03.qa.atl2.redhat.com</rhq.test.ds.server-name>
            <rhq.test.ds.port>5432</rhq.test.ds.port>
            <rhq.test.ds.db-name>rhq_release</rhq.test.ds.db-name>
            <rhq.test.ds.hibernate-dialect>org.hibernate.dialect.PostgreSQLDialect</rhq.test.ds.hibernate-dialect>
            <!-- quartz properties -->
            <rhq.test.quartz.driverDelegateClass>org.quartz.impl.jdbcjobstore.PostgreSQLDelegate</rhq.test.quartz.driverDelegateClass>
            <rhq.test.quartz.selectWithLockSQL>SELECT * FROM {0}LOCKS ROWLOCK WHERE LOCK_NAME = ? FOR UPDATE</rhq.test.quartz.selectWithLockSQL>
            <rhq.test.quartz.lockHandlerClass>org.quartz.impl.jdbcjobstore.StdRowLockSemaphore</rhq.test.quartz.lockHandlerClass>

            <DatabaseTest.nofail>true</DatabaseTest.nofail>

            <rhq.testng.excludedGroups>agent-comm,comm-client,postgres-plugin,native-system</rhq.testng.excludedGroups>
         </properties>
      </profile>
 
   </profiles>
</settings>
EOF


# Clone and/or checkout the source from git.

CLONE_DIR="$WORK_DIR/rhq"
if [ -d "$CLONE_DIR" ]; then
   cd "$CLONE_DIR"
   git status >/dev/null 2>&1
   # Note, git 1.6 and earlier returns an exit code of 1, rather than 0, if there are any uncommitted changes,
   # and git 1.7 returns 0, so we check if the exit code is less than or equal to 1 to determine if $CLONE_DIR
   # is a git working copy.
   if [ $? -le 1 ]; then       
       echo "Checking out a clean copy of the master branch..."
       git checkout "$BRANCH"
       git reset --hard
       git clean -dxf
   else
       cd "$WORK_DIR"
       rm -rf "$CLONE_DIR"
   fi
fi
if [ ! -d "$CLONE_DIR" ]; then
   echo "Cloning the RHQ git repo (this will take about 10-15 minutes)..."
   git clone ssh://git.fedorahosted.org/git/rhq/rhq.git
   cd "$CLONE_DIR"
   git checkout "$BRANCH"
fi


# Create a branch for the release, so we don't have to make any changes to master.

echo "Creating release branch $RELEASE_BRANCH and checking it out..."
git push origin master:$RELEASE_BRANCH
git checkout $RELEASE_BRANCH


# Run a test build before tagging. This will also publish the snapshot artifacts to the local repo to "bootstrap" the repo.

echo "Building project to ensure tests pass and to bootstrap local Maven repo (this will take about 15-30 minutes)..."
# TODO: Add -Djava5.home=$JAVA5_HOME/jre to the below mvn command line once the Java6 API usages have been removed from the Deployer class.
# TODO: Add -Ddbreset to the below mvn command line - this was removed temporarily to speed up development and testing of this release script.
# TODO: Remove the -Dmaven.test.skip=true - this was added temporarily to speed up development and testing of this release script.
# NOTE: No need to do a mvn clean, since we just did either a clone or clean checkout above.
mvn install $MAVEN_ARGS -Dmaven.test.skip=true
if [ "$?" -ne 0 ]; then
   abort "Test build failed. Please see above Maven output for details, fix any issues, then try again."
fi
echo
echo "Test build succeeded!"


# Clean up the snapshot jars produced by the test build.

echo "Cleaning up snapshot jars produced by test build..."
mvn clean $MAVEN_ARGS


# Do a dry run of tagging the release.

echo "Doing a dry run of tagging the release..."
mvn release:prepare $MAVEN_ARGS --batch-mode -DreleaseVersion=$RELEASE_VERSION -DdevelopmentVersion=$DEVELOPMENT_VERSION -Dresume=false -Dtag=$RELEASE_TAG "-DpreparationGoals=install $MAVEN_ARGS -Dmaven.test.skip=true -Ddbsetup-do-not-check-schema=true" -DdryRun=true
EXIT_CODE=$?
mvn release:clean $MAVEN_ARGS
if [ "$EXIT_CODE" -ne 0 ]; then
   abort "Tagging dry run failed. Please see above Maven output for details, fix any issues, then try again."
fi
echo
echo "Tagging dry run succeeded!"


# If the dry run succeeded, tag it for real.

echo "Tagging the release..."
mvn release:prepare $MAVEN_ARGS --batch-mode -DreleaseVersion=$RELEASE_VERSION -DdevelopmentVersion=$DEVELOPMENT_VERSION -Dresume=false -Dtag=$RELEASE_TAG "-DpreparationGoals=install $MAVEN_ARGS -Dmaven.test.skip=true -Ddbsetup-do-not-check-schema=true" -DdryRun=false
EXIT_CODE=$?
mvn release:clean $MAVEN_ARGS
if [ "$EXIT_CODE" -ne 0 ]; then
   abort "Tagging failed. Please see above Maven output for details, fix any issues, then try again."
fi
echo
echo "Tagging succeeded!"


# Checkout the tag and build it.

echo "Checking out release tag $RELEASE_TAG..."
git checkout "$RELEASE_TAG"
echo "Building release from tag (this will take about 5-10 minutes)..."
mvn install $MAVEN_ARGS -Dmaven.test.skip=true -Ddbsetup-do-not-check-schema=true
if [ "$?" -ne 0 ]; then
   abort "Release build failed. Please see above Maven output for details, fix any issues, then try again."
fi
echo
echo "Release build succeeded!"


echo
echo "=============================== Release Info =================================="
echo "Version: $RELEASE_VERSION"
echo "Branch URL: $PROJECT_GIT_WEB_URL;a=shortlog;h=refs/heads/$RELEASE_BRANCH"
echo "Tag URL: $PROJECT_GIT_WEB_URL;a=shortlog;h=refs/tags/$RELEASE_TAG"
echo "==============================================================================="

