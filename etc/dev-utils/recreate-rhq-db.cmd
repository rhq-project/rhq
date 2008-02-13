@echo off

set DEFAULT_POSTGRES_HOME=C:\Program Files\PostgreSQL\8.2
if "%POSTGRES_HOME%"=="" set POSTGRES_HOME=%DEFAULT_POSTGRES_HOME%
echo POSTGRES_HOME="%POSTGRES_HOME%"

echo Dropping rhq db...
"%POSTGRES_HOME%\bin\dropdb" -U postgres rhq >NUL
echo Creating rhq db...
"%POSTGRES_HOME%\bin\createdb" -U postgres -O rhq rhq >NUL
