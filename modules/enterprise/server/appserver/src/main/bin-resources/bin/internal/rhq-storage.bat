@echo off

rem ===========================================================================
rem RHQ Storage Node (Cassandra) Windows Startup Script
rem
rem This file is used to install, start, stop and remove the RHQ Storage Node
rem (cassandra) Windows Service for the Windows platform.  The RHQ Storage node
rem is actually wrapped by the Java Service Wrapper (JSW) and it is the JSW that
rem is the actual executable that is registered as the Windows Service.
rem
rem This script is customizable by setting the following environment variables:
rem

rem    RHQ_STORAGE_DEBUG - If this is defined, the script will emit debug
rem                      messages. If unset or "false", debug is turned off.
rem
rem    RHQ_SERVER_HOME - Defines where the Server's home install directory is.
rem                      If not defined, it will be assumed to be the parent
rem                      directory of the directory where this script lives.
rem
rem    RHQ_STORAGE_HOME - Defines where the Storage Node's home install directory
rem                      is. If not defined, it will be assumed to be
rem                      %RHQ_SERVER_HOME%\rhq-storage.
rem
rem    RHQ_JAVA_HOME - The location of the JRE that the server will use. This
rem                    will be ignored if RHQ_JAVA_EXE_FILE_PATH is set.
rem                    If this and RHQ_JAVA_EXE_FILE_PATH are not set, then
rem                    JAVA_HOME will be used.
rem
rem    RHQ_JAVA_EXE_FILE_PATH - Defines the full path to the Java executable to
rem                             use. If this is set, RHQ_JAVA_HOME is ignored.
rem                             If this is not set, then $RHQ_JAVA_HOME/bin/java
rem                             is used. If this and RHQ_JAVA_HOME are not set,
rem                             then $JAVA_HOME/bin/java will be used.
rem
rem    RHQ_STORAGE_INSTANCE_NAME - The name of the Windows Service; it must
rem                      conform to the Windows Service naming conventions. By
rem                      default, this is the name "rhqstorage-%COMPUTERNAME%"
rem
rem    RHQ_STORAGE_WRAPPER_LOG_DIR_PATH - The full path to the location where
rem                      the wrapper log file will go.
rem
rem    RHQ_STORAGE_RUN_AS - if defined, then when the Windows Service is
rem                      installed, the value is the domain\username of the
rem                      user that the Windows Service will run as. It is
rem                      required to also set RHQ_STORAGE_PASSWORD for the
rem                      specified user account.
rem                       
rem    RHQ_STORAGE_RUN_AS_ME - if defined, then when the Windows Service is
rem                      installed, the domain\username of the user that the Windows
rem                      Service will run as will be the current user (.\%USERNAME%).
rem                      This takes precedence over RHQ_STORAGE_RUN_AS. It is
rem                      required to also set RHQ_STORAGE_PASSWORD for the
rem                      specified user account.
rem                       
rem Note that you cannot define custom Java VM parameters or command line
rem arguments to pass to Cassandra.  If you wish to pass in  specific arguments,
rem modify the rhq-storage-wrapper.conf file.
rem
rem This script does not use the built-in cassandra.bat. 
rem ===========================================================================

setlocal enabledelayedexpansion

rem if debug variable is set, it is assumed to be on, unless its value is false
if "%RHQ_STORAGE_DEBUG%" == "false" (
   set RHQ_STORAGE_DEBUG=
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

if defined RHQ_STORAGE_DEBUG echo RHQ_SERVER_HOME: %RHQ_SERVER_HOME%

rem ----------------------------------------------------------------------
rem Change directory so the current directory is the Server home.
rem ----------------------------------------------------------------------

set RHQ_STORAGE_BIN_DIR_PATH=%~dp0

if not defined RHQ_STORAGE_HOME (
   set RHQ_STORAGE_HOME=%RHQ_SERVER_HOME%\rhq-storage
)
set RHQ_STORAGE_BIN_DIR_PATH=%RHQ_STORAGE_HOME%\bin

if not exist %RHQ_STORAGE_BIN_DIR_PATH% (
   echo Cannot find the RHQ_STORAGE bin directory: %RHQ_STORAGE_BIN_DIR_PATH%
      exit /B 1
)

if defined RHQ_STORAGE_DEBUG echo RHQ_STORAGE_HOME: %RHQ_STORAGE_HOME%

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
      if defined RHQ_STORAGE_DEBUG echo No RHQ JAVA property set, defaulting to JAVA_HOME: !JAVA_HOME!
      set RHQ_JAVA_HOME=!JAVA_HOME!
   )
)
if not defined RHQ_JAVA_EXE_FILE_PATH (
   set RHQ_JAVA_EXE_FILE_PATH=!RHQ_JAVA_HOME!\bin\java.exe
)

