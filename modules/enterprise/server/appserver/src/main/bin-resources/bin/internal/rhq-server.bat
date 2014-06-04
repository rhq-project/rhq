@echo off

rem ===========================================================================
rem RHQ Server Windows Startup Script
rem
rem This file is used to install, start, stop and remove the RHQ Server Windows
rem Service for the Windows platform.  It can also be used to start the
rem RHQ Server in a console window. The RHQ Server is actually wrapped
rem by the Java Service Wrapper (JSW) and it is the JSW that is the actual
rem executable that is registered as the Windows Service.
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

rem ----------------------------------------------------------------------
rem Define the name used for the name of the Windows Service.
rem If this is not defined, the name of the computer is used.
rem ----------------------------------------------------------------------

if not defined RHQ_SERVER_INSTANCE_NAME (
   set RHQ_SERVER_INSTANCE_NAME=rhqserver-%COMPUTERNAME%
)
if defined RHQ_SERVER_DEBUG echo RHQ_SERVER_INSTANCE_NAME: %RHQ_SERVER_INSTANCE_NAME%

rem ----------------------------------------------------------------------
rem Determine the wrapper directory.
rem ----------------------------------------------------------------------

set RHQ_SERVER_WRAPPER_DIR_PATH=%RHQ_SERVER_BIN_DIR_PATH%\..\wrapper
if defined RHQ_SERVER_DEBUG echo RHQ_SERVER_WRAPPER_DIR_PATH: %RHQ_SERVER_WRAPPER_DIR_PATH%

rem ----------------------------------------------------------------------
rem If this is 64 bit windows and a 64-Bit JVM then run the 64 bit wrapper,
rem otherwise 32-bit.
rem ----------------------------------------------------------------------

set RHQ_SERVER_OS_PLATFORM=windows-x86_32
if /I "%PROCESSOR_ARCHITECTURE:~-2%"=="64" set RHQ_SERVER_OS_PLATFORM=windows-x86_64
if /I "%PROCESSOR_ARCHITEW6432:~-2%"=="64" set RHQ_SERVER_OS_PLATFORM=windows-x86_64
rem For 64 bit OS, ensure it's a 64 bit JVM
"%RHQ_JAVA_EXE_FILE_PATH%" -version 2>>&1 | findstr /I /C:"64-Bit" >>nul:
if errorlevel 1 (
    set RHQ_SERVER_OS_PLATFORM=windows-x86_32
)
if defined RHQ_SERVER_DEBUG echo RHQ_SERVER_OS_PLATFORM: %RHQ_SERVER_OS_PLATFORM%

rem ----------------------------------------------------------------------
rem Determine the wrapper executable that this script will run.
rem ----------------------------------------------------------------------

set RHQ_SERVER_WRAPPER_EXE_FILE_PATH=%RHQ_SERVER_WRAPPER_DIR_PATH%\%RHQ_SERVER_OS_PLATFORM%\wrapper.exe
if defined RHQ_SERVER_DEBUG echo RHQ_SERVER_WRAPPER_EXE_FILE_PATH: %RHQ_SERVER_WRAPPER_EXE_FILE_PATH%

rem ----------------------------------------------------------------------
rem Determine the Server wrapper configuration file.
rem ----------------------------------------------------------------------

set RHQ_SERVER_WRAPPER_CONF_FILE_PATH=%RHQ_SERVER_WRAPPER_DIR_PATH%\rhq-server-wrapper.conf
if defined RHQ_SERVER_DEBUG echo RHQ_SERVER_WRAPPER_CONF_FILE_PATH: %RHQ_SERVER_WRAPPER_CONF_FILE_PATH%

rem ----------------------------------------------------------------------
rem Create and configure the wrapper log directory.
rem ----------------------------------------------------------------------

if not defined RHQ_SERVER_WRAPPER_LOG_DIR_PATH (
   set RHQ_SERVER_WRAPPER_LOG_DIR_PATH=%RHQ_SERVER_HOME%\logs
)
if not exist "%RHQ_SERVER_WRAPPER_LOG_DIR_PATH%" (
   mkdir "%RHQ_SERVER_WRAPPER_LOG_DIR_PATH%"
)

if defined RHQ_SERVER_DEBUG echo RHQ_SERVER_WRAPPER_LOG_DIR_PATH: %RHQ_SERVER_WRAPPER_LOG_DIR_PATH%

rem ----------------------------------------------------------------------
rem Determine what to do and do it.
rem ----------------------------------------------------------------------

rem Determine if there should be debug VM options passed into it.
rem For some reason, this can't go inside another if statement.
if defined RHQ_SERVER_DEBUG set _DEBUG_OPTS=wrapper.debug=true

