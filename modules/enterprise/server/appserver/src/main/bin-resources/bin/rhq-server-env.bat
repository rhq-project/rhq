@echo off

rem ===========================================================================
rem RHQ Environment Variables

rem This file defines environment variables that affect the RHQ components on a
rem Windows machine.  Not every variable is applicable to every component. See
rem the section comments for more information.
rem ===========================================================================


rem ===========================================================================
rem RHQ Environment Variables - General
rem
rem   These properties apply to all rhqctl installed RHQ components.

rem RHQ_JAVA_HOME
rem
rem   The JRE location use by RHQ components. This will be ignored if
rem   RHQ_JAVA_EXE_FILE_PATH is set. If this and RHQ_JAVA_EXE_FILE_PATH are
rem   both unset then JAVA_HOME will be used.
rem
rem set RHQ_JAVA_HOME=

rem RHQ_JAVA_EXE_FILE_PATH
rem
rem   Defines the full path to the Java executable RHQ components will use. If
rem   this is set, RHQ_JAVA_HOME is ignored.  If this is not set, then
rem   %RHQ_JAVA_HOME%/bin/java is used. If this and RHQ_JAVA_HOME are both
rem   unset then %JAVA_HOME%/bin/java will be used.
rem
rem set RHQ_JAVA_EXE_FILE_PATH=

rem RHQ_SERVER_HOME
rem
rem   Defines where the RHQ server's home install directory is. If not defined
rem   it will be assumed to be the parent directory of where this script lives.
rem
rem set RHQ_SERVER_HOME=

rem RHQ_SERVER_JBOSS_HOME
rem
rem   The location of the AS instance that will host RHQ. If this is set, it
rem   overrides any JBOSS_HOME that might be set. If this not set JBOSS_HOME
rem   is used as a fallback. If neither is set it is assumed the AS bundled
rem   under %RHQ_SERVER_HOME%/jbossas is to be used.
rem
rem set RHQ_SERVER_JBOSS_HOME=
rem ===========================================================================


rem ===========================================================================
rem RHQ Environment Variables - Debug Flags
rem
rem RHQ_XXX_DEBUG
rem
rem   If defined the applicable component will emit debug messages. If not
rem   defined or set to "false" debug messages are not emitted.
rem
rem set RHQ_CONTROL_DEBUG=true
rem
rem set RHQ_DATA_MIGRATION_DEBUG=true
rem
rem set RHQ_SERVER_DEBUG=true
rem
rem set RHQ_STORAGE_DEBUG=true
rem ===========================================================================


rem ===========================================================================
rem RHQ Environment Variables - Java Options
rem
rem RHQ_XXX_JAVA_OPTS
rem
rem   Specifies Java VM command line options to be passed  into the Java VM for
rem   the applicable component. If not defined the script will pass in a
rem   default set of options.  To keep the default options but add to them, use
rem   RHQ_XXX_ADDITIONAL_JAVA_OPTS instead.
rem
rem RHQ_XXX_ADDITIONAL_JAVA_OPTS
rem
rem   Specifies additional Java VM command line options to be passed into the
rem   VM.  This is added to the default options or the options specified by
rem   RHQ_XXX_JAVA_OPTS; it is mainly used to augment the default set of
rem   options.  Leave unset if not needed.
rem
rem set RHQ_CONTROL_JAVA_OPTS=
rem set RHQ_CONTROL_ADDITIONAL_JAVA_OPTS=
rem
rem set RHQ_DATA_MIGRATION_JAVA_OPTS=
rem set RHQ_DATA_MIGRATION_ADDITIONAL_JAVA_OPTS=
rem
rem set RHQ_SERVER_INSTALLER_JAVA_OPTS=
rem set RHQ_SERVER_INSTALLER_ADDITIONAL_JAVA_OPTS=
rem
rem set RHQ_STORAGE_INSTALLER_JAVA_OPTS=
rem set RHQ_STORAGE_INSTALLER_ADDITIONAL_JAVA_OPTS=
rem
rem ** Note that to add additional Java options to a windows service you must
rem    supply them via the rhq-xxx-wrapper.inc file. See those files in the
rem    %RHQ_SERVER_HOME%/bin/wrapper directory.
rem =============================================================================


rem ===========================================================================
rem RHQ Environment Variables - Windows Service Options
rem
rem These options affect the Windows Services installed by rhqctl.  Note that
rem the AGENT settings affect only rhqctl-installed agents.
rem
rem
rem RHQ_XXX_INSTANCE_NAME
rem
rem   Specifies the name of the Windows Service; it must conform to the Windows
rem   Service naming conventions. Default: "rhqxxx-%COMPUTERNAME%".
rem
rem RHQ_XXX_WRAPPER_LOG_DIR_PATH
rem
rem   The full path for the wrapper log file. It defaults to the standard
rem   server logs directory, %RHQ_SERVER_HOME%/logs.
rem
rem RHQ_XXX_RUN_AS
rem
rem   If defined the Windows Service runs as the specified domain\username
rem   user.  It is required to also set RHQ_XXX_PASSWORD for the specified
rem   user account.
rem
rem RHQ_XXX_RUN_AS_ME
rem
rem   If defined and not false, the Windows Service runs as the domain\username
rem   of the the current user (.\%USERNAME%).  This takes precedence over
rem   RHQ_XXX_RUN_AS. It is required to also set RHQ_XXX_PASSWORD for the
rem   current user account.
rem
rem RHQ_XXX_PASSWORD
rem
rem   The password for the user running the service, when the RUN_AS or
rem   RUN_AS_ME option is in use.
rem
rem set RHQ_AGENT_INSTANCE_NAME=
rem set RHQ_AGENT_WRAPPER_LOG_DIR_PATH=
rem set RHQ_AGENT_RUN_AS=
rem set RHQ_AGENT_RUN_AS_ME=
rem set RHQ_AGENT_PASSWORD=
rem
rem set RHQ_SERVER_INSTANCE_NAME=
rem set RHQ_SERVER_WRAPPER_LOG_DIR_PATH=
rem set RHQ_SERVER_RUN_AS=
rem set RHQ_SERVER_RUN_AS_ME=
rem set RHQ_SERVER_PASSWORD=
rem
rem set RHQ_STORAGE_INSTANCE_NAME=
rem set RHQ_STORAGE_WRAPPER_LOG_DIR_PATH=
rem set RHQ_STORAGE_RUN_AS=
rem set RHQ_STORAGE_RUN_AS_ME=
rem set RHQ_STORAGE_PASSWORD=
rem
rem ** Note that to add additional Java options to a windows service you must
rem    supply them via the rhq-xxx-wrapper.inc file. See those files in the
rem    %RHQ_SERVER_HOME%/bin/wrapper directory.
rem ===========================================================================


rem ===========================================================================
rem RHQ Environment Variables - Storage Options
rem
rem   Environment variables specific to the RHQ Storage Node service.
rem
rem RHQ_STORAGE_HOME
rem
rem   Defines where the Storage Node's home install directory is. If not
rem   defined it will be assumed to be %RHQ_SERVER_HOME%/rhq-storage.
rem
rem set RHQ_STORAGE_HOME=
rem ===========================================================================

