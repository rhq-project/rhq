@echo off
setlocal

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
cd %RHQ_SERVER_HOME%\bin

set _INSTALLER_SCRIPT=%RHQ_SERVER_HOME%\bin\rhq-installer.bat
set RHQ_CCM_SCRIPT=%RHQ_SERVER_HOME%\bin\rhq-ccm.bat

echo Installing embedded Cassandra cluster...
CALL "%RHQ_CCM_SCRIPT% deploy"


rem we are normally executed just before the server starts, so give it time to initialize
rem yes, this is a sleep of 5s, backwards compatible to XP
ping 127.0.0.1 -n 5 -w 1000 > nul

for /L %%i in (1,1,2) do (
   echo Running Installer Now...
   CALL "%_INSTALLER_SCRIPT%"
   if ERRORLEVEL 2 (
      echo Installer failed - will try again in a few seconds
      ping 127.0.0.1 -n 5 -w 1000 > nul
   ) else (
      if ERRORLEVEL 1 (
         echo The installer has been disabled - please fix rhq-server.properties
         goto :done
      ) else (
         echo Installer finished
         goto :done
      )
   )
)
echo Aborting installation - installer failed.

:done
endlocal
