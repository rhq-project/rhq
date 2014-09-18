#!/bin/sh

####################################################################################################
# this script has been deprecated and will be removed in the next release
####################################################################################################

echo 'WARNING! rhq-encode-password.sh has been deprecated. Please use rhq-encode-value.sh utility.'
echo ' '

####################################################################################################

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

command -v readlink >/dev/null 2>&1
if [ $? -ne 0 ]; then
    echo >&2 'WARNING: The readlink command is not available on this platform.'
    echo >&2 '         If this script was launched from a symbolic link, errors may occur.'
    echo >&2 '         Consider installing readlink on this platform.'
    _DOLLARZERO="$0"
else
    # only certain platforms support the -e argument for readlink
    if [ -n "${_LINUX}${_SOLARIS}${_CYGWIN}" ]; then
       _READLINK_ARG="-e"
    fi
    _DOLLARZERO="`readlink $_READLINK_ARG "$0" 2>/dev/null || echo "$0"`"
fi

_SCRIPT_DIR="`dirname $_DOLLARZERO`"
debug_msg "Sourcing $_SCRIPT_DIR/rhq-server-env.sh"
if [ -f "$_SCRIPT_DIR/rhq-server-env.sh" ]; then
   . "$_SCRIPT_DIR/rhq-server-env.sh" $*
else
   debug_msg "Failed to find rhq-server-env.sh. Continuing with current environment..."
fi

# internal scripts assume they are running in the current working directory
cd "$_SCRIPT_DIR/internal"

./rhq-installer.sh --encodevalue