@echo off

rem ===========================================================================
rem RHQ Agent Windows Startup Script
rem
rem This file is used to execute the RHQ Agent on a Windows platform.
rem Run this script with the --help option for the runtime options.
rem
rem Note that this script can also be used to simply set environment variables
rem that define the agent's environment but it will not actually run the agent.
rem Pass in _SETENV_ONLY as the first argument to this script if you want this
rem behavior.
rem
rem This script is customizable by setting certain environment variables, which
rem are described in comments in rhq-agent-env.bat. The variables can also be
rem set via rhq-agent-env.bat, which is sourced by this script.
rem
rem If the embedded JRE is to be used but is not available, the fallback
rem JRE to be used will be determined by the JAVA_HOME environment variable.
rem ===========================================================================

setlocal

if "%1"=="_SETENV_ONLY" (
   set _SETENV_ONLY=true
) else (
   title RHQ Agent
)

rem ----------------------------------------------------------------------
rem Change directory so the current directory is the Agent home.
rem Here we assume this script is a child directory of the Agent home
rem We also assume our custom environment script is located in the same
rem place as this script.
rem ----------------------------------------------------------------------

set RHQ_AGENT_BIN_DIR_PATH=%~dp0

if exist "%RHQ_AGENT_BIN_DIR_PATH%\rhq-agent-env.bat" (
   if defined RHQ_AGENT_DEBUG echo Loading environment script: %RHQ_AGENT_BIN_DIR_PATH%\rhq-agent-env.bat
   call "%RHQ_AGENT_BIN_DIR_PATH%\rhq-agent-env.bat" %*
) else (
   if defined RHQ_AGENT_DEBUG echo No environment script found at: %RHQ_AGENT_BIN_DIR_PATH%\rhq-agent-env.bat
)

if not defined RHQ_AGENT_HOME (
   cd "%RHQ_AGENT_BIN_DIR_PATH%\.."
) else (
   cd "%RHQ_AGENT_HOME%" || (
      echo Cannot go to the RHQ_AGENT_HOME directory: %RHQ_AGENT_HOME%
      exit /B 1
      )
)

set RHQ_AGENT_HOME=%CD%

if defined RHQ_AGENT_DEBUG echo RHQ_AGENT_HOME: %RHQ_AGENT_HOME%

rem ----------------------------------------------------------------------
rem Find the Java executable and verify we have a VM available
rem ----------------------------------------------------------------------

if not defined RHQ_AGENT_JAVA_EXE_FILE_PATH (
   if not defined RHQ_AGENT_JAVA_HOME call :prepare_embedded_jre
)

if not defined RHQ_AGENT_JAVA_EXE_FILE_PATH (
   set RHQ_AGENT_JAVA_EXE_FILE_PATH=%RHQ_AGENT_JAVA_HOME%\bin\java.exe
)

if defined RHQ_AGENT_DEBUG echo RHQ_AGENT_JAVA_HOME: %RHQ_AGENT_JAVA_HOME%
if defined RHQ_AGENT_DEBUG echo RHQ_AGENT_JAVA_EXE_FILE_PATH: %RHQ_AGENT_JAVA_EXE_FILE_PATH%

if not exist "%RHQ_AGENT_JAVA_EXE_FILE_PATH%" (
   echo There is no JVM available.
   echo Please set RHQ_AGENT_JAVA_HOME or RHQ_AGENT_JAVA_EXE_FILE_PATH appropriately.
   exit /B 1
)

rem ----------------------------------------------------------------------
rem Prepare the classpath
rem ----------------------------------------------------------------------

set CLASSPATH=
call :append_classpath "%RHQ_AGENT_HOME%\conf"
for /R "%RHQ_AGENT_HOME%\lib" %%G in ("*.jar") do (
   call :append_classpath "%%G"
   if defined RHQ_AGENT_DEBUG echo CLASSPATH entry: %%G
)

rem ----------------------------------------------------------------------
rem Prepare the VM command line options to be passed in
rem ----------------------------------------------------------------------

if not defined RHQ_AGENT_JAVA_OPTS (
   set RHQ_AGENT_JAVA_OPTS=-Xms64m -Xmx128m -Djava.net.preferIPv4Stack=true
)

set RHQ_AGENT_JAVA_OPTS="-Djava.endorsed.dirs=%RHQ_AGENT_HOME%\lib\endorsed" %RHQ_AGENT_JAVA_OPTS%
 
