@echo off

rem ===========================================================================
rem RHQ Agent Windows Service Installer Script
rem
rem This file is used to install, start, stop and remove the RHQ Agent Windows
rem Service for the Windows platform.  The RHQ Agent is actually wrapped
rem by the Java Service Wrapper (JSW) and it is the JSW that is the actual
rem executable that is registered as the Windows Service.
rem
rem Note that if the agent is not yet fully configured, you cannot start
rem the RHQ Agent as a Windows Service (because the agent will need to
rem prompt you for some configuration settings - you therefore need to run
rem the agent in a console for this).  Once the agent is fully configured,
rem you can then install and run it as a Windows Service.
rem
rem This script is customizable by setting certain environment variables, which
rem are described in comments in rhq-agent-env.bat. The variables can also be
rem set via rhq-agent-env.bat, which is sourced by this script.
rem
rem Note that you cannot define custom Java VM parameters or agent
rem command line arguments to pass to the RHQ Agent VM.  If you wish to
rem pass in specific arguments, modify the rhq-agent-wrapper.conf file
rem or create a rhq-agent-wrapper.inc include file and place it in the same
rem directory as the rhq-agent-wrapper.conf file.
rem
rem If the embedded JRE is to be used but is not available, the fallback
rem JRE to be used will be determined by the JAVA_HOME environment variable.
rem ===========================================================================

setlocal enabledelayedexpansion

rem ----------------------------------------------------------------------
rem Let's load in the env script first. We assume our custom environment
rem script is located in the same place as this script. We do this first
rem because even though the "rhq-agent.bat _SETENV_ONLY" does it too,
rem it only prepares some variables but doesn't pass through some
rem that may (or may not) be defined in the env.bat.
rem ----------------------------------------------------------------------

set _ENV_SCRIPT_PATH=%~dp0
if exist "%_ENV_SCRIPT_PATH%\rhq-agent-env.bat" (
   call "%_ENV_SCRIPT_PATH%\rhq-agent-env.bat"
)

rem if debug variable is set, it is assumed to be on, unless its value is false
if "%RHQ_AGENT_DEBUG%" == "false" (
   set RHQ_AGENT_DEBUG=
)

rem ----------------------------------------------------------------------
rem Call the agent start script but have it only setup our environment.
rem Note that this script is assumed to be in the same directory as the
rem agent start script.
rem This script will set up the following environment variables for us:
rem    RHQ_AGENT_HOME
rem    RHQ_AGENT_BIN_DIR_PATH
rem    RHQ_JAVA_EXE_FILE_PATH
rem Some other RHQ_AGENT_ variables might also be set due to the
rem calling of the custom environment script earlier.
rem ----------------------------------------------------------------------

set _SCRIPT_DIR=%~dp0
call "%_SCRIPT_DIR%\rhq-agent.bat" _SETENV_ONLY

rem ----------------------------------------------------------------------
rem Define the name used for the name of the Windows Service.
rem If this is not defined, the name of the computer is used.
rem ----------------------------------------------------------------------

if "%RHQ_AGENT_INSTANCE_NAME%"=="" (
   set RHQ_AGENT_INSTANCE_NAME=rhqagent-%COMPUTERNAME%
)
if defined RHQ_AGENT_DEBUG echo RHQ_AGENT_INSTANCE_NAME: %RHQ_AGENT_INSTANCE_NAME%

rem ----------------------------------------------------------------------
rem Determine the wrapper directory.
rem ----------------------------------------------------------------------

set RHQ_AGENT_WRAPPER_DIR_PATH=%RHQ_AGENT_BIN_DIR_PATH%\wrapper
if defined RHQ_AGENT_DEBUG echo RHQ_AGENT_WRAPPER_DIR_PATH: %RHQ_AGENT_WRAPPER_DIR_PATH%

rem ----------------------------------------------------------------------
rem The Windows OS platform name is also the wrapper subdirectory name.
rem ----------------------------------------------------------------------

set RHQ_AGENT_OS_PLATFORM=windows-x86_32
if defined RHQ_AGENT_DEBUG echo RHQ_AGENT_OS_PLATFORM: %RHQ_AGENT_OS_PLATFORM%

rem ----------------------------------------------------------------------
rem Determine the wrapper executable that this script will run.
rem ----------------------------------------------------------------------

