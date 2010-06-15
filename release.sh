#!/bin/sh -x

# Constants

PROJECT_NAME="rhq"
PROJECT_DISPLAY_NAME="RHQ"
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
   abort "$@" "Usage:   $EXE community|enterprise RELEASE_VERSION DEVELOPMENT_VERSION RELEASE_BRANCH GIT_USER" "Example: $EXE enterprise 3.0.0.GA 3.0.0-SNAPSHOT release-3.0.0 ips"
}


# Process command line args.

EXE=`basename $0`
if [ "$#" -ne 5 ]; then
   usage
fi  
RELEASE_TYPE="$1"
if [ "$RELEASE_TYPE" != "community" ] && [ "$RELEASE_TYPE" != "enterprise" ]; then
   usage "Invalid release type: $RELEASE_TYPE"
fi
RELEASE_VERSION="$2"
DEVELOPMENT_VERSION="$3"
RELEASE_BRANCH="$4"
GIT_USER="$5"


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

# TODO: Check that JDK version is < 1.7.


# If this is an enterprise release, make sure JAVA5_HOME points to a valid JDK 1.5 install. 
# We need this to validate only Java 5 or earlier APIs are used in all modules, except the CLI, which requires Java 6.

if [ "$RELEASE_TYPE" = "enterprise" ]; then
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
fi


# Make sure M2_HOME points to a valid Maven 2.1.x or 2.2.x install.

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

mvn -version >/dev/null
[ $? -ne 0 ] && abort "mvn --version failed with exit code $?."
MAVEN_VERSION=`mvn -version | head -1 | sed 's|[^0-9]*\([^ ]*\).*|\1|'`
if echo $MAVEN_VERSION | grep -v "^2.[12]"; then
   abort "Unsupported Maven version - $MAVEN_VERSION. Only Maven 2.1.x or 2.2.x are supported. Please update the value of M2_HOME, then try again."
fi


# Make sure git 1.6.x or 1.7.x is in the PATH.

if ! which git >/dev/null 2>&1; then
   abort "git not found in PATH ($PATH)."
fi

git --version >/dev/null
[ $? -ne 0 ] && abort "git --version failed with exit code $?."
GIT_VERSION=`git --version | sed 's|[^0-9]*\([^ ]*\).*|\1|'`
if echo $GIT_VERSION | grep -v "^1.[67]"; then
   abort "Unsupported git version - $GIT_VERSION. Only git 1.6.x or 1.7.x are supported. Please add a directory containing a supported version of git to your PATH, then try again."
fi


# Set various local variables.

if [ -n "$HUDSON_URL" ] && [ -n "$WORKSPACE" ]; then
   echo "We appear to be running in a Hudson job." 
   WORKING_DIR="$WORKSPACE"
   MAVEN_LOCAL_REPO_DIR="$HOME/.m2/hudson-$JOB_NAME-repository"
   MAVEN_SETTINGS_FILE="$HOME/.m2/hudson-$JOB_NAME-settings.xml"
elif [ -z "$WORKING_DIR" ]; then
   WORKING_DIR="$HOME/release/rhq"
   MAVEN_LOCAL_REPO_DIR="$HOME/release/m2-repository"
   MAVEN_SETTINGS_FILE="$HOME/release/m2-settings.xml"
fi

PROJECT_GIT_URL="ssh://${GIT_USER}@git.fedorahosted.org/git/rhq/rhq.git"

MAVEN_ARGS="--settings $MAVEN_SETTINGS_FILE --batch-mode --errors -Penterprise,dist,release"
if [ "$RELEASE_TYPE" = "enterprise" ]; then
   MAVEN_ARGS="$MAVEN_ARGS -Dexclude-webdav -Djava5.home=$JAVA5_HOME/jre"
fi
if [ -z "$RHQ_RELEASE_QUIET" ]; then
   MAVEN_ARGS="$MAVEN_ARGS --debug"
fi
if [ -n "$RHQ_RELEASE_ADDITIONAL_MAVEN_ARGS" ]; then
   MAVEN_ARGS="$MAVEN_ARGS $RHQ_RELEASE_ADDITIONAL_MAVEN_ARGS"
fi
if [ -z "$MAVEN_LOCAL_REPO_PURGE_INTERVAL_HOURS" ]; then
   MAVEN_LOCAL_REPO_PURGE_INTERVAL_HOURS="12"
fi
# TODO: Set MAVEN_OPTS environment variable.

TAG_VERSION=`echo $RELEASE_VERSION | sed 's/\./_/g'`
RELEASE_TAG="${TAG_PREFIX}_${TAG_VERSION}"


# Print out a summary of the environment.

