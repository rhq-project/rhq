/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.agent.i18n;

import mazz.i18n.annotation.I18NMessage;
import mazz.i18n.annotation.I18NResourceBundle;

/**
 * I18N resource bundle keys that identify the messages needed by the agent.
 *
 * @author John Mazzitelli
 */
@I18NResourceBundle(baseName = "agent-messages", defaultLocale = "en")
public interface AgentI18NResourceKeys {
    @I18NMessage("The server and agent clocks are not in sync. Server=[{0,number,#}][{1,date,long} {1,time,full}], Agent=[{2,number,#}][{3,date,long} {3,time,full}]")
    String TIME_NOT_SYNCED = "AgentMain.time-not-synced";

    @I18NMessage("Failed to determine the server time. Cause: {0}")
    String TIME_UNKNOWN = "AgentMain.time-unknown";

    @I18NMessage("Timed out waiting for the connectAgent R/W lock to avoid a possible deadlock")
    String TIMEOUT_WAITING_FOR_CONNECT_LOCK = "AgentMain.connect-lock-timeout";

    @I18NMessage("Not sending another connect message since one was recently sent: [{0}]")
    String NOT_SENDING_DUP_CONNECT = "AgentMain.not-sending-dup-connect";

    @I18NMessage("The agent is not talking to its primary server [{0}:{1,number,#}] - it is talking to [{2}:{3,number,#}]")
    String NOT_TALKING_TO_PRIMARY_SERVER = "PrimaryServerSwitchoverThread.not-talking-to-primary";

    @I18NMessage("An exception occurred during the primary server switchover check. Cause: {0}")
    String PRIMARY_SERVER_SWITCHOVER_EXCEPTION = "PrimaryServerSwitchoverThread.exception";

    @I18NMessage("Primary server appears to be back online at [{0}:{1,number,#}] - attempting to switch back to it")
    String PRIMARY_SERVER_UP = "PrimaryServerSwitchoverThread.primary-up";

    @I18NMessage("Primary server at [{0}:{1,number,#}] is still down - cannot switch back to it yet")
    String PRIMARY_SERVER_STILL_DOWN = "PrimaryServerSwitchoverThread.primary-still-down";

    @I18NMessage("The primary server switchover thread has started.")
    String PRIMARY_SERVER_SWITCHOVER_THREAD_STARTED = "PrimaryServerSwitchoverThread.started";

    @I18NMessage("The primary server switchover thread has stopped.")
    String PRIMARY_SERVER_SWITCHOVER_THREAD_STOPPED = "PrimaryServerSwitchoverThread.stopped";

    @I18NMessage("The agent has triggered its failover mechanism and switched to server [{0}]")
    String FAILED_OVER_TO_SERVER = "AgentMain.failed-over-to-server";

    @I18NMessage("Failed to failover to another server. Cause: {0}")
    String FAILOVER_FAILED = "AgentMain.failover-failed";

    @I18NMessage("During failover attempt, the discovery features failed to start. Discovery may be disabled.")
    String FAILOVER_DISCOVERY_START_FAILURE = "AgentMain.failover-discovery-start-failure";

    @I18NMessage("Too many failover attempts have been made [{0}]. Exception that triggered the failover: [{1}]")
    String TOO_MANY_FAILOVER_ATTEMPTS = "AgentMain.too-many-failover-attempts";

    @I18NMessage("The server failover list has been loaded from [{0}] - there are [{1}] servers in the list")
    String FAILOVER_LIST_LOADED = "AgentMain.failover-list-loaded";

    @I18NMessage("The server failover list cannot be loaded from [{0}]. Cause: {1}")
    String FAILOVER_LIST_CANNOT_BE_LOADED = "AgentMain.failover-list-cannot-be-loaded";

    @I18NMessage("The server failover list is empty; but [{0}] cannot be deleted")
    String FAILOVER_LIST_CANNOT_BE_DELETED = "AgentMain.failover-list-cannot-be-deleted";

    @I18NMessage("The server failover list is empty; [{0}] has been deleted")
    String FAILOVER_LIST_PERSISTED_EMPTY = "AgentMain.failover-list-persisted-empty";

    @I18NMessage("The server failover list has been persisted to [{0}]")
    String FAILOVER_LIST_PERSISTED = "AgentMain.failover-list-persisted";

    @I18NMessage("The server failover list cannot be persisted to [{0}]. Cause: {1}")
    String FAILOVER_LIST_CANNOT_BE_PERSISTED = "AgentMain.failover-list-cannot-be-persisted";

    @I18NMessage("Downloaded an updated server failover list of size [{0}]")
    String FAILOVER_LIST_DOWNLOADED = "AgentMain.failover-list-downloaded";

    @I18NMessage("Failed to download an updated server failover list. Cause: {0}")
    String FAILOVER_LIST_DOWNLOAD_FAILURE = "AgentMain.failover-list-download-failure";

    @I18NMessage("(type it again to confirm) ")
    String PROMPT_CONFIRM = "AgentNativePromptInfo.prompt-confirm";

    @I18NMessage("Your answers did not match - please try again: ")
    String PROMPT_CONFIRM_FAILED = "AgentNativePromptInfo.prompt-confirm-failed";

    @I18NMessage("Received a command - sounds like the server is up so the sender has been started")
    String RECEIVED_COMMAND_STARTED_SENDER = "AgentMain.received-msg-started-sender";

    @I18NMessage("Agent has been asked to start up clean - cleaning out the data directory: {0}")
    String CLEANING_DATA_DIRECTORY = "AgentMain.clean-data-dir";

    @I18NMessage("Deleting data file [{0}]. deleted successfully=[{1}]")
    String CLEANING_DATA_FILE = "AgentMain.clean-data-file";

    @I18NMessage("A failure occurred while trying to clean the data directory. Cause: {0}")
    String CLEAN_DATA_DIR_FAILURE = "AgentMain.clean-data-dir-failure";

    @I18NMessage("Failed to delete a data file [{0}]")
    String CLEAN_DATA_FILE_FAILURE = "AgentMain.clean-data-file-failure";

    @I18NMessage("The native system has been disabled.")
    String NATIVE_SYSTEM_DISABLED = "AgentMain.native-system-disabled";

    @I18NMessage("Remoting agent service [{0}] with remote interface [{1}]")
    String REMOTING_NEW_AGENT_SERVICE = "AgentServiceRemoting.remoting-agent-service";

    @I18NMessage("Failed to remote agent service [{0}] with remote interface [{1}]")
    String ERROR_REMOTING_NEW_AGENT_SERVICE = "AgentServiceRemoting.error-remoting-agent-service";

    @I18NMessage("Unremoting agent service [{0}] (remote interface [{1}]) because it is stopping")
    String UNREMOTING_AGENT_SERVICE = "AgentServiceRemoting.unremoting-agent-service";

    @I18NMessage("Failed to remote an input stream. Cause: {0}")
    String FAILED_TO_REMOTE_STREAM = "AgentServiceRemoting.failed-to-remote-stream";

    @I18NMessage("Failed to remote an output stream. Cause: {0}")
    String FAILED_TO_REMOTE_OUTSTREAM = "AgentServiceRemoting.failed-to-remote-outstream";

    @I18NMessage("The agent is waiting for the server - will sleep for [{0}] milliseconds")
    String WAITING_FOR_SERVER = "AgentMain.waiting-for-server";

    @I18NMessage("The agent is shutdown - it cannot wait for the server")
    String CANNOT_WAIT_FOR_SERVER = "AgentMain.cannot-wait-for-server";

    @I18NMessage("The agent is not configured to register at startup; however, its local registration data is missing so the agent will be forced to re-register again")
    String FORCING_AGENT_REGISTRATION = "AgentMain.forcing-agent-registration";

    @I18NMessage("Failed to update the plugins.")
    String UPDATING_PLUGINS_FAILURE = "AgentMain.plugin-update-failure";

    @I18NMessage("Updating plugins to their latest versions.")
    String UPDATING_PLUGINS = "PluginUpdate.updating";

    @I18NMessage("Completed updating the plugins to their latest versions.")
    String UPDATING_PLUGINS_COMPLETE = "PluginUpdate.updating-complete";

    @I18NMessage("Failed to create updater marker file [{0}] - will continue but agent startup may fail.  If so, restart agent. Cause. {1}")
    String UPDATING_PLUGINS_MARKER_CREATE_FAILURE = "PluginUpdate.marker-create-failure";

    @I18NMessage("Failed to delete updater marker file [{0}] - will continue but agent startup may fail.  If so, delete the file manually.")
    String UPDATING_PLUGINS_MARKER_DELETE_FAILURE = "PluginUpdate.marker-delete-failure";

    @I18NMessage("The plugin [{0}] is current and does not need to be updated.")
    String PLUGIN_ALREADY_AT_LATEST = "PluginUpdate.already-at-latest";

    @I18NMessage("The plugin [{0}] does not exist on the Server - renaming it to [{1}] so it will not get deployed by the Plugin Container.")
    String PLUGIN_NOT_ON_SERVER = "PluginUpdate.plugin-not-on-server";

    @I18NMessage("Failed to rename illegitimate plugin [{0}] to [{1}].")
    String PLUGIN_RENAME_FAILED = "PluginUpdate.plugin-rename-failed";

    @I18NMessage("The plugin [{0}] does not yet exist - will retrieve the latest version.")
    String NEED_MISSING_PLUGIN = "PluginUpdate.need-missing-plugin";

    @I18NMessage("The plugin [{0}] exists but is old and will be retrieved and updated to the latest version. Current MD5 is [{1}] and the latest MD5 is [{2}]")
    String PLUGIN_NEEDS_TO_BE_UPDATED = "PluginUpdate.need-to-update";

    @I18NMessage("Downloading the plugin [{0}]...")
    String DOWNLOADING_PLUGIN = "PluginUpdate.downloading";

    @I18NMessage("Failed to backup the old plugin [{0}] to [{1}] - if the plugin download fails, the plugin will not be able to be restored")
    String PLUGIN_BACKUP_FAILURE = "PluginUpdate.backup-failure";

    @I18NMessage("Failed to download the plugin [{0}]")
    String DOWNLOAD_PLUGIN_FAILURE = "PluginUpdate.download-failure";

    @I18NMessage("The plugin [{0}] has been updated.")
    String DOWNLOADING_PLUGIN_COMPLETE = "PluginUpdate.downloading-complete";

    @I18NMessage("All plugins are already up-to-date.")
    String UPDATING_PLUGINS_ALREADY_UPTODATE = "PluginUpdate.already-uptodate";

    @I18NMessage("Failed to restore the plugin backup - the original plugin and its functionality is lost! [{0}]->[{1}]")
    String PLUGIN_BACKUP_RESTORE_FAILURE = "PluginUpdate.backup-restore-failure";

