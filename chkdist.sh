#!/bin/sh

#
# A script to validate an RHQ distribution zipfile.
#

abort()
{
   _MSG="$1"
   if [ -n "$_MSG" ]; then
      echo "$_MSG" >&2
   fi
   echo "Usage: $SCRIPT_NAME rhq-dist-zipfile" >&2
   exit 1
}

pass() 
{
   _MSG="$1"
   echo "[PASSED] $_MSG"
   return 0
}

fail()
{
   _MSG="$1"
   echo "[FAILED] $_MSG" >&2
   EXIT_CODE=2
   return 1
}

chkfile()
{
   _PATH=`eval echo $1`
   _DESC="$2"   
   _COUNT=`ls -d1 "$_PATH" 2>/dev/null | wc -l`
   if [ $_COUNT -eq 1 ]; then
      pass "$_DESC is included."
   else
      fail "$_DESC ($_PATH) is missing."
   fi 
   return $?
}

chknofile()
{
   _PATH=`eval echo $1`
   _DESC="$2"   
   _COUNT=`ls -d1 "$_PATH" 2>/dev/null | wc -l`
   if [ $_COUNT -eq 0 ]; then
      pass "$_DESC is not included."
   else
      fail "$_DESC ($_PATH) is present."
   fi 
   return $?
}

chkdir()
{
   _PATH="$1"
   _DESC="$2"
   if [ -d "$_PATH" ]; then
      pass "$_DESC is included."
   else
      fail "$_DESC ($_PATH) is missing."
   fi
   return $?
}

SCRIPT_NAME=`basename $0`

RHQ_DIST_ZIPFILE="$1"

# Validate arguments.
[ -z "$RHQ_DIST_ZIPFILE" ] && abort "Argument missing."
[ -d "$RHQ_DIST_ZIPFILE" ] && abort "$RHQ_DIST_ZIPFILE is a directory, not a regular file."
[ ! -e "$RHQ_DIST_ZIPFILE" ] && abort "$RHQ_DIST_ZIPFILE does not exist."

# Check zip file dize is in expected range.
ZIPFILESIZE=`ls -l "$RHQ_DIST_ZIPFILE" | awk '{print $5}'`
if [ "$ZIPFILESIZE" -lt 185000000 ]; then
   fail "Dist zipfile size is less than 185 MB."   
elif [ "$ZIPFILESIZE" -gt 200000000 ]; then
   fail "Dist zipfile size is greater than 200 MB."
else
   pass "Dist zipfile size is between 185 and 200 MB."
fi

# Unzip the dist zip to a temp dir.
TMP_DIR="/tmp/chkjondist$$"
unzip -q "$RHQ_DIST_ZIPFILE" -d $TMP_DIR

# e.g.: /tmp/chkjondist12345/rhq-server-4.0.0)
RHQ_DIR=`ls -d $TMP_DIR/*`

AS_CONFIG_DIR="$RHQ_DIR/jbossas/server/default"
RHQ_EAR_DIR="$AS_CONFIG_DIR/deploy/rhq.ear.rej"

EXIT_CODE=0

# Check for existence of various dirs and files.
chkdir "$RHQ_EAR_DIR" "RHQ EAR"
chknofile "$RHQ_EAR_DIR/rhq-downloads/connectors/connector-apache.zip" "Apache Connectors zipfile"
chkfile "$RHQ_EAR_DIR/rhq-downloads/connectors/connector-rtfilter.zip" "Servlet RT Connectors zipfile"
chkfile "$AS_CONFIG_DIR/lib/postgresql-*.jar" "PostgreSQL JDBC driver"
chknofile "$AS_CONFIG_DIR/lib/ojdbc5-*.jar" "Oracle JDBC driver"

# Check that JSPs are precompiled.
if [ -f "$RHQ_EAR_DIR/rhq-portal.war/WEB-INF/classes/jsp/portal/MainLayout_jsp.class" ]; then
   pass "JSPs are precompiled."
else
   fail "JSPs are not precompiled."
fi

# Check that no SNAPSHOT jars are included.
SNAPSHOT_JARS=`find "$RHQ_EAR_DIR" -name "*-SNAPSHOT.jar"` 
if [ -z "$SNAPSHOT_JARS" ]; then
   pass "No snapshot jars are included."
else 
   fail "The following snapshot jars are included: $SNAPSHOT_JARS"
fi

# Check encoding of message bundles.
ENCODING=`file $RHQ_DIR/jbossas/server/default/deploy/rhq-installer.war/WEB-INF/classes/InstallerMessages_de.properties | cut -d: -f2 | cut -d' ' -f2`
if [ "$ENCODING" = "ISO-8859" ]; then
   pass "Message bundle encoding is correct."
else 
   fail "Message bundle encoding is incorrect (expected: ISO-8859, actual: $ENCODING). LANG environment variable was probably not set to en_US.iso88591 in shell that was used to build the release."
fi

# Cleanup.
rm -rf "$TMP_DIR"

exit $EXIT_CODE