set RHQ_AGENT_WRAPPER_EXE_FILE_PATH=%RHQ_AGENT_WRAPPER_DIR_PATH%\%RHQ_AGENT_OS_PLATFORM%\wrapper.exe
if defined RHQ_AGENT_DEBUG echo RHQ_AGENT_WRAPPER_EXE_FILE_PATH: %RHQ_AGENT_WRAPPER_EXE_FILE_PATH%

rem ----------------------------------------------------------------------
rem Determine the agent wrapper configuration file.
rem ----------------------------------------------------------------------

set RHQ_AGENT_WRAPPER_CONF_FILE_PATH=%RHQ_AGENT_WRAPPER_DIR_PATH%\rhq-agent-wrapper.conf
if defined RHQ_AGENT_DEBUG echo RHQ_AGENT_WRAPPER_CONF_FILE_PATH: %RHQ_AGENT_WRAPPER_CONF_FILE_PATH%

rem ----------------------------------------------------------------------
rem Create and configure the wrapper log directory.
rem ----------------------------------------------------------------------

if "%RHQ_AGENT_WRAPPER_LOG_DIR_PATH%"=="" (
   if not exist "%RHQ_AGENT_HOME%\logs" (
      mkdir "%RHQ_AGENT_HOME%\logs"
   )
   set RHQ_AGENT_WRAPPER_LOG_DIR_PATH=%RHQ_AGENT_HOME%\logs
)
if defined RHQ_AGENT_DEBUG echo RHQ_AGENT_WRAPPER_LOG_DIR_PATH: %RHQ_AGENT_WRAPPER_LOG_DIR_PATH%

rem ----------------------------------------------------------------------
rem Determine what to do and do it.
rem ----------------------------------------------------------------------

rem Determine if there should be debug VM options passed into it
rem For some reason, this can't go inside another if statement
if defined RHQ_AGENT_DEBUG set _DEBUG_OPTS=wrapper.debug=true wrapper.java.additional.1=-Dlog4j.configuration=log4j-debug.xml wrapper.java.additional.4=-Di18nlog.dump-stack-traces=true wrapper.java.additional.5=-Dsigar.nativeLogging=true

if /i "%1"=="install" (
   rem Determine what user the Windows Service will run as
   rem If this is an rhqctl install then ensure password is set as needed because we can't perform interactive prompt
   rem Note - using the RHQ_CONTROL_JAVA_OPTS env var as a flag indicating we're being called from rhqctl
   if defined RHQ_CONTROL_JAVA_OPTS (
      if defined RHQ_AGENT_RUN_AS (
         if not defined RHQ_AGENT_PASSWORD (
            echo Exiting. RHQ_AGENT_PASSWORD is not set but is required because RHQ_AGENT_RUN_AS is set: %RHQ_AGENT_RUN_AS%.
            exit /B 1
         )
         set _WRAPPER_NTSERVICE_ACCOUNT="wrapper.ntservice.account=%RHQ_AGENT_RUN_AS%"
      )
      if defined RHQ_AGENT_RUN_AS_ME (
         if not defined RHQ_AGENT_PASSWORD (
            echo Exiting. RHQ_AGENT_PASSWORD is not set but is required because RHQ_AGENT_RUN_AS_ME is set.
            exit /B 1
         )
         set _WRAPPER_NTSERVICE_ACCOUNT="wrapper.ntservice.account=.\%USERNAME%"
      )
   ) else (
      if defined RHQ_AGENT_RUN_AS set _WRAPPER_NTSERVICE_ACCOUNT="wrapper.ntservice.account=%RHQ_AGENT_RUN_AS%"
      if defined RHQ_AGENT_RUN_AS_ME set _WRAPPER_NTSERVICE_ACCOUNT="wrapper.ntservice.account=.\%USERNAME%"
      if not defined RHQ_AGENT_PASSWORD_PROMPT set RHQ_AGENT_PASSWORD_PROMPT=true
   )

   rem note that we set RHQ_AGENT_JAVA_EXE_FILE_PATH on purpose.  The services use this legacy env var, as opposed to
   rem RHQ_JAVA_EXE_FILE_PATH, to be backward compatible with existing services that get auto-upgraded.
   "%RHQ_AGENT_WRAPPER_EXE_FILE_PATH%" -i "%RHQ_AGENT_WRAPPER_CONF_FILE_PATH%" "set.RHQ_AGENT_HOME=%RHQ_AGENT_HOME%" "set.RHQ_AGENT_INSTANCE_NAME=%RHQ_AGENT_INSTANCE_NAME%" "set.RHQ_AGENT_JAVA_EXE_FILE_PATH=%RHQ_JAVA_EXE_FILE_PATH%" "set.RHQ_AGENT_OS_PLATFORM=%RHQ_AGENT_OS_PLATFORM%" "set.RHQ_AGENT_WRAPPER_LOG_DIR_PATH=%RHQ_AGENT_WRAPPER_LOG_DIR_PATH%" !_WRAPPER_NTSERVICE_ACCOUNT! %_DEBUG_OPTS%
   if ERRORLEVEL 1 goto error
   goto done
)