if /i "%1"=="console" (
   rem Determine what user the Windows Service will run as.
   if defined RHQ_SERVER_RUN_AS (
      if not defined RHQ_SERVER_PASSWORD (
         echo Exiting. RHQ_SERVER_PASSWORD is not set but is required because RHQ_SERVER_RUN_AS is set: %RHQ_SERVER_RUN_AS%.
         exit /B 1
      )
      set _WRAPPER_NTSERVICE_ACCOUNT="wrapper.ntservice.account=%RHQ_SERVER_RUN_AS%"
   )
   if defined RHQ_SERVER_RUN_AS_ME (
      if not defined RHQ_SERVER_PASSWORD (
         echo Exiting. RHQ_SERVER_PASSWORD is not set but is required because RHQ_SERVER_RUN_AS_ME is set.
         exit /B 1
      )
      set _WRAPPER_NTSERVICE_ACCOUNT="wrapper.ntservice.account=.\%USERNAME%"
   )

   rem START SERVER as console
   "%RHQ_SERVER_WRAPPER_EXE_FILE_PATH%" -c "%RHQ_SERVER_WRAPPER_CONF_FILE_PATH%" "set.RHQ_SERVER_HOME=%RHQ_SERVER_HOME%" "set.RHQ_SERVER_INSTANCE_NAME=%RHQ_SERVER_INSTANCE_NAME%" "set.RHQ_JAVA_EXE_FILE_PATH=%RHQ_JAVA_EXE_FILE_PATH%" "set.RHQ_SERVER_OS_PLATFORM=%RHQ_SERVER_OS_PLATFORM%" "set.RHQ_SERVER_WRAPPER_LOG_DIR_PATH=%RHQ_SERVER_WRAPPER_LOG_DIR_PATH%" !_WRAPPER_NTSERVICE_ACCOUNT! %_DEBUG_OPTS%
   goto done
)

if /i "%1"=="install" (
   rem Determine what user the Windows Service will run as.
   if defined RHQ_SERVER_RUN_AS (
      if not defined RHQ_SERVER_PASSWORD (
         echo Exiting. RHQ_SERVER_PASSWORD is not set but is required because RHQ_SERVER_RUN_AS is set: %RHQ_SERVER_RUN_AS%.
         exit /B 1
      )
      set _WRAPPER_NTSERVICE_ACCOUNT="wrapper.ntservice.account=%RHQ_SERVER_RUN_AS%"
   )
   if defined RHQ_SERVER_RUN_AS_ME (
      if not defined RHQ_SERVER_PASSWORD (
         echo Exiting. RHQ_SERVER_PASSWORD is not set but is required because RHQ_SERVER_RUN_AS_ME is set.
         exit /B 1
      )
      set _WRAPPER_NTSERVICE_ACCOUNT="wrapper.ntservice.account=.\%USERNAME%"
   )

   "%RHQ_SERVER_WRAPPER_EXE_FILE_PATH%" -i "%RHQ_SERVER_WRAPPER_CONF_FILE_PATH%" "set.RHQ_SERVER_HOME=%RHQ_SERVER_HOME%" "set.RHQ_SERVER_INSTANCE_NAME=%RHQ_SERVER_INSTANCE_NAME%" "set.RHQ_JAVA_EXE_FILE_PATH=%RHQ_JAVA_EXE_FILE_PATH%" "set.RHQ_SERVER_OS_PLATFORM=%RHQ_SERVER_OS_PLATFORM%" "set.RHQ_SERVER_WRAPPER_LOG_DIR_PATH=%RHQ_SERVER_WRAPPER_LOG_DIR_PATH%" !_WRAPPER_NTSERVICE_ACCOUNT! %_DEBUG_OPTS%
   goto done
)

if /i "%1"=="start" (
   rem START SERVER
   "%RHQ_SERVER_WRAPPER_EXE_FILE_PATH%" -t "%RHQ_SERVER_WRAPPER_CONF_FILE_PATH%"
   goto done
)

if /i "%1"=="stop" (
   "%RHQ_SERVER_WRAPPER_EXE_FILE_PATH%" -p "%RHQ_SERVER_WRAPPER_CONF_FILE_PATH%"
   goto done
)

if /i "%1"=="remove" (
   "%RHQ_SERVER_WRAPPER_EXE_FILE_PATH%" -r "%RHQ_SERVER_WRAPPER_CONF_FILE_PATH%"
   goto done
)

if /i "%1"=="status" (
   "%RHQ_SERVER_WRAPPER_EXE_FILE_PATH%" -q "%RHQ_SERVER_WRAPPER_CONF_FILE_PATH%"
   goto done
)

echo Usage: %0 { install ^| start ^| stop ^| remove ^| status ^| console }
goto :done


rem ----------------------------------------------------------------------
rem CALL subroutine that exits this script normally
rem ----------------------------------------------------------------------

:done
endlocal
