# NOTE: This script is meant for setting the JAVA_HOME environment
#       variable in the current shell, and therefore must be "sourced"
#       rather than executed.
# i.e.: . path/to/jdk.sh [n]

if [ $# -gt 1 ]; then
   echo "Usage: jdk" >&2
   echo "(displays the current value of JAVA_HOME)" >&2
   echo "Usage: jdk n" >&2
   echo "(sets JAVA_HOME to the value of the JAVAn_HOME environment variable, if it is defined)" >&2
   echo >&2
   echo "For example, jdk 5 sets JAVA_HOME to the value of JAVA5_HOME." >&2
   return
fi

if [ -z "${JAVA_HOME}" ]; then
   echo "ERROR: JAVA_HOME is not defined." >&2
   return
fi

export JAVA_HOME
echo current JAVA_HOME=${JAVA_HOME}
if [ ! -d "${JAVA_HOME}" ]; then
   echo "WARNING: ${JAVA_HOME} does not exist."
elif [ ! -f "${JAVA_HOME}/bin/java" ]; then
   echo "WARNING: ${JAVA_HOME}/bin/java does not exist." 
fi

if [ $# -eq 0 ]; then
   return
fi

_JDK_VERSION="$1"
if [ "${_JDK_VERSION}" -lt 3 ] || [ "${_JDK_VERSION}" -gt 7 ]; then
   echo "ERROR: unrecognized JDK version: ${_JDK_VERSION} - supported values are [3|4|5|6|7]. JAVA_HOME will not be updated." >&2
   unset _JDK_VERSION
   return 
fi

if [ "${_JDK_VERSION}" -eq 3 ]; then
   _NEW_JAVA_HOME=${JAVA3_HOME}
elif [ "${_JDK_VERSION}" -eq 4 ]; then
   _NEW_JAVA_HOME=${JAVA4_HOME}
elif [ "${_JDK_VERSION}" -eq 5 ]; then
   _NEW_JAVA_HOME=${JAVA5_HOME}   
elif [ "${_JDK_VERSION}" -eq 6 ]; then
   _NEW_JAVA_HOME=${JAVA6_HOME}
elif [ "${_JDK_VERSION}" -eq 7 ]; then
   _NEW_JAVA_HOME=${JAVA7_HOME}
fi

if [ -z "${_NEW_JAVA_HOME}" ]; then 
   echo "ERROR: JAVA${_JDK_VERSION}_HOME environment variable is not defined. JAVA_HOME will not be updated." >&2
   unset _JDK_VERSION _NEW_JAVA_HOME
   return
fi

if [ ! -d "${_NEW_JAVA_HOME}" ]; then
   echo "ERROR: ${_NEW_JAVA_HOME} does not exist. JAVA_HOME will not be updated." >&2
   unset _JDK_VERSION _NEW_JAVA_HOME
   return
fi

if [ ! -f "${_NEW_JAVA_HOME}/bin/java" ]; then
   echo "ERROR: Invalid JAVA_HOME (JAVA${_JDK_VERSION}_HOME=${_NEW_JAVA_HOME}). ${_NEW_JAVA_HOME}/bin/java does not exist. JAVA_HOME will not be updated." >&2
   unset _JDK_VERSION _NEW_JAVA_HOME
   return
fi

unset _JDK_VERSION _NEW_JAVA_HOME

JAVA_HOME=${_NEW_JAVA_HOME}
echo "new JAVA_HOME=${JAVA_HOME}"
