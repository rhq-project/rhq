@echo off

rem ===========================================================================
rem RHQ CLI client Windows Startup Script
rem
rem This file is used to execute the RHQ CLI on a Windows platform.
rem Run this script with the --help option for the runtime options.
rem
rem This script is customizable by setting certain environment variables, which
rem are described in comments in rhq-cli-env.bat. The variables can also be
rem set via rhq-cli-env.bat, which is sourced by this script.
rem ===========================================================================

setlocal enabledelayedexpansion

title RHQ CLI

rem if debug variable is set, it is assumed to be on, unless its value is false
if "%RHQ_CLI_DEBUG%" == "false" (
   set RHQ_CLI_DEBUG=
)

rem ----------------------------------------------------------------------
rem Change directory so the current directory is the CLI home.
rem Here we assume this script is a child directory of the CLI home
rem We also assume our custom environment script is located in the same
rem place as this script.
rem ----------------------------------------------------------------------

set RHQ_CLI_BIN_DIR_PATH=%~dp0

if exist "%RHQ_CLI_BIN_DIR_PATH%\rhq-cli-env.bat" (
   if defined RHQ_CLI_DEBUG echo Loading environment script: %RHQ_CLI_BIN_DIR_PATH%\rhq-cli-env.bat
   call "%RHQ_CLI_BIN_DIR_PATH%\rhq-cli-env.bat" %*
) else (
   if defined RHQ_CLI_DEBUG echo No environment script found at: %RHQ_CLI_BIN_DIR_PATH%\rhq-cli-env.bat
)

rem if debug variable is set, it is assumed to be on, unless its value is false
if "%RHQ_CLI_DEBUG%" == "false" (
   set RHQ_CLI_DEBUG=
)

if not defined RHQ_CLI_HOME (
   cd "%RHQ_CLI_BIN_DIR_PATH%\.."
) else (
   cd "%RHQ_CLI_HOME%" || (
      echo Cannot go to the RHQ_CLI_HOME directory: %RHQ_CLI_HOME%
      exit /B 1
      )
)

set RHQ_CLI_HOME=%CD%

if defined RHQ_CLI_DEBUG echo RHQ_CLI_HOME: %RHQ_CLI_HOME%

rem ----------------------------------------------------------------------
rem Find the Java executable and verify we have a VM available
rem Note that RHQ_CLI_JAVA_* props are still handled for back compat
rem ----------------------------------------------------------------------

if not defined RHQ_JAVA_EXE_FILE_PATH (
   if defined RHQ_CLI_JAVA_EXE_FILE_PATH (
      set RHQ_JAVA_EXE_FILE_PATH=!RHQ_CLI_JAVA_EXE_FILE_PATH!
   )
)
if not defined RHQ_JAVA_HOME (
   if defined RHQ_CLI_JAVA_HOME (
      set RHQ_JAVA_HOME=!RHQ_CLI_JAVA_HOME!
   )
)

if not defined RHQ_JAVA_EXE_FILE_PATH (
   if not defined RHQ_JAVA_HOME (
      if defined RHQ_CLI_DEBUG echo No RHQ JAVA property set, defaulting to JAVA_HOME: !JAVA_HOME!
      set RHQ_JAVA_HOME=!JAVA_HOME!
   )
)
if not defined RHQ_JAVA_EXE_FILE_PATH (
   set RHQ_JAVA_EXE_FILE_PATH=!RHQ_JAVA_HOME!\bin\java.exe
)

if defined RHQ_CLI_DEBUG echo RHQ_JAVA_HOME: %RHQ_JAVA_HOME%
if defined RHQ_CLI_DEBUG echo RHQ_JAVA_EXE_FILE_PATH: %RHQ_JAVA_EXE_FILE_PATH%

if not exist "%RHQ_JAVA_EXE_FILE_PATH%" (
   echo There is no JVM available.
   echo Please set RHQ_JAVA_HOME or RHQ_JAVA_EXE_FILE_PATH appropriately.
   exit /B 1
)

rem ----------------------------------------------------------------------
rem Prepare the classpath
rem ----------------------------------------------------------------------

set CLASSPATH=
call :append_classpath "%RHQ_CLI_HOME%\conf"
for /R "%RHQ_CLI_HOME%\lib" %%G in ("*.jar") do (
   call :append_classpath "%%G"
   if defined RHQ_CLI_DEBUG echo CLASSPATH entry: %%G
)

