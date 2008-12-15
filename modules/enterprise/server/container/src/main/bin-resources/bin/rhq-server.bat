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
rem This script is customizable by setting the following environment variables:
rem
rem    RHQ_SERVER_DEBUG - If this is defined, the script will emit debug
rem                       messages.
rem                       If not set or set to "false", debug is turned off.
rem
rem    RHQ_SERVER_HOME - Defines where the server's home install directory is.
rem                      If not defined, it will be assumed to be the parent
rem                      directory of the directory where this script lives.
rem
rem    RHQ_SERVER_JAVA_HOME - The location of the JRE that the server will
rem                           use. This will be ignored if
rem                           RHQ_SERVER_JAVA_EXE_FILE_PATH is set.
rem                           If this and RHQ_SERVER_JAVA_EXE_FILE_PATH are
rem                           not set, the server's embedded JRE will be used.
rem
rem    RHQ_SERVER_JAVA_EXE_FILE_PATH - Defines the full path to the Java
rem                                    executable to use. If this is set,
rem                                    RHQ_SERVER_JAVA_HOME is ignored.
rem                                    If this is not set, then
rem                                    %RHQ_SERVER_JAVA_HOME%\bin\java.exe
rem                                    is used. If this and
rem                                    RHQ_SERVER_JAVA_HOME are not set, the
rem                                    server's embedded JRE will be used.
rem
rem    RHQ_SERVER_INSTANCE_NAME - The name of the Windows Service; it must
rem                               conform to the Windows Service naming
rem                               conventions. By default, this is the
rem                               name "rhqserver-%COMPUTERNAME%"
rem
rem    RHQ_SERVER_WRAPPER_LOG_DIR_PATH - The full path to the location where
rem                                      the wrapper log file will go.
rem
rem    RHQ_SERVER_RUN_AS - if defined, then when the Windows Service is
rem                        installed, the value is the domain\username of the
rem                        user that the Windows Service will run as 
rem                       
rem    RHQ_SERVER_RUN_AS_ME - if defined, then when the Windows Service is
rem                           installed, the domain\username of the
rem                           user that the Windows Service will run as will
rem                           be the current user (.\%USERNAME%).  This takes
rem                           precedence over RHQ_SERVER_RUN_AS.
rem                       
rem Note that you cannot define custom Java VM parameters or server
rem command line arguments to pass to the RHQ Server VM.  If you wish to
rem pass in specific arguments, modify the rhq-server-wrapper.conf file.
rem
rem If the embedded JRE is to be used but is not available, the fallback
rem JRE to be used will be determined by the JAVA_HOME environment variable.
rem
rem This script does not use the built-in JBossAS run.bat. 
rem ===========================================================================

setlocal

rem if debug variable is set, it is assumed to be on, unless its value is false
if "%RHQ_SERVER_DEBUG%" == "false" (
   set RHQ_SERVER_DEBUG=
)

rem ----------------------------------------------------------------------
rem Change directory so the current directory is the server home.
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
rem Find the Java executable and verify we have a VM available
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

rem ----------------------------------------------------------------------
rem Define the name used for the name of the Windows Service.
rem If this is not defined, the name of the computer is used.
rem ----------------------------------------------------------------------

if "%RHQ_SERVER_INSTANCE_NAME%"=="" (
   set RHQ_SERVER_INSTANCE_NAME=rhqserver-%COMPUTERNAME%
)
if defined RHQ_SERVER_DEBUG echo RHQ_SERVER_INSTANCE_NAME: %RHQ_SERVER_INSTANCE_NAME%

rem ----------------------------------------------------------------------
rem Determine the wrapper directory.
rem ----------------------------------------------------------------------

set RHQ_SERVER_WRAPPER_DIR_PATH=%RHQ_SERVER_BIN_DIR_PATH%\wrapper
if defined RHQ_SERVER_DEBUG echo RHQ_SERVER_WRAPPER_DIR_PATH: %RHQ_SERVER_WRAPPER_DIR_PATH%

rem ----------------------------------------------------------------------
rem The Windows OS platform name is also the wrapper subdirectory name.
rem ----------------------------------------------------------------------

set RHQ_SERVER_OS_PLATFORM=windows-x86_32
if defined RHQ_SERVER_DEBUG echo RHQ_SERVER_OS_PLATFORM: %RHQ_SERVER_OS_PLATFORM%