echo
echo "========================== Environment Variables =============================="
echo "JAVA_HOME=$JAVA_HOME"
[ "$RELEASE_TYPE" = "enterprise" ] && echo "JAVA5_HOME=$JAVA5_HOME"
echo "M2_HOME=$M2_HOME"
echo "MAVEN_OPTS=$MAVEN_OPTS"
echo "PATH=$PATH"
echo "============================= Local Variables ================================="
echo "WORKING_DIR=$WORKING_DIR"
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

if [ -d "$WORKING_DIR" ]; then
   cd "$WORKING_DIR"
   git status >/dev/null 2>&1
   GIT_STATUS_EXIT_CODE=$?
   # Note, git 1.6 and earlier returns an exit code of 1, rather than 0, if there are any uncommitted changes,
   # and git 1.7 returns 0, so we check if the exit code is less than or equal to 1 to determine if $CLONE_DIR
   # is a git working copy.
   if [ "$GIT_STATUS_EXIT_CODE" -le 1 ]; then       
       echo "Checking out a clean copy of the release branch ($RELEASE_BRANCH)..."
       git checkout "$RELEASE_BRANCH"
       [ $? -ne 0 ] && abort "Failed to checkout release branch ($RELEASE_BRANCH)."
       git pull
       [ $? -ne 0 ] && abort "Failed to update release branch ($RELEASE_BRANCH)."
       git reset --hard
       [ $? -ne 0 ] && abort "Failed to reset release branch ($RELEASE_BRANCH)."
       git clean -dxf
       [ $? -ne 0 ] && abort "Failed to clean release branch ($RELEASE_BRANCH)."
   else
       echo "$WORKING_DIR does not appear to be a git working directory ('git status' returned $GIT_STATUS_EXIT_CODE) - removing it so we can freshly clone the repo..."
       cd ..
       rm -rf "$WORKING_DIR"
   fi
fi
if [ ! -d "$WORKING_DIR" ]; then
   echo "Cloning the $PROJECT_NAME git repo (this will take about 10-15 minutes)..."
   git clone "$PROJECT_GIT_URL" "$WORKING_DIR"
   [ $? -ne 0 ] && abort "Failed to clone $PROJECT_NAME git repo ($PROJECT_GIT_URL)."
   cd "$CLONE_DIR"
   git checkout "$RELEASE_BRANCH"
   [ $? -ne 0 ] && abort "Failed to checkout release branch ($RELEASE_BRANCH)."
fi


# Run a test build before tagging. This will also publish the snapshot artifacts to the local repo to "bootstrap" the repo.

echo "Building project to ensure tests pass and to bootstrap local Maven repo (this will take about 15-30 minutes)..."
# NOTE: There is no need to do a mvn clean below, since we just did either a clone or clean checkout above.
mvn install $MAVEN_ARGS -Ddbreset
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
mvn release:prepare $MAVEN_ARGS -DreleaseVersion=$RELEASE_VERSION -DdevelopmentVersion=$DEVELOPMENT_VERSION -Dresume=false -Dtag=$RELEASE_TAG "-DpreparationGoals=install $MAVEN_ARGS -Dmaven.test.skip=true -Ddbsetup-do-not-check-schema=true" -DdryRun=true
EXIT_CODE=$?
mvn release:clean $MAVEN_ARGS
if [ "$EXIT_CODE" -ne 0 ]; then
   abort "Tagging dry run failed. Please see above Maven output for details, fix any issues, then try again."
fi
echo
echo "Tagging dry run succeeded!"


# If the dry run succeeded, tag it for real.

echo "Tagging the release..."
mvn release:prepare $MAVEN_ARGS -DreleaseVersion=$RELEASE_VERSION -DdevelopmentVersion=$DEVELOPMENT_VERSION -Dresume=false -Dtag=$RELEASE_TAG "-DpreparationGoals=install $MAVEN_ARGS -Dmaven.test.skip=true -Ddbsetup-do-not-check-schema=true" -DdryRun=false
EXIT_CODE=$?
mvn release:clean $MAVEN_ARGS
if [ "$EXIT_CODE" -ne 0 ]; then
   abort "Tagging failed. Please see above Maven output for details, fix any issues, then try again."
fi
echo
echo "Tagging succeeded!"


# Checkout the tag and build and publish the Maven artifacts.

echo "Checking out release tag $RELEASE_TAG..."
git checkout "$RELEASE_TAG"
git clean -dxf
echo "Building release from tag and publishing Maven artifacts (this will take about 10-15 minutes)..."
mvn deploy $MAVEN_ARGS -Dmaven.test.skip=true -Ddbsetup-do-not-check-schema=true
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

