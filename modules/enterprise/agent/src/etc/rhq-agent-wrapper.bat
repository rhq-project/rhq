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
rem This script is customizable by setting the following environment variables:
rem
rem    RHQ_AGENT_DEBUG - If this is defined (with any value), the script
rem                      will emit debug messages. It will also enable debug
rem                      messages to be emitted from the agent itself.
rem
rem    RHQ_AGENT_HOME - Defines where the agent's home install directory is.
rem                     If not defined, it will be assumed to be the parent
rem                     directory of the directory where this script lives.
rem
rem    RHQ_AGENT_JAVA_HOME - The location of the JRE that the agent will
rem                          use. This will be ignored if
rem                          RHQ_AGENT_JAVA_EXE_FILE_PATH is set.
rem                          If this and RHQ_AGENT_JAVA_EXE_FILE_PATH are
rem                          not set, the agent's embedded JRE will be used.
rem
rem    RHQ_AGENT_JAVA_EXE_FILE_PATH - Defines the full path to the Java
rem                                   executable to use. If this is set,
rem                                   RHQ_AGENT_JAVA_HOME is ignored.
rem                                   If this is not set, then
rem                                   %RHQ_AGENT_JAVA_HOME%\bin\java.exe
rem                                   is used. If this and
rem                                   RHQ_AGENT_JAVA_HOME are not set, the
rem                                   agent's embedded JRE will be used.
rem
rem    RHQ_AGENT_INSTANCE_NAME - The name of the Windows Service; it must
rem                              conform to the Windows Service naming
rem                              conventions. By default, this is the
rem                              name "rhqagent-%COMPUTERNAME%"
rem
rem    RHQ_AGENT_WRAPPER_LOG_DIR_PATH - The full path to the location where
rem                                     the wrapper log file will go.
rem
rem    RHQ_AGENT_RUN_AS - if defined, then when the Windows Service is
rem                       installed, the value is the domain\username of the
rem                       user that the Windows Service will run as 
rem                       
rem    RHQ_AGENT_RUN_AS_ME - if defined, then when the Windows Service is
rem                          installed, the domain\username of the
rem                          user that the Windows Service will run as will
rem                          be the current user (.\%USERNAME%).  This takes
rem                          precedence over RHQ_AGENT_RUN_AS.
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

setlocal

rem ----------------------------------------------------------------------
rem Call the agent start script but have it only setup our environment.
rem Note that this script is assumed to be in the same directory as the
rem agent start script.
rem This script will set up the following environment variables for us:
rem    RHQ_AGENT_HOME
rem    RHQ_AGENT_BIN_DIR_PATH
rem    RHQ_AGENT_JAVA_EXE_FILE_PATH
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

rem ----------------------------------------------------------------------
rem Determine the wrapper directory.
rem ----------------------------------------------------------------------

set RHQ_AGENT_WRAPPER_DIR_PATH=%RHQ_AGENT_BIN_DIR_PATH%\wrapper

rem ----------------------------------------------------------------------
rem The Windows OS platform name is also the wrapper subdirectory name.
rem ----------------------------------------------------------------------

set RHQ_AGENT_OS_PLATFORM=windows-x86_32

rem ----------------------------------------------------------------------
rem Determine the wrapper executable that this script will run.
rem ----------------------------------------------------------------------

set RHQ_AGENT_WRAPPER_EXE_FILE_PATH=%RHQ_AGENT_WRAPPER_DIR_PATH%\%RHQ_AGENT_OS_PLATFORM%\wrapper.exe

rem ----------------------------------------------------------------------
rem Determine the agent wrapper configuration file.
rem ----------------------------------------------------------------------

set RHQ_AGENT_WRAPPER_CONF_FILE_PATH=%RHQ_AGENT_WRAPPER_DIR_PATH%\rhq-agent-wrapper.conf

rem ----------------------------------------------------------------------
rem Create and configure the wrapper log directory.
rem ----------------------------------------------------------------------

if "%RHQ_AGENT_WRAPPER_LOG_DIR_PATH%"=="" (
   if not exist "%RHQ_AGENT_HOME%\logs" (
      mkdir "%RHQ_AGENT_HOME%\logs"
   )
   set RHQ_AGENT_WRAPPER_LOG_DIR_PATH=%RHQ_AGENT_HOME%\logs
)

rem ----------------------------------------------------------------------
rem Determine what to do and do it.
rem ----------------------------------------------------------------------

rem Determine if there should be debug VM options passed into it
rem For some reason, this can't go inside another if statement
if defined RHQ_AGENT_DEBUG set _DEBUG_OPTS=wrapper.debug=true wrapper.java.additional.1=-Dlog4j.configuration=log4j-debug.xml wrapper.java.additional.4=-Di18nlog.dump-stack-traces=true wrapper.java.additional.5=-Dsigar.nativeLogging=true

rem Determine what user the Windows Service will run as
if defined RHQ_AGENT_RUN_AS set _WRAPPER_NTSERVICE_ACCOUNT="wrapper.ntservice.account=%RHQ_AGENT_RUN_AS%"
if defined RHQ_AGENT_RUN_AS_ME set _WRAPPER_NTSERVICE_ACCOUNT="wrapper.ntservice.account=.\%USERNAME%"

if /i "%1"=="install" (

   "%RHQ_AGENT_WRAPPER_EXE_FILE_PATH%" -i "%RHQ_AGENT_WRAPPER_CONF_FILE_PATH%" "set.RHQ_AGENT_HOME=%RHQ_AGENT_HOME%" "set.RHQ_AGENT_INSTANCE_NAME=%RHQ_AGENT_INSTANCE_NAME%" "set.RHQ_AGENT_JAVA_EXE_FILE_PATH=%RHQ_AGENT_JAVA_EXE_FILE_PATH%" "set.RHQ_AGENT_OS_PLATFORM=%RHQ_AGENT_OS_PLATFORM%" "set.RHQ_AGENT_WRAPPER_LOG_DIR_PATH=%RHQ_AGENT_WRAPPER_LOG_DIR_PATH%" %_WRAPPER_NTSERVICE_ACCOUNT% %_DEBUG_OPTS%
   goto done
)

if /i "%1"=="start" (
   "%RHQ_AGENT_WRAPPER_EXE_FILE_PATH%" -t "%RHQ_AGENT_WRAPPER_CONF_FILE_PATH%"
   goto done
)

if /i "%1"=="stop" (
   "%RHQ_AGENT_WRAPPER_EXE_FILE_PATH%" -p "%RHQ_AGENT_WRAPPER_CONF_FILE_PATH%"
   goto done
)

if /i "%1"=="remove" (
   "%RHQ_AGENT_WRAPPER_EXE_FILE_PATH%" -r "%RHQ_AGENT_WRAPPER_CONF_FILE_PATH%"
   goto done
)

if /i "%1"=="status" (
   "%RHQ_AGENT_WRAPPER_EXE_FILE_PATH%" -q "%RHQ_AGENT_WRAPPER_CONF_FILE_PATH%"
   goto done
)

echo Usage: %0 { install ^| start ^| stop ^| remove ^| status }

:done
endlocal
