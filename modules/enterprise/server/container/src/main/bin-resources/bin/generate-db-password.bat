@echo off

rem ===========================================================================
rem RHQ Server Windows Generate db password script
rem
rem This file is used to execute the generate a new encrypted db password.
rem
rem This script is customizable by setting the following environment variables:
rem
rem    RHQ_SERVER_DEBUG - If this is defined, the script will emit debug
rem                       messages.
rem                       If not set or set to "false", debug is turned off.
rem
rem    RHQ_SERVER_HOME - Defines where the Server's home install directory is.
rem                      If not defined, it will be assumed to be the parent
rem                      directory of the directory where this script lives.
rem
rem    RHQ_SERVER_JAVA_HOME - The location of the JRE that the Server will
rem                           use. This will be ignored if
rem                           RHQ_SERVER_JAVA_EXE_FILE_PATH is set.
rem                           If this and RHQ_SERVER_JAVA_EXE_FILE_PATH are
rem                           not set, the Server's embedded JRE will be used.
rem
rem    RHQ_SERVER_JAVA_EXE_FILE_PATH - Defines the full path to the Java
rem                                    executable to use. If this is set,
rem                                    RHQ_SERVER_JAVA_HOME is ignored.
rem                                    If this is not set, then
rem                                    %RHQ_SERVER_JAVA_HOME%\bin\java.exe
rem                                    is used. If this and
rem                                    RHQ_SERVER_JAVA_HOME are not set, the
rem                                    Server's embedded JRE will be used.
rem
rem
rem Note that you cannot define custom Java VM parameters or command line
rem arguments to pass to the RHQ Server run.sh.  If you wish to pass in
rem specific arguments, modify the rhq-server-wrapper.conf file.
rem
rem If the embedded JRE is to be used but is not available, the fallback
rem JRE to be used will be determined by the JAVA_HOME environment variable.
rem
rem ===========================================================================

setlocal

rem if debug variable is set, it is assumed to be on, unless its value is false
if "%RHQ_SERVER_DEBUG%" == "false" (
   set RHQ_SERVER_DEBUG=
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

if defined RHQ_SERVER_DEBUG echo RHQ_SERVER_HOME: %RHQ_SERVER_HOME%

rem ----------------------------------------------------------------------
rem Find the Java executable and verify we have a VM available.
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

set _JB_DIR = %RHQ_SERVER_HOME%\jbossas
%RHQ_SERVER_JAVA_EXE_FILE_PATH% -cp %_JB_DIR%\lib\jboss-common.jar;%_JB_DIR%\lib\jboss-jmx.jar;%_JB_DIR%\server\default\lib\jbosssx.jar;%_JB_DIR%\server\default\lib\jboss-jca.jar org.jboss.resource.security.SecureIdentityLoginModule %1


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