if defined RHQ_STORAGE_DEBUG echo RHQ_JAVA_HOME: %RHQ_JAVA_HOME%
if defined RHQ_STORAGE_DEBUG echo RHQ_JAVA_EXE_FILE_PATH: %RHQ_JAVA_EXE_FILE_PATH%

if not exist "%RHQ_JAVA_EXE_FILE_PATH%" (
   echo There is no JVM available.
   echo Please set RHQ_JAVA_HOME or RHQ_JAVA_EXE_FILE_PATH appropriately.
   exit /B 1
)


rem ----------------------------------------------------------------------
rem Define the name used for the name of the Windows Service.
rem If this is not defined, the name of the computer is used.
rem ----------------------------------------------------------------------

if not defined RHQ_STORAGE_INSTANCE_NAME (
   set RHQ_STORAGE_INSTANCE_NAME=rhqstorage-%COMPUTERNAME%
)
if defined RHQ_STORAGE_DEBUG echo RHQ_STORAGE_INSTANCE_NAME: %RHQ_STORAGE_INSTANCE_NAME%

rem ----------------------------------------------------------------------
rem Determine the wrapper directory.
rem ----------------------------------------------------------------------

set RHQ_STORAGE_WRAPPER_DIR_PATH=%RHQ_SERVER_BIN_DIR_PATH%\..\wrapper
if defined RHQ_STORAGE_DEBUG echo RHQ_STORAGE_WRAPPER_DIR_PATH: %RHQ_STORAGE_WRAPPER_DIR_PATH%

rem ----------------------------------------------------------------------
rem The Windows OS platform name is also the wrapper subdirectory name.
rem ----------------------------------------------------------------------

set RHQ_STORAGE_OS_PLATFORM=windows-x86_32
if defined RHQ_STORAGE_DEBUG echo RHQ_STORAGE_OS_PLATFORM: %RHQ_STORAGE_OS_PLATFORM%

rem ----------------------------------------------------------------------
rem Determine the wrapper executable that this script will run.
rem ----------------------------------------------------------------------

set RHQ_STORAGE_WRAPPER_EXE_FILE_PATH=%RHQ_STORAGE_WRAPPER_DIR_PATH%\%RHQ_STORAGE_OS_PLATFORM%\wrapper.exe
if defined RHQ_STORAGE_DEBUG echo RHQ_STORAGE_WRAPPER_EXE_FILE_PATH: %RHQ_STORAGE_WRAPPER_EXE_FILE_PATH%

rem ----------------------------------------------------------------------
rem Determine the Storage wrapper configuration file.
rem ----------------------------------------------------------------------

set RHQ_STORAGE_WRAPPER_CONF_FILE_PATH=%RHQ_STORAGE_WRAPPER_DIR_PATH%\rhq-storage-wrapper.conf
if defined RHQ_STORAGE_DEBUG echo RHQ_STORAGE_WRAPPER_CONF_FILE_PATH: %RHQ_STORAGE_WRAPPER_CONF_FILE_PATH%

rem ----------------------------------------------------------------------
rem Create and configure the wrapper log directory.
rem ----------------------------------------------------------------------

if not defined RHQ_STORAGE_WRAPPER_LOG_DIR_PATH (
   set RHQ_STORAGE_WRAPPER_LOG_DIR_PATH=%RHQ_SERVER_HOME%\logs
)
if not exist "%RHQ_STORAGE_WRAPPER_LOG_DIR_PATH%" (
   mkdir "%RHQ_STORAGE_WRAPPER_LOG_DIR_PATH%"
)

if defined RHQ_STORAGE_DEBUG echo RHQ_STORAGE_WRAPPER_LOG_DIR_PATH: %RHQ_STORAGE_WRAPPER_LOG_DIR_PATH%

rem ----------------------------------------------------------------------
rem Determine what to do and do it.
rem ----------------------------------------------------------------------

rem Determine if there should be debug VM options passed into it.
rem For some reason, this can't go inside another if statement.
if defined RHQ_STORAGE_DEBUG set _DEBUG_OPTS=wrapper.debug=true

