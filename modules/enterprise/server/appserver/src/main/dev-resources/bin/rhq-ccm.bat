@echo off

rem ===========================================================================
rem RHQ Cassandra Cluster Manager Script
rem
rem This file is used to complete the installation of the RHQ Server on a
rem Windows platform.
rem
rem This script is customizable by setting the following environment variables:
rem
rem    RHQ_SERVER_DEBUG - If this is defined, the script will emit debug
rem                       messages.
rem                       If not set or set to "false", debug is turned off.
rem
rem    RHQ_SERVER_HOME - Defines where the Server's home install directory is.
rem                      If not defined, it will be assumed to be the parent
rem                      directory of the directory where this script lives.
rem
rem
rem    RHQ_SERVER_JAVA_HOME - The location of the JRE that the Server will
rem                           use. This will be ignored if
rem                           RHQ_SERVER_JAVA_EXE_FILE_PATH is set.
rem                           If this and RHQ_SERVER_JAVA_EXE_FILE_PATH are
rem                           not set, the Server's embedded JRE will be used.
rem
rem    RHQ_SERVER_JAVA_EXE_FILE_PATH - Defines the full path to the Java
rem                                    executable to use. If this is set,
rem                                    RHQ_SERVER_JAVA_HOME is ignored.
rem                                    If this is not set, then
rem                                    %RHQ_SERVER_JAVA_HOME%\bin\java.exe
rem                                    is used. If this and
rem                                    RHQ_SERVER_JAVA_HOME are not set, the
rem                                    Server's embedded JRE will be used.
rem
rem    RHQ_SERVER_INSTALLER_JAVA_OPTS - Java VM command line options to be
rem                         passed into the Java VM. If this is not defined
rem                         this script will pass in a default set of options.
rem                         If you only want to add options to the defaults,
rem                         then you will want to use
rem                         RHQ_SERVER_INSTALLER_ADDITIONAL_JAVA_OPTS instead.
rem
rem    RHQ_SERVER_INSTALLER_ADDITIONAL_JAVA_OPTS - additional Java VM command
rem                                    line options to be passed into the VM.
rem                                    This is added to
rem                                    RHQ_SERVER_INSTALLER_JAVA_OPTS; it
rem                                    is mainly used to augment the
rem                                    default set of options. This can be
rem                                    left unset if it is not needed.
rem
rem If the embedded JRE is to be used but is not available, the fallback
rem JRE to be used will be determined by the JAVA_HOME environment variable.
rem ===========================================================================

setlocal

rem if debug variable is set, it is assumed to be on, unless its value is false
if "%RHQ_SERVER_DEBUG%" == "false" (
   set RHQ_SERVER_DEBUG=
)

rem ----------------------------------------------------------------------
rem Change directory so the current directory is the Server home.
rem ----------------------------------------------------------------------

set RHQ_SERVER_BIN_DIR_PATH=%~dp0

if not defined RHQ_SERVER_HOME (
   cd "%RHQ_SERVER_BIN_DIR_PATH%\.."
) else (
   cd "%RHQ_SERVER_HOME%" || (
      echo Cannot go to the RHQ_SERVER_HOME directory: %RHQ_SERVER_HOME%
      exit /B 1
      )
)

set RHQ_SERVER_HOME=%CD%

if defined RHQ_SERVER_DEBUG echo RHQ_SERVER_HOME: %RHQ_SERVER_HOME%

rem ----------------------------------------------------------------------
rem Find the Java executable and verify we have a VM available.
rem ----------------------------------------------------------------------

if not defined RHQ_SERVER_JAVA_EXE_FILE_PATH (
   if not defined RHQ_SERVER_JAVA_HOME call :prepare_embedded_jre
)

if not defined RHQ_SERVER_JAVA_EXE_FILE_PATH set RHQ_SERVER_JAVA_EXE_FILE_PATH=%RHQ_SERVER_JAVA_HOME%\bin\java.exe

if defined RHQ_SERVER_DEBUG echo RHQ_SERVER_JAVA_HOME: %RHQ_SERVER_JAVA_HOME%
if defined RHQ_SERVER_DEBUG echo RHQ_SERVER_JAVA_EXE_FILE_PATH: %RHQ_SERVER_JAVA_EXE_FILE_PATH%

