@echo off
rem -------------------------------------------------------------------------
rem JBoss JVM Launcher
rem -------------------------------------------------------------------------

rem $Id$

if not "%ECHO%" == ""  echo %ECHO%
if "%OS%" == "Windows_NT"  setlocal

set MAIN_JAR_NAME=jbas5-jnp-client-1.0.jar
set MAIN_CLASS=test.RmiClient

set JBOSS_HOME=C:\opt\jboss-5.1.0.GA-SNAPSHOT

set DIRNAME=.\
if "%OS%" == "Windows_NT" set DIRNAME=%~dp0%
set PROGNAME=run.bat
if "%OS%" == "Windows_NT" set PROGNAME=%~nx0%

rem Find MAIN_JAR, or we can't continue

set MAIN_JAR=%DIRNAME%\target\%MAIN_JAR_NAME%
if exist "%MAIN_JAR%" goto FOUND_MAIN_JAR
echo Could not locate %MAIN_JAR%. Please check that you are in the
echo bin directory when running this script.
goto END

:FOUND_MAIN_JAR

if not "%JAVA_HOME%" == "" goto HAVE_JAVA_HOME

set JAVA=java

echo JAVA_HOME is not set.  Unexpected results may occur.
echo Set JAVA_HOME to the directory of your local JDK to avoid this message.
goto SKIP_SET_JAVA_HOME

:HAVE_JAVA_HOME

set JAVA=%JAVA_HOME%\bin\java

:SKIP_SET_JAVA_HOME

rem only include jbossall-client.jar in classpath, if
rem JBOSS_CLASSPATH was not yet set
if not "%JBOSS_CLASSPATH%" == "" GOTO HAVE_JB_CP
rem set JBOSS_CLASSPATH=%JBOSS_HOME%\client\jbossall-client.jar

rem For the call to new InitialContext() (using org.jnp.interfaces.NamingContextFactory)...
set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\jnp-client.jar
set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\common\lib\jboss-security-aspects.jar
set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\jbosssx-client.jar
set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\jboss-aop-client.jar
set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\jboss-common-core.jar
rem For the call to InitialContext.lookup()...
set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\jboss-remoting.jar
set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\jboss-aspect-jdk50-client.jar
set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\trove.jar
set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\javassist.jar
set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\jboss-security-spi.jar
set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\jboss-javaee.jar
rem For remote invocations on the ProfileService proxy (e.g. ProfileService.getViewManager())...
set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\jboss-integration.jar
set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\concurrent.jar
set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\jboss-client.jar
set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\client\jboss-mdr.jar
set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\lib\jboss-managed.jar
set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%JBOSS_HOME%\lib\jboss-metatype.jar

:HAVE_JB_CP

set JBOSS_CLASSPATH=%JBOSS_CLASSPATH%;%MAIN_JAR%

rem Setup JBoss sepecific properties
set JBOSS_ENDORSED_DIRS=%JBOSS_HOME%\lib\endorsed

@echo on
"%JAVA%" %JAVA_OPTS% "-Djava.endorsed.dirs=%JBOSS_ENDORSED_DIRS%" -classpath "%JBOSS_CLASSPATH%" %MAIN_CLASS% %*

:END
