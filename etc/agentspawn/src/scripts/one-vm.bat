@echo off

rem A sample script that shows how you can run a single VM
rem that spawns many agents.  Notice that you can define
rem some override properties for things like pointing
rem to the server's host and port.

set ORIGINAL_DIR=%~dp0
set TARGET_DIR=..\..\target

cd %TARGET_DIR%

java -Dperftest.spawncount=10 -Don.perftest.scenario=configurable-average -Don.perftest.server-a-count=10 -Don.perftest.service-a-count=25 -Don.perftest.service-ab-count=2 -Dperftest.startpause=2000 -Drhq.agent.server.bind-address=127.0.0.1 -Drhq.agent.server.bind-port=7080 -Xms1024m -Xmx1024m -XX:PermSize=256m -XX:MaxPermSize=256m -jar org.rhq.agentspawn-1.0-SNAPSHOT.jar %1 %2

cd %ORIGINAL_DIR%