rem ----------------------------------------------------------------------
rem Determine the wrapper executable that this script will run.
rem ----------------------------------------------------------------------

set RHQ_SERVER_WRAPPER_EXE_FILE_PATH=%RHQ_SERVER_WRAPPER_DIR_PATH%\%RHQ_SERVER_OS_PLATFORM%\wrapper.exe
if defined RHQ_SERVER_DEBUG echo RHQ_SERVER_WRAPPER_EXE_FILE_PATH: %RHQ_SERVER_WRAPPER_EXE_FILE_PATH%

rem ----------------------------------------------------------------------
rem Determine the server wrapper configuration file.
rem ----------------------------------------------------------------------

set RHQ_SERVER_WRAPPER_CONF_FILE_PATH=%RHQ_SERVER_WRAPPER_DIR_PATH%\rhq-server-wrapper.conf
if defined RHQ_SERVER_DEBUG echo RHQ_SERVER_WRAPPER_CONF_FILE_PATH: %RHQ_SERVER_WRAPPER_CONF_FILE_PATH%

rem ----------------------------------------------------------------------
rem Create and configure the wrapper log directory.
rem ----------------------------------------------------------------------

if "%RHQ_SERVER_WRAPPER_LOG_DIR_PATH%" == "" (
   if not exist "%RHQ_SERVER_HOME%\logs" (
      mkdir "%RHQ_SERVER_HOME%\logs"
   )
   set RHQ_SERVER_WRAPPER_LOG_DIR_PATH=%RHQ_SERVER_HOME%\logs
)
if defined RHQ_SERVER_DEBUG echo RHQ_SERVER_WRAPPER_LOG_DIR_PATH: %RHQ_SERVER_WRAPPER_LOG_DIR_PATH%

rem ----------------------------------------------------------------------
rem Determine what to do and do it.
rem ----------------------------------------------------------------------

rem Determine if there should be debug VM options passed into it
rem For some reason, this can't go inside another if statement
if defined RHQ_SERVER_DEBUG set _DEBUG_OPTS=wrapper.debug=true

rem Determine what user the Windows Service will run as
if defined RHQ_SERVER_RUN_AS set _WRAPPER_NTSERVICE_ACCOUNT="wrapper.ntservice.account=%RHQ_SERVER_RUN_AS%"
if defined RHQ_SERVER_RUN_AS_ME set _WRAPPER_NTSERVICE_ACCOUNT="wrapper.ntservice.account=.\%USERNAME%"

if /i "%1"=="console" (
   "%RHQ_SERVER_WRAPPER_EXE_FILE_PATH%" -c "%RHQ_SERVER_WRAPPER_CONF_FILE_PATH%" "set.RHQ_SERVER_HOME=%RHQ_SERVER_HOME%" "set.RHQ_SERVER_INSTANCE_NAME=%RHQ_SERVER_INSTANCE_NAME%" "set.RHQ_SERVER_JAVA_EXE_FILE_PATH=%RHQ_SERVER_JAVA_EXE_FILE_PATH%" "set.RHQ_SERVER_OS_PLATFORM=%RHQ_SERVER_OS_PLATFORM%" "set.RHQ_SERVER_WRAPPER_LOG_DIR_PATH=%RHQ_SERVER_WRAPPER_LOG_DIR_PATH%" %_WRAPPER_NTSERVICE_ACCOUNT% %_DEBUG_OPTS%
   goto done
)

if /i "%1"=="install" (
   "%RHQ_SERVER_WRAPPER_EXE_FILE_PATH%" -i "%RHQ_SERVER_WRAPPER_CONF_FILE_PATH%" "set.RHQ_SERVER_HOME=%RHQ_SERVER_HOME%" "set.RHQ_SERVER_INSTANCE_NAME=%RHQ_SERVER_INSTANCE_NAME%" "set.RHQ_SERVER_JAVA_EXE_FILE_PATH=%RHQ_SERVER_JAVA_EXE_FILE_PATH%" "set.RHQ_SERVER_OS_PLATFORM=%RHQ_SERVER_OS_PLATFORM%" "set.RHQ_SERVER_WRAPPER_LOG_DIR_PATH=%RHQ_SERVER_WRAPPER_LOG_DIR_PATH%" %_WRAPPER_NTSERVICE_ACCOUNT% %_DEBUG_OPTS%
   goto done
)

if /i "%1"=="start" (
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
