#!/bin/sh

if [ $# -ne 1 ]; then
  echo "Usage: upgrade {old-agent-install-dir}" >&2
  exit 1
fi

OLD_AGENT_DIR="$1"
if [ ! -d "${OLD_AGENT_DIR}" ]; then
  echo "Specified old Agent installation directory (${OLD_AGENT_DIR}) does not exist." >&2
  exit 1
fi

echo "Importing data from old Agent installation directory (${OLD_AGENT_DIR})..."
SCRIPT_DIR=`dirname $0`
NEW_AGENT_DIR="${SCRIPT_DIR}/.."
mkdir -p ${NEW_AGENT_DIR}/data/JBossAS 2>/dev/null
echo "Copying jboss.identity..."
cp ${OLD_AGENT_DIR}/data/jboss.identity ${NEW_AGENT_DIR}/data
echo "Copying inventory.dat..."
cp ${OLD_AGENT_DIR}/data/inventory.dat ${NEW_AGENT_DIR}/data
echo "Copying application-versions.dat..."
cp ${OLD_AGENT_DIR}/data/JBossAS/application-versions.dat ${NEW_AGENT_DIR}/data/JBossAS
