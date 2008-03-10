@echo off

echo cmd.exe /S /C java %_JAVAOPTS% -jar %_JAR% %1 > %_OUT% 2>&1
cmd.exe /S /C java %_JAVAOPTS% -jar %_JAR% %1 > %_OUT% 2>&1

