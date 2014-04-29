@echo off

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

cd "%_SCRIPT_DIR%\internal"

rem internal scripts assume they are running in the current working directory
rhq-installer --encodepassword

endlocal
