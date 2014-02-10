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

setlocal enabledelayedexpansion

if "%1"=="_SETENV_ONLY" (
   set _SETENV_ONLY=true
) else (
   title RHQ Agent
)

rem if debug variable is set, it is assumed to be on, unless its value is false
if "%RHQ_AGENT_DEBUG%" == "false" (
   set RHQ_AGENT_DEBUG=
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

rem if debug variable is set, it is assumed to be on, unless its value is false
if "%RHQ_AGENT_DEBUG%" == "false" (
   set RHQ_AGENT_DEBUG=
)

rem if sigar debug variable is set, it is assumed to be on, unless its value is false
if "%RHQ_AGENT_SIGAR_DEBUG%" == "false" (
   set RHQ_AGENT_SIGAR_DEBUG=
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
rem Note that RHQ_AGENT_JAVA_* props are still handled for back compat
rem ----------------------------------------------------------------------

if not defined RHQ_JAVA_EXE_FILE_PATH (
   if defined RHQ_AGENT_JAVA_EXE_FILE_PATH (
      set RHQ_JAVA_EXE_FILE_PATH=!RHQ_AGENT_JAVA_EXE_FILE_PATH!
   )
)
if not defined RHQ_JAVA_HOME (
   if defined RHQ_AGENT_JAVA_HOME (
      set RHQ_JAVA_HOME=!RHQ_AGENT_JAVA_HOME!
   )
)

if not defined RHQ_JAVA_EXE_FILE_PATH (
   if not defined RHQ_JAVA_HOME (
      if defined RHQ_AGENT_DEBUG echo No RHQ JAVA property set, defaulting to JAVA_HOME: !JAVA_HOME!
      set RHQ_JAVA_HOME=!JAVA_HOME!
   )
)
if not defined RHQ_JAVA_EXE_FILE_PATH (
   set RHQ_JAVA_EXE_FILE_PATH=!RHQ_JAVA_HOME!\bin\java.exe
)

if defined RHQ_AGENT_DEBUG echo RHQ_JAVA_HOME: %RHQ_JAVA_HOME%
if defined RHQ_AGENT_DEBUG echo RHQ_JAVA_EXE_FILE_PATH: %RHQ_JAVA_EXE_FILE_PATH%

if not exist "%RHQ_JAVA_EXE_FILE_PATH%" (
   echo There is no JVM available.
   echo Please set RHQ_JAVA_HOME or RHQ_JAVA_EXE_FILE_PATH appropriately.
   exit /B 1
)

rem ----------------------------------------------------------------------
rem Prepare the classpath
rem ----------------------------------------------------------------------

set CLASSPATH=
call :append_classpath "%RHQ_AGENT_HOME%\conf"
for /R "%RHQ_AGENT_HOME%\lib" %%G in ("*.jar") do (
   call :append_classpath "%%G"
)
for %%G in ("!RHQ_JAVA_HOME!\lib\tools.jar" "!RHQ_JAVA_HOME!\..\lib\tools.jar") do (
   if exist "%%G" (
      call :append_classpath %%G
      goto end_classpath
   )
)
:end_classpath
if defined RHQ_AGENT_DEBUG echo CLASSPATH: %CLASSPATH%

rem ----------------------------------------------------------------------
rem Prepare the VM command line options to be passed in
rem ----------------------------------------------------------------------

if not defined RHQ_AGENT_JAVA_OPTS (
   set RHQ_AGENT_JAVA_OPTS=-Xms64m -Xmx128m -Djava.net.preferIPv4Stack=true -Drhq.preferences.file="%RHQ_AGENT_HOME%\conf\agent-prefs.properties"
)
if defined RHQ_AGENT_DEBUG echo RHQ_AGENT_JAVA_OPTS: %RHQ_AGENT_JAVA_OPTS%

rem ----------------------------------------------------------------------
rem Ensure the agent uses our custom JavaPreferences implementation
rem ----------------------------------------------------------------------
set _JAVA_PREFERENCES_FACTORY_OPT=-Djava.util.prefs.PreferencesFactory=org.rhq.core.util.preferences.FilePreferencesFactory

if "%RHQ_AGENT_JAVA_ENDORSED_DIRS%" == "none" (
   if defined RHQ_AGENT_DEBUG echo Not explicitly setting java.endorsed.dirs
   goto :skip_java_endorsed_dirs
)
if not defined RHQ_AGENT_JAVA_ENDORSED_DIRS set RHQ_AGENT_JAVA_ENDORSED_DIRS=%RHQ_AGENT_HOME%\lib\endorsed
if defined RHQ_AGENT_DEBUG echo RHQ_AGENT_JAVA_ENDORSED_DIRS: %RHQ_AGENT_JAVA_ENDORSED_DIRS%
set _JAVA_ENDORSED_DIRS_OPT=-Djava.endorsed.dirs="%RHQ_AGENT_JAVA_ENDORSED_DIRS%"
:skip_java_endorsed_dirs

if "%RHQ_AGENT_JAVA_LIBRARY_PATH%" == "none" (
   if defined RHQ_AGENT_DEBUG echo Not explicitly setting java.library.path
   goto :skip_java_library_path
)
if not defined RHQ_AGENT_JAVA_LIBRARY_PATH set RHQ_AGENT_JAVA_LIBRARY_PATH=%RHQ_AGENT_HOME%\lib
if defined RHQ_AGENT_DEBUG echo RHQ_AGENT_JAVA_LIBRARY_PATH: %RHQ_AGENT_JAVA_LIBRARY_PATH%
set _JAVA_LIBRARY_PATH_OPT=-Djava.library.path="%RHQ_AGENT_JAVA_LIBRARY_PATH%"
:skip_java_library_path

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
   set _LOG_CONFIG=-Dlog4j.configuration=log4j-debug.xml -Di18nlog.dump-stack-traces=true
) else (
   set _LOG_CONFIG=-Dlog4j.configuration=log4j.xml
)

rem if sigar debug is enabled, the log configuration is different - sigar debugging is noisy, so it has its own debug var
if defined RHQ_AGENT_SIGAR_DEBUG (
   set _LOG_CONFIG=%_LOG_CONFIG% -Dsigar.nativeLogging=true
)
rem to support other agents/plugin containers, allow the caller to override the main classname
if not defined RHQ_AGENT_MAINCLASS (
   set RHQ_AGENT_MAINCLASS=org.rhq.enterprise.agent.AgentMain
)

if not defined _SETENV_ONLY (
   rem note - currently not using custom Java Prefs as the default, use commented command line to activate. If installing
   rem note - the agent as a windows service, you must also uncomment lines in wrapper/rhq-agent-wrapper.conf.
   rem set CMD="%RHQ_JAVA_EXE_FILE_PATH%" %_JAVA_ENDORSED_DIRS_OPT% %_JAVA_LIBRARY_PATH_OPT% %_JAVA_PREFERENCES_FACTORY_OPT% %RHQ_AGENT_JAVA_OPTS% %RHQ_AGENT_ADDITIONAL_JAVA_OPTS% %_LOG_CONFIG% -cp "%CLASSPATH%" %RHQ_AGENT_MAINCLASS% %RHQ_AGENT_CMDLINE_OPTS%
   set     CMD="%RHQ_JAVA_EXE_FILE_PATH%" %_JAVA_ENDORSED_DIRS_OPT% %_JAVA_LIBRARY_PATH_OPT% %RHQ_AGENT_JAVA_OPTS% %RHQ_AGENT_ADDITIONAL_JAVA_OPTS% %_LOG_CONFIG% -cp "%CLASSPATH%" %RHQ_AGENT_MAINCLASS% %RHQ_AGENT_CMDLINE_OPTS%

   rem log4j 1.2.8 does not create the directory for us (later versions do)
   if not exist "%RHQ_AGENT_HOME%\logs" (
      mkdir "%RHQ_AGENT_HOME%\logs"
   )
   if defined RHQ_AGENT_DEBUG (
      echo Executing the agent with this command line:
      echo %CMD%
   )
   cmd.exe /S /C "!CMD!"
)

goto :done

rem ----------------------------------------------------------------------
rem CALL subroutine that appends the first argument to CLASSPATH
rem ----------------------------------------------------------------------

:append_classpath
set "_entry=%1"
if defined RHQ_AGENT_DEBUG echo CLASSPATH entry: !_entry!
if not defined CLASSPATH (
   set CLASSPATH=!_entry:"=!
) else (
   set CLASSPATH=!CLASSPATH!;!_entry:"=!
)
goto :eof

rem ----------------------------------------------------------------------
rem CALL subroutine that exits this script normally
rem ----------------------------------------------------------------------

:done
if defined RHQ_AGENT_DEBUG echo %0 done.
if defined _SETENV_ONLY (
endlocal & SET "RHQ_AGENT_HOME=%RHQ_AGENT_HOME%" & SET "RHQ_JAVA_EXE_FILE_PATH=%RHQ_JAVA_EXE_FILE_PATH%" & SET "RHQ_AGENT_JAVA_OPTS=%RHQ_AGENT_JAVA_OPTS%" & SET "RHQ_AGENT_ADDITIONAL_JAVA_OPTS=%RHQ_AGENT_ADDITIONAL_JAVA_OPTS%" & SET "RHQ_AGENT_BIN_DIR_PATH=%RHQ_AGENT_BIN_DIR_PATH%"
) else (
endlocal
)

exit /B 0
