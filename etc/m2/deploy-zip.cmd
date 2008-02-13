@echo off

if "%4" == "" goto USAGE

setlocal
set FILE=%1
set GROUP_ID=%2
set ARTIFACT_ID=%3
set VERSION=%4
set POM_ARGS=
set POM_FILE=%5
if not "%POM_FILE%" == "" set POM_ARGS=-DgeneratePom=false -DpomFile=%POM_FILE%
set PACKAGING=zip
if "%JON_M2_REPO%" == "" set JON_M2_REPO=%USERPROFILE%\.m2\repository

mvn deploy:deploy-file -Dfile=%FILE% -DgroupId=%GROUP_ID% -DartifactId=%ARTIFACT_ID% -Dversion=%VERSION% -Dpackaging=%PACKAGING% %POM_ARGS% -Durl="file://%JON_M2_REPO%" -DrepositoryId=local

endlocal
exit /b 0

:USAGE
echo Usage: deploy-zip path/to/zipfile groupId artifactId version [path/to/pomfile] >&2
echo. >&2
echo For example: deploy-zip jboss-4.2.1.GA.zip jboss jboss 4.2.1.GA >&2
exit /b 1
