#!/bin/sh

# Constants


PROJECT_NAME="rhq"
PROJECT_DISPLAY_NAME="RHQ"
PROJECT_SVN_URL="http://svn.rhq-project.org/repos/rhq"
TAG_PREFIX="RHQ"
MINIMUM_MAVEN_VERSION="2.0.10"


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
   EXE=`basename $0`
   abort "Usage:   $EXE community|enterprise RELEASE_VERSION DEVELOPMENT_VERSION" "Example: $EXE community 1.3.1 1.3.2-SNAPSHOT"   
}


# Process command line args.

if [ "$#" -ne 3 ]; then
   usage
fi  
RELEASE_TYPE="$1"
if [ "$RELEASE_TYPE" != "community" ] && [ "$RELEASE_TYPE" != "enterprise" ]; then
   usage
fi
RELEASE_VERSION="$2"
TAG_VERSION=`echo $RELEASE_VERSION | sed 's/\./_/g'`
RELEASE_TAG="${TAG_PREFIX}_${TAG_VERSION}"
DEVELOPMENT_VERSION="$3"


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


# Make sure SUBVERSION_HOME points to a valid Subversion install.

if [ -z "$SUBVERSION_HOME" ]; then
   abort "SUBVERSION_HOME environment variable is not set." >&2
fi

if [ ! -d "$SUBVERSION_HOME" ]; then
   abort "SUBVERSION_HOME ($SUBVERSION_HOME) does not exist or is not a directory."
fi

echo "Prepending $SUBVERSION_HOME/bin to PATH..."
PATH="$SUBVERSION_HOME/bin:$PATH"

if ! which svn >/dev/null 2>&1; then
   abort "svn not found in PATH ($PATH) - SUBVERSION_HOME must point to an SVN install dir."
fi

echo "Prepending $SUBVERSION_HOME/lib to LD_LIBRARY_PATH..."
LD_LIBRARY_PATH="$SUBVERSION_HOME/lib:$LD_LIBRARY_PATH"
export LD_LIBRARY_PATH


# Set additional required env vars.

LANG=en_US.iso88591
export LANG


# Set various local variables.

if [ -z "$BASE_DIR" ]; then
   BASE_DIR="$HOME"
fi
WORK_DIR="$BASE_DIR/${PROJECT_NAME}-${RELEASE_TYPE}-${RELEASE_VERSION}"   

RELEASE_BRANCH_CHECKOUT_DIR="$WORK_DIR/branch"
RELEASE_TAG_CHECKOUT_DIR="$WORK_DIR/tag"
if [ -n "$RELEASE_BRANCH" ]; then
   RELEASE_BRANCH_SVN_URL="$PROJECT_SVN_URL/branches/$RELEASE_BRANCH"
else
   RELEASE_BRANCH_SVN_URL="$PROJECT_SVN_URL/trunk"
fi
RELEASE_TAG_SVN_URL="$PROJECT_SVN_URL/tags/$RELEASE_TAG"

MAVEN_LOCAL_REPO_DIR="$BASE_DIR/release-m2-repo"
MAVEN_SETTINGS_FILE="$WORK_DIR/settings.xml"
MAVEN_OPTS="--settings "$MAVEN_SETTINGS_FILE" --debug --errors -Penterprise -Pdist -Prelease"
if [ "$RELEASE_TYPE" = "enterprise" ]; then
   MAVEN_OPTS="$MAVEN_OPTS -Pojdbc-driver -Dpackage-connectors -Dexclude-webdav"
fi
if [ -z "$MAVEN_LOCAL_REPO_PURGE_INTERVAL_HOURS" ]; then
   MAVEN_LOCAL_REPO_PURGE_INTERVAL_HOURS="24"
fi


# Print out summary of environment.

echo
echo "========================= Environment Variables =============================="
echo "JAVA_HOME=$JAVA_HOME"
echo "M2_HOME=$M2_HOME"
echo "SUBVERSION_HOME=$SUBVERSION_HOME"
echo "PATH=$PATH"
echo "LD_LIBRARY_PATH=$LD_LIBRARY_PATH"
echo "LANG=$LANG"
echo "============================ Local Variables ================================="
echo "WORK_DIR=$WORK_DIR"
echo "PROJECT_NAME=$PROJECT_NAME"
echo "RELEASE_TYPE=$RELEASE_TYPE"
echo "RELEASE_VERSION=$RELEASE_VERSION"
echo "DEVELOPMENT_VERSION=$DEVELOPMENT_VERSION"
echo "RELEASE_BRANCH=$RELEASE_BRANCH"
echo "RELEASE_BRANCH_SVN_URL=$RELEASE_BRANCH_SVN_URL"
echo "RELEASE_BRANCH_CHECKOUT_DIR=$RELEASE_BRANCH_CHECKOUT_DIR"
echo "RELEASE_TAG=$RELEASE_TAG"
echo "RELEASE_TAG_SVN_URL=$RELEASE_TAG_SVN_URL"
echo "RELEASE_TAG_CHECKOUT_DIR=$RELEASE_TAG_CHECKOUT_DIR"
echo "MAVEN_LOCAL_REPO_DIR=$MAVEN_LOCAL_REPO_DIR"
echo "MAVEN_LOCAL_REPO_PURGE_INTERVAL_HOURS=$MAVEN_LOCAL_REPO_PURGE_INTERVAL_HOURS"
echo "MAVEN_SETTINGS_FILE=$MAVEN_SETTINGS_FILE"
echo "MAVEN_OPTS=$MAVEN_OPTS"
echo "============================ Program Versions ================================"
mvn --version
echo
svn --version | head -2
echo "=============================================================================="
echo