rem The RHQ Agent has a JNI library that it needs to find in order to
rem do things like execute PIQL queries and access low-level operating
rem system data. Here we add the java.library.path system property
rem to point to the JNI libraries.  If you deploy a custom plugin
rem that also requires JNI libraries, you must add to the library path
rem here, you must not remove the RHQ Agent library path that it needs.

set _JNI_PATH=%RHQ_AGENT_HOME%\lib

set RHQ_AGENT_JAVA_OPTS="-Djava.library.path=%_JNI_PATH%" %RHQ_AGENT_JAVA_OPTS%

if defined RHQ_AGENT_DEBUG echo RHQ_AGENT_JAVA_OPTS: %RHQ_AGENT_JAVA_OPTS%
if defined RHQ_AGENT_DEBUG echo RHQ_AGENT_ADDITIONAL_JAVA_OPTS: %RHQ_AGENT_ADDITIONAL_JAVA_OPTS%

rem ----------------------------------------------------------------------
rem Prepare the command line arguments passed to the RHQ Agent
rem ----------------------------------------------------------------------

if not defined RHQ_AGENT_CMDLINE_OPTS (
   set RHQ_AGENT_CMDLINE_OPTS=%*
)

if defined RHQ_AGENT_DEBUG echo RHQ_AGENT_CMDLINE_OPTS: %RHQ_AGENT_CMDLINE_OPTS%

rem ----------------------------------------------------------------------
rem Execute the VM which starts the agent
rem ----------------------------------------------------------------------

if defined RHQ_AGENT_DEBUG (
   set _LOG_CONFIG=-Dlog4j.configuration=log4j-debug.xml -Dsigar.nativeLogging=true -Di18nlog.dump-stack-traces=true
) else (
   set _LOG_CONFIG=-Dlog4j.configuration=log4j.xml
)

set CMD="%RHQ_AGENT_JAVA_EXE_FILE_PATH%" %RHQ_AGENT_JAVA_OPTS% %RHQ_AGENT_ADDITIONAL_JAVA_OPTS% %_LOG_CONFIG% -cp "%CLASSPATH%" org.rhq.enterprise.agent.AgentMain %RHQ_AGENT_CMDLINE_OPTS%

if not defined _SETENV_ONLY (
   rem log4j 1.2.8 does not create the directory for us (later versions do)
   if not exist "%RHQ_AGENT_HOME%\logs" (
      mkdir "%RHQ_AGENT_HOME%\logs"
   )
   if defined RHQ_AGENT_DEBUG (
      echo Executing the agent with this command line:
      echo %CMD%
   )
   cmd.exe /S /C "%CMD%"
)

goto :done

rem ----------------------------------------------------------------------
rem CALL subroutine that appends the first argument to CLASSPATH
rem ----------------------------------------------------------------------

:append_classpath
set _entry=%1
if not defined CLASSPATH (
   set CLASSPATH=%_entry:"=%
) else (
   set CLASSPATH=%CLASSPATH%;%_entry:"=%
)
goto :eof

rem ----------------------------------------------------------------------
rem CALL subroutine that prepares to use the embedded JRE
rem ----------------------------------------------------------------------

:prepare_embedded_jre
set RHQ_AGENT_JAVA_HOME=%RHQ_AGENT_HOME%\jre
if defined RHQ_AGENT_DEBUG echo Using the embedded JRE
if not exist "%RHQ_AGENT_JAVA_HOME%" (
   if defined RHQ_AGENT_DEBUG echo No embedded JRE found - will try to use JAVA_HOME: %JAVA_HOME%
   set RHQ_AGENT_JAVA_HOME=%JAVA_HOME%
)
goto :eof

rem ----------------------------------------------------------------------
rem CALL subroutine that exits this script normally
rem ----------------------------------------------------------------------

:done
if defined RHQ_AGENT_DEBUG echo %0 done.
if defined _SETENV_ONLY (
endlocal & SET "RHQ_AGENT_HOME=%RHQ_AGENT_HOME%" & SET "RHQ_AGENT_JAVA_EXE_FILE_PATH=%RHQ_AGENT_JAVA_EXE_FILE_PATH%" & SET "RHQ_AGENT_JAVA_OPTS=%RHQ_AGENT_JAVA_OPTS%" & SET "RHQ_AGENT_ADDITIONAL_JAVA_OPTS=%RHQ_AGENT_ADDITIONAL_JAVA_OPTS%" & SET "RHQ_AGENT_BIN_DIR_PATH=%RHQ_AGENT_BIN_DIR_PATH%"
) else (
endlocal
)

exit /B 0