    @I18NMessage("A new security token has been persisted: {0}")
    String NEW_SECURITY_TOKEN = "SecurityTokenCommandPreprocessor.new-security-token";

    @I18NMessage("There is no security token yet - the server will not accept commands from this agent until the agent is registered.")
    String NO_SECURITY_TOKEN_YET = "SecurityTokenCommandPreprocessor.no-security-token-yet";

    @I18NMessage("Agent will now attempt to register with the server [{0}]")
    String AGENT_REGISTRATION_ATTEMPT = "AgentMain.agent-registration-attempt";

    @I18NMessage("Agent has successfully registered with the server. The results are: [{0}]")
    String AGENT_REGISTRATION_RESULTS = "AgentMain.agent-registration-results";

    @I18NMessage("Agent failed to register with the server. retry=[{0}], retry interval=[{1}]. Cause: {2}")
    String AGENT_REGISTRATION_FAILURE = "AgentMain.agent-registration-failure";

    @I18NMessage("Agent registered with the server but failed its postprocessing. Registration=[{1}]. Cause: {2}")
    String AGENT_POSTREGISTRATION_FAILURE = "AgentMain.agent-postregistration-failure";

    @I18NMessage("Aborting the server registration attempt.")
    String AGENT_REGISTRATION_ABORTED = "AgentMain.agent-registration-aborted";

    @I18NMessage("The server has rejected the agent registration request. Cause: [{0}]")
    String AGENT_REGISTRATION_REJECTED = "AgentMain.agent-registration-rejected";

    @I18NMessage("started")
    String PROMPT_STRING_STARTED = "AgentMain.prompt-string.started";

    @I18NMessage("shutdown")
    String PROMPT_STRING_SHUTDOWN = "AgentMain.prompt-string.shutdown";

    @I18NMessage("sending")
    String PROMPT_STRING_SENDING = "AgentMain.prompt-string.sending";

    @I18NMessage("The Agent has auto-detected the Server coming online [{0}] - the agent will be able to start sending messages now")
    String SERVER_ONLINE = "AgentAutoDiscoveryListener.server-online";

    @I18NMessage("The Agent has auto-detected the Server going offline [{0}] - the agent will stop sending new messages")
    String SERVER_OFFLINE = "AgentAutoDiscoveryListener.server-offline";

    @I18NMessage("Failed to determine the actual identity of the server. This is normally due to the server not being up yet. You can usually ignore this message since it will be tried again later, however, you should ensure this failure was not really caused by a misconfiguration. Cause: {0}")
    String SERVER_ID_FAILURE = "AgentAutoDiscoveryListener.server-id-failure";

    @I18NMessage("The configured server locator URI is invalid [{0}]")
    String INVALID_LOCATOR_URI = "AgentAutoDiscoveryListener.invalid-locator-uri";

    @I18NMessage("Agent configuration preferences must not be null")
    String PREFS_MUST_NOT_BE_NULL = "AgentConfiguration.prefs-must-not-be-null";

    @I18NMessage("The [{0}] preference value specified is invalid [{1}] - it must be greater than 0; will use the default of [{2}]")
    String PREF_MUST_BE_GREATER_THAN_0 = "AgentConfiguration.pref-must-be-greater-than-0";

    @I18NMessage("Command spool file maximum size must be equal to or greater than 10000")
    String COMMAND_SPOOL_INVALID_MAX_SIZE = "AgentConfiguration.command-spool-invalid-max-size";

    @I18NMessage("Command spool file purge percentage must be between 0 and 99")
    String COMMAND_SPOOL_INVALID_PURGE_PERCENTAGE = "AgentConfiguration.command-spool-invalid-purge-percentage";

    @I18NMessage("Command spool file params format does not specify the parameters in the proper format")
    String COMMAND_SPOOL_INVALID_FORMAT = "AgentConfiguration.command-spool-invalid-format";

    @I18NMessage("The [{0}] preference value specified is invalid [{1}] - it must be in the form \"max-file-size:purge-percentage\". Cause: [{2}]")
    String BAD_COMMAND_SPOOL_PREF = "AgentConfiguration.bad-command-spool-pref";

    @I18NMessage("Send throttling max commands must be larger than 0")
    String SEND_THROTTLE_INVALID_MAX = "AgentConfiguration.send-throttle-invalid-max";

    @I18NMessage("Send throttling quiet period must be equal to or greater than [{0}]")
    String SEND_THROTTLE_INVALID_QUIET_PERIOD = "AgentConfiguration.send-throttle-invalid-quiet-period";

    @I18NMessage("Send throttling format does not specify the throttling parameters in the proper format")
    String SEND_THROTTLE_INVALID_FORMAT = "AgentConfiguration.send-throttle-invalid-format";

    @I18NMessage("The [{0}] preference value specified is invalid [{1}] - it must be in the form \"max-commands:quiet-period-milliseconds\". Send throttling configuration will be disabled. Cause: [{2}]")
    String BAD_SEND_THROTTLE_PREF = "AgentConfiguration.bad-send-throttle-pref";

    @I18NMessage("Queue throttling max commands must be larger than 0")
    String QUEUE_THROTTLE_INVALID_MAX = "AgentConfiguration.queue-throttle-invalid-max";

    @I18NMessage("Queue throttling burst period must be equal to or greater than [{0}]")
    String QUEUE_THROTTLE_INVALID_BURST_PERIOD = "AgentConfiguration.queue-throttle-invalid-burst-period";

    @I18NMessage("Queue throttling format does not specify the throttling parameters in the proper format")
    String QUEUE_THROTTLE_INVALID_FORMAT = "AgentConfiguration.queue-throttle-invalid-format";

    @I18NMessage("The [{0}] preference value specified is invalid [{1}] - it must be in the form \"max-commands-per-burst:burst-period-milliseconds\". Queue throttling configuration will be disabled. Cause: [{2}]")
    String BAD_QUEUE_THROTTLE_PREF = "AgentConfiguration.bad-queue-throttle-pref";

    @I18NMessage("<cannot get preferences: {0}>")
    String CANNOT_GET_PREFERENCES = "AgentConfiguration.cannot-get-preferences";

    @I18NMessage("Failed to store preference key [{0}] : {1}")
    String CANNOT_STORE_PREFERENCES = "AgentConfiguration.cannot-store-preferences";

    @I18NMessage("<unknown>")
    String UNKNOWN = "AgentConfiguration.unknown";

    @I18NMessage("Failed to start the agent")
    String AGENT_START_FAILURE = "AgentMain.start-failure";

    @I18NMessage("Agent being created now")
    String CREATING_AGENT = "AgentMain.creating-agent";

    @I18NMessage("The plugin container has been initialized with the following configuration: {0}")
    String PLUGIN_CONTAINER_INITIALIZED = "AgentMain.plugin-container-initialized";

    @I18NMessage("The plugin container initialization was interrupted - it will not be started.")
    String PLUGIN_CONTAINER_INITIALIZATION_INTERRUPTED = "AgentMain.plugin-container-initialization-interrupted";

    @I18NMessage("There are no plugins and the agent was told not to update them at startup.\\n\\\n"
        + "The agent will continue but it will not start the plugin container.")
    String NO_PLUGINS = "AgentMain.no-plugins";

    @I18NMessage("Creating server service client end points.")
    String CREATING_PLUGIN_CONTAINER_SERVER_SERVICES = "AgentMain.creating-plugin-container-server-services";

    @I18NMessage("Failed to create server service client end points. Cause: {0}")
    String FAILED_TO_CREATE_PLUGIN_CONTAINER_SERVER_SERVICES = "AgentMain.failed-to-create-plugin-container-server-services";

    @I18NMessage("The plugin container has been shutdown")
    String PLUGIN_CONTAINER_SHUTDOWN = "AgentMain.plugin-container-shutdown";

    @I18NMessage("Server auto-detection is enabled; listening for server at [{0}]")
    String SERVER_AUTO_DETECT_ENABLED = "AgentMain.server-auto-detect-enabled";

    @I18NMessage("The agent has been configured with server auto-detection enabled; however, the multicast detector communications service has been disabled. Auto-detection needs that multicast detector in order to work. Auto-detection will, therefore, be disabled.")
    String WEIRD_AUTO_DETECT_CONFIG = "AgentMain.weird-auto-detect-config";

    @I18NMessage("Neither server auto-detection nor polling is enabled - the client will be allowed to start sending commands immediately, but when the server is offline, be prepared for alot of errors to be logged")
    String NO_AUTO_DETECT = "AgentMain.no-auto-detect";

    @I18NMessage("The agent encountered an error during startup and must abort")
    String STARTUP_ERROR = "AgentMain.startup-error";

    @I18NMessage("Agent is being shut down...")
    String SHUTTING_DOWN = "AgentMain.shutting-down";

    @I18NMessage("Agent has been shut down")
    String AGENT_SHUTDOWN = "AgentMain.shut-down";

    @I18NMessage("Agent failed to notify the server of the pending shutdown. Cause: {0}")
    String FAILED_TO_NOTIFY_SERVER_OF_SHUTDOWN = "AgentMain.server-shutdown-notification-failure";

    @I18NMessage("Agent is notifying the server that it is shutting down.")
    String NOTIFYING_SERVER_OF_SHUTDOWN = "AgentMain.notifying-server-of-shutdown";

    @I18NMessage("Agent is not notifying the server that it is shutting down because the server appears to be down.")
    String NOT_NOTIFYING_SERVER_OF_SHUTDOWN = "AgentMain.not-notifying-server-of-shutdown";

    @I18NMessage("Cannot find the configuration file [{0}]")
    String CANNOT_FIND_CONFIG_FILE = "AgentMain.cannot-find-config-file";

    @I18NMessage("Loading configuration file [{0}]")
    String LOADING_CONFIG_FILE = "AgentMain.loading-config-file";

    @I18NMessage("The configuration file [{0}] does not have preferences under the node name [{1}]; use the -p option or a different config file")
    String BAD_NODE_NAME_IN_CONFIG_FILE = "AgentMain.bad-node-name-in-config-file";

    @I18NMessage("Configuration file loaded [{0}]")
    String LOADED_CONFIG_FILE = "AgentMain.loaded-config-file";

    @I18NMessage("Overlay configuration preferences with system property [{0}]=[{1}]")
    String OVERLAY_SYSPROP = "AgentMain.overlay-sysprop";

    @I18NMessage("Done.")
    String INPUT_DONE = "AgentMain.input-done";

    @I18NMessage("Prompt command invoked: {0}")
    String PROMPT_COMMAND_INVOKED = "AgentMain.prompt-command-invoked";

    @I18NMessage("Unknown command: {0}")
    String UNKNOWN_COMMAND = "AgentMain.unknown-command";

    @I18NMessage("Failed to execute prompt command [{0}]. Cause: {1}")
    String COMMAND_FAILURE = "AgentMain.command-failure";

