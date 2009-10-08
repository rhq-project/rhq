@echo off

setlocal

echo Enabling Agent DEBUG output...
set RHQ_AGENT_DEBUG=1
echo Enabling JPWP for remote debugging...
set RHQ_AGENT_ADDITIONAL_JAVA_OPTS=-agentlib:jdwp=transport=dt_socket,address=9797,server=y,suspend=n
call rhq-agent.bat %*

endlocal

exit /b %errorlevel%
