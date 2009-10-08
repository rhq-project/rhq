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
set PACKAGING=jar
if "%RHQ_M2_REPO%" == "" set RHQ_M2_REPO=%USERPROFILE%\.m2\repository

echo mvn deploy:deploy-file -Dfile=%FILE% -DgroupId=%GROUP_ID% -DartifactId=%ARTIFACT_ID% -Dversion=%VERSION% -Dpackaging=%PACKAGING% %POM_ARGS% -Durl="file://%RHQ_M2_REPO%" -DrepositoryId=local
mvn deploy:deploy-file -Dfile=%FILE% -DgroupId=%GROUP_ID% -DartifactId=%ARTIFACT_ID% -Dversion=%VERSION% -Dpackaging=%PACKAGING% %POM_ARGS% -Durl="file://%RHQ_M2_REPO%" -DrepositoryId=local

endlocal
exit /b 0

:USAGE
echo Usage: deploy-jar path/to/jarfile groupId artifactId version [path/to/pomfile] >&2
echo. >&2
echo For example: deploy-jar ojdbc14-10.2.0.2.0.jar ojdbc14 ojdbc14 10.2.0.2.0 ojdbc14-10.2.0.2.0.pom >&2
exit /b 1