if not exist "%RHQ_SERVER_JAVA_EXE_FILE_PATH%" (
   echo There is no JVM available.
   echo Please set RHQ_SERVER_JAVA_HOME or RHQ_SERVER_JAVA_EXE_FILE_PATH appropriately.
   exit /B 1
)

if not exist "%RHQ_SERVER_HOME%\jbossas\jboss-modules.jar" (
   echo ERROR! RHQ_SERVER_HOME is not pointing to a valid RHQ instance
   echo Missing %RHQ_SERVER_HOME%\jboss-modules.jar
   exit /B 1
)

rem ----------------------------------------------------------------------
rem Determine the logs directory
rem ----------------------------------------------------------------------

set _LOG_DIR_PATH=%RHQ_SERVER_HOME%\logs

rem ----------------------------------------------------------------------
rem Prepare the VM command line options to be passed in
rem ----------------------------------------------------------------------

if not defined RHQ_CCM_JAVA_OPTS set RHQ_CCM_JAVA_OPTS=-Xms512M -Xmx512M -XX:PermSize=128M -XX:MaxPermSize=128M -Djava.net.preferIPv4Stack=true -Dorg.jboss.resolver.warning=true

rem Add the JVM opts that we always want to specify, whether or not the user set RHQ_SERVER_INSTALLER_JAVA_OPTS.
if defined RHQ_SERVER_DEBUG set _RHQ_LOGLEVEL=DEBUG
if not defined RHQ_SERVER_DEBUG set _RHQ_LOGLEVEL=INFO
set RHQ_CCM_JAVA_OPTS=%RHQ_CCM_JAVA_OPTS% -Djava.awt.headless=true -Drhq.server.properties-file=%RHQ_SERVER_HOME%\bin\rhq-server.properties -Drhq.ccm.logdir=%RHQ_SERVER_HOME%\logs -Drhq.ccm.loglevel=%_RHQ_LOGLEVEL%

rem Sample JPDA settings for remote socket debugging
rem RHQ_CCM_JAVA_OPTS=%RHQ_CCM_JAVA_OPTS% -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=y

if defined RHQ_SERVER_DEBUG echo RHQ_CCM_JAVA_OPTS: %RHQ_CCM_JAVA_OPTS%
if defined RHQ_SERVER_DEBUG echo RHQ_CCM_ADDITIONAL_JAVA_OPTS: %RHQ_CCM_ADDITIONAL_JAVA_OPTS%

rem ----------------------------------------------------------------------
rem We need to add our own modules to the core set of JBossAS modules.
rem ----------------------------------------------------------------------
set _RHQ_MODULES_PATH=%RHQ_SERVER_HOME%\modules
set _INTERNAL_MODULES_PATH=%RHQ_SERVER_HOME%\jbossas\modules
set _JBOSS_MODULEPATH=%_RHQ_MODULES_PATH%;%_INTERNAL_MODULES_PATH%
if defined RHQ_SERVER_DEBUG echo _JBOSS_MODULEPATH: %_JBOSS_MODULEPATH%

echo Starting RHQ CCM ...

rem start the AS instance with our main installer module
"%RHQ_SERVER_JAVA_EXE_FILE_PATH%" %RHQ_SCCM_JAVA_OPTS% %RHQ_CCM_ADDITIONAL_JAVA_OPTS% -jar "%RHQ_SERVER_HOME%\jbossas\jboss-modules.jar" -mp "%_JBOSS_MODULEPATH%" org.rhq.rhq-cassandra-ccm-cli %*

goto :done

rem ----------------------------------------------------------------------
rem CALL subroutine that prepares to use the embedded JRE
rem ----------------------------------------------------------------------

:prepare_embedded_jre
set RHQ_SERVER_JAVA_HOME=%RHQ_SERVER_HOME%\jre
if defined RHQ_SERVER_DEBUG echo Using the embedded JRE
if not exist "%RHQ_SERVER_JAVA_HOME%" (
   if defined RHQ_SERVER_DEBUG echo No embedded JRE found - will try to use JAVA_HOME: %JAVA_HOME%
   set RHQ_SERVER_JAVA_HOME=%JAVA_HOME%
)
goto :eof

rem ----------------------------------------------------------------------
rem CALL subroutine that exits this script normally
rem ----------------------------------------------------------------------

:done
endlocal