    @I18NMessage("Command failure stack trace follows:")
    String COMMAND_FAILURE_STACK_TRACE = "AgentMain.command-failure-stack-trace";

    @I18NMessage("RHQ Agent\\n\\\n"
        + "\\n\\\n"
        + "Usage: {0} [options]\\n\\\n"
        + "\\n\\\n"
        + "options:\\n\\\n"
        + "\\   -a, --advanced                If setup is needed at startup, the advanced setup is run, rather than the basic\\n\\\n"
        + "\\   -c, --config=<filename>       Specifies an agent configuration preferences file (on filesystem or classpath)\\n\\\n"
        + "\\   -d, --daemon                  Agent runs in daemon mode - will not read from stdin for commands\\n\\\n"
        + "\\   -D<name>[=<value>]            Overrides an agent configuration preference and sets a system property\\n\\\n"
        + "\\   -h, --help                    Shows this help message (default)\\n\\\n"
        + "\\   -i, --input=<filename>        Specifies a script file to be used for input\\n\\\n"
        + "\\   -l, --cleanconfig             Clears out any existing configuration and data files so the agent starts with a totally clean slate\\n\\\n"
        + "\\   -n, --nostart                 If specified, the agent will not be automatically started\\n\\\n"
        + "\\   -o, --output=<filename>       Specifies a file to write all output (excluding log messages)\\n\\\n"
        + "\\   -p, --pref=<preferences name> Specifies the agent preferences name used to identify what configuration to use\\n\\\n"
        + "\\   -s, --setup                   Forces the agent to ask setup questions, even if it is fully configured\\n\\\n"
        + "\\   -t, --nonative                Forces the agent to disable the native system, even if it is configured for it\\n\\\n"
        + "\\   -u, --purgedata               Purges persistent inventory and other data files\\n\\\n"
        + "\\   --                            Stop processing options\\n")
    String USAGE = "AgentMain.usage";

    @I18NMessage("Bad arguments")
    String BAD_ARGS = "AgentMain.bad-args";

    @I18NMessage("Unused option: {0}")
    String UNUSED_OPTION = "AgentMain.unused-option";

    @I18NMessage("Help has been displayed - agent will not be created")
    String HELP_SHOWN = "AgentMain.help-shown";

    @I18NMessage("Set system property: {0}={1}")
    String SYSPROP_SET = "AgentMain.sysprop-set";

    @I18NMessage("The agent preference node name cannot have slash (\"/\") characters [{0}]")
    String NO_SLASHES_ALLOWED = "AgentMain.no-slashes-allowed";

    @I18NMessage("Failed to access the input file [{0}]. Cause: {1}")
    String BAD_INPUT_FILE = "AgentMain.bad-input-file";

    @I18NMessage("Failed to access the output file [{0}]. Cause: {1}")
    String BAD_OUTPUT_FILE = "AgentMain.bad-output-file";

    @I18NMessage("Failed to load configuration file [{0}]. Cause: {1}")
    String LOAD_CONFIG_FILE_FAILURE = "AgentMain.load-config-file-failure";

    @I18NMessage("Agent container has processed its command line arguments: {0}")
    String ARGS_PROCESSED = "AgentMain.args-processed";

    @I18NMessage("Found preferences already loaded in persisted store; using those for the configuration")
    String PREFERENCES_ALREADY_EXIST = "AgentMain.preferences-already-exist";

    @I18NMessage("Preferences configuration schema: [{0}]")
    String PREFERENCES_SCHEMA = "AgentMain.preferences-schema";

    @I18NMessage("Preferences node path where configuration is persisted: [{0}]")
    String PREFERENCES_NODE_PATH = "AgentMain.preferences-node-path";

    @I18NMessage("Agent configuration: {0}")
    String CONFIGURATION = "AgentMain.configuration";

    @I18NMessage("help")
    String HELP = "PromptCommand.help";

    @I18NMessage("help [command]")
    String HELP_SYNTAX = "PromptCommand.help.syntax";

    @I18NMessage("Shows help for a given command")
    String HELP_HELP = "PromptCommand.help.help";

    @I18NMessage("Provides the list of prompt commands that can be issued.\\n\\\n"
        + "If you specify a particular help command, this will show a\\n\\\n"
        + "detailed help message for that specific command.")
    String HELP_DETAILED_HELP = "PromptCommand.help.detailed-help";

    @I18NMessage("Syntax: {0}")
    String HELP_SYNTAX_LABEL = "PromptCommand.help.syntax-label";

    @I18NMessage("Cannot get help for unknown prompt command [{0}]")
    String HELP_UNKNOWN = "PromptCommand.help.unknown";

    @I18NMessage("log")
    String LOG = "PromptCommand.log";

    @I18NMessage("log [locale <language[_country[_variant]]> | dumpstacks <true|false> | dumpkeys <true|false>]")
    String LOG_SYNTAX = "PromptCommand.log.syntax";

    @I18NMessage("Configures some settings for the log messages")
    String LOG_HELP = "PromptCommand.log.help";

    @I18NMessage("Provides the ability to set some log message settings.\\n\\\n"
        + "If you do not specify any argument to the log command, it\\n\\\n"
        + "dumps the current settings. Not all log messages will\\n\\\n"
        + "be affected; this mostly affects the core agent/comm logs.\\n\\\n" + "\\n\\\n"
        + "dumpstacks: if true, stack traces are dumped at all log levels\\n\\\n"
        + "\\            when exceptions are logged.  If false, only stacks\\n\\\n"
        + "\\            are dumped if the exception is logged at the\\n\\\n" + "\\            fatal log level.\\n\\\n"
        + "\\  dumpkeys: if true, the log messages will be prefixed with their\\n\\\n"
        + "\\            associated resource bundle key codes.  Useful when you\\n\\\n"
        + "\\            need to send log files to support.\\n\\\n"
        + "\\    locale: allows you to change the locale of the log messages;\\n\\\n"
        + "\\            language is required but country and variant are optional\\n\\\n"
        + "\\            (for example: en or de_DE or fr_FR_EURO)")
    String LOG_DETAILED_HELP = "PromptCommand.log.detailed-help";

    @I18NMessage("locale")
    String LOG_LOCALE = "PromptCommand.log.locale";

    @I18NMessage("dumpstacks")
    String LOG_DUMPSTACKS = "PromptCommand.log.dumpstacks";

    @I18NMessage("dumpkeys")
    String LOG_DUMPKEYS = "PromptCommand.log.dumpkeys";

    @I18NMessage("true")
    String LOG_TRUE = "PromptCommand.log.true";

    @I18NMessage("Current log settings are:\\n\\\n" + "Log locale={0}\\n\\\n" + "Dump all stack traces={1}\\n\\\n"
        + "Dump message key codes={2}\\n")
    String LOG_SHOW_CURRENT_SETTINGS = "PromptCommand.log.show-current-settings";

    @I18NMessage("Failed to perform log command; stack trace follows:")
    String LOG_FAILURE = "PromptCommand.log.failure";

    @I18NMessage("sender")
    String SENDER = "PromptCommand.sender";

    @I18NMessage("sender <start | stop | metrics | status >")
    String SENDER_SYNTAX = "PromptCommand.sender.syntax";

    @I18NMessage("start")
    String SENDER_START = "PromptCommand.sender.start";

    @I18NMessage("stop")
    String SENDER_STOP = "PromptCommand.sender.stop";

    @I18NMessage("metrics")
    String SENDER_METRICS = "PromptCommand.sender.metrics";

    @I18NMessage("status")
    String SENDER_STATUS = "PromptCommand.sender.status";

    @I18NMessage("Controls the command sender to start or stop sending commands")
    String SENDER_HELP = "PromptCommand.sender.help";

    @I18NMessage("This forcibly tells the command sender to start or stop sending commands.\\n\\\n"
        + "Auto-detection or server polling will automatically start the command\\n\\\n"
        + "sender when they detect the RHQ Server has come online.  They will\\n\\\n"
        + "also automatically stop the command sender from sending commands when\\n\\\n"
        + "they detect the RHQ Server has gone offline.  If you have disabled\\n\\\n"
        + "auto-detection and server polling or if they are configured but for\\n\\\n"
        + "some reason cannot detect the RHQ Server's state, this prompt command\\n\\\n"
        + "can be used to manually tell the command sender what to do.\\n\\\n"
        + "The metrics and status options do not alter the state of the sender;\\n\\\n"
        + "they just report information on the current state of the sender.")
    String SENDER_DETAILED_HELP = "PromptCommand.sender.detailed-help";

    @I18NMessage("The agent is shutdown - you cannot get or change the sender's status.")
    String SENDER_AGENT_NOT_STARTED = "PromptCommand.sender.agent-not-started";

    @I18NMessage("The metric data for the command sender are as follows:\\n{0}")
    String SENDER_METRICS_OUTPUT = "PromptCommand.sender.metrics-output";

    @I18NMessage("The command sender will now be started...")
    String SENDER_STARTING = "PromptCommand.sender.starting";

    @I18NMessage("The command sender will now be stopped - please note that any commands\\n\\\n"
        + "currently waiting in the queue will be sent first. Depending on the number\\n\\\n"
        + "of commands waiting in the queue, this may or may not take awhile...")
    String SENDER_STOPPING = "PromptCommand.sender.stopping";

    @I18NMessage("The command sender has been started - it is in sending mode.")
    String SENDER_IS_SENDING = "PromptCommand.sender.is-sending";

    @I18NMessage("The command sender has been stopped - it is NOT in sending mode.")
    String SENDER_IS_NOT_SENDING = "PromptCommand.sender.is-not-sending";

    @I18NMessage("The sender is pointing to the server endpoint of [{0}]")
    String SENDER_SERVER_ENDPOINT = "PromptCommand.sender.server-endpoint";

    @I18NMessage("The configured server endpoint is [{0}]")
    String SENDER_SERVER_ENDPOINT_CONFIG = "PromptCommand.sender.server-endpoint-config";

    @I18NMessage("Failed to perform sender command; stack trace follows:")
    String SENDER_FAILURE = "PromptCommand.sender.failure";

    @I18NMessage("exit")
    String EXIT = "PromptCommand.exit";

    @I18NMessage("Shuts down the agent's communications services and kills the agent")
    String EXIT_HELP = "PromptCommand.exit.help";

    @I18NMessage("Shutting down...")
    String EXIT_SHUTTING_DOWN = "PromptCommand.exit.shutting-down";

    @I18NMessage("Shutdown complete. Exiting...")
    String EXIT_SHUTDOWN_COMPLETE = "PromptCommand.exit.shutdown-complete";

    @I18NMessage("An error occurred during shutdown. Cause: {0}")
    String EXIT_SHUTDOWN_ERROR = "PromptCommand.exit.shutdown-error";

    @I18NMessage("failover")
    String FAILOVER = "PromptCommand.failover";

