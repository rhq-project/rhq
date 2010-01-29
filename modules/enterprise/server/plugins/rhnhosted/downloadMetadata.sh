#!/bin/sh

if [ -z $1 ]; then
    echo "Usage: $0 {channel-name}"
    echo "example: $0 rhel-i386-server-5"
    exit 1
fi

mvn -e exec:java -Dexec.mainClass="org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.DownloadPackageMetadataTool" -Drhn.channel=$1 -Dexec.args="" -Dlog4j.configuration="file://${PWD}/src/test/resources/test-log4j.xml"
