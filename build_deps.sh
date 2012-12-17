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

build_datastax_driver()
{
    echo "Building DataStax Java driver..."
    #clone_repo "git://github.com/datastax/java-driver.git"
    clone_repo "git@github.com:jsanda/java-driver.git"
    pushd java-driver
    git checkout -b master-1.2.0-rc1 origin/master-1.2.0-rc1
    run_mvn install -DskipTests
    popd
}

build_cassandra_jdbc()
{
    echo "Building cassandra-jdbc..."
    #clone_repo "https://code.google.com/a/apache-extras.org/p/cassandra-jdbc/"
    clone_repo "git@github.com:jsanda/cassandra-jdbc.git"
    pushd cassandra-jdbc
    git fetch origin
    #git checkout -b trunk origin/trunk
    git checkout -b trunk-1.2.0-rc1 origin/trunk-1.2.0-rc1
    run_mvn install -DskipTests
    popd
}

build_liquibase()
{
    echo "Building Liquibase..."
    clone_repo git@github.com:jsanda/liquibase.git
    pushd liquibase
    git remote add upstream git://github.com/liquibase/liquibase.git
    git fetch origin
    git fetch upstream
    git branch upstream-master upstream/master
    git checkout -b extensible-lock-service origin/extensible-lock-service
    git rebase upstream-master
    run_mvn install -DskipTests
    popd
}

build_cassandra_liquibase_ext()
{
    echo "Building cassandra-liquibase-ext..."
    git clone git@github.com:jsanda/cassandra-liquibase-ext.git
    pushd cassandra-liquibase-ext
    mvn install -DskipTests
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

build_datastax_driver
build_cassandra_jdbc
build_liquibase
build_cassandra_liquibase_ext
