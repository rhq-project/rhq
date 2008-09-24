@echo off

rem because Windows has no replacement for the UNIX $!, we tell our Java code to generate the pidfile

echo cmd.exe /S /C java %_JAVAOPTS% -Dperftest.pidfile=%_PIDFILE% -jar %_JAR% %1 > %_OUT% 2>&1
cmd.exe /S /C java %_JAVAOPTS% -Dperftest.pidfile=%_PIDFILE% -jar %_JAR% %1 > %_OUT% 2>&1

