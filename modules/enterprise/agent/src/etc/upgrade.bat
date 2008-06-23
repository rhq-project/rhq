@echo off

if "%1" == "" goto usage 

setlocal

set OLD_AGENT_DIR=%1
if not exist %OLD_AGENT_DIR%\nul.0 goto bad_dir 

echo Importing data from old Agent installation directory (%OLD_AGENT_DIR%)...
set NEW_AGENT_DIR=%~dp0..
md %NEW_AGENT_DIR%\data\JBossAS 2>nul
echo Copying jboss.identity...
copy %OLD_AGENT_DIR%\data\jboss.identity %NEW_AGENT_DIR%\data
echo Copying inventory.dat...
copy %OLD_AGENT_DIR%\data\inventory.dat %NEW_AGENT_DIR%\data
echo Copying application-versions.dat...
copy %OLD_AGENT_DIR%\data\JBossAS\application-versions.dat %NEW_AGENT_DIR%\data\JBossAS

endlocal
exit /b

:bad_dir
echo Specified old Agent installation directory (%OLD_AGENT_DIR%) does not exist. >&2
exit /b 1

:usage
echo Usage: upgrade {old-agent-install-dir} >&2
exit /b 1
