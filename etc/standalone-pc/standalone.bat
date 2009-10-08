@echo off

rem uncomment below if you want to enable JPDA debugging
rem set RHQ_AGENT_ADDITIONAL_JAVA_OPTS=-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=9797,server=y,suspend=n

set RHQ_AGENT_MAINCLASS=org.rhq.core.pc.StandaloneContainer
call rhq-agent.bat %*
