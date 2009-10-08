#!/bin/sh

# A sample script that shows how you can run a single VM
# that spawns many agents.  Notice that you can define
# some override properties for things like pointing
# to the server's host and port.

cd ../../target

java -Dperftest.spawncount=2 \
     -Dperftest.startpause=10000 \
     -Drhq.agent.server.bind-address=127.0.0.1 \
     -Drhq.agent.server.bind-port=7080 \
     -Xms512m \
     -Xmx512m \
     -XX:PermSize=256m \
     -XX:MaxPermSize=256m \
     -jar org.rhq.agentspawn-1.0-SNAPSHOT.jar \
     $*