rem ----------------------------------------------------------------------
rem Prepare the VM command line options to be passed in
rem ----------------------------------------------------------------------

if not defined RHQ_CLI_JAVA_OPTS (
   set RHQ_CLI_JAVA_OPTS=-Xms64m -Xmx128m -Djava.net.preferIPv4Stack=true
)
if defined RHQ_CLI_DEBUG echo RHQ_CLI_JAVA_OPTS: %RHQ_CLI_JAVA_OPTS%

if "%RHQ_CLI_JAVA_ENDORSED_DIRS%" == "none" (
   if defined RHQ_CLI_DEBUG echo Not explicitly setting java.endorsed.dirs
   goto :skip_java_endorsed_dirs
)
if not defined RHQ_CLI_JAVA_ENDORSED_DIRS set RHQ_CLI_JAVA_ENDORSED_DIRS=%RHQ_CLI_HOME%\lib\endorsed
if defined RHQ_CLI_DEBUG echo RHQ_CLI_JAVA_ENDORSED_DIRS: %RHQ_CLI_JAVA_ENDORSED_DIRS%
set _JAVA_ENDORSED_DIRS_OPT="-Djava.endorsed.dirs=%RHQ_CLI_JAVA_ENDORSED_DIRS%"
:skip_java_endorsed_dirs

if "%RHQ_CLI_JAVA_LIBRARY_PATH%" == "none" (
   if defined RHQ_CLI_DEBUG echo Not explicitly setting java.library.path
   goto :skip_java_library_path
)
if not defined RHQ_CLI_JAVA_LIBRARY_PATH set RHQ_CLI_JAVA_LIBRARY_PATH=%RHQ_CLI_HOME%\lib
if defined RHQ_CLI_DEBUG echo RHQ_CLI_JAVA_LIBRARY_PATH: %RHQ_CLI_JAVA_LIBRARY_PATH%
set _JAVA_LIBRARY_PATH_OPT="-Djava.library.path=%RHQ_CLI_JAVA_LIBRARY_PATH%"
:skip_java_library_path

if defined RHQ_CLI_DEBUG echo RHQ_CLI_ADDITIONAL_JAVA_OPTS: %RHQ_CLI_ADDITIONAL_JAVA_OPTS%

rem ----------------------------------------------------------------------
rem Prepare the command line arguments passed to the RHQ CLI
rem ----------------------------------------------------------------------

if not defined RHQ_CLI_CMDLINE_OPTS (
   set RHQ_CLI_CMDLINE_OPTS=%*
)

if defined RHQ_CLI_DEBUG echo RHQ_CLI_CMDLINE_OPTS: %RHQ_CLI_CMDLINE_OPTS%

rem ----------------------------------------------------------------------
rem Execute the VM which starts the CLI
rem ----------------------------------------------------------------------

if defined RHQ_CLI_DEBUG (
   set _LOG_CONFIG=-Dlog4j.configuration=log4j-debug.xml
) else (
   set _LOG_CONFIG=-Dlog4j.configuration=log4j.xml
)

set CMD="%RHQ_JAVA_EXE_FILE_PATH%" %_JAVA_ENDORSED_DIRS_OPT% %_JAVA_LIBRARY_PATH_OPT% %RHQ_CLI_JAVA_OPTS% %RHQ_CLI_ADDITIONAL_JAVA_OPTS% %_LOG_CONFIG% -cp "%CLASSPATH%" org.rhq.enterprise.client.ClientMain %RHQ_CLI_CMDLINE_OPTS%

rem log4j 1.2.8 does not create the directory for us (later versions do)
if not exist "%RHQ_CLI_HOME%\logs" (
   mkdir "%RHQ_CLI_HOME%\logs"
)
if defined RHQ_CLI_DEBUG (
   echo Executing the CLI with this command line:
   echo %CMD%
)
cmd.exe /S /C "%CMD%"
if ERRORLEVEL 1 goto :error

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
rem CALL subroutine that exits this script normally
rem ----------------------------------------------------------------------

:done
if defined RHQ_CLI_DEBUG echo %0 done.

endlocal

exit /B 0

rem ----------------------------------------------------------------------
rem CALL subroutine that exits this script with error
rem ----------------------------------------------------------------------

:error
if defined RHQ_CLI_DEBUG echo %0 done.
endlocal

if ERRORLEVEL 1 exit /B 1