if /i "%1"=="console" (
   rem Determine what user the Windows Service will run as.
   if defined RHQ_STORAGE_RUN_AS (
      if not defined RHQ_STORAGE_PASSWORD (
         echo Exiting. RHQ_STORAGE_PASSWORD is not set but is required because RHQ_STORAGE_RUN_AS is set: %RHQ_STORAGE_RUN_AS%.
         exit /B 1
      )
      set _WRAPPER_NTSERVICE_ACCOUNT="wrapper.ntservice.account=%RHQ_STORAGE_RUN_AS%"
   )
   if defined RHQ_STORAGE_RUN_AS_ME (
      if not defined RHQ_STORAGE_PASSWORD (
         echo Exiting. RHQ_STORAGE_PASSWORD is not set but is required because RHQ_STORAGE_RUN_AS_ME is set.
         exit /B 1
      )
      set _WRAPPER_NTSERVICE_ACCOUNT="wrapper.ntservice.account=.\%USERNAME%"
   )

   rem START STORAGE NODE
   start "%RHQ_STORAGE_WRAPPER_EXE_FILE_PATH%" -c "%RHQ_STORAGE_WRAPPER_CONF_FILE_PATH%" "set.RHQ_SERVER_HOME=%RHQ_SERVER_HOME%" "set.RHQ_STORAGE_HOME=%RHQ_STORAGE_HOME%" "set.RHQ_STORAGE_INSTANCE_NAME=%RHQ_STORAGE_INSTANCE_NAME%" "set.RHQ_JAVA_EXE_FILE_PATH=%RHQ_JAVA_EXE_FILE_PATH%" "set.RHQ_STORAGE_OS_PLATFORM=%RHQ_STORAGE_OS_PLATFORM%" "set.RHQ_STORAGE_WRAPPER_LOG_DIR_PATH=%RHQ_STORAGE_WRAPPER_LOG_DIR_PATH%" !_WRAPPER_NTSERVICE_ACCOUNT! %_DEBUG_OPTS%
   goto done
)

if /i "%1"=="install" (
   rem Determine what user the Windows Service will run as.
   if defined RHQ_STORAGE_RUN_AS (
      if not defined RHQ_STORAGE_PASSWORD (
         echo Exiting. RHQ_STORAGE_PASSWORD is not set but is required because RHQ_STORAGE_RUN_AS is set: %RHQ_STORAGE_RUN_AS%.
         exit /B 1
      )
      set _WRAPPER_NTSERVICE_ACCOUNT="wrapper.ntservice.account=%RHQ_STORAGE_RUN_AS%"
   )
   if defined RHQ_STORAGE_RUN_AS_ME (
      if not defined RHQ_STORAGE_PASSWORD (
         echo Exiting. RHQ_STORAGE_PASSWORD is not set but is required because RHQ_STORAGE_RUN_AS_ME is set.
         exit /B 1
      )
      set _WRAPPER_NTSERVICE_ACCOUNT="wrapper.ntservice.account=.\%USERNAME%"
   )

   "%RHQ_STORAGE_WRAPPER_EXE_FILE_PATH%" -i "%RHQ_STORAGE_WRAPPER_CONF_FILE_PATH%" "set.RHQ_SERVER_HOME=%RHQ_SERVER_HOME%" "set.RHQ_STORAGE_HOME=%RHQ_STORAGE_HOME%" "set.RHQ_STORAGE_INSTANCE_NAME=%RHQ_STORAGE_INSTANCE_NAME%" "set.RHQ_JAVA_EXE_FILE_PATH=%RHQ_JAVA_EXE_FILE_PATH%" "set.RHQ_STORAGE_OS_PLATFORM=%RHQ_STORAGE_OS_PLATFORM%" "set.RHQ_STORAGE_WRAPPER_LOG_DIR_PATH=%RHQ_STORAGE_WRAPPER_LOG_DIR_PATH%" !_WRAPPER_NTSERVICE_ACCOUNT! %_DEBUG_OPTS%
   goto done
)

if /i "%1"=="start" (
   rem START STORAGE NODE
   "%RHQ_STORAGE_WRAPPER_EXE_FILE_PATH%" -t "%RHQ_STORAGE_WRAPPER_CONF_FILE_PATH%"
   goto done
)

if /i "%1"=="stop" (
   "%RHQ_STORAGE_WRAPPER_EXE_FILE_PATH%" -p "%RHQ_STORAGE_WRAPPER_CONF_FILE_PATH%"
   goto done
)

if /i "%1"=="remove" (
   "%RHQ_STORAGE_WRAPPER_EXE_FILE_PATH%" -r "%RHQ_STORAGE_WRAPPER_CONF_FILE_PATH%"
   goto done
)

if /i "%1"=="status" (
   "%RHQ_STORAGE_WRAPPER_EXE_FILE_PATH%" -q "%RHQ_STORAGE_WRAPPER_CONF_FILE_PATH%"
   goto done
)

echo Usage: %0 { install ^| start ^| stop ^| remove ^| status ^| console }
goto :done


rem ----------------------------------------------------------------------
rem CALL subroutine that exits this script normally
rem ----------------------------------------------------------------------

:done
endlocal