    @I18NMessage("failover -c | -l | -r")
    String FAILOVER_SYNTAX = "PromptCommand.failover.syntax";

    @I18NMessage("Provides HA failover functionality")
    String FAILOVER_HELP = "PromptCommand.failover.help";

    @I18NMessage("Provides HA failover functionality.\\n\\\n"
        + "-c|--check: Checks to see if the agent is connected to its primary server,\\n\\\n"
        + "\\            as opposed to one of its secondary failover servers. If not,\\n\\\n"
        + "\\            will attempt to switchover to primary.\\n\\\n"
        + "-l|--list:  Will show the server failover list.\\n\\\n"
        + "-r|--reset: Will reset the failover list index such that the next server to\\n\\\n"
        + "\\            be failed over to (when necessary) will be the first server\\n\\\n"
        + "\\            in the failover list.")
    String FAILOVER_DETAILED_HELP = "PromptCommand.failover.detailed-help";

    @I18NMessage("The failover list index has been reset to the top. The next time\\n\\\n"
        + "the agent needs to failover to a different server, it will try\\n\\\n"
        + "the server at the front of the list.")
    String FAILOVER_RESET_DONE = "PromptCommand.failover.reset-done";

    @I18NMessage("The agent will perform its primary server switchover check now.\\n\\\n"
        + "If it is not connected to its primary server, it will attempt\\n\\\n"
        + "to switch over now.  Use the <sender> prompt command to see\\n\\\n"
        + "which server the agent is connected to.")
    String FAILOVER_CHECK_NOW = "PromptCommand.failover.check-now";

    @I18NMessage("quit")
    String QUIT = "PromptCommand.quit";

    @I18NMessage("version")
    String VERSION = "PromptCommand.version";

    @I18NMessage("version [sysprops]")
    String VERSION_SYNTAX = "PromptCommand.version.syntax";

    @I18NMessage("Shows agent version information")
    String VERSION_HELP = "PromptCommand.version.help";

    @I18NMessage("sysprops")
    String VERSION_SYSPROPS = "PromptCommand.version.sysprops";

    @I18NMessage("System Properties:")
    String VERSION_SYSPROPS_LABEL = "PromptCommand.version.sysprops-label";

    @I18NMessage("identify")
    String IDENTIFY = "PromptCommand.identify";

    @I18NMessage("identify [endpoint-locator-URI]")
    String IDENTIFY_SYNTAX = "PromptCommand.identify.syntax";

    @I18NMessage("Asks to identify a remote server")
    String IDENTIFY_HELP = "PromptCommand.identify.help";

    @I18NMessage("Asking for identification of RHQ Server...")
    String IDENTIFY_ASK_SERVER_FOR_ID = "PromptCommand.identify.ask-server-for-id";

    @I18NMessage("Asking for identification of remote server at [{0}]")
    String IDENTIFY_ASK_REMOTE_SERVER_FOR_ID = "PromptCommand.identify.ask-remote-server-for-id";

    @I18NMessage("You specified an invalid locator URI [{0}]")
    String IDENTIFY_INVALID_LOCATOR_URI = "PromptCommand.identify.invalid-locator-uri";

    @I18NMessage("An error occurred asking the remote endpoint for identification")
    String IDENTIFY_REMOTE_FAILURE = "PromptCommand.identify.remote-failure";

    @I18NMessage("The agent is not able to send data to the RHQ Server right now.")
    String IDENTIFY_NOT_SENDING = "PromptCommand.identify.not-sending";

    @I18NMessage("This will send a request-for-identification message to a remote server.\\n\\\n"
        + "If you do not specify an endpoint locator URI, this will\\n\\\n"
        + "send the command to the RHQ Server.  If you wish to identify\\n\\\n"
        + "a different endpoint, you can specify it as an argument to this command.")
    String IDENTIFY_DETAILED_HELP = "PromptCommand.identify.detailed-help";

    @I18NMessage("config")
    String CONFIG = "PromptCommand.config";

    @I18NMessage("config list | import <file.xml> | export <file.xml>")
    String CONFIG_SYNTAX = "PromptCommand.config.syntax";

    @I18NMessage("Manages the agent configuration")
    String CONFIG_HELP = "PromptCommand.config.help";

    @I18NMessage("list")
    String CONFIG_LIST = "PromptCommand.config.list";

    @I18NMessage("import")
    String CONFIG_IMPORT = "PromptCommand.config.import";

    @I18NMessage("Agent configuration has been imported and overlayed on top of current configuration: ")
    String CONFIG_IMPORT_CONFIG_IMPORTED = "PromptCommand.config.import.config-imported";

    @I18NMessage("export")
    String CONFIG_EXPORT = "PromptCommand.config.export";

    @I18NMessage("Agent configuration has been exported to: ")
    String CONFIG_EXPORT_CONFIG_EXPORTED = "PromptCommand.config.export.config-exported";

    @I18NMessage("Failed to perform config command; stack trace follows:")
    String CONFIG_FAILURE = "PromptCommand.config.failure";

    @I18NMessage("\\  list: Allows you to list the configuration preferences in XML format\\n\\\n"
        + "import: Imports a configuration file containing the new preferences\\n\\\n"
        + "export: Exports the current configuration to a file in XML format")
    String CONFIG_DETAILED_HELP = "PromptCommand.config.detailed-help";

    @I18NMessage("getconfig")
    String GETCONFIG = "PromptCommand.getconfig";

    @I18NMessage("getconfig [preference name]...")
    String GETCONFIG_SYNTAX = "PromptCommand.getconfig.syntax";

    @I18NMessage("Cannot get preferences: {0}")
    String GETCONFIG_CANNOT_GET = "PromptCommand.getconfig.cannot-get";

    @I18NMessage("<unknown>")
    String GETCONFIG_UNKNOWN_VALUE = "PromptCommand.getconfig.unknown-value";

    @I18NMessage("Displays one, several or all agent configuration preferences")
    String GETCONFIG_HELP = "PromptCommand.getconfig.help";

    @I18NMessage("If no arguments are given to this command, all preferences that have\\n\\\n"
        + "been explicitly set are listed in name/value pair format. Note that\\n\\\n"
        + "undefined preferences (i.e. those not explictly set) will not be listed, in\\n\\\n"
        + "which case an internally defined default will be used. You can provide names\\n\\\n"
        + "of preferences to this command if you only wish to see specific ones.")
    String GETCONFIG_DETAILED_HELP = "PromptCommand.getconfig.detailed-help";

    @I18NMessage("ping")
    String PING = "PromptCommand.ping";

    @I18NMessage("guaranteed")
    String PING_GUARANTEED = "PromptCommand.ping.guaranteed";

    @I18NMessage("You cannot guarantee synchronous commands - only asynchronous commands can be guaranteed.\\n\\\n"
        + "Set rhq.agent.test.blast-count to something greater than 1 for asynchronous pinging.")
    String PING_GUARANTEED_FOR_ASYNC_ONLY = "PromptCommand.ping.guaranteed-for-async-only";

    @I18NMessage("Pinging...")
    String PING_PINGING = "PromptCommand.ping.pinging";

    @I18NMessage("Ping results: [{0}]")
    String PING_PING_RESULTS = "PromptCommand.ping.ping-results";

    @I18NMessage("{0,date,short} {0,time,short}: Ping results: [{1}]")
    String PING_TIMESTAMPED_PING_RESULTS = "PromptCommand.ping.timestamped-ping-results";

    @I18NMessage("Ping failed: [{0}]")
    String PING_PING_FAILED = "PromptCommand.ping.ping-failed";

    @I18NMessage("Making asynchronous ping call #{0} - guaranteed flag is [{1}]")
    String PING_ASYNC_PING = "PromptCommand.ping.async-ping";

    @I18NMessage("The agent is shutdown - you cannot send ping messages until you start the agent.")
    String PING_AGENT_NOT_STARTED = "PromptCommand.ping.not-started";

    @I18NMessage("ping [guaranteed]")
    String PING_SYNTAX = "PromptCommand.ping.syntax";

    @I18NMessage("Pings the RHQ Server")
    String PING_HELP = "PromptCommand.ping.help";

    @I18NMessage("This sends a ping request to the RHQ Server.\\n\\\n"
        + "As a testing mechanism, you can send multiple ping requests\\n\\\n"
        + "asynchronously by setting the \"rhq.agent.test.blast-count\" preference\\n\\\n"
        + "to the number of times you want to send the ping request.")
    String PING_DETAILED_HELP = "PromptCommand.ping.detailed-help";

    @I18NMessage("piql")
    String PIQL = "PromptCommand.piql";

    @I18NMessage("piql [verbose] <query>")
    String PIQL_SYNTAX = "PromptCommand.piql.syntax";

    @I18NMessage("Executes a PIQL query to search for running processes")
    String PIQL_HELP = "PromptCommand.piql.help";

    @I18NMessage("This will probe the operating system's process table and query\\n\\\n"
        + "the running processess to see which ones match the PIQL query string.\\n\\\n"
        + "A valid PIQL query string has the format:\\n\\\n" + "\\   <category>|<attribute>|<operator>=<value>\\n\\\n"
        + "where:\\n\\\n" + "\\   <category> is one of: process, arg\\n\\\n"
        + "\\   <attribute> is name, basename, pidfile or an actual process arg name/number\\n\\\n"
        + "\\   <operation> is one of: match, nomatch\\n\\\n"
        + "Please see the PIQL documentation for more information.")
    String PIQL_DETAILED_HELP = "PromptCommand.piql.detailed-help";

    @I18NMessage("verbose")
    String PIQL_VERBOSE = "PromptCommand.piql.verbose";

    @I18NMessage("Your platform/operating system is not supported. You cannot run PIQL queries.\\n\\\n({0})")
    String PIQL_NO_NATIVE_SUPPORT = "PromptCommand.piql.no-native-support";

    @I18NMessage("Could not execute the PIQL query.  The exception message is: {0}")
    String PIQL_FAILURE = "PromptCommand.piql.failure";

    @I18NMessage("PIQL Query: [{0}]\\n\\\n" + "Found the following matching processes:\\n\\\n")
    String PIQL_RESULTS_HEADER = "PromptCommand.piql.results-header";

    @I18NMessage("pid={0,number,#}, ppid={1,number,#}, name={2}, created={3,date,short}, kernel-time={4,number,#}, user-time={5,number,#}, command-line={6}")
    String PIQL_RESULTS_FULL = "PromptCommand.piql.results-full";

    @I18NMessage("{0,number,#} {1}")
    String PIQL_RESULTS_SHORT = "PromptCommand.piql.results-short";

    @I18NMessage("remote")
    String REMOTE = "PromptCommand.remote";

    @I18NMessage("remote <command and arguments to send to remote server>")
    String REMOTE_SYNTAX = "PromptCommand.remote.syntax";

