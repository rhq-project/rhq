#!/bin/sh

debug_msg ()
{
   # if debug variable is set, it is assumed to be on, unless its value is false
   if [ -n "$RHQ_CONTROL_DEBUG" ] && [ "$RHQ_CONTROL_DEBUG" != "false" ]; then
      echo $1
   fi
}

# ----------------------------------------------------------------------
# Determine what specific platform we are running on.
# Set some platform-specific variables.
# ----------------------------------------------------------------------

case "`uname`" in
   CYGWIN*) _CYGWIN=true
            ;;
   Linux*)  _LINUX=true
            ;;
   Darwin*) _DARWIN=true
            ;;
   SunOS*) _SOLARIS=true
            ;;
   AIX*)   _AIX=true
            ;;
esac

# only certain platforms support the -e argument for readlink
if [ -n "${_LINUX}${_SOLARIS}${_CYGWIN}" ]; then
   _READLINK_ARG="-e"
fi

_SCRIPT_DIR_AND_NAME="`readlink $_READLINK_ARG "$0" 2>/dev/null || echo "$0"`"
_SCRIPT_DIR="`dirname $_SCRIPT_DIR_AND_NAME`"
debug_msg "Sourcing $_SCRIPT_DIR/rhq-server-env.sh"
if [ -f "$_SCRIPT_DIR/rhq-server-env.sh" ]; then
   . "$_SCRIPT_DIR/rhq-server-env.sh" $*
else
   debug_msg "Failed to find rhq-server-env.sh. Continuing with current environment..."
fi

# internal scripts assume they are running in the current working directory
cd "$_SCRIPT_DIR/internal"

./rhq-installer.sh --encodevalue
