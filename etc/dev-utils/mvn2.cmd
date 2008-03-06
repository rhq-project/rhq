@echo on

call mvn --version

if "%1" neq "clean" goto install
shift
echo.
call mvn clean 

if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%

:install
echo.
call mvn install %*