    @I18NMessage("Issuing command...")
    String REMOTE_ISSUING = "PromptCommand.remote.issuing";

    @I18NMessage("Failed to issue remote command")
    String REMOTE_FAILURE = "PromptCommand.remote.failure";

    @I18NMessage("Executes a command on a remote server")
    String REMOTE_HELP = "PromptCommand.remote.help";

    @I18NMessage("This sends a generic command to any remote server for execution.\\n\\\n"
        + "This uses the CmdlineClient to parse and send the command.  Its usage is as follows:\\n\\n\\\n" + "{0}")
    String REMOTE_DETAILED_HELP = "PromptCommand.remote.detailed-help";

    @I18NMessage("server")
    String SERVER = "PromptCommand.server";

    @I18NMessage("remote <command and arguments to send to RHQ Server>")
    String SERVER_SYNTAX = "PromptCommand.server.syntax";

    @I18NMessage("Sending command to RHQ Server...")
    String SERVER_SENDING = "PromptCommand.server.sending";

    @I18NMessage("Failed to issue remote command")
    String SERVER_FAILURE = "PromptCommand.server.failure";

    @I18NMessage("Sends a command to the RHQ Server")
    String SERVER_HELP = "PromptCommand.server.help";

    @I18NMessage("This sends a generic command to the RHQ Server for execution.\\n\\\n"
        + "This uses the CmdlineClient to parse the command.\\n\\\n" + "Its usage is as follows:\\n\\n\\\n" + "{0}")
    String SERVER_DETAILED_HELP = "PromptCommand.server.detailed-help";

    @I18NMessage("start")
    String START = "PromptCommand.start";

    @I18NMessage("Starting the agent...")
    String START_STARTING = "PromptCommand.start.starting";

    @I18NMessage("Started the agent successfully.")
    String START_STARTED = "PromptCommand.start.started";

    @I18NMessage("Agent is already started.")
    String START_ALREADY_STARTED = "PromptCommand.start.already-started";

    @I18NMessage("Failed to start the agent")
    String START_FAILURE = "PromptCommand.start.failure";

    @I18NMessage("Starts the agent comm services so it can accept remote requests")
    String START_HELP = "PromptCommand.start.help";

    @I18NMessage("Starts the agent comm services, plugin container and all\\n\\\n"
        + "other internal services necessary to bring the agent fully\\n\\\n"
        + "up and ready to manage resources.  Note specifically that\\n\\\n"
        + "this command will automatically start your plugin container\\n\\\n"
        + "and sender object even if you explicitly stopped them earlier\\n\\\n"
        + "via the pc or sender command. In other words, when this command\\n\\\n"
        + "is executed, all internal services will be started for you,\\n\\\n" + "regardless of their past state.")
    String START_DETAILED_HELP = "PromptCommand.start.detailed-help";

    @I18NMessage("shutdown")
    String SHUTDOWN = "PromptCommand.shutdown";

    @I18NMessage("Shutting down...")
    String SHUTDOWN_SHUTTING_DOWN = "PromptCommand.shutdown.shutting-down";

    @I18NMessage("Shutdown complete.")
    String SHUTDOWN_DONE = "PromptCommand.shutdown.done";

    @I18NMessage("Agent is already shutdown.")
    String SHUTDOWN_ALREADY_SHUTDOWN = "PromptCommand.shutdown.already-shutdown";

    @I18NMessage("Shuts down all communications services without killing the agent")
    String SHUTDOWN_HELP = "PromptCommand.shutdown.help";

    @I18NMessage("Instructs the agent to shutdown the communications services\\n\\\n"
        + "but does not kill the agent. After this command executes,\\n\\\n"
        + "the agent cannot send communications out or receive communications in.")
    String SHUTDOWN_DETAILED_HELP = "PromptCommand.shutdown.detailed-help";

    @I18NMessage("setup")
    String SETUP = "PromptCommand.setup";

    @I18NMessage("advanced")
    String SETUP_ADVANCED = "PromptCommand.setup.advanced";

    @I18NMessage("all")
    String SETUP_ALL = "PromptCommand.setup.all";

    @I18NMessage("setup [advanced | all]")
    String SETUP_SYNTAX = "PromptCommand.setup.prefix";

    @I18NMessage("Sets up the agent configuration by asking a series of questions")
    String SETUP_HELP = "PromptCommand.setup.help";

    @I18NMessage("Answer the following questions to setup this RHQ Agent instance.")
    String SETUP_INTRO = "PromptCommand.setup.intro";

    @I18NMessage("** Advanced Setup **\\n\\\n"
        + "Answer the following questions to setup this RHQ Agent instance.\\n\\\n"
        + "This will ask for basic and secondary configuration preferences\\n\\\n"
        + "thus allowing you to fine tune the agent via these advanced settings.\\n\\\n"
        + "Please refer to the help text and documentation if you are not sure\\n\\\n"
        + "what a setting does or what are its appropriate values.")
    String SETUP_INTRO_ADVANCED = "PromptCommand.setup.intro-advanced";

    @I18NMessage("!! Full Advanced Setup !!\\n\\\n"
        + "Answer the following questions to setup this RHQ Agent instance.\\n\\\n"
        + "This will ask for practically all possible configuration preferences\\n\\\n"
        + "thus allowing you to fine tune the agent via every available setting.\\n\\\n"
        + "Please refer to the help text and documentation if you are not sure\\n\\\n"
        + "what a setting does or what are its appropriate values.")
    String SETUP_INTRO_ALL = "PromptCommand.setup.intro-all";

    @I18NMessage("This RHQ Agent instance has started - you cannot setup a started RHQ Agent.\\n\\\n"
        + "Please stop it using the shutdown command before attempting to set it up.")
    String SETUP_MUST_BE_STOPPED = "PromptCommand.setup.must-be-stopped";

    @I18NMessage("[{0}] is invalid.  Valid client authentication modes are: {1}, {2}, {3}.")
    String SETUP_BAD_CLIENT_AUTH_MODE = "PromptCommand.setup.bad-client-auth-mode";

    @I18NMessage("That value [{0}] is invalid.  Please make sure you use the proper format and values for the command spool file parameters.  See the help text for more information.")
    String SETUP_BAD_COMMAND_SPOOL_PARAMS = "PromptCommand.setup.bad-command-spool-params";

    @I18NMessage("That value [{0}] is invalid.  Please make sure you use the proper format and values for the queue throttling parameters.  See the help text for more information.")
    String SETUP_BAD_QUEUE_THROTTLING_PARAMS = "PromptCommand.setup.bad-queue-throttling-params";

    @I18NMessage("That value [{0}] is invalid.  Please make sure you use the proper format and values for the send throttling parameters.  See the help text for more information.")
    String SETUP_BAD_SEND_THROTTLING_PARAMS = "PromptCommand.setup.bad-send-throttling-params";

    @I18NMessage("setconfig")
    String SETCONFIG = "PromptCommand.setconfig";

    @I18NMessage("setconfig <name>[=<value>]")
    String SETCONFIG_SYNTAX = "PromptCommand.setconfig.syntax";

    @I18NMessage("Set preference: {0}={1}")
    String SETCONFIG_SET = "PromptCommand.setconfig.set";

    @I18NMessage("Removed preference: {0}")
    String SETCONFIG_REMOVED = "PromptCommand.setconfig.removed";

    @I18NMessage("Sets an agent configuration preference")
    String SETCONFIG_HELP = "PromptCommand.setconfig.help";

    @I18NMessage("This sets a given named preference to the agent configuration.\\n\\\n"
        + "If just the name is given to this command, this will remove the named\\n\\\n"
        + "preference from the agent configuration.  Note that when adding/removing\\n\\\n"
        + "preferences from the agent configuration, it also will add/remove the\\n\\\n"
        + "same name/value pair to/from the JVM System properties.")
    String SETCONFIG_DETAILED_HELP = "PromptCommand.setconfig.detailed-help";

    @I18NMessage("serverasync")
    String SERVERASYNC = "PromptCommand.serverasync";

    @I18NMessage("serverasync <command and arguments to send to server>")
    String SERVERASYNC_SYNTAX = "PromptCommand.serverasync.syntax";

    @I18NMessage("Command queued: {0}")
    String SERVERASYNC_QUEUED = "PromptCommand.serverasync.queued";

    @I18NMessage("Failed to issue remote command")
    String SERVERASYNC_FAILURE = "PromptCommand.serverasync.failure";

    @I18NMessage("Sends a command asynchronously to the RHQ Server")
    String SERVERASYNC_HELP = "PromptCommand.serverasync.help";

    @I18NMessage("This sends a generic command to the RHQ Server asyncronously for execution.\\n\\\n"
        + "As a testing mechanism, you can send multiple copies of the same\\n\\\n"
        + "command by setting the \"rhq.agent.test.blast-count\" preference\\n\\\n"
        + "to the number of times you want to send the command.\\n\\\n"
        + "This uses the CmdlineClient to parse the command.  Its usage is as follows:\\n\\n\\\n" + "{0}")
    String SERVERASYNC_DETAILED_HELP = "PromptCommand.serverasync.detailed-help";

    @I18NMessage("Command has asynchronously finished execution [{0}]")
    String SERVERASYNC_FINISHED = "PromptCommand.serverasync.finished";

    @I18NMessage("Command was recovered and executed [{0}]")
    String SERVERASYNC_RECOVERED = "PromptCommand.serverasync.recovered";

    @I18NMessage("dumpspool")
    String DUMPSPOOL = "PromptCommand.dumpspool";

    @I18NMessage("dumpspool [object|hex]")
    String DUMPSPOOL_SYNTAX = "PromptCommand.dumpspool.syntax";

    @I18NMessage("object")
    String DUMPSPOOL_OBJ = "PromptCommand.dumpspool.object";

    @I18NMessage("hex")
    String DUMPSPOOL_HEX = "PromptCommand.dumpspool.hexidecimal";

    @I18NMessage("Shows the entries found in the command spool file")
    String DUMPSPOOL_HELP = "PromptCommand.dumpspool.help";

    @I18NMessage("Shows the entries found in the command spool file as objects or as raw data.\\n\\\n"
        + "If you do not enter any arguments, this simply shows you how many commands\\n\\\n"
        + "are spooled waiting to be sent.  You can provide an argument indicating\\n\\\n"
        + "how you want to display each command entry in the spool file: as an object\\n\\\n"
        + "or as raw data in hexidecimal format.")
    String DUMPSPOOL_DETAILED_HELP = "PromptCommand.dumpspool.detailed-help";

    @I18NMessage("There is no command spool file configured - the agent is not persisting commands")
    String DUMPSPOOL_NO_FILE = "PromptCommand.dumpspool.no-file";

    @I18NMessage("An error occurred while attempting to read and display the entries from the command spool file [{0}]. Cause: {1}")
    String DUMPSPOOL_ERROR = "PromptCommand.dumpspool.error";

