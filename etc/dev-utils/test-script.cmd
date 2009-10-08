@echo off

echo Present working directory
echo -------------------------
cd
echo.

echo Command-line arguments
echo ----------------------
:startArgsLoop
if ""%1""=="""" goto endArgsLoop
echo %1
shift
goto startArgsLoop
:endArgsLoop
echo.

echo Environment variables
echo ---------------------
env
echo.

echo stdout/stderr interleave test
echo -----------------------------
echo 1 (stdout)
echo 2 (stderr) >&2
echo 3 (stdout)
echo 4 (stderr) >&2
echo.

echo Exiting with exit code 42...
exit /b 42
