#!/bin/sh

if [ -n "$RELEASE_DEBUG" ]; then
   echo "Debug output is enabled."
   set -x
fi


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
   abort "$@" "Usage:   $EXE community|enterprise RELEASE_VERSION DEVELOPMENT_VERSION RELEASE_BRANCH GIT_USERNAME test|production" "Example: $EXE enterprise 3.0.0.GA 3.0.0-SNAPSHOT release-3.0.0 ips test"
}


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
if echo $MAVEN_VERSION | grep -Ev "^(2\.[12]|3\.0)"; then
   abort "Unsupported Maven version - $MAVEN_VERSION. Only Maven 2.1.x, 2.2.x, or 3.0.x is supported. Please update the value of M2_HOME, then try again."
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
   MAVEN_LOCAL_REPO_DIR="$HOME/release/m2-repository"
   #MAVEN_LOCAL_REPO_DIR="$HOME/.m2/repository"
   MAVEN_SETTINGS_FILE="$HOME/release/m2-settings.xml"
fi

PROJECT_GIT_URL="git://git.fedorahosted.org/rhq/rhq.git"

MAVEN_ARGS="--settings $MAVEN_SETTINGS_FILE --batch-mode --errors -Penterprise,dist,release"

if [ "$MODE" = "test" ]; then
   MAVEN_ARGS="$MAVEN_ARGS -DskipTests=true"
fi

if [ "$RELEASE_TYPE" = "enterprise" ]; then
   MAVEN_ARGS="$MAVEN_ARGS -Dexclude-webdav -Djava5.home=$JAVA5_HOME/jre"
fi
if [ -n "$RELEASE_DEBUG" ]; then
   MAVEN_ARGS="$MAVEN_ARGS --debug"
fi
if [ -n "$RELEASE_ADDITIONAL_MAVEN_ARGS" ]; then
   MAVEN_ARGS="$MAVEN_ARGS $RELEASE_ADDITIONAL_MAVEN_ARGS"
fi
if [ -z "$MAVEN_LOCAL_REPO_PURGE_INTERVAL_HOURS" ]; then
   MAVEN_LOCAL_REPO_PURGE_INTERVAL_HOURS="6"
fi

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


# Print out a summary of the environment.

