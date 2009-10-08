#!/bin/sh
# -------------------------------------------------------------------------
# a script for a JBoss remote EJB client
# -------------------------------------------------------------------------

# $Id $

MAIN_JAR_NAME=jbas5-ejb-client-1.0.jar
MAIN_CLASS=test.EjbClient

JBOSS_HOME=/home/metlos/Projects/java/jbossas-5-EAP/build/output/jboss-5.0.0.Beta

DIRNAME="."

# Find MAIN_JAR, or we can't continue

MAIN_JAR="$DIRNAME/target/$MAIN_JAR_NAME"
if [ ! -e "$MAIN_JAR" ]; then
    echo Could not locate $MAIN_JAR. Please check that you are in the
    echo bin directory when running this script.
    exit
fi

JAVA=java

if [ "x" = "x$JAVA_HOME" ]; then
    echo JAVA_HOME is not set.  Unexpected results may occur.
    echo Set JAVA_HOME to the directory of your local JDK to avoid this message.
else
    JAVA="$JAVA_HOME/bin/java"
fi    
    
# only include jbossall-client.jar in classpath, if
# JBOSS_CLASSPATH was not yet set
if [ "x" = "x$JBOSS_CLASSPATH" ]; then
    JBOSS_CLASSPATH="$JBOSS_HOME/client/jbossall-client.jar"
    JBOSS_CLASSPATH="$JBOSS_CLASSPATH:$JBOSS_HOME/lib/jboss-dependency.jar"
    # Below is temporary!!!
    if [ "x" = "x$USE_PROFILESERVICE_CLIENT" ]; then
        JBOSS_CLASSPATH="$JBOSS_CLASSPATH:$JBOSS_HOME/common/lib/jboss-profileservice.jar"
    else
        JBOSS_CLASSPATH="$JBOSS_CLASSPATH:$JBOSS_HOME/client/jboss-profileservice-client.jar"
    fi
fi

# These seem to be skipped in the client.bat
#    rem For the call to new InitialContext() (using org.jnp.interfaces.NamingContextFactory)...
#    set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\jnp-client.jar
#    rem set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\common\lib\jboss-security-aspects.jar
#    rem set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\jbosssx-client.jar
#    rem set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\jboss-aop-client.jar
#    rem set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\jboss-common-core.jar
#    rem For the call to InitialContext.lookup()...
#    set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\jboss-remoting.jar
#    set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\jboss-aspect-jdk50-client.jar
#    set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\trove.jar
#    set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\javassist.jar
#    set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\jboss-security-spi.jar
#    set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\jboss-javaee.jar
#    rem For remote invocations on the ProfileService proxy (e.g. ProfileService.getDomains())...
#    set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\jboss-integration.jar
#    set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\jboss-ejb3-common-client.jar
#    set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\jboss-ejb3-core-client.jar
#    set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\jboss-ejb3-ext-api.jar
#    rem set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\jboss-ejb3-ext-api-impl.jar
#    set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\jboss-ejb3-proxy-spi-client.jar
#    set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\jboss-ejb3-proxy-impl-client.jar
#    set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\jboss-ejb3-security-client.jar
#    set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\concurrent.jar
#    set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\jboss-client.jar
#    set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\jboss-mdr.jar
#    
#    set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\lib\jboss-managed.jar
#    set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\lib\jboss-metatype.jar
#    
#    set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\lib\jboss-dependency.jar
#    rem Below is temporary!!!
#    set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\common\lib\jboss-profileservice.jar

JBOSS_CLASSPATH="$JBOSS_CLASSPATH:$MAIN_JAR"

# Setup JBoss sepecific properties
JBOSS_ENDORSED_DIRS="$JBOSS_HOME/lib/endorsed"

echo "$JAVA" $JAVA_OPTS -Xmx200M "-Djava.endorsed.dirs=$JBOSS_ENDORSED_DIRS" -classpath "$JBOSS_CLASSPATH" $MAIN_CLASS $@
"$JAVA" $JAVA_OPTS -Xmx200M "-Djava.endorsed.dirs=$JBOSS_ENDORSED_DIRS" -classpath "$JBOSS_CLASSPATH" $MAIN_CLASS $@
