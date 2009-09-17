#!/bin/sh

usage() 
{
   EXE=`basename $0`
   echo "Usage: $EXE community|enterprise VERSION" >&2
   echo "e.g.: $EXE community 1.3.1" >&2
   exit 1
}

# Process command line args.
if [ "$#" -ne 2 ]; then
   usage
fi  
RELEASE_TYPE="$1"
if [ "$RELEASE_TYPE" != "community" ] && [ "$RELEASE_TYPE" != "enterprise" ]; then
   usage
fi
RELEASE_VERSION="$2"
TAG_PREFIX="RHQ"
TAG_VERSION=`echo $RELEASE_VERSION | sed 's/\./_/g'`
RELEASE_TAG="${TAG_PREFIX}_${TAG_VERSION}"


# Make sure JAVA_HOME points to a valid JDK 1.5 install.
if [ -z "$JAVA_HOME" ]; then
   echo "JAVA_HOME environment variable is not set." >&2
   exit 1
fi

if [ ! -d "$JAVA_HOME" ]; then
   echo "JAVA_HOME ($JAVA_HOME) does not exist or is not a directory." >&2
   exit 1
fi

echo "Prepending $JAVA_HOME/bin to PATH..."
PATH="$JAVA_HOME/bin:$PATH"

if ! which java >/dev/null 2>&1; then
   echo "java not found in PATH ($PATH)." >&2
   exit 1
fi

if ! which javac >/dev/null 2>&1; then
   echo "javac not found in PATH ($PATH) - JAVA_HOME must point to a JDK5 install dir, not a JRE install dir." >&2
   exit 1   
fi

if ! javap java.lang.Enum >/dev/null 2>&1; then
   echo "java.lang.Enum not found - Java version appears to be less than 1.5 - Jave version must be 1.5.x." >&2
   exit 1
fi

if javap java.util.Deque >/dev/null 2>&1; then
   echo "java.util.Deque found - Java version appears to be greater than or equal to 1.6 - Jave version must be 1.5.x." >&2
   exit 1
fi


# Make sure M2_HOME points to a valid Maven 2 install.

if [ -z "$M2_HOME" ]; then
   echo "M2_HOME environment variable is not set." >&2
   exit 1
fi

if [ ! -d "$M2_HOME" ]; then
   echo "M2_HOME ($M2_HOME) does not exist or is not a directory." >&2
   exit 1
fi

echo "Prepending $M2_HOME/bin to PATH..."
PATH="$M2_HOME/bin:$PATH"

if ! which mvn >/dev/null 2>&1; then
   echo "mvn not found in PATH ($PATH) - M2_HOME must point to a Maven 2 install dir." >&2
   exit 1
fi


# Make sure SUBVERSION_HOME points to a valid Subversion install.

if [ -z "$SUBVERSION_HOME" ]; then
   echo "SUBVERSION_HOME environment variable is not set." >&2
   exit 1
fi

if [ ! -d "$SUBVERSION_HOME" ]; then
   echo "SUBVERSION_HOME ($SUBVERSION_HOME) does not exist or is not a directory." >&2
   exit 1
fi

echo "Prepending $SUBVERSION_HOME/bin to PATH..."
PATH="$SUBVERSION_HOME/bin:$PATH"

if ! which svn >/dev/null 2>&1; then
   echo "svn not found in PATH ($PATH) - SUBVERSION_HOME must point to an SVN install dir." >&2
   exit 1
fi

echo "Prepending $SUBVERSION_HOME/lib to LD_LIBRARY_PATH..."
LD_LIBRARY_PATH="$SUBVERSION_HOME/lib:$LD_LIBRARY_PATH"
export LD_LIBRARY_PATH


# Set additional required env vars.

LANG=en_US.iso88591
export LANG


# Set various local variables.

PROJECT_NAME="rhq"
PROJECT_SVN_URL="http://svn.rhq-project.org/repos/rhq"
if [ -z "$WORK_DIR" ]; then
   WORK_DIR="/tmp/${PROJECT_NAME}-${RELEASE_VERSION}"   
fi
RELEASE_BRANCH_CHECKOUT_DIR="$WORK_DIR/branch"
RELEASE_TAG_CHECKOUT_DIR="$WORK_DIR/tag"
if [ -n "$RELEASE_BRANCH" ]; then
   RELEASE_BRANCH_SVN_URL="$PROJECT_SVN_URL/branches/$RELEASE_BRANCH"
else
   RELEASE_BRANCH_SVN_URL="$PROJECT_SVN_URL/trunk"
fi
RELEASE_TAG_SVN_URL="$PROJECT_SVN_URL/tags/$RELEASE_TAG"

