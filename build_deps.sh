#!/bin/bash
#
#==============================================================================
# Usage:
# ./build_deps.sh
#
# Description:
# There are several libraries on which this branch depends that currently have
# to be built from source. This script performs a clean build from each of the
# upstream libraries and installs them into the local Maven repo.
#
# TODO:
# - verify that we are in the correct branch for each library being built.
#==============================================================================

# include utility library
source `dirname $0`/rhq_bash.lib

build_snappy_java()
{
    echo "Building Snappy for Java..."
    clone_repo git://github.com/jsanda/snappy-java.git
    pushd snappy-java
    git checkout -b 1.0.5-M3 origin/1.0.5-M3
    run_mvn compile
    make
}

build_datastax_driver()
{
    echo "Building DataStax Java driver..."
    #clone_repo "git://github.com/datastax/java-driver.git"
    clone_repo git://github.com/jsanda/java-driver.git
    pushd java-driver
    git checkout -b master-1.2.2 origin/master-1.2.2
    run_mvn install -DskipTests
    popd
}

clone_repo()
{
    git clone $1
    [ $? -ne 0 ] && abort "Failed to clone git repository $1"
}

run_mvn()
{
    cmd_line=$@
    project_dir=$(basename `pwd`)
    mvn $@
    [ $? -ne 0 ] && abort "Maven build failed for $project_dir"
}

######### MAIN SCRIPT ##########
BUILD_DIR=$HOME/.rhq/cassandra-deps
echo "build directory set to $BUILD_DIR"
rm -rf $BUILD_DIR
mkdir -p $BUILD_DIR
pushd $BUILD_DIR

build_snappy_java
build_datastax_driver