# Clean the Maven local repo.

if [ -f "$MAVEN_LOCAL_REPO_DIR" ]; then
   OUTPUT=`find "$MAVEN_LOCAL_REPO_DIR" -maxdepth 0 -mtime $MAVEN_LOCAL_REPO_PURGE_INTERVAL_HOURS`
   if [ -n "$OUTPUT" ]; then 
      
      echo "MAVEN_LOCAL_REPO_DIR ($MAVEN_LOCAL_REPO_DIR) has existed for more than 24 hours - purging it for a clean-clean build..."
      rm -rf "$MAVEN_LOCAL_REPO_DIR"
   fi
fi
mkdir -p "$MAVEN_LOCAL_REPO_DIR"


# Create the Maven settings file.
SETTINGS=${WORK_DIR}/settings.xml
cat <<EOF >${SETTINGS}
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
 
      <profile>
         <id>ojdbc-driver</id>
         <repositories>
            <repository>
               <id>internal</id>
               <name>Internal Repository</name>
               <url>http://jon01.qa.atl2.redhat.com:8042/m2-repo/</url>
            </repository>
         </repositories>              
      </profile>
   </profiles>
</settings>
EOF


# Run a test build before tagging.

if [ -f "$RELEASE_BRANCH_CHECKOUT_DIR" ]; then
   echo "Purging contents of RELEASE_BRANCH_CHECKOUT_DIR ($RELEASE_BRANCH_CHECKOUT_DIR)..."
   rm -rf "$RELEASE_BRANCH_CHECKOUT_DIR"
fi
mkdir -p "$RELEASE_BRANCH_CHECKOUT_DIR"

echo "Checking out branch source from $RELEASE_BRANCH_SVN_URL to $RELEASE_BRANCH_CHECKOUT_DIR (this will take about 5-10 minutes)..."
# We only need pom.xml and modules/**. Save some time by not checking out etc/**.
svn co -N $RELEASE_BRANCH_SVN_URL "$RELEASE_BRANCH_CHECKOUT_DIR"
cd "$RELEASE_BRANCH_CHECKOUT_DIR"
svn co $RELEASE_BRANCH_SVN_URL/modules

echo "Building project to ensure tests pass and to boostrap local Maven repo (this will take about 10-15 minutes)..."
# This will build everything except the CLI, enforcing Java 5 APIs.
mvn install $MAVEN_OPTS -Ddbsetup -Djava5.home=$JAVA5_HOME/jre
if [ "$?" -ne 0 ]; then
   abort "Build failed. Please see above Maven output for details, fix any issues, then try again."
fi
# Now build the CLI, enforcing Java 6 APIs.
cd modules/enterprise/remoting/cli
mvn install $MAVEN_OPTS
if [ "$?" -ne 0 ]; then
   abort "Build failed. Please see above Maven output for details, fix any issues, then try again."
fi

echo
echo "Test build succeeded!"


# Tag the release.

echo "Tagging the release..."
cd "$RELEASE_BRANCH_CHECKOUT_DIR"
mvn release:prepare $MAVEN_OPTS -DreleaseVersion=$RELEASE_VERSION -DdevelopmentVersion=$DEVELOPMENT_VERSION -Dresume=false -Dtag=$RELEASE_TAG "-DpreparationGoals=clean verify $MAVEN_OPTS -Dmaven.test.skip=true -Ddbsetup-do-not-check-schema=true" -DdryRun=true
if [ "$?" -ne 0 ]; then
   abort "Tagging failed. Please see above Maven output for details, fix any issues, then try again."
fi
echo
echo "Tagging succeeded!"


# Checkout the tag and build it.

if [ -f "$RELEASE_TAG_CHECKOUT_DIR" ]; then
   echo "Purging contents of RELEASE_TAG_CHECKOUT_DIR ($RELEASE_TAG_CHECKOUT_DIR)..."
   rm -rf "$RELEASE_TAG_CHECKOUT_DIR"
fi
mkdir -p "$RELEASE_TAG_CHECKOUT_DIR"

echo "Checking out tag source from $RELEASE_TAG_SVN_URL to $RELEASE_TAG_CHECKOUT_DIR (this will take about 5-10 minutes)..."
svn co -N $RELEASE_TAG_SVN_URL "$RELEASE_TAG_CHECKOUT_DIR"
cd "$RELEASE_TAG_CHECKOUT_DIR"
svn co $RELEASE_TAG_SVN_URL/modules

echo "Building release from tag (this will take about 10-15 minutes)..."
mvn install $MAVEN_OPTS -Dmaven.test.skip=true -Ddbsetup-do-not-check-schema=true
if [ "$?" -ne 0 ]; then
   abort "Build failed. Please see above Maven output for details, fix any issues, then try again."
fi
echo
echo "Release build succeeded!"

echo "=========================== Release Info ==============================="
echo "Version: $RELEASE_VERSION"
echo "Branch SVN URL: $RELEASE_BRANCH_SVN_URL"
echo "Tag SVN URL: $RELEASE_TAG_SVN_URL"
echo "========================================================================"