    @I18NMessage("timer")
    String TIMER = "PromptCommand.timer";

    @I18NMessage("timer <another prompt command and its arguments>")
    String TIMER_SYNTAX = "PromptCommand.timer.syntax";

    @I18NMessage("Times how long it takes to execute another prompt command")
    String TIMER_HELP = "PromptCommand.timer.help";

    @I18NMessage("This will execute another prompt command and, once complete,\\n\\\n"
        + "will report how long it took to execute that command.")
    String TIMER_DETAILED_HELP = "PromptCommand.timer.detailed-help";

    @I18NMessage("It took [{0,number,#.###}] seconds to execute that [{1}] command.\\n\\\n"
        + "Memory Footprint Before: Free=[{2,number,#.##}] MB, Total=[{3,number,#.##}] MB\\n\\\n"
        + "Memory Footprint After:  Free=[{4,number,#.##}] MB, Total=[{5,number,#.##}] MB")
    String TIMER_RESULTS = "PromptCommand.timer.results";

    @I18NMessage("Please specify the command you want to execute and time.")
    String TIMER_MISSING_COMMAND = "PromptCommand.timer.missing-command";

    @I18NMessage("register")
    String REGISTER = "PromptCommand.register";

    @I18NMessage("regenerate")
    String REGISTER_REGENERATE = "PromptCommand.register.regenerate";

    @I18NMessage("register [<wait-time>] [regenerate]")
    String REGISTER_SYNTAX = "PromptCommand.register.syntax";

    @I18NMessage("Registers this agent with the RHQ Server")
    String REGISTER_HELP = "PromptCommand.register.help";

    @I18NMessage("Registers this agent with the RHQ Server. Once registered, the agent\\n\\\n"
        + "will be assigned a security token to indicate it is known to the server.\\n\\\n"
        + "The agent will automatically attempt to register itself at startup but this\\n\\\n"
        + "command can be used to re-register the agent and get another token assigned.\\n\\\n"
        + "You can optionally provide the amount of time you want this command to wait;\\n\\\n"
        + "by default, this command will wait 30 seconds before giving up waiting for\\n\\\n"
        + "the server to respond. This wait limit does not abort the registration\\n\\\n"
        + "attempt; the agent will continue to try to contact the server in background.\\n\\\n"
        + "You can also optionally ask that the server regenerate a new security token\\n\\\n"
        + "for the agent.  That is, the agent can ask the server for a new, different\\n\\\n"
        + "token.  If you do not specify the regenerate option, the agent will continue\\n\\\n"
        + " to use its current token, if it has one already (otherwise, it will get a\\n\\\n"
        + "new token, with or without the regenerate option specified).")
    String REGISTER_DETAILED_HELP = "PromptCommand.register.detailed-help";

    @I18NMessage("The agent will now attempt to register itself with the server.\\n\\\n" + "Regenerating token=[{0}]")
    String REGISTER_SENT_REQUEST = "PromptCommand.register.sent-request";

    @I18NMessage("Waiting {0} seconds...")
    String REGISTER_WAITING = "PromptCommand.register.waiting";

    @I18NMessage("The agent was not able to register itself. Is the server up?")
    String REGISTER_FAILED = "PromptCommand.register.failed";

    @I18NMessage("The current registration for this agent is: {0}")
    String REGISTER_REGISTRATION = "PromptCommand.register.registered";

    @I18NMessage("The agent cannot register itself with the server until it has been started.\\n\\\n"
        + "Please execute the start command if you wish to register (or re-register)\\n\\\n"
        + "this agent with the server.")
    String REGISTER_MUST_BE_STARTED = "PromptCommand.register.must-be-started";

    @I18NMessage("download")
    String DOWNLOAD = "PromptCommand.download";

    @I18NMessage("download <file-to-download> [directory-to-store-file]")
    String DOWNLOAD_SYNTAX = "PromptCommand.download.syntax";

    @I18NMessage("Downloads a file from the RHQ Server")
    String DOWNLOAD_HELP = "PromptCommand.download.help";

    @I18NMessage("Downloads a file and stores it in a given directory location.\\n\\\n"
        + "The first argument to this command can be a URL or a\\n\\\n"
        + "path relative to the RHQ Server. The second argument is\\n\\\n"
        + "optional and is the path to the local directory where you want\\n\\\n"
        + "to store the file. If this is not specified, the file\\n\\\n"
        + "will be stored in the agent data directory.")
    String DOWNLOAD_DETAILED_HELP = "PromptCommand.download.detailed-help";

    @I18NMessage("Downloading [{0}]...")
    String DOWNLOAD_INPROGRESS = "PromptCommand.download.in-progress";

    @I18NMessage("Downloaded the file and stored it as [{0}]")
    String DOWNLOAD_SUCCESS = "PromptCommand.download.success";

    @I18NMessage("Unable to download the file [{0}] from the RHQ Server. Cause: {1}")
    String DOWNLOAD_ERROR = "PromptCommand.download.error";

    @I18NMessage("Unable to download the file [{0}] - not currently able to talk to the RHQ Server.")
    String DOWNLOAD_ERROR_NOT_SENDING = "PromptCommand.download.error-not-sending";

    @I18NMessage("pc")
    String PLUGIN_CONTAINER = "PromptCommand.plugin-container";

    @I18NMessage("pc < start | stop | status >")
    String PLUGIN_CONTAINER_SYNTAX = "PromptCommand.plugin-container.syntax";

    @I18NMessage("start")
    String PLUGIN_CONTAINER_ARG_START = "PromptCommand.plugin-container.syntax.args.start";

    @I18NMessage("stop")
    String PLUGIN_CONTAINER_ARG_STOP = "PromptCommand.plugin-container.syntax.args.stop";

    @I18NMessage("status")
    String PLUGIN_CONTAINER_ARG_STATUS = "PromptCommand.plugin-container.syntax.args.status";

    @I18NMessage("Starts and stops the plugin container and all deployed plugins")
    String PLUGIN_CONTAINER_HELP = "PromptCommand.plugin-container.help";

    @I18NMessage("The plugin container is the component running inside the agent\\n\\\n"
        + "that manages all deployed plugins.  You can explicitly start and\\n\\\n"
        + "stop the plugin container running inside this agent using this command.\\n\\\n"
        + "The agent must be started in order to start the plugin container.  Note\\n\\\n"
        + "that the agent does not need to be in contact with the server in order to\\n\\\n"
        + "start the plugin container.  This means even if the server is down, the\\n\\\n"
        + "plugin container and its plugins can run and collect data.")
    String PLUGIN_CONTAINER_DETAILED_HELP = "PromptCommand.plugin-container.detailed-help";

    @I18NMessage("The plugin container has been started.")
    String PLUGIN_CONTAINER_START_DONE = "PromptCommand.plugin-container.start.done";

    @I18NMessage("The plugin container has been stopped.")
    String PLUGIN_CONTAINER_STOP_DONE = "PromptCommand.plugin-container.stop.done";

    @I18NMessage("The plugin container is currently started.")
    String PLUGIN_CONTAINER_STATUS_STARTED = "PromptCommand.plugin-container.started";

    @I18NMessage("The plugin container is currently stopped.")
    String PLUGIN_CONTAINER_STATUS_STOPPED = "PromptCommand.plugin-container.stopped";

    @I18NMessage("The plugin container configuration is:\\n\\\n{0}")
    String PLUGIN_CONTAINER_STATUS_CONFIG = "PromptCommand.plugin-container.config";

    @I18NMessage("An error occurred while attempting to start the plugin container:\\n\\\n" + "Cause: {0}")
    String PLUGIN_CONTAINER_START_ERROR = "PromptCommand.plugin-container.error.start";

    @I18NMessage("An error occurred while attempting to stop the plugin container:\\n\\\n" + "Cause: {0}")
    String PLUGIN_CONTAINER_STOP_ERROR = "PromptCommand.plugin-container.error.stop";

    @I18NMessage("plugins")
    String PLUGINS = "PromptCommand.plugins";

    @I18NMessage("plugins < update | info >")
    String PLUGINS_SYNTAX = "PromptCommand.plugins.syntax";

    @I18NMessage("update")
    String PLUGINS_ARG_UPDATE = "PromptCommand.plugins.syntax.args.update";

    @I18NMessage("info")
    String PLUGINS_ARG_INFO = "PromptCommand.plugins.syntax.args.info";

    @I18NMessage("Updates the agent plugins with the latest versions from the server")
    String PLUGINS_HELP = "PromptCommand.plugins.help";

    @I18NMessage("Compares the current versions of the agent's plugins with\\n\\\n"
        + "the latest versions found on the server.  If a plugin is found\\n\\\n"
        + "to be out of date, the latest plugin will be downloaded from the\\n\\\n"
        + "server and the agent's old plugin will be replaced with the new one.")
    String PLUGINS_DETAILED_HELP = "PromptCommand.plugins.detailed-help";

    @I18NMessage("Cannot update plugins - not currently able to talk to the RHQ Server.")
    String PLUGINS_ERROR_NOT_SENDING = "PromptCommand.plugins.error-not-sending";

    @I18NMessage("An error occurred while updating the plugins. Cause: {0}")
    String PLUGINS_ERROR_UPDATING = "PromptCommand.plugins.error-updating";

    @I18NMessage("There are no plugins currently installed.\\n\\\n"
        + "Perform an update to download the latest plugins from the server.")
    String PLUGINS_NO_CURRENT_PLUGINS = "PromptCommand.plugins.no-current-plugins";

    @I18NMessage("Plugins that are currently installed:")
    String PLUGINS_LISTING_PLUGINS = "PromptCommand.plugins.listing-plugins";

    @I18NMessage("Total number of plugins currently installed: [{0}]")
    String PLUGINS_NUM_CURRENT_PLUGINS = "PromptCommand.plugins.num-current-plugins";

    @I18NMessage("{0}")
    String PLUGINS_PLUGINS_INFO_FILENAME = "PromptCommand.plugins.plugin-info.filename";

    @I18NMessage("Plugin Name:  {0}")
    String PLUGINS_PLUGINS_INFO_NAME = "PromptCommand.plugins.plugin-info.name";

    @I18NMessage("File Size:    {0,number} bytes")
    String PLUGINS_PLUGINS_INFO_FILESIZE = "PromptCommand.plugins.plugin-info.filesize";

    @I18NMessage("Last Updated: {0,date,long} {0,time,long}")
    String PLUGINS_PLUGINS_INFO_LASTMOD = "PromptCommand.plugins.plugin-info.lastmod";

    @I18NMessage("MD5 Hashcode: {0}")
    String PLUGINS_PLUGINS_INFO_MD5 = "PromptCommand.plugins.plugin-info.md5";

