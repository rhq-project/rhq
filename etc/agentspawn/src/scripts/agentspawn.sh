#!/bin/sh

echo java ${_JAVAOPTS} -jar ${_JAR} ${1} > ${_OUT} 2>&1
java ${_JAVAOPTS} -jar ${_JAR} ${1} >> ${_OUT} 2>&1 &
echo $! > ${_PIDFILE}
