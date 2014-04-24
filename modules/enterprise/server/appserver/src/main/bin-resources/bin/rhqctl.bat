@echo off

rem =============================================================================
rem RHQ Control Script

rem This file is used to control the RHQ components on a Windows machine.

rem This script is customized by the settings in rhq-server-env.bat.  The options
rem set there will be applied to this script.  It is not recommended to edit this
rem script directly.
rem =============================================================================

setlocal enabledelayedexpansion

rem if debug variable is set, it is assumed to be on, unless its value is false
if "%RHQ_CONTROL_DEBUG%" == "false" (
   set RHQ_CONTROL_DEBUG=
)

set _SCRIPT_DIR=%~dp0
if exist "%_SCRIPT_DIR%\rhq-server-env.bat" (
   call "%_SCRIPT_DIR%\rhq-server-env.bat" %*
) else (
   if defined RHQ_CONTROL_DEBUG echo Failed to find rhq-server-env.bat. Continuing with current environment...
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

if defined RHQ_CONTROL_DEBUG echo RHQ_SERVER_HOME: %RHQ_SERVER_HOME%

rem ----------------------------------------------------------------------
rem Determine what JBossAS instance to use.
rem If RHQ_SERVER_JBOSS_HOME and JBOSS_HOME are both not defined, we will
rem assume we are to run the embedded AS instance from the RHQ
rem installation directory - RHQ_SERVER_HOME\jbossas
rem ----------------------------------------------------------------------

if not defined RHQ_SERVER_JBOSS_HOME (
   if not defined JBOSS_HOME (
      set RHQ_SERVER_JBOSS_HOME=%RHQ_SERVER_HOME%\jbossas
   ) else (
      if not exist "%JBOSS_HOME%" (
         echo ERROR! JBOSS_HOME is not pointing to a valid AS directory
         echo JBOSS_HOME: "%JBOSS_HOME%"
         exit /B 1
      )
      set RHQ_SERVER_JBOSS_HOME=%JBOSS_HOME%
   )
) else (
   cd %RHQ_SERVER_JBOSS_HOME% || (
      echo ERROR! RHQ_SERVER_JBOSS_HOME is not pointing to a valid AS directory
      echo RHQ_SERVER_JBOSS_HOME: "%RHQ_SERVER_JBOSS_HOME%"
      exit /B 1
   )
)

cd %RHQ_SERVER_JBOSS_HOME%
set RHQ_SERVER_JBOSS_HOME=%CD%

if defined RHQ_CONTROL_DEBUG echo RHQ_SERVER_JBOSS_HOME: %RHQ_SERVER_JBOSS_HOME%


if not exist "%RHQ_SERVER_JBOSS_HOME%\jboss-modules.jar" (
   echo ERROR! RHQ_SERVER_JBOSS_HOME is not pointing to a valid AS instance
   echo Missing "%RHQ_SERVER_JBOSS_HOME%\jboss-modules.jar"
   exit /B 1
)

rem we want the rest of this script to be able to assume cwd is the RHQ install dir
cd "%RHQ_SERVER_HOME%"


rem ----------------------------------------------------------------------
rem Create the logs directory
rem ----------------------------------------------------------------------

set _LOG_DIR_PATH=%RHQ_SERVER_HOME%\logs
if not exist "%_LOG_DIR_PATH%" (
   mkdir "%_LOG_DIR_PATH%"
)


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
      if defined RHQ_CONTROL_DEBUG echo No RHQ JAVA property set, defaulting to JAVA_HOME: !JAVA_HOME!
      set RHQ_JAVA_HOME=!JAVA_HOME!
   )
)
if not defined RHQ_JAVA_EXE_FILE_PATH (
   set RHQ_JAVA_EXE_FILE_PATH=!RHQ_JAVA_HOME!\bin\java.exe
)

if defined RHQ_CONTROL_DEBUG echo RHQ_JAVA_HOME: %RHQ_JAVA_HOME%
if defined RHQ_CONTROL_DEBUG echo RHQ_JAVA_EXE_FILE_PATH: %RHQ_JAVA_EXE_FILE_PATH%

if not exist "%RHQ_JAVA_EXE_FILE_PATH%" (
   echo There is no JVM available.
   echo Please set RHQ_JAVA_HOME or RHQ_JAVA_EXE_FILE_PATH appropriately.
   exit /B 1
)


rem ----------------------------------------------------------------------
rem Prepare the VM command line options to be passed in
rem ----------------------------------------------------------------------

if not defined RHQ_CONTROL_JAVA_OPTS (
   set RHQ_CONTROL_JAVA_OPTS=-Xmx512M -XX:MaxPermSize=128M -Djava.net.preferIPv4Stack=true -Dorg.jboss.resolver.warning=true
)

rem Add the JVM opts that we always want to specify, whether or not the user set RHQ_CCM_JAVA_OPTS.
if defined RHQ_CONTROL_DEBUG (
   set _RHQ_LOGLEVEL=DEBUG
) else (
   set _RHQ_LOGLEVEL=INFO
)

rem Set some of the base required options
set RHQ_CONTROL_JAVA_OPTS=%RHQ_CONTROL_JAVA_OPTS% -Djava.awt.headless=true -Drhq.server.properties-file="%RHQ_SERVER_HOME%\bin\rhq-server.properties" -Drhq.control.logdir="%RHQ_SERVER_HOME%\logs" -Drhq.control.loglevel=%_RHQ_LOGLEVEL% -Drhq.server.basedir="%RHQ_SERVER_HOME%" -Drhqctl.properties-file="%RHQ_SERVER_HOME%\bin\rhqctl.properties" -Drhq.java-exe-file-path="%RHQ_JAVA_EXE_FILE_PATH%"

rem Sample JPDA settings for remote socket debugging
rem set RHQ_CONTROL_JAVA_OPTS=%RHQ_CONTROL_JAVA_OPTS% -Xrunjdwp:transport=dt_socket,address=8786,server=y,suspend=y

if defined RHQ_CONTROL_DEBUG echo RHQ_CONTROL_JAVA_OPTS: %RHQ_CONTROL_JAVA_OPTS%
if defined RHQ_CONTROL_DEBUG echo RHQ_CONTROL_ADDITIONAL_JAVA_OPTS: %RHQ_CONTROL_ADDITIONAL_JAVA_OPTS%

rem ----------------------------------------------------------------------
rem We need to add our own modules to the core set of JBossAS modules.
rem ----------------------------------------------------------------------
set _RHQ_MODULES_PATH=%RHQ_SERVER_HOME%\modules
set _INTERNAL_MODULES_PATH=%RHQ_SERVER_JBOSS_HOME%\modules
set _JBOSS_MODULEPATH=%_RHQ_MODULES_PATH%;%_INTERNAL_MODULES_PATH%

if defined RHQ_CONTROL_DEBUG echo _JBOSS_MODULEPATH: %_JBOSS_MODULEPATH%

rem start the AS instance with our main installer module
"%RHQ_JAVA_EXE_FILE_PATH%" %RHQ_CONTROL_JAVA_OPTS% %RHQ_CONTROL_ADDITIONAL_JAVA_OPTS% -jar "%RHQ_SERVER_JBOSS_HOME%\jboss-modules.jar" -mp "%_JBOSS_MODULEPATH%" org.rhq.rhq-server-control %*
if not errorlevel 1 goto done
exit /B %ERRORLEVEL%

:done
endlocal
