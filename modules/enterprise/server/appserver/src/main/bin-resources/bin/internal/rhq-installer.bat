@echo off

rem ===========================================================================
rem RHQ Server Windows Install Script
rem
rem This file is used to complete the installation of the RHQ Server on a
rem Windows platform.
rem
rem This script is customized by the settings in rhq-server-env.bat.  The options
rem set there will be applied to this script.  It is not recommended to edit this
rem script directly.
rem =============================================================================

setlocal enabledelayedexpansion

rem if debug variable is set, it is assumed to be on, unless its value is false
if "%RHQ_SERVER_DEBUG%" == "false" (
   set RHQ_SERVER_DEBUG=
)

if exist "..\rhq-server-env.bat" (
   call "..\rhq-server-env.bat" %*
) else (
   if defined RHQ_SERVER_DEBUG echo Failed to find rhq-server-env.bat. Continuing with current environment...
)

rem ----------------------------------------------------------------------
rem Change directory so the current directory is the Server home.
rem ----------------------------------------------------------------------

set RHQ_SERVER_BIN_DIR_PATH=%~dp0

if not defined RHQ_SERVER_HOME (
   cd "%RHQ_SERVER_BIN_DIR_PATH%\..\.."
) else (
   cd "%RHQ_SERVER_HOME%" || (
      echo Cannot go to the RHQ_SERVER_HOME directory: %RHQ_SERVER_HOME%
      exit /B 1
      )
)

set RHQ_SERVER_HOME=%CD%

if defined RHQ_SERVER_DEBUG echo RHQ_SERVER_HOME: %RHQ_SERVER_HOME%

rem ----------------------------------------------------------------------
rem Find the Java executable and verify we have a VM available
rem Note that RHQ_SERVER_JAVA_* props are still handled for back compat
rem ----------------------------------------------------------------------

if not defined RHQ_JAVA_EXE_FILE_PATH (
   if defined RHQ_SERVER_JAVA_EXE_FILE_PATH (
      set RHQ_JAVA_EXE_FILE_PATH=!RHQ_SERVER_JAVA_EXE_FILE_PATH!
   )
)
if not defined RHQ_JAVA_HOME (
   if defined RHQ_SERVER_JAVA_HOME (
      set RHQ_JAVA_HOME=!RHQ_SERVER_JAVA_HOME!
   )
)

if not defined RHQ_JAVA_EXE_FILE_PATH (
   if not defined RHQ_JAVA_HOME (
      if defined RHQ_SERVER_DEBUG echo No RHQ JAVA property set, defaulting to JAVA_HOME: !JAVA_HOME!
      set RHQ_JAVA_HOME=!JAVA_HOME!
   )
)
if not defined RHQ_JAVA_EXE_FILE_PATH (
   set RHQ_JAVA_EXE_FILE_PATH=!RHQ_JAVA_HOME!\bin\java.exe
)

if defined RHQ_SERVER_DEBUG echo RHQ_JAVA_HOME: %RHQ_JAVA_HOME%
if defined RHQ_SERVER_DEBUG echo RHQ_JAVA_EXE_FILE_PATH: %RHQ_JAVA_EXE_FILE_PATH%

if not exist "%RHQ_JAVA_EXE_FILE_PATH%" (
   echo There is no JVM available.
   echo Please set RHQ_JAVA_HOME or RHQ_JAVA_EXE_FILE_PATH appropriately.
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

if not defined RHQ_SERVER_INSTALLER_JAVA_OPTS set RHQ_SERVER_INSTALLER_JAVA_OPTS=-Xms512M -Xmx512M -XX:PermSize=128M -XX:MaxPermSize=128M -Djava.net.preferIPv4Stack=true -Dorg.jboss.resolver.warning=true

rem Add the JVM opts that we always want to specify, whether or not the user set RHQ_SERVER_INSTALLER_JAVA_OPTS.
if defined RHQ_SERVER_DEBUG set _RHQ_LOGLEVEL=DEBUG
if not defined RHQ_SERVER_DEBUG set _RHQ_LOGLEVEL=INFO
set RHQ_SERVER_INSTALLER_JAVA_OPTS=%RHQ_SERVER_INSTALLER_JAVA_OPTS% -Djava.awt.headless=true -Di18nlog.logger-type=commons -Drhq.server.properties-file=%RHQ_SERVER_HOME%\bin\rhq-server.properties -Drhq.server.installer.logdir=%RHQ_SERVER_HOME%\logs -Drhq.server.installer.loglevel=%_RHQ_LOGLEVEL%  -Drhq.server.basedir=%RHQ_SERVER_HOME%

rem Sample JPDA settings for remote socket debugging
rem set RHQ_SERVER_INSTALLER_JAVA_OPTS=%RHQ_SERVER_INSTALLER_JAVA_OPTS% -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=y

if defined RHQ_SERVER_DEBUG echo RHQ_SERVER_INSTALLER_JAVA_OPTS: %RHQ_SERVER_INSTALLER_JAVA_OPTS%
if defined RHQ_SERVER_DEBUG echo RHQ_SERVER_INSTALLER_ADDITIONAL_JAVA_OPTS: %RHQ_SERVER_INSTALLER_ADDITIONAL_JAVA_OPTS%

rem ----------------------------------------------------------------------
rem We need to add our own modules to the core set of JBossAS modules.
rem ----------------------------------------------------------------------
set _RHQ_MODULES_PATH=%RHQ_SERVER_HOME%\modules
set _INTERNAL_MODULES_PATH=%RHQ_SERVER_HOME%\jbossas\modules
set _JBOSS_MODULEPATH=%_RHQ_MODULES_PATH%;%_INTERNAL_MODULES_PATH%
if defined RHQ_SERVER_DEBUG echo _JBOSS_MODULEPATH: %_JBOSS_MODULEPATH%

rem start the AS instance with our main installer module
"%RHQ_JAVA_EXE_FILE_PATH%" %RHQ_SERVER_INSTALLER_JAVA_OPTS% %RHQ_SERVER_INSTALLER_ADDITIONAL_JAVA_OPTS% -jar "%RHQ_SERVER_HOME%\jbossas\jboss-modules.jar" -mp "%_JBOSS_MODULEPATH%" org.rhq.rhq-installer-util %*

goto :done

rem ----------------------------------------------------------------------
rem CALL subroutine that exits this script normally
rem ----------------------------------------------------------------------

:done
endlocal