    @I18NMessage("You cannot start the plugin container - please start the agent first")
    String CANNOT_START_PLUGIN_CONTAINER_AGENT_NOT_STARTED = "AgentMain.cannot-start-pc-agent-shutdown";

    @I18NMessage("The agent will now wait until it has registered with the server...")
    String WAITING_TO_BE_REGISTERED_BEGIN = "AgentMain.waiting-to-be-registered-begin";

    @I18NMessage("The agent does not have plugins - it will now wait for them to be downloaded...")
    String WAITING_FOR_PLUGINS = "AgentMain.waiting-for-plugins";

    @I18NMessage("The agent does not have plugins - it will now wait for them to be downloaded to [{0}]...")
    String WAITING_FOR_PLUGINS_WITH_DIR = "AgentMain.waiting-for-plugins-with-dir";

    @I18NMessage("[{0}] plugins downloaded.")
    String DONE_WAITING_FOR_PLUGINS = "AgentMain.done-waiting-for-plugins";

    @I18NMessage("Done! The agent is now registered with the server.")
    String WAITING_TO_BE_REGISTERED_END = "AgentMain.waiting-to-be-registered-end";

    @I18NMessage("The wait time has expired and the agent is still not registered.")
    String CANNOT_WAIT_TO_BE_REGISTERED_ANY_LONGER = "AgentMain.waiting-to-be-registered-timeout";

    @I18NMessage("The name of this agent is not defined - you cannot start the agent until you give it a valid name")
    String AGENT_NAME_NOT_DEFINED = "AgentMain.agent-name-not-defined";

    @I18NMessage("Agent is not starting the plugin container at startup, as per its configuration")
    String NOT_STARTING_PLUGIN_CONTAINER_AT_STARTUP = "AgentMain.not-starting-pc-at-startup";

    @I18NMessage("Agent is not notifying the server that it is shutting down, as per its configuration")
    String TOLD_TO_NOT_NOTIFY_SERVER_OF_SHUTDOWN = "AgentMain.told-to-not-notify-server-of-shutdown";

    @I18NMessage("metrics")
    String METRICS = "PromptCommand.metrics";

    @I18NMessage("metrics")
    String METRICS_SYNTAX = "PromptCommand.metrics.syntax";

    @I18NMessage("Shows the agent metrics")
    String METRICS_HELP = "PromptCommand.metrics.help";

    @I18NMessage("Displays all the current metric data emitted by the agent itself.")
    String METRICS_DETAILED_HELP = "PromptCommand.metrics.detailed-help";

    @I18NMessage("The agent has not created the management services - no metrics are available.")
    String METRICS_NO_SERVICES = "PromptCommand.metrics.no-services";

    @I18NMessage("An error occurred attempting to get the agent metric data. Cause: {0}")
    String METRICS_EXCEPTION = "PromptCommand.metrics.exception";

    @I18NMessage("Agent Metrics:")
    String METRICS_HEADER = "PromptCommand.metrics.header";

    @I18NMessage("seconds")
    String UNITS_SECONDS = "units.seconds";

    @I18NMessage("minutes")
    String UNITS_MINUTES = "units.minutes";

    @I18NMessage("hours")
    String UNITS_HOURS = "units.hours";

    @I18NMessage("days")
    String UNITS_DAYS = "units.days";

    @I18NMessage("native")
    String NATIVE = "PromptCommand.native";

    @I18NMessage("native [--version|--ps[=verbose]|--os|\\n\\\n"
        + "\\                --enable|--disable|--shutdown]...")
    String NATIVE_SYNTAX = "PromptCommand.native.syntax";

    @I18NMessage("Obtains native system information")
    String NATIVE_HELP = "PromptCommand.native.help";

    @I18NMessage("Obtains information directly from the native system.\\n\\\n"
        + "If you do not provide any command line arguments, this command will tell\\n\\\n"
        + "you if the native system is available and if it has been disabled.\\n\\\n"
        + "The valid command line arguments are:\\n\\\n"
        + "\\  -v, --version : displays the version of the native system\\n\\\n"
        + "\\  -p, --ps[=verbose] : output the process table\\n\\\n"
        + "\\  -o, --os : output operating system information\\n\\\n"
        + "\\  -e, --enable : enables the use of the native system\\n\\\n"
        + "\\  -d, --disable : turns off the native system\\n\\\n"
        + "\\  -s, --shutdown : shuts down the native components and disables their use")
    String NATIVE_DETAILED_HELP = "PromptCommand.native.detailed-help";

    @I18NMessage("version")
    String NATIVE_VERSION = "PromptCommand.native.version";

    @I18NMessage("ps")
    String NATIVE_PS = "PromptCommand.native.ps";

    @I18NMessage("verbose")
    String NATIVE_VERBOSE = "PromptCommand.native.verbose";

    @I18NMessage("os")
    String NATIVE_OS = "PromptCommand.native.os";

    @I18NMessage("shutdown")
    String NATIVE_SHUTDOWN = "PromptCommand.native.shutdown";

    @I18NMessage("enable")
    String NATIVE_ENABLE = "PromptCommand.native.enable";

    @I18NMessage("disable")
    String NATIVE_DISABLE = "PromptCommand.native.disable";

    @I18NMessage("The native system is available on this agent platform.")
    String NATIVE_IS_AVAILABLE = "PromptCommand.native.is-available";

    @I18NMessage("The native system is currently disabled.")
    String NATIVE_IS_DISABLED = "PromptCommand.native.is-disabled";

    @I18NMessage("The native system is currently loaded and initialized.")
    String NATIVE_IS_INITIALIZED = "PromptCommand.native.is-initialized";

    @I18NMessage("The native system is NOT available on this agent platform.")
    String NATIVE_IS_NOT_AVAILABLE = "PromptCommand.native.is-not-available";

    @I18NMessage("The native system is currently enabled.")
    String NATIVE_IS_NOT_DISABLED = "PromptCommand.native.is-not-disabled";

    @I18NMessage("The native system is NOT initialized.")
    String NATIVE_IS_NOT_INITIALIZED = "PromptCommand.native.is-not-initialized";

    @I18NMessage("This is not natively supported on your platform.")
    String NATIVE_NOT_SUPPORTED = "PromptCommand.native.not-supported";

    @I18NMessage("Native system has been shutdown and disabled.")
    String NATIVE_SHUTDOWN_DONE = "PromptCommand.native.shutdown-done";

    @I18NMessage("You cannot shutdown the native system when the agent is started.\\n\\\n"
        + "Shutdown the agent first, then you can shutdown the native system.")
    String NATIVE_SHUTDOWN_FAILED_AGENT_STARTED = "PromptCommand.native.shutdown-done-failed-agent-started";

    @I18NMessage("Native system has been enabled.")
    String NATIVE_ENABLE_DONE = "PromptCommand.native.enable-done";

    @I18NMessage("Native system has been disabled.")
    String NATIVE_DISABLE_DONE = "PromptCommand.native.disable-done";

    @I18NMessage("Operating System Name: {0}\\n\\\n" + "Operating System Version: {1}\\n\\\n" + "Hostname: {2}")
    String NATIVE_OS_OUTPUT = "PromptCommand.native.os-output";

    @I18NMessage("PID\\tNAME")
    String NATIVE_PS_OUTPUT_SHORT_HEADER = "PromptCommand.native.ps-output-short-header";

    @I18NMessage("{0,number,######}\\t{1} ")
    String NATIVE_PS_OUTPUT_SHORT = "PromptCommand.native.ps-output-short";

    @I18NMessage("PID\\tPARENT\\tNAME\\tCOMMAND LINE")
    String NATIVE_PS_OUTPUT_VERBOSE_HEADER = "PromptCommand.native.ps-output-verbose-header";

    @I18NMessage("{0,number,#####}\\t{1,number,#####}\\t{2}\\t{3}")
    String NATIVE_PS_OUTPUT_VERBOSE = "PromptCommand.native.ps-output-verbose";

    @I18NMessage("execute")
    String EXECUTE = "PromptCommand.execute";

    @I18NMessage("execute [-E name=value]* [--capture] [--wait=<ms>] [--killOnTimeout]\\n\\\n"
        + "\\                [--directory=<working dir>] <executable> [args...]")
    String EXECUTE_SYNTAX = "PromptCommand.execute.syntax";

    @I18NMessage("Executes an external program")
    String EXECUTE_HELP = "PromptCommand.execute.help";

    @I18NMessage("Executes an external program in its own process and reports the results.\\n\\\n"
        + "The valid command line arguments are:\\n\\\n"
        + "\\  -E name=value : an environment variable to pass to the process.  You can\\n\\\n"
        + "\\                  specify zero, one or more environment variables. If you\\n\\\n"
        + "\\                  do not specify any, the agent's environment will be used.\\n\\\n"
        + "\\  -c, --capture : captures the output of the process and prints it to the\\n\\\n"
        + "\\                  console once the process exits.  Do not use this option if\\n\\\n"
        + "\\                  the process is expected to output alot of data.\\n\\\n"
        + "\\  -k, --killOnTimeout : if the process does not exit before the -w wait time\\n\\\n"
        + "\\                        expires, the process will be killed.  If -w is 0\\n\\\n"
        + "\\                        or less, this option is ignored.\\n\\\n"
        + "\\  -w, --wait=ms : specifies the amount of time, in milliseconds, that the\\n\\\n"
        + "\\                  agent will wait for the process to exit. If you specify\\n\\\n"
        + "\\                  0 or less, the agent will not wait before returning\\n\\\n"
        + "\\                  control back to the console (in which case you will not\\n\\\n"
        + "\\                  be able to know the exit code or output of the process.\\n\\\n"
        + "\\  -d, --directory=<dir> : defines the working directory for the new process.\\n\\\n"
        + "\\                  If you do not specify this, it will be the working\\n\\\n"
        + "\\                  of the agent.\\n\\\n" + "\\  executable : the full path to the executable to run\\n\\\n"
        + "\\  args : optional set of arguments to be passed to the executable")
    String EXECUTE_DETAILED_HELP = "PromptCommand.execute.detailed-help";

    @I18NMessage("Invalid wait time [{0}] - must be a valid amount of milliseconds.")
    String EXECUTE_BAD_WAIT_ARG = "PromptCommand.execute.bad-wait-arg";

    @I18NMessage("You must specify an executable.")
    String EXECUTE_MISSING_EXECUTABLE = "PromptCommand.execute.missing-executable";

    @I18NMessage("Executing [{0}] with arguments {1}")
    String EXECUTE_EXECUTING = "PromptCommand.execute.executing";

    @I18NMessage("Process will have these environment variables: {0}")
    String EXECUTE_ENV = "PromptCommand.execute.env";

    @I18NMessage("Will wait [{0}] milliseconds for the process to exit")
    String EXECUTE_WILL_WAIT = "PromptCommand.execute.will-wait";