echo
echo "========================== Environment Variables =============================="
echo "JAVA_HOME=$JAVA_HOME"
[ "$RELEASE_TYPE" = "enterprise" ] && echo "JAVA5_HOME=$JAVA5_HOME"
echo "M2_HOME=$M2_HOME"
echo "MAVEN_OPTS=$MAVEN_OPTS"
echo "PATH=$PATH"
echo "LANG=$LANG"
echo "============================= Local Variables ================================="
echo "WORKING_DIR=$WORKING_DIR"
echo "PROJECT_NAME=$PROJECT_NAME"
echo "PROJECT_GIT_URL=$PROJECT_GIT_URL"
echo "RELEASE_TYPE=$RELEASE_TYPE"
echo "RELEASE_VERSION=$RELEASE_VERSION"
echo "DEVELOPMENT_VERSION=$DEVELOPMENT_VERSION"
echo "RELEASE_BRANCH=$RELEASE_BRANCH"
echo "RELEASE_TAG=$RELEASE_TAG"
echo "MODE=$MODE"
echo "MAVEN_LOCAL_REPO_DIR=$MAVEN_LOCAL_REPO_DIR"
echo "MAVEN_LOCAL_REPO_PURGE_INTERVAL_HOURS=$MAVEN_LOCAL_REPO_PURGE_INTERVAL_HOURS"
echo "MAVEN_SETTINGS_FILE=$MAVEN_SETTINGS_FILE"
echo "MAVEN_ARGS=$MAVEN_ARGS"
echo "MAVEN_RELEASE_PERFORM_GOAL=$MAVEN_RELEASE_PERFORM_GOAL"
echo "JBOSS_ORG_USERNAME=$JBOSS_ORG_USERNAME"
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
   if [ "$MODE" = "production" ]; then
      echo "Purging MAVEN_LOCAL_REPO_DIR ($MAVEN_LOCAL_REPO_DIR) since this is a production build..."
      #rm -rf "$MAVEN_LOCAL_REPO_DIR"
   else
      echo boo
      #OUTPUT=`find "$MAVEN_LOCAL_REPO_DIR" -maxdepth 0 -mtime $MAVEN_LOCAL_REPO_PURGE_INTERVAL_HOURS`
      #if [ -n "$OUTPUT" ]; then       
      #   echo "MAVEN_LOCAL_REPO_DIR ($MAVEN_LOCAL_REPO_DIR) has existed for more than $MAVEN_LOCAL_REPO_PURGE_INTERVAL_HOURS hours - purging it for a clean-clean build..."
      #   rm -rf "$MAVEN_LOCAL_REPO_DIR"
      #fi
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
<rhq.test.ds.server-name>vmg01.mw.lab.eng.bos.redhat.com</rhq.test.ds.server-name>
	    <rhq.db.admin.username>jon2</rhq.db.admin.username>
            <rhq.db.admin.password>jboss42</rhq.db.admin.password>
	    <rhq.test.ds.user-name>jon2</rhq.test.ds.user-name>
	    <rhq.test.ds.password>jboss42</rhq.test.ds.password>
            <rhq.test.ds.db-name>rhq_release</rhq.test.ds.db-name>
	    <rhq.test.ds.connection-url>jdbc:postgresql://${rhq.test.ds.server-name}:5432/${rhq.test.ds.db-name}</rhq.test.ds.connection-url>
            <rhq.test.ds.type-mapping>PostgreSQL</rhq.test.ds.type-mapping>
            <rhq.test.ds.driver-class>org.postgresql.Driver</rhq.test.ds.driver-class>
            <rhq.test.ds.xa-datasource-class>org.postgresql.xa.PGXADataSource</rhq.test.ds.xa-datasource-class>
            <rhq.test.ds.hibernate-dialect>org.hibernate.dialect.PostgreSQLDialect</rhq.test.ds.hibernate-dialect>
            <!-- quartz properties -->
            <rhq.test.quartz.driverDelegateClass>org.quartz.impl.jdbcjobstore.PostgreSQLDelegate</rhq.test.quartz.driverDelegateClass>
            <rhq.test.quartz.selectWithLockSQL>SELECT * FROM {0}LOCKS ROWLOCK WHERE LOCK_NAME = ? FOR UPDATE</rhq.test.quartz.selectWithLockSQL>
            <rhq.test.quartz.lockHandlerClass>org.quartz.impl.jdbcjobstore.StdRowLockSemaphore</rhq.test.quartz.lockHandlerClass>

            <DatabaseTest.nofail>true</DatabaseTest.nofail>

            <rhq.testng.excludedGroups>agent-comm,comm-client,postgres-plugin,native-system</rhq.testng.excludedGroups>

            <gwt-plugin.extraJvmArgs>-Xms512M -Xmx1024M -XX:PermSize=256M -XX:MaxPermSize=512M</gwt-plugin.extraJvmArgs>
         </properties>
      </profile>

   </profiles>

   <!-- This is used by the deploy plugin to publish release artifacts to the jboss.org Nexus repo. -->
   <servers>
      <server>
         <id>jboss-releases-repository</id>
         <username>$JBOSS_ORG_USERNAME</username>
         <password>$JBOSS_ORG_PASSWORD</password>
      </server>
   </servers>
 
</settings>
EOF

# We only need to worry about cloning the repo if we are not running on hudson
if [ ! "$HUDSON_URL" ]; then
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
        cd "$WORKING_DIR"
        if [ "$RELEASE_BRANCH" != "master" ]; then
            git checkout --track -b $RELEASE_BRANCH "origin/$RELEASE_BRANCH"
        fi
        [ "$?" -ne 0 ] && abort "Failed to checkout release branch ($RELEASE_BRANCH)."
    fi
fi


echo "Building release from tag and publishing Maven artifacts (this will take about 10-15 minutes)..."
mvn $MAVEN_RELEASE_PERFORM_GOAL $MAVEN_ARGS -Ddbreset
[ "$?" -ne 0 ] && abort "Release build failed. Please see above Maven output for details, fix any issues, then try again."
echo
echo "Release build succeeded!"


echo
echo "=============================== Release Info =================================="
echo "Version: $RELEASE_VERSION"
echo "Branch URL: $PROJECT_GIT_WEB_URL;a=shortlog;h=refs/heads/$RELEASE_BRANCH"
echo "Tag URL: $PROJECT_GIT_WEB_URL;a=shortlog;h=refs/tags/$RELEASE_TAG"
echo "==============================================================================="