MAVEN_LOCAL_REPO_DIR="$WORK_DIR/m2-repo"
MAVEN_OPTS="-X -e -Penterprise -Pdist -Ddbsetup-do-not-check-schema=true"
if [ "$RELEASE_TYPE" = "enterprise" ]; then
   MAVEN_OPTS="$MAVEN_OPTS -Pojdbc-driver -Dpackage-connectors -Dexclude-webdav"
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
echo "RELEASE_BRANCH=$RELEASE_BRANCH"
echo "RELEASE_BRANCH_SVN_URL=$RELEASE_BRANCH_SVN_URL"
echo "RELEASE_BRANCH_CHECKOUT_DIR=$RELEASE_BRANCH_CHECKOUT_DIR"
echo "RELEASE_TAG=$RELEASE_TAG"
echo "RELEASE_TAG_SVN_URL=$RELEASE_TAG_SVN_URL"
echo "RELEASE_TAG_CHECKOUT_DIR=$RELEASE_TAG_CHECKOUT_DIR"
echo "MAVEN_LOCAL_REPO_DIR=$MAVEN_LOCAL_REPO_DIR"
echo "MAVEN_OPTS=$MAVEN_OPTS"
echo "============================ Program Versions ================================"
mvn --version
echo
svn --version | head -2
echo "=============================================================================="
echo

# Clean the Maven local repo.

if [ -f "$MAVEN_LOCAL_REPO_DIR" ]; then
   echo "Purging contents of MAVEN_LOCAL_REPO_DIR ($MAVEN_LOCAL_REPO_DIR)..."
   rm -rf "$MAVEN_LOCAL_REPO_DIR"
fi
mkdir -p "$MAVEN_LOCAL_REPO_DIR"


# Create the Maven settings file.
SETTINGS=${WORK_DIR}/settings.xml
cat <<EOF >${SETTINGS}
<settings>
   <localRepository>$MAVEN_LOCAL_REPO_DIR</localRepository>
   
   <!-- TODO: Add other settings. -->
</settings>
EOF


# Run a test build before tagging.

if [ -f "$RELEASE_BRANCH_CHECKOUT_DIR" ]; then
   echo "Purging contents of RELEASE_BRANCH_CHECKOUT_DIR ($RELEASE_BRANCH_CHECKOUT_DIR)..."
   rm -rf "$RELEASE_BRANCH_CHECKOUT_DIR"
fi

echo "Checking out branch source from $RELEASE_BRANCH_SVN_URL to $RELEASE_BRANCH_CHECKOUT_DIR (this will take about 5-10 minutes)..."
svn co -N $RELEASE_BRANCH_SVN_URL "$RELEASE_BRANCH_CHECKOUT_DIR"
svn co $RELEASE_BRANCH_SVN_URL/modules "$RELEASE_BRANCH_CHECKOUT_DIR/modules"

cd "$RELEASE_BRANCH_CHECKOUT_DIR"

echo "Building project to ensure tests pass and to boostrap local Maven repo (this will take about 10-15 minutes)..."
mvn install $MAVEN_OPTS
if [ "$?" -ne 0 ]; then
   echo "Build failed. Please see above Maven output for details, fix any issues, then try again." >&2
   exit 1
fi
echo "Test build succeeded!"


# Tag the release.

echo "Tagging the release..."
mvn release:prepare $MAVEN_OPTS -Dresume=false -Dtag=$RELEASE_TAG
if [ "$?" -ne 0 ]; then
   echo "Tagging failed. Please see above Maven output for details, fix any issues, then try again." >&2
   exit 1
fi
echo "Tagging succeeded!"


# Checkout the tag and build it.

if [ -f "$RELEASE_TAG_CHECKOUT_DIR" ]; then
   echo "Purging contents of RELEASE_TAG_CHECKOUT_DIR ($RELEASE_TAG_CHECKOUT_DIR)..."
   rm -rf "$RELEASE_TAG_CHECKOUT_DIR"
fi

echo "Checking out tag source from $RELEASE_TAG_SVN_URL to $RELEASE_TAG_CHECKOUT_DIR (this will take about 5-10 minutes)..."
svn co -N $RELEASE_TAG_SVN_URL "$RELEASE_TAG_CHECKOUT_DIR"
svn co $RELEASE_TAG_SVN_URL/modules "$RELEASE_TAG_CHECKOUT_DIR/modules"

cd "$RELEASE_TAG_CHECKOUT_DIR"

echo "Building release from tag (this will take about 10-15 minutes)..."
mvn install $MAVEN_OPTS -Dmaven.test.skip=true
if [ "$?" -ne 0 ]; then
   echo "Build failed. Please see above Maven output for details, fix any issues, then try again." >&2
   exit 1
fi
echo "Release build succeeded!"
