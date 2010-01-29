#!/bin/sh

if [ -z $1 ]; then
    echo "Usage: $0 channel-name [start-index] [end-index] [save-file-name]"
    echo "example: $0 rhel-i386-server-5  - fetch all of rhel5 i386 package metadata...warning this is REALLY SLOW from network I/O"
    echo "         $0 rhel-i386-server-5 2100 2400  - to fetch the 300 packages from 2100-2400 of rhel5 i386"
    echo "         $0 rhel-i386-server-5 2100 2400 /tmp/data.xml - to fetch the 300 packages from 2100-2400 of rhel5 i386 and save to /tmp/data.xml"
    echo "         $0 rhel-i386-server-vt-5 0 -1 /tmp/data.xml - to fetch all the packages of rhel5-vt i386 and save to /tmp/data.xml"
    exit 1
fi

if [ -z $2 ]; then
    START_INDEX=0
else
    START_INDEX=$2
fi
echo "Setting START_INDEX to ${START_INDEX}"

if [ -z $3 ]; then
    END_INDEX=-1
else
    END_INDEX=$3
fi
echo "Setting END_INDEX to ${END_INDEX}"

if [ -n $4 ]; then
    SAVE_FILE=$4
    echo "Setting SAVE_FILE to ${SAVE_FILE}"
fi

mvn -e exec:java -Dexec.mainClass="org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.DownloadPackageMetadataTool" -Drhn.channel=$1 -Drhn.index.start=${START_INDEX} -Drhn.index.end=${END_INDEX} -Drhn.save.file.path=${SAVE_FILE} -Dexec.args="" -Dlog4j.configuration="file://${PWD}/src/test/resources/test-log4j.xml"
