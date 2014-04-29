# ===========================================================================
# RHQ Environment Variables

# This file defines environment variables that affect the RHQ components on a
# Linux machine.  Not every variable is applicable to every component. See
# the section comments for more information.
# ===========================================================================


# ===========================================================================
# RHQ Environment Variables - General
#
#   These properties apply to all rhqctl installed RHQ components.

# RHQ_JAVA_HOME
#
#   The JRE location use by RHQ components. This will be ignored if
#   RHQ_JAVA_EXE_FILE_PATH is set. If this and RHQ_JAVA_EXE_FILE_PATH are
#   both unset then JAVA_HOME will be used.
#
# RHQ_JAVA_HOME=

# RHQ_JAVA_EXE_FILE_PATH
#
#   Defines the full path to the Java executable RHQ components will use. If
#   this is set, RHQ_JAVA_HOME is ignored.  If this is not set, then
#   %RHQ_JAVA_HOME%/bin/java is used. If this and RHQ_JAVA_HOME are both
#   unset then %JAVA_HOME%/bin/java will be used.
#
# RHQ_JAVA_EXE_FILE_PATH=

# RHQ_SERVER_HOME
#
#   Defines where the RHQ server's home install directory is. If not defined
#   it will be assumed to be the parent directory of where this script lives.
#
# RHQ_SERVER_HOME=

# RHQ_SERVER_JBOSS_HOME
#
#   The location of the AS instance that will host RHQ. If this is set, it
#   overrides any JBOSS_HOME that might be set. If this not set JBOSS_HOME
#   is used as a fallback. If neither is set it is assumed the AS bundled
#   under %RHQ_SERVER_HOME%/jbossas is to be used.
#
# RHQ_SERVER_JBOSS_HOME=
# ===========================================================================


# ===========================================================================
# RHQ Environment Variables - Debug Flags
#
# RHQ_XXX_DEBUG
#
#   If defined the applicable component will emit debug messages. If not
#   defined or set to "false" debug messages are not emitted.
#
# RHQ_CONTROL_DEBUG=true
#
# RHQ_DATA_MIGRATION_DEBUG=true
#
# RHQ_SERVER_DEBUG=true
#
# RHQ_STORAGE_DEBUG=true
# ===========================================================================


# ===========================================================================
# RHQ Environment Variables - Java Options
#
# RHQ_XXX_JAVA_OPTS
#
#   Specifies Java VM command line options to be passed  into the Java VM for
#   the applicable component. If not defined the script will pass in a
#   default set of options.  To keep the default options but add to them, use
#   RHQ_XXX_ADDITIONAL_JAVA_OPTS instead.
#
# RHQ_XXX_ADDITIONAL_JAVA_OPTS
#
#   Specifies additional Java VM command line options to be passed into the
#   VM.  This is added to the default options or the options specified by
#   RHQ_XXX_JAVA_OPTS; it is mainly used to augment the default set of
#   options.  Leave unset if not needed.
#
# RHQ_CONTROL_JAVA_OPTS=
# RHQ_CONTROL_ADDITIONAL_JAVA_OPTS=
#
# RHQ_DATA_MIGRATION_JAVA_OPTS=
# RHQ_DATA_MIGRATION_ADDITIONAL_JAVA_OPTS=
#
# RHQ_SERVER_JAVA_OPTS=
# RHQ_SERVER_ADDITIONAL_JAVA_OPTS=
#
# RHQ_SERVER_INSTALLER_JAVA_OPTS=
# RHQ_SERVER_INSTALLER_ADDITIONAL_JAVA_OPTS=
#
# RHQ_STORAGE_INSTALLER_JAVA_OPTS=
# RHQ_STORAGE_INSTALLER_ADDITIONAL_JAVA_OPTS=
# =============================================================================


# ===========================================================================
# RHQ Environment Variables - Server Options
#
#   Environment variables specific to the RHQ Server.
#
# RHQ_SERVER_CMDLINE_OPTS
#
#   The command line arguments that will be passed to the RHQ Server JBossAS
#   standalone.sh. This overrides the defaults.  To only add options to the
#   Server's defaults use RHQ_SERVER_ADDITIONAL_CMDLINE_OPTS instead.
#
# RHQ_SERVER_CMDLINE_OPTS=
#
# RHQ_SERVER_ADDITIONAL_CMDLINE_OPTS
#
#   Additional command line arguments to be passed to the RHQ Server JBossAS
#   standalone.sh. This is added to RHQ_SERVER_CMDLINE_OPTS; it is primarily
#   used to augment the Server's default set of options. Leave unset if not
#   needed.
#
# RHQ_SERVER_ADDITIONAL_CMDLINE_OPTS=
#
# RHQ_SERVER_PIDFILE_DIR
#
#   A full path of the writable directory to which this script writes its
#   pidfile. Default: the Server's bin directory.
#
# RHQ_SERVER_PIDFILE_DIR=
#
# RHQ_SERVER_STOP_DELAY
#
#   The number of minutes to wait for the server to go down after sending
#   the TERM signal. Default: 5 minutes.
#
# RHQ_SERVER_STOP_DELAY=
#
# RHQ_SERVER_KILL_AFTER_STOP_DELAY
#
#   If defined and not false the server is terminated if still running after
#   RHQ_SERVER_STOP_DELAY has expired. Otherwise the script will exit with
#   error code 127.
# 
# RHQ_SERVER_KILL_AFTER_STOP_DELAY=
# ===========================================================================

