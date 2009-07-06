@echo off

rem ----------------------------------------------------------------------
rem Prepare the classpath
rem ----------------------------------------------------------------------

SET RHQ_PLUGINDOC_HOME=..

set CLASSPATH=
call :append_classpath "%RHQ_PLUGINDOC_HOME%\conf"
for /R "%RHQ_PLUGINDOC_HOME%\lib" %%G in ("*.jar") do (
   call :append_classpath "%%G"
)

set RHQ_PLUGINDOC_CMDLINE_OPTS=%*
set RHQ_PLUGINDOC_MAINCLASS=org.rhq.core.tool.plugindoc.PluginDocGenerator

set CMD=java.exe -cp "%CLASSPATH%" %RHQ_PLUGINDOC_MAINCLASS% %RHQ_PLUGINDOC_CMDLINE_OPTS%
cmd.exe /S /C "%CMD%"

rem ----------------------------------------------------------------------
rem CALL subroutine that appends the first argument to CLASSPATH
rem ----------------------------------------------------------------------

:append_classpath
set _entry=%1
if not defined CLASSPATH (
   set CLASSPATH=%_entry:"=%
) else (
   set CLASSPATH=%CLASSPATH%;%_entry:"=%
)
goto :eof

exit /B 0
