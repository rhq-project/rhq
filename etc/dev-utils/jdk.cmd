@echo off

if not [%2] == [] goto USAGE

echo current JAVA_HOME=%JAVA_HOME%
if not exist "%JAVA_HOME%" echo WARNING: %JAVA_HOME% does not exist.

if [%1] == [] goto END

setlocal
set _JDK_VERSION_SUPPORTED=
set _NEW_JAVA_HOME=
if "%1" == "3" (set _JDK_VERSION_SUPPORTED=1 & set _NEW_JAVA_HOME=%JAVA3_HOME%)
if "%1" == "4" (set _JDK_VERSION_SUPPORTED=1 & set _NEW_JAVA_HOME=%JAVA4_HOME%)
if "%1" == "5" (set _JDK_VERSION_SUPPORTED=1 & set _NEW_JAVA_HOME=%JAVA5_HOME%)
if "%1" == "6" (set _JDK_VERSION_SUPPORTED=1 & set _NEW_JAVA_HOME=%JAVA6_HOME%)
if "%1" == "7" (set _JDK_VERSION_SUPPORTED=1 & set _NEW_JAVA_HOME=%JAVA7_HOME%)
if not defined _JDK_VERSION_SUPPORTED (echo ERROR: unrecognized JDK version: %1 - supported values are [3^|4^|5^|6^|7]. JAVA_HOME will not be updated. & goto END)
if not defined _NEW_JAVA_HOME (echo ERROR: JAVA%1_HOME environment variable is not defined. JAVA_HOME will not be updated. & goto END)
if not exist "%_NEW_JAVA_HOME%" (echo ERROR: %_NEW_JAVA_HOME% ^(as defined by JAVA%1_HOME environment variable^) does not exist. JAVA_HOME will not be updated. & goto END)
:: NOTE: The below line is a trick that allows the _NEW_JAVA_HOME local variable to be referenced outside the local block.
endlocal & set JAVA_HOME=%_NEW_JAVA_HOME%

echo new JAVA_HOME=%JAVA_HOME%

:END
exit /b 0

:USAGE
echo Usage: jdk >&2
echo (displays the current value of JAVA_HOME) >&2
echo Usage: jdk n >&2
echo (sets JAVA_HOME to the value of the JAVAn_HOME environment variable, if it is defined)
echo.
echo For example, 'jdk 5' sets JAVA_HOME to the value of JAVA5_HOME.
exit /b 1