    @I18NMessage("The working directory will be [{0}]")
    String EXECUTE_DIR = "PromptCommand.execute.dir";

    @I18NMessage("Exit Code={0,number,#}")
    String EXECUTE_EXIT_CODE = "PromptCommand.execute.exit-code";

    @I18NMessage("Error={0}")
    String EXECUTE_ERROR = "PromptCommand.execute.error";

    @I18NMessage("Output:\\n\\\n{0}")
    String EXECUTE_OUTPUT = "PromptCommand.execute.output";

    @I18NMessage("discovery")
    String DISCOVERY = "PromptCommand.discovery";

    @I18NMessage("discovery [--plugin=<plugin name>] [--resourceType=<type name>] [--verbose]\\n\\\n"
        + "        discovery --full")
    String DISCOVERY_SYNTAX = "PromptCommand.discovery.syntax";

    @I18NMessage("Asks a plugin to run a server scan discovery")
    String DISCOVERY_HELP = "PromptCommand.discovery.help";

    @I18NMessage("Asks a plugin to run a server discovery scan. This is a way to determine\\n\\\n"
        + "what servers a plugin can actually find.  Note that this will run a server\\n\\\n"
        + "scan, not a service scan (i.e. it will not try to discover child services\\n\\\n"
        + "for parent servers already in inventory) unless you use --full. Also note\\n\\\n"
        + "that only --full will actually send an inventory report to the server.\\n\\\n"
        + "The valid command line arguments are:\\n\\\n"
        + "\\  -f, --full : Runs a detailed discovery inside the plugin container.\\n\\\n"
        + "\\               This will update the actual agent inventory by sending\\n\\\n"
        + "\\               an inventory report to the server. \\n\\\n"
        + "\\               Note that the results are not output.\\n\\\n"
        + "\\  -p, --plugin=<name> : The name of the plugin whose discovery will run.\\n\\\n"
        + "\\                        If you do not specify a plugin, all plugins will\\n\\\n"
        + "\\                        run their discovery.\\n\\\n"
        + "\\  -r, --resourceType=<type> : specifies the specific resource type to be\\n\\\n"
        + "\\                              discovered.  If not specified, all resource\\n\\\n"
        + "\\                              types that a plugin supports will be\\n\\\n"
        + "\\                              discovered (and if no plugin was specified,\\n\\\n"
        + "\\                              then all resource types for all plugins will\\n\\\n"
        + "\\                              be discovered).\\n\\\n"
        + "\\  -v, --verbose : Prints the plugin configuration of each discovered server.")
    String DISCOVERY_DETAILED_HELP = "PromptCommand.discovery.detailed-help";

    @I18NMessage("You must first start the plugin container before attempting discovery.")
    String DISCOVERY_PC_NOT_STARTED = "PromptCommand.discovery.pc-not-started";

    @I18NMessage("An error occurred running the discovery. Cause: {0}")
    String DISCOVERY_ERROR = "PromptCommand.discovery.error";

    @I18NMessage("There is no plugin named [{0}]")
    String DISCOVERY_BAD_PLUGIN_NAME = "PromptCommand.discovery.bad-plugin-name";

    @I18NMessage("There is no resource type named [{0}]")
    String DISCOVERY_BAD_RESOURCE_TYPE_NAME = "PromptCommand.discovery.bad-resource-type-name";

    @I18NMessage("The plugin [{0}] has no resource type named [{1}]")
    String DISCOVERY_BAD_PLUGIN_RESOURCE_TYPE_NAME = "PromptCommand.discovery.bad-plugin-resource-type-name";

    @I18NMessage("{0}.{1}: Starting discovery...")
    String DISCOVERY_DISCOVERING_RESOURCE_TYPE = "PromptCommand.discovery.discovering-resource-type";

    @I18NMessage("{0}.{1}: Done.")
    String DISCOVERY_DISCOVERING_RESOURCE_TYPE_DONE = "PromptCommand.discovery.discovering-resource-type-done";

    @I18NMessage("{0}.{1}: Process scan detected a server - scan=[{2}], process=[{3}]")
    String DISCOVERY_PROCESS_SCAN = "PromptCommand.discovery.process-scan";

    @I18NMessage("{0}.{1}: key=[{2}], name=[{3}], version=[{4}], description=[{5}]")
    String DISCOVERY_COMPONENT_RESULT = "PromptCommand.discovery.component-result";

    @I18NMessage("inventory")
    String INVENTORY = "PromptCommand.inventory";

    @I18NMessage("inventory [--xml] [--export=<file>] [--norecurse]\\n\\\n"
        + "\\                  [--id=<#>] | --types | <inventory-binary-file>]")
    String INVENTORY_SYNTAX = "PromptCommand.inventory.syntax";

    @I18NMessage("Provides information about the current inventory of resources")
    String INVENTORY_HELP = "PromptCommand.inventory.help";

    @I18NMessage("This will allow you to view the resources currently in inventory.\\n\\\n"
        + "The valid command line arguments are:\\n\\\n"
        + "\\ -e, --export=file : Writes the inventory information to the given file.\\n\\\n"
        + "\\                     If this is not specified, the output will go to the\\n\\\n"
        + "\\                     console window.\\n\\\n"
        + "\\ -x, --xml : Dumps the information in XML format; otherwise, will dump\\n\\\n"
        + "\\             the information in a simple, one-line-per-resource, format.\\n\\\n"
        + "\\ -t, --types : Will only dump the resource type definitions not the\\n\\\n"
        + "\\               actual resources that are in inventory.\\n\\\n"
        + "\\ -i, --id=# : Identifies the specific resource you want to show.\\n\\\n"
        + "\\              If not specified, all resources will be shown.\\n\\\n"
        + "\\              You cannot specify this if --types is specified.\\n\\\n"
        + "\\ -n, --norecurse : Will not traverse children of a resource.\\n\\\n"
        + "\\                   Only the specified resource will be shown. If --id\\n\\\n"
        + "\\                   is not specified, the platform will be shown.\\n\\\n"
        + "\\                   This is ignored if --types is specified.\\n\\\n"
        + "\\ inventory-binary-file : if specified, this contains the inventory data\\n\\\n"
        + "\\                         in binary form (e.g. data/inventory.dat).\\n\\\n"
        + "\\                         If not specified, the inventory currently\\n\\\n"
        + "\\                         in memory will be used. If you specify this,\\n\\\n"
        + "\\                         you cannot specify --types or --id.")
    String INVENTORY_DETAILED_HELP = "PromptCommand.inventory.detailed-help";

    @I18NMessage("You specified an invalid ID number [{0}]")
    String INVENTORY_BAD_ID = "PromptCommand.inventory.bad-id";

    @I18NMessage("Could not write to the export file [{0}]. Cause: {1}")
    String INVENTORY_BAD_EXPORT_FILE = "PromptCommand.inventory.bad-export-file";

    @I18NMessage("You specified an unknown resource ID [{0}].")
    String INVENTORY_INVALID_RESOURCE_ID = "PromptCommand.inventory.invalid-resource-id";

    @I18NMessage("Could not load the inventory file [{0}]. Cause: {1}")
    String INVENTORY_BAD_INVENTORY_FILE = "PromptCommand.inventory.bad-inventory-file";

    @I18NMessage("The agent/plugin container must be started to get inventory data.\\n\\\n"
        + "Please use the start command to make sure the agent and its PC are started.")
    String INVENTORY_MUST_BE_STARTED = "PromptCommand.inventory.must-be-started";

    @I18NMessage("You asked to see only the resource types, but specified a binary\\n\\\n"
        + "file - those two are mutually exclusive")
    String INVENTORY_DUMP_TYPES_AND_BINARY_FILE_SPECIFIED = "PromptCommand.inventory.dump-types-and-file-specified";

    @I18NMessage("You asked to view a specific ID, but specified a binary\\n\\\n"
        + "file - those two are mutually exclusive")
    String INVENTORY_ID_AND_BINARY_FILE_SPECIFIED = "PromptCommand.inventory.id-and-file-specified";

    @I18NMessage("You asked to see only the resource types, but specified an ID - those\\n\\\n"
        + "two are mutually exclusive")
    String INVENTORY_ID_AND_DUMP_TYPES_SPECIFIED = "PromptCommand.inventory.id-and-dump-types-specified";

    @I18NMessage("avail")
    String AVAILABILITY = "PromptCommand.availability";

    @I18NMessage("avail [--changed] [--verbose]")
    String AVAILABILITY_SYNTAX = "PromptCommand.availability.syntax";

    @I18NMessage("Get availability of inventoried resources")
    String AVAILABILITY_HELP = "PromptCommand.availability.help";

    @I18NMessage("This will request an availability report that will tell you\\n\\\n"
        + "which resources are up and which are down. By default, this will\\n\\\n"
        + "show the availability of all inventoried resources. If --changed\\n\\\n"
        + "was specified, only those resources whose availability status changed\\n\\\n"
        + "from their last known state are shown. After displaying the contents\\n\\\n"
        + "of the report, it will be sent up to the server.\\n\\\n"
        + "\\ -c, --changed : if specified, the report will contain availability\\n\\\n"
        + "\\                 for only those resources whose status changed.\\n\\\n"
        + "\\ -v, --verbose : if true, additional information is displayed.")
    String AVAILABILITY_DETAILED_HELP = "PromptCommand.availability.detailed-help";

    @I18NMessage("The agent/plugin container must be started to get availability data.\\n\\\n"
        + "Please use the start command to make sure the agent and its PC are started.")
    String AVAILABILITY_MUST_BE_STARTED = "PromptCommand.availability.must-be-started";

    @I18NMessage("This agent does not have any committed resources in inventory.\\n\\\n"
        + "You must commit at least the platform resource to inventory\\n\\\n"
        + "in order to obtain availability reports.")
    String AVAILABILITY_NO_COMMITTED_INVENTORY = "PromptCommand.availability.no-committed-inventory";

    @I18NMessage("Availability Report from {0,date,medium} {0,time,medium}\\n\\\n" + "Resources in report: {1}\\n\\\n"
        + "Includes Only Changed Resources: {2}")
    String AVAILABILITY_REPORT_HEADER = "PromptCommand.availability.report-header";

    @I18NMessage("{0}:\\tname=[{1}] id=[{2,number,#}] key=[{3}]")
    String AVAILABILITY_REPORT_RESOURCE_VERBOSE = "PromptCommand.availability.report-resource-verbose";

    @I18NMessage("{0}:\\t{1}")
    String AVAILABILITY_REPORT_RESOURCE = "PromptCommand.availability.report-resource";

    @I18NMessage("Sending the availability report to the server...")
    String AVAILABILITY_REPORT_SENDING = "PromptCommand.availability.sending";

    @I18NMessage("Done.")
    String AVAILABILITY_REPORT_SENT = "PromptCommand.availability.sent";
}