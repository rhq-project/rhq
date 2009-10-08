#!/bin/sh

DEFAULT_POSTGRES_HOME="/usr/local/pgsql"
if [ -z "${POSTGRES_HOME}" ]; then 
   POSTGRES_HOME="${DEFAULT_POSTGRES_HOME}"
fi
echo "POSTGRES_HOME=\"${POSTGRES_HOME}\""

echo "Dropping rhq db..."
"${POSTGRES_HOME}/bin/dropdb" rhq >/dev/null
echo "Creating rhq db..."
"${POSTGRES_HOME}/bin/createdb" -O rhq rhq >/dev/null