if /i "%1"=="start" (
   "%RHQ_AGENT_WRAPPER_EXE_FILE_PATH%" -t "%RHQ_AGENT_WRAPPER_CONF_FILE_PATH%"
   if ERRORLEVEL 1 goto error
   goto done
)

if /i "%1"=="stop" (
   "%RHQ_AGENT_WRAPPER_EXE_FILE_PATH%" -p "%RHQ_AGENT_WRAPPER_CONF_FILE_PATH%"
   if ERRORLEVEL 1 goto error
   goto done
)

if /i "%1"=="remove" (
   "%RHQ_AGENT_WRAPPER_EXE_FILE_PATH%" -r "%RHQ_AGENT_WRAPPER_CONF_FILE_PATH%"
   if ERRORLEVEL 1 goto error
   goto done
)

if /i "%1"=="restart" (
   "%RHQ_AGENT_WRAPPER_EXE_FILE_PATH%" -p "%RHQ_AGENT_WRAPPER_CONF_FILE_PATH%"
   "%RHQ_AGENT_WRAPPER_EXE_FILE_PATH%" -t "%RHQ_AGENT_WRAPPER_CONF_FILE_PATH%"
   if ERRORLEVEL 1 goto error
   goto done
)

if /i "%1"=="status" (
   "%RHQ_AGENT_WRAPPER_EXE_FILE_PATH%" -q "%RHQ_AGENT_WRAPPER_CONF_FILE_PATH%"
   if ERRORLEVEL 1 goto error
   goto done
)

echo Usage: %0 { install ^| start ^| stop ^| remove ^| restart ^| status }
goto done

rem Java Service Wrapper returns exit codes up to 39 based on how its installed/running
:error
if ERRORLEVEL 39 exit /B 39
if ERRORLEVEL 38 exit /B 38
if ERRORLEVEL 37 exit /B 37
if ERRORLEVEL 36 exit /B 36
if ERRORLEVEL 35 exit /B 35
if ERRORLEVEL 34 exit /B 34
if ERRORLEVEL 33 exit /B 33
if ERRORLEVEL 32 exit /B 32
if ERRORLEVEL 31 exit /B 31
if ERRORLEVEL 30 exit /B 30
if ERRORLEVEL 29 exit /B 29
if ERRORLEVEL 28 exit /B 28
if ERRORLEVEL 27 exit /B 27
if ERRORLEVEL 26 exit /B 26
if ERRORLEVEL 25 exit /B 25
if ERRORLEVEL 24 exit /B 24
if ERRORLEVEL 23 exit /B 23
if ERRORLEVEL 22 exit /B 22
if ERRORLEVEL 21 exit /B 21
if ERRORLEVEL 20 exit /B 20
if ERRORLEVEL 19 exit /B 19
if ERRORLEVEL 18 exit /B 18
if ERRORLEVEL 17 exit /B 17
if ERRORLEVEL 16 exit /B 16
if ERRORLEVEL 15 exit /B 15
if ERRORLEVEL 14 exit /B 14
if ERRORLEVEL 13 exit /B 13
if ERRORLEVEL 12 exit /B 12
if ERRORLEVEL 11 exit /B 11
if ERRORLEVEL 10 exit /B 10
if ERRORLEVEL 9 exit /B 9
if ERRORLEVEL 8 exit /B 8
if ERRORLEVEL 7 exit /B 7
if ERRORLEVEL 6 exit /B 6
if ERRORLEVEL 5 exit /B 5
if ERRORLEVEL 4 exit /B 4
if ERRORLEVEL 3 exit /B 3
if ERRORLEVEL 2 exit /B 2
if ERRORLEVEL 1 exit /B 1

:done
endlocal
