/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
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
    @I18NMessage("Failed to move old cert file [{0}] to new default location [{1}] - agent communication may fail!")
    String CERT_FILE_COPY_ERROR = "AgentConfiguration.cert-file-copy-error";

    @I18NMessage("Explicitly setting file location [{0}] to [{1}]")
    String CERT_FILE_LOCATION = "AgentConfiguration.cert-file-location";

    @I18NMessage("The configured server alias [{0}] cannot be resolved - will use localhost address. Cause: {1}")
    String SERVER_ALIAS_UNKNOWN = "AgentConfiguration.server-alias-unknown";

    @I18NMessage("Specified bad console type [{0}]")
    String AGENT_INPUT_READER_FACTORY_BAD_TYPE = "AgentMain.input-reader-factory-bad-type";

    @I18NMessage("Failed to create console input reader of type [{0}]")
    String AGENT_INPUT_READER_FACTORY_ERROR = "AgentMain.input-reader-factory-error";

    @I18NMessage("!!! This agent is registering under the loopback address [{0}] - this should only be done for testing "
        + "or demo purposes - this agent will only be able to interact with a server running on the same host as this agent")
    String REGISTERING_WITH_LOOPBACK = "AgentMain.registering-with-loopback";

    @I18NMessage("There are still [{0}] threads left - the kill thread will\\n\\\n"
        + "exit the VM shortly if these threads do not die ")
    String SHUTDOWNHOOK_THREADS_STILL_ALIVE = "AgentShutdownHook.threads-still-alive";

    @I18NMessage("Missing the envvar [{0}] - will try to find a Java executable to use.")
    String UPDATE_THREAD_LOOKING_FOR_JAVA_EXE = "AgentUpdateThread.looking-for-java-exe";

    @I18NMessage("Will use the Java executable [{0}]")
    String UPDATE_THREAD_USING_JAVA_EXE = "AgentUpdateThread.using-java-exe";

    @I18NMessage("The agent cannot restart after the aborted update, will try to update again in [{0}]ms")
    String UPDATE_THREAD_CANNOT_RESTART_RETRY = "AgentUpdateThread.cannot-restart-retry";

    @I18NMessage("The agent will wait for [{0}] threads to die")
    String SHUTDOWNHOOK_THREAD_WAIT = "AgentShutdownHook.wait";

    @I18NMessage("Thread [{0}] is still alive - its stack trace follows:\\n{1}")
    String SHUTDOWNHOOK_THREAD_IS_STILL_ACTIVE = "AgentShutdownHook.thread-is-still-active";

    @I18NMessage("The agent failed waiting for threads to die: {0}")
    String SHUTDOWNHOOK_THREAD_CANNOT_WAIT = "AgentShutdownHook.cannot-wait";

    @I18NMessage("The agent failed to interrupt threads: {0}")
    String SHUTDOWNHOOK_THREAD_CANNOT_INT = "AgentShutdownHook.cannot-int";

    @I18NMessage("[{0}] threads are not dying - agent will not wait anymore")
    String SHUTDOWNHOOK_THREAD_NO_MORE_WAIT = "AgentShutdownHook.no-more-wait";

    @I18NMessage("The agent update thread encountered an exception: {0}")
    String UPDATE_THREAD_EXCEPTION = "AgentUpdateThread.exception";

    @I18NMessage("Now executing agent update - if all goes well, this is the last you will hear of this agent: [{0}]")
    String UPDATE_THREAD_EXECUTING_UPDATE_PROCESS = "AgentUpdateThread.executing-update-process";

    @I18NMessage("The agent update thread has started - will begin the agent auto-update now!")
    String UPDATE_THREAD_STARTED = "AgentUpdateThread.started";

    @I18NMessage("The agent is already in the process of updating itself")
    String UPDATE_THREAD_DUP = "AgentUpdateThread.duplicate";

    @I18NMessage("AGENT AUTO-UPDATE FAILED! This agent cannot update itself after [{0}] attempts!\\n\\\n"
        + "The agent is probably no longer able to talk to the server cloud successfully.\\n\\\n"
        + "Manual intervention by an administrator is usually required now.")
    String UPDATE_THREAD_FAILURE = "AgentUpdateThread.failure";

    @I18NMessage("Cannot validate agent update binary - it has not been downloaded yet [{0}]")
    String UPDATE_DOWNLOAD_MD5_MISSING_FILE = "AgentUpdateDownload.md5.missing-file";

    @I18NMessage("The downloaded agent update binary [{0}] did NOT validate with the expected MD5")
    String UPDATE_DOWNLOAD_MD5_INVALID = "AgentUpdateDownload.md5.invalid";

    @I18NMessage("Cannot download agent update binary - agent is configured to disable updates")
    String UPDATE_DOWNLOAD_DISABLED_BY_AGENT = "AgentUpdateDownload.update-download-disabled-by-agent";

    @I18NMessage("Cannot download agent update binary - agent updates have been disabled by the server [{0}]")
    String UPDATE_DOWNLOAD_DISABLED_BY_SERVER = "AgentUpdateDownload.update-download-disabled-by-server";

    @I18NMessage("Server did not tell us what filename to give our download, will use [{0}]")
    String UPDATE_DOWNLOAD_NO_NAME = "AgentUpdateDownload.update-download-no-name";

    @I18NMessage("Server told us an invalid name to call our agent update binary file: [{0}]")
    String UPDATE_DOWNLOAD_BAD_NAME = "AgentUpdateDownload.update-download-bad-name";

    @I18NMessage("Already have the agent binary update from a previous download at [{0}]")
    String UPDATE_DOWNLOAD_ALREADY_HAVE_IT = "AgentUpdateDownload.update-download-already-have-it";

    @I18NMessage("Failed to download the agent update binary from [{0}]. Cause: {1}")
    String UPDATE_DOWNLOAD_FAILURE = "AgentUpdateDownload.update-download-failure";

    @I18NMessage("Downloading the agent update binary [{0}] via URL [{1}]")
    String UPDATE_DOWNLOAD_RETRIEVAL = "AgentUpdateDownload.update-download-retrieval";

    @I18NMessage("Agent update binary [{0}] downloaded from URL [{1}] and stored at [{2}]")
    String UPDATE_DOWNLOAD_DONE = "AgentUpdateDownload.update-download-done";

    @I18NMessage("Downloading the agent update binary [{0}] failed - server temporarily rejecting our request [{1}]")
    String UPDATE_DOWNLOAD_UNAVAILABLE = "AgentUpdateDownload.update-download-unavailable";

    @I18NMessage("Failed to get the agent update version from [{0}]. Cause: {1}")
    String UPDATE_VERSION_FAILURE = "AgentUpdateVersion.update-version-failure";

    @I18NMessage("Getting the agent update version failed - server temporarily rejecting our request [{0}]")
    String UPDATE_VERSION_UNAVAILABLE = "AgentUpdateVersion.update-version-unavailable";

    @I18NMessage("Getting the agent update version via URL [{0}]")
    String UPDATE_VERSION_RETRIEVAL = "AgentUpdateVersion.update-version-retrieval";

    @I18NMessage("Agent update version retrieved via URL [{0}]: {1}")
    String UPDATE_VERSION_RETRIEVED = "AgentUpdateVersion.update-version-retrieved";

    @I18NMessage("Cannot get agent update version - agent is configured to disable updates")
    String UPDATE_VERSION_DISABLED_BY_AGENT = "AgentUpdateVersion.update-version-disabled-by-agent";

    @I18NMessage("Cannot get agent update version - agent updates have been disabled by the server [{0}]")
    String UPDATE_VERSION_DISABLED_BY_SERVER = "AgentUpdateVersion.update-version-disabled-by-server";

    @I18NMessage("Version=[{0}], Build Number=[{1}], Build Date=[{2,date,medium} {2,time,short}]")
    String IDENTIFY_VERSION = "AgentMain.identify-version";

    @I18NMessage("VM health check thread has started and will check every [{0}] milliseconds")
    String VM_HEALTH_CHECK_THREAD_STARTED = "VMHealthCheckThread.started";

    @I18NMessage("VM Health check thread has been stopped")
    String VM_HEALTH_CHECK_THREAD_STOPPED = "VMHealthCheckThread.stopped";

    @I18NMessage("VM Health check thread has encountered a fatal exception and will be stopped. Cause: {0}")
    String VM_HEALTH_CHECK_THREAD_EXCEPTION = "VMHealthCheckThread.exception";

    @I18NMessage("VM health check thread has detected [{0}] memory has crossed the threshold [{1}] and is low: memory-usage=[{2}]")
    String VM_HEALTH_CHECK_THREAD_MEM_LOW = "VMHealthCheckThread.mem-low";

    @I18NMessage("VM health check thread is invoking the garbage collector to see if more memory can be freed")
    String VM_HEALTH_CHECK_THREAD_GC = "VMHealthCheckThread.gc";

    @I18NMessage("VM health check thread sees that memory is critically low and will try to reboot the agent")
    String VM_HEALTH_CHECK_SEES_MEM_PROBLEM = "VMHealthCheckThread.mem-problem";

    @I18NMessage("Cannot switch to server - the sender is not ready. Is the agent shutdown?. Cause: {0}")
    String CANNOT_SWITCH_NULL_COMMUNICATOR = "AgentMain.cannot-switch-null-communicator";

    @I18NMessage("Cannot switch to server - the server endpoint is invalid [{0}]. Cause: {1}")
    String CANNOT_SWITCH_TO_INVALID_SERVER = "AgentMain.cannot-switch-to-invalid-server";

    @I18NMessage("The server and agent clocks are not in sync. Server=[{0,number,#}][{1,date,long} {1,time,full}], Agent=[{2,number,#}][{3,date,long} {3,time,full}]")
    String TIME_NOT_SYNCED = "AgentMain.time-not-synced";

    @I18NMessage("Failed to determine the server time. Cause: {0}")
    String TIME_UNKNOWN = "AgentMain.time-unknown";

    @I18NMessage("The server thinks this agent is down. Will notify the server of up-to-date information when possible.")
    String SERVER_THINKS_AGENT_IS_DOWN = "AgentMain.server-thinks-agent-is-down";

    @I18NMessage("Timed out waiting for the connectAgent R/W lock to avoid a possible deadlock")
    String TIMEOUT_WAITING_FOR_CONNECT_LOCK = "AgentMain.connect-lock-timeout";

    @I18NMessage("Not sending another connect message since one was recently sent: [{0}]")
    String NOT_SENDING_DUP_CONNECT = "AgentMain.not-sending-dup-connect";

    @I18NMessage("This version of the agent is not supported by the server - an agent update must be applied")
    String AGENT_NOT_SUPPORTED = "AgentMain.agent-not-supported";

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

    @I18NMessage("During failover attempt, the discovery features failed to start. Discovery may be disabled. Cause: {0}")
    String FAILOVER_DISCOVERY_START_FAILURE = "AgentMain.failover-discovery-start-failure";

    @I18NMessage("Too many failover attempts have been made [{0}]. Exception that triggered the failover: [{1}]")
    String TOO_MANY_FAILOVER_ATTEMPTS = "AgentMain.too-many-failover-attempts";

    @I18NMessage("!!! A server has registered under a loopback address [{0}] - this should only be done for testing and demo purposes. "
        + "Only agents running on the same machine as that server will be able to interact with that server successfully. "
        + "Please double check that you really want your server to have a public endpoint of [{0}]. "
        + "See the Administration (Topology) > Servers menu in the server GUI to change the public endpoint of the server.")
    String FAILOVER_LIST_HAS_LOCALHOST = "AgentMain.failover-list-has-localhost";

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

    @I18NMessage("Failover list has an unknown host [{0}]")
    String FAILOVER_LIST_UNKNOWN_HOST = "AgentMain.failover-list-unknown-host";

    @I18NMessage("Failover list has an unreachable host [{0}] (tested ports [{1,number,#}] and [{2,number,#}]). Cause: {3}")
    String FAILOVER_LIST_UNREACHABLE_HOST = "AgentMain.failover-list-unreachable-host";

    @I18NMessage("!!! There are [{0}] servers that are potentially unreachable by this agent.\\n\\\n"
        + "Please double check all public endpoints of your servers and ensure\\n\\\n"
        + "they are all reachable by this agent. The failed server endpoints are:\\n\\\n" //
        + "{1}\\n\\\n" //
        + "See the Administration (Topology) > Servers in the server GUI\\n\\\n"
        + "to change the public endpoint of a server.\\n\\\n"
        + "THIS AGENT WILL WAIT UNTIL ONE OF ITS SERVERS BECOMES REACHABLE!")
    String FAILOVER_LIST_CHECK_FAILED = "AgentMain.failover-list-check-failed";

    @I18NMessage("Testing failover connectivity to server [{0}:{1,number,#}]")
    String TEST_FAILOVER_LIST_ENTRY = "AgentMain.test-failover-list-entry";

    @I18NMessage("Testing connectivity to servers found in the failover list has been DISABLED and will be skipped.")
    String TEST_FAILOVER_LIST_AT_STARTUP_DISABLED = "AgentMain.test-failover-list-at-startup-disabled";

    @I18NMessage("The prompt input reader returned null. EOF?")
    String INPUT_EOF = "AgentMain.input-eof";

    @I18NMessage("The prompt input reader stopped providing input due to an exception. EOF? Cause: {0}")
    String INPUT_EXCEPTION = "AgentMain.input-exception";

    @I18NMessage("Failed to create prompt input reader.")
    String INPUT_FACTORY_EXCEPTION = "AgentMain.input-factory-exception";

    @I18NMessage("(type it again to confirm) ")
    String PROMPT_CONFIRM = "AgentNativePromptInfo.prompt-confirm";

    @I18NMessage("Your answers did not match - please try again: ")
    String PROMPT_CONFIRM_FAILED = "AgentNativePromptInfo.prompt-confirm-failed";

    @I18NMessage("Received a command - sounds like the server is up so the sender has been started")
    String RECEIVED_COMMAND_STARTED_SENDER = "AgentMain.received-msg-started-sender";

    @I18NMessage("Agent has been asked to start up clean - cleaning out the data directory: {0}")
    String CLEANING_DATA_DIRECTORY = "AgentMain.clean-data-dir";

    @I18NMessage("Agent has been asked to purge all plugins - cleaning out the plugins directory: {0}")
    String CLEANING_PLUGINS_DIRECTORY = "AgentMain.clean-plugins-dir";

    @I18NMessage("Purging file [{0}]. deleted successfully=[{1}]")
    String CLEANING_FILE = "AgentMain.clean-file";

    @I18NMessage("A failure occurred while trying to clean the data directory. Cause: {0}")
    String CLEAN_DATA_DIR_FAILURE = "AgentMain.clean-data-dir-failure";

    @I18NMessage("A failure occurred while trying to purge the plugins. Cause: {0}")
    String CLEAN_PLUGINS_FAILURE = "AgentMain.clean-plugins-failure";

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

    @I18NMessage("Reconfiguring Java Logging...")
    String RECONFIGURE_JAVA_LOGGING_START = "AgentMain.java-logging-start";

    @I18NMessage("Done reconfiguring Java Logging.")
    String RECONFIGURE_JAVA_LOGGING_DONE = "AgentMain.java-logging-done";

    @I18NMessage("Failed to reconfigure Java Logging.")
    String RECONFIGURE_JAVA_LOGGING_ERROR = "AgentMain.java-logging-error";

    @I18NMessage("The server has [{0}] plugins available for download")
    String LATEST_PLUGINS_COUNT = "PluginUpdate.latest-plugins-count";

    @I18NMessage("Plugin available for download: id=[{0}], name=[{1}], displayName=[{2}], version=[{3}], path=[{4}], md5=[{5}], enabled=[{6}], description=[{7}]")
    String LATEST_PLUGIN = "PluginUpdate.latest-plugin";

    @I18NMessage("Updating plugins to their latest versions.")
    String UPDATING_PLUGINS = "PluginUpdate.updating";

    @I18NMessage("Completed updating the plugins to their latest versions.")
    String UPDATING_PLUGINS_COMPLETE = "PluginUpdate.updating-complete";

    @I18NMessage("The plugin [{0}] is current and does not need to be updated.")
    String PLUGIN_ALREADY_AT_LATEST = "PluginUpdate.already-at-latest";

    @I18NMessage("The plugin [{0}] is disabled and will not be downloaded.")
    String PLUGIN_DISABLED_PLUGIN_DOWNLOAD_SKIPPED = "PluginUpdate.disabled-plugin-download-skipped";

    @I18NMessage("The disabled plugin file [{0}] is deleted,")
    String PLUGIN_DISABLED_PLUGIN_DELETED = "PluginUpdate.disabled-plugin-deleted";

    @I18NMessage("The disabled plugin file [{0}] failed to be deleted.")
    String PLUGIN_DISABLED_PLUGIN_DELETE_FAILED = "PluginUpdate.disabled-plugin-delete-failed";

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

    @I18NMessage("Failed to download the plugin [{0}]. This was attempt #[{1}]. Will retry again in [{2}]ms. Cause: [{3}]")
    String DOWNLOAD_PLUGIN_FAILURE_WILL_RETRY = "PluginUpdate.download-failure-will-retry";

    @I18NMessage("Failed to download the plugin [{0}]. This was attempt #[{1}] - will no longer retry. This plugin will not be deployed in the agent. Cause: {2}")
    String DOWNLOAD_PLUGIN_FAILURE_WILL_NOT_RETRY = "PluginUpdate.download-failure-will-not-retry";

    @I18NMessage("The plugin [{0}] has been updated at [{1}].")
    String DOWNLOADING_PLUGIN_COMPLETE = "PluginUpdate.downloading-complete";

    @I18NMessage("The plugin [{0}] is disabled and will not be updated.")
    String DOWNLOADING_PLUGIN_SKIPPED = "PluginUpdate.downloading-skipped";

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

    @I18NMessage("Will retry the agent registration request soon...")
    String AGENT_REGISTRATION_RETRY = "AgentMain.agent-registration-retry";

    @I18NMessage("The agent cannot register with the server. Admin intervention needed!")
    String AGENT_CANNOT_REGISTER = "AgentMain.agent-cannot-register";

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

    @I18NMessage("Agent will be restarted soon to see if the failure condition cleared up")
    String AGENT_START_RETRY_AFTER_FAILURE = "AgentMain.start-retry-after-failure";

    @I18NMessage("Agent being created now")
    String CREATING_AGENT = "AgentMain.creating-agent";

    @I18NMessage("The plugin container has been initialized with the following configuration: {0}")
    String PLUGIN_CONTAINER_INITIALIZED = "AgentMain.plugin-container-initialized";

    @I18NMessage("The plugin container has not been initialized. Check the log for further errors.")
    String PLUGIN_CONTAINER_NOT_INITIALIZED = "AgentMain.plugin-container-not-initialized";

    @I18NMessage("The plugin container initialization was interrupted - it will not be started.")
    String PLUGIN_CONTAINER_INITIALIZATION_INTERRUPTED = "AgentMain.plugin-container-initialization-interrupted";

    @I18NMessage("There are no plugins and the agent was told not to update them at startup.\\n\\\n"
        + "The agent will continue but it will not start the plugin container.")
    String NO_PLUGINS = "AgentMain.no-plugins";

    @I18NMessage("Failed to start plugin container because the agent did not register properly. Cause: {0}")
    String PC_START_FAILED_WAITING_FOR_REGISTRATION = "AgentMain.pc-start-failed-waiting-for-registration";

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

    @I18NMessage("Server auto-detection is not enabled - starting the poller immediately")
    String NO_AUTO_DETECT = "AgentMain.no-auto-detect";

    @I18NMessage("Native system info is enabled, but the system info API is not accessible on this platform "
        + "(sigar.jar not found in classpath or SIGAR shared library not found in shared library path). "
        + "Enable DEBUG logging for more details.")
    String NATIVE_SYSINFO_UNAVAILABLE = "AgentMain.native-sysinfo-unavailable";

    @I18NMessage("Native system info is enabled, but the system info API is not accessible on this platform "
        + "(sigar.jar not found in classpath or SIGAR shared library not found in shared library path).")
    String NATIVE_SYSINFO_UNAVAILABLE_DEBUG = "AgentMain.native-sysinfo-unavailable-debug";

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

    @I18NMessage("Agent no longer accepting input at prompt.")
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
        + "\\   -e, --console=<type>          Specifies the implementation to use when reading console input: jline, sigar, java\\n\\\n"
        + "\\   -g, --purgeplugins            Deletes all plugins, forcing the agent to re-download all of them\\n\\\n"
        + "\\   -h, --help                    Shows this help message (default)\\n\\\n"
        + "\\   -i, --input=<filename>        Specifies a script file to be used for input\\n\\\n"
        + "\\   -l, --cleanconfig             Clears out existing configuration and data files, except for the security token.\\n\\\n"
        + "\\   -L, --fullcleanconfig         Clears out all existing configuration and data files so the agent starts with a totally clean slate\\n\\\n"
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

    @I18NMessage("schedules")
    String SCHEDULES = "PromptCommand.schedules";

    @I18NMessage("Retrieves measurement schedule information for the specified resource")
    String SCHEDULES_HELP = "PromptCommand.schedules.help";

    @I18NMessage("schedules <resourceId>")
    String SCHEDULES_SYNTAX = "PromptCommand.schedules.syntax";

    @I18NMessage("This agent does not manage a resource with id {0}")
    String SCHEDULES_UNKNOWN_RESOURCE = "PromptCommand.schedules.unknown-resource";

    @I18NMessage("Failed to perform schedules command; stack trace follows:")
    String SCHEDULES_FAILURE = "PromptCommand.schedules.failure";

    @I18NMessage("exit")
    String EXIT = "PromptCommand.exit";

    @I18NMessage("Shuts down the agent's communications services and kills the agent")
    String EXIT_HELP = "PromptCommand.exit.help";

    @I18NMessage("Shutting down...")
    String EXIT_SHUTTING_DOWN = "AgentShutdownHook.exit.shutting-down";

    @I18NMessage("Shutdown complete - agent will now exit.")
    String EXIT_SHUTDOWN_COMPLETE = "AgentShutdownHook.exit.shutdown-complete";

    @I18NMessage("An error occurred during shutdown. Cause: {0}")
    String EXIT_SHUTDOWN_ERROR = "AgentShutdownHook.exit.shutdown-error";

    @I18NMessage("debug")
    String DEBUG = "PromptCommand.debug";

    @I18NMessage("debug [--disable] [--enable] [--file=<conf>]\\n\\\n" //
        + "\\              [--comm=true|false] [--threaddump]")
    String DEBUG_SYNTAX = "PromptCommand.debug.syntax";

    @I18NMessage("Provides features to help debug the agent.")
    String DEBUG_HELP = "PromptCommand.debug.help";

    @I18NMessage("Provides features to help debug the agent.\\n\\\n"
        + "-c|--comm=true|false: enables or disables server-agent communications trace.\\n\\\n"
        + "-f|--file=<conf>: reconfigures the logging system with the given file.\\n\\\n"
        + "\\                  <conf> can be 'log4j.xml', 'log4j-debug.xml', \\n\\\n"
        + "\\                  'log4j-warn.xml' or any file found in /conf.\\n\\\n"
        + "\\                  You can even use your own log configuration file by\\n\\\n"
        + "\\                  placing it in the /conf directory.\\n\\\n"
        + "-d|--disable: disabled debug logging. This is the same as '-f log4j.xml'.\\n\\\n"
        + "-e|--enable: enable debug logging. This is the same as '-f log4j-debug.xml'.\\n\\\n"
        + "-t|--threaddump: dumps the stacks for all threads in the agent VM.\\n\\\n")
    String DEBUG_DETAILED_HELP = "PromptCommand.debug.detailed-help";

    @I18NMessage("Switched to log file [{0}]. Root log level is [{1}]")
    String DEBUG_LOG_FILE_LOADED = "PromptCommand.debug.log-file-loaded";

    @I18NMessage("Cannot load log file [{0}]. Cause: {1}")
    String DEBUG_CANNOT_LOAD_LOG_FILE = "PromptCommand.debug.cannot-load-log-file";

    @I18NMessage("Cannot dump thread stack traces. Cause: {0}")
    String DEBUG_CANNOT_DUMP_THREADS = "PromptCommand.debug.cannot-dump-threads";

    @I18NMessage("Agent-server communications tracing has been enabled.\\n\\\n"
        + "You may set the following, additional configuration settings\\n\\\n"
        + "to collect more detailed trace data. You can set these\\n\\\n"
        + "using the 'setconfig' prompt command. Please refer to the\\n\\\n"
        + "documentation for more information on these settings. The\\n\\\n"
        + "values you see here are the current settings:\\n\\\n" //
        + "\\   rhq.trace-command-config={0}\\n\\\n" //
        + "\\   rhq.trace-command-response-results={1,number,#}\\n\\\n" //
        + "\\   rhq.trace-command-size-threshold={2,number,#}\\n\\\n" //
        + "\\   rhq.trace-command-response-size-threshold={3,number,#}")
    String DEBUG_CMD_TRACING_ENABLED = "PromptCommand.debug.cmd-tracing-enabled";

    @I18NMessage("Agent-server communications tracing has been disabled.")
    String DEBUG_CMD_TRACING_DISABLED = "PromptCommand.debug.cmd-tracing-disabled";

    @I18NMessage("failover")
    String FAILOVER = "PromptCommand.failover";

    @I18NMessage("failover --check | --list | --reset | --switch <server>")
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
        + "\\            in the failover list.\\n\\\n"
        + "-s|--switch <server>: Immediately switch to the given server. This switch is\\n\\\n"
        + "\\                      to be considered a temporary change, unless you switch\\n\\\n"
        + "\\                      to the primary server of this agent. If you switch to\\n\\\n"
        + "\\                      a server that is not the primary, the agent will\\n\\\n"
        + "\\                      eventually perform its switchover check which will\\n\\\n"
        + "\\                      bring the agent back to its primary.\\n\\\n"
        + "\\                      <server> can be a server name, in which case\\n\\\n"
        + "\\                      the same transport, port and transport params that are\\n\\\n"
        + "\\                      configured for the current server will stay the same.\\n\\\n"
        + "\\                      If you want to talk to the new server over a\\n\\\n"
        + "\\                      different transport or port, <server> can be specified\\n\\\n"
        + "\\                      as a full endpoint URL.")
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

    @I18NMessage("Switched over to server [{0}]")
    String FAILOVER_IMMEDIATE_SWITCH_DONE = "PromptCommand.failover.immediate_switch-done";

    @I18NMessage("Failed to switch over to server [{0}]. See log file for details.")
    String FAILOVER_IMMEDIATE_SWITCH_FAILED = "PromptCommand.failover.immediate_switch-failed";

    @I18NMessage("update")
    String UPDATE = "PromptCommand.update";

    @I18NMessage("update <--update | --version | --enable | --disable | --download | --status>")
    String UPDATE_SYNTAX = "PromptCommand.update.syntax";

    @I18NMessage("Provides agent update functionality")
    String UPDATE_HELP = "PromptCommand.update.help";

    @I18NMessage("Provides agent update functionality.\\n\\\n"
        + "-u, --update:   Updates the agent now. The current agent will exit.\\n\\\n"
        + "-v, --version:  Checks the version information for the available agent update\\n\\\n"
        + "-e, --enable:   Enables the agent to be able to update itself\\n\\\n"
        + "-d, --disable:  Disallow the agent from being able to update itself\\n\\\n"
        + "-o, --download: Downloads the agent update binary\\n\\\n"
        + "-s, --status:   Indicates if the agent is allowed to update itself or not")
    String UPDATE_DETAILED_HELP = "PromptCommand.update.detailed-help";

    @I18NMessage("Agent update version obtained from [{0}]:\\n\\\n" //
        + "Agent Update Binary Version: {1} ({2})\\n\\\n" //
        + "\\         This Agent Version: {3} ({4})")
    String UPDATE_CHECK_INFO = "PromptCommand.update.check-info";

    @I18NMessage("Agent is up to date.")
    String UPDATE_CHECK_NOT_OOD = "PromptCommand.update.check-not-ood";

    @I18NMessage("Agent is out of date and needs to be updated!")
    String UPDATE_CHECK_OOD = "PromptCommand.update.check-ood";

    @I18NMessage("Agent has the same version as the agent update binary,\\n\\\n"
        + "but it has a different build as the update. Therefore,\\n\\\n"
        + "the agent is out of date and needs to be updated!")
    String UPDATE_CHECK_OOD_STRICT = "PromptCommand.update.check-ood-strict";

    @I18NMessage("Failed to get the agent update version from [{0}]. Cause: {1}")
    String UPDATE_CHECK_FAILED = "PromptCommand.update.check-failed";

    @I18NMessage("Agent updates are enabled.")
    String UPDATE_ENABLED = "PromptCommand.update.enabled";

    @I18NMessage("Agent updates are disabled.")
    String UPDATE_DISABLED = "PromptCommand.update.disabled";

    @I18NMessage("Downloaded the agent update binary to [{0}]")
    String UPDATE_DOWNLOADED = "PromptCommand.update.downloaded";

    @I18NMessage("Failed to download the agent update binary. Cause: {0}")
    String UPDATE_DOWNLOAD_FAILED = "PromptCommand.update.download-failed";

    @I18NMessage("quit")
    String QUIT = "PromptCommand.quit";

    @I18NMessage("version")
    String VERSION = "PromptCommand.version";

    @I18NMessage("version [--sysprops[=name]] [--env[=name]] [--host]")
    String VERSION_SYNTAX = "PromptCommand.version.syntax";

    @I18NMessage("Shows information on agent version and agent environment")
    String VERSION_HELP = "PromptCommand.version.help";

    @I18NMessage("System Properties:")
    String VERSION_SYSPROPS_LABEL = "PromptCommand.version.sysprops-label";

    @I18NMessage("Environment Variables:")
    String VERSION_ENV_LABEL = "PromptCommand.version.env-label";

    @I18NMessage("Host Information:")
    String VERSION_HOST_LABEL = "PromptCommand.version.host-label";

    @I18NMessage("identify")
    String IDENTIFY = "PromptCommand.identify";

    @I18NMessage("identify [endpoint]")
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

    @I18NMessage("The agent is not able to send data right now (is the agent shutdown?)")
    String IDENTIFY_NOT_SENDING = "PromptCommand.identify.not-sending";

    @I18NMessage("This will send a request-for-identification message to a remote server.\\n\\\n"
        + "If you do not specify an endpoint (either as a hostname or full URI), this will\\n\\\n"
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

    @I18NMessage("sleep")
    String SLEEP = "PromptCommand.sleep";

    @I18NMessage("sleep <seconds>")
    String SLEEP_SYNTAX = "PromptCommand.sleep.syntax";

    @I18NMessage("Sleeping...")
    String SLEEP_SLEEPING = "PromptCommand.sleep.sleeping";

    @I18NMessage("Sleeping error: {0}")
    String SLEEP_SLEEPING_ERROR = "PromptCommand.sleep.sleeping-error";

    @I18NMessage("Sleeping done.")
    String SLEEP_SLEEPING_DONE = "PromptCommand.sleep.sleeping-done";

    @I18NMessage("Puts the agent prompt to sleep for a given amount of seconds.")
    String SLEEP_HELP = "PromptCommand.sleep.help";

    @I18NMessage("Puts the agent prompt to sleep for a given amount of seconds.\\n\\\n"
        + "This does not put the entire agent to sleep, only the prompt.\\n\\\n"
        + "This is normally useful when testing the agent.")
    String SLEEP_DETAILED_HELP = "PromptCommand.sleep.detailed-help";

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

    @I18NMessage("Failed to flush the new configuration. Cause: {0}")
    String SETCONFIG_FLUSH_FAILED = "PromptCommand.setconfig.flush-failed";

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

    @I18NMessage("The plugin container has been stopped gracefully (i.e. all threads have willingly terminated).")
    String PLUGIN_CONTAINER_STOP_DONE_GRACEFULLY = "PromptCommand.plugin-container.stop.done-gracefully";

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

    @I18NMessage("Summary of installed plugins:\\n{0}")
    String PLUGINS_LISTING_PLUGINS_SUMMARY = "PromptCommand.plugins.listing-plugins-summary";

    @I18NMessage("The following plugins will be disabled:\\n{0}")
    String PLUGINS_LISTING_PLUGINS_DISABLED = "PromptCommand.plugins.listing-plugins-disabled";

    @I18NMessage("Details of the plugins that are currently installed:")
    String PLUGINS_LISTING_PLUGINS_DETAILS = "PromptCommand.plugins.listing-plugins-details";

    @I18NMessage("Total number of plugins currently installed: [{0}]")
    String PLUGINS_NUM_CURRENT_PLUGINS = "PromptCommand.plugins.num-current-plugins";

    @I18NMessage("{0}")
    String PLUGINS_PLUGINS_INFO_FILENAME = "PromptCommand.plugins.plugin-info.filename";

    @I18NMessage("Plugin Name:  {0}")
    String PLUGINS_PLUGINS_INFO_NAME = "PromptCommand.plugins.plugin-info.name";

    @I18NMessage("Display Name: {0}")
    String PLUGINS_PLUGINS_INFO_DISPLAY_NAME = "PromptCommand.plugins.plugin-info.display-name";

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

    @I18NMessage("The agent is waiting for plugins to be downloaded...")
    String WAITING_FOR_PLUGINS = "AgentMain.waiting-for-plugins";

    @I18NMessage("The agent is waiting for plugins to be downloaded to [{0}]...")
    String WAITING_FOR_PLUGINS_WITH_DIR = "AgentMain.waiting-for-plugins-with-dir";

    @I18NMessage("[{0}] plugins downloaded.")
    String DONE_WAITING_FOR_PLUGINS = "AgentMain.done-waiting-for-plugins";

    @I18NMessage("Done! The agent is now registered with the server.")
    String WAITING_TO_BE_REGISTERED_END = "AgentMain.waiting-to-be-registered-end";

    @I18NMessage("The wait time has expired and the agent is still not registered.")
    String CANNOT_WAIT_TO_BE_REGISTERED_ANY_LONGER = "AgentMain.waiting-to-be-registered-timeout";

    @I18NMessage("The name of this agent is not defined and cannot be generated - you cannot start the agent until you give it a valid name.")
    String AGENT_NAME_NOT_DEFINED = "AgentMain.agent-name-not-defined";

    @I18NMessage("The name of this agent was not predefined so it was auto-generated. The agent name is now [{0}]")
    String AGENT_NAME_AUTO_GENERATED = "AgentMain.agent-name-auto-generated";

    @I18NMessage("Agent is not starting the plugin container at startup, as per its configuration")
    String NOT_STARTING_PLUGIN_CONTAINER_AT_STARTUP = "AgentMain.not-starting-pc-at-startup";

    @I18NMessage("Agent is not notifying the server that it is shutting down, as per its configuration")
    String TOLD_TO_NOT_NOTIFY_SERVER_OF_SHUTDOWN = "AgentMain.told-to-not-notify-server-of-shutdown";

    @I18NMessage("Agent failed to shutdown component [{0}]. Cause: {1}")
    String FAILED_TO_SHUTDOWN_COMPONENT = "AgentMain.failed-to-shutdown-component";

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

    @I18NMessage("listdata")
    String LISTDATA = "PromptCommand.listdata";

    @I18NMessage("listdata [--delete] [--recurse] [--verbose] <'bundles'|path_name>")
    String LISTDATA_SYNTAX = "PromptCommand.listdata.syntax";

    @I18NMessage("Lists and optionally deletes agent data files. USE WITH CAUTION!")
    String LISTDATA_HELP = "PromptCommand.listdata.help";

    @I18NMessage("Lists files found in the data directory, optionally deleting them.\\n\\\n"
        + "This is an advanced command for administrators use only.  You should not\\n\\\n"
        + "delete data files unless you know what you are doing. You could render\\n\\\n"
        + "the agent useless if you delete files that you should not.\\n\\\n"
        + "\\  -v, --verbose : enables more detailed file lists\\n\\\n"
        + "\\  -r, --recurse : recurse into subdirectories\\n\\\n"
        + "\\  -d, --delete : delete the files that are listed\\n\\\n"
        + "\\  'bundles'|path_name : the relative path under the data directory to list.\\n\\\n"
        + "\\                        If 'bundles', will list data files from the bundle\\n\\\n"
        + "\\                        subsystem.")
    String LISTDATA_DETAILED_HELP = "PromptCommand.listdata.detailed-help";

    @I18NMessage("You cannot use .. in the path - you can only list files within the data directory")
    String LISTDATA_DOTDOT_NOT_ALLOWED = "PromptCommand.listdata.dotdot-not-allowed";

    @I18NMessage("You can only list files within the data directory - no absolute paths allowed")
    String LISTDATA_ABSOLUTE_NOT_ALLOWED = "PromptCommand.listdata.absolute-not-allowed";

    @I18NMessage("Agent data directory: [{0}]")
    String LISTDATA_DATA_DIR = "PromptCommand.listdata.data_dir";

    @I18NMessage("File not found: [{0}]")
    String LISTDATA_FILE_NOT_FOUND = "PromptCommand.listdata.file_not_found";

    @I18NMessage("Deleted: [{0}]")
    String LISTDATA_DELETED = "PromptCommand.listdata.deleted";

    @I18NMessage("Delete failed: [{0}]")
    String LISTDATA_DELETED_FAILED = "PromptCommand.listdata.deleted_failed";

    @I18NMessage("{0}")
    String LISTDATA_FILE_INFO = "PromptCommand.listdata.file_info";

    @I18NMessage("{1,date,short} {1,time,short}\t{2} bytes\t{0}")
    String LISTDATA_FILE_INFO_VERBOSE = "PromptCommand.listdata.file_info_verbose";

    @I18NMessage("{0}")
    String LISTDATA_DIR_INFO = "PromptCommand.listdata.dir_info";

    @I18NMessage("{1,date,short} {1,time,short}\t{2} files\t{0}")
    String LISTDATA_DIR_INFO_VERBOSE = "PromptCommand.listdata.dir_info_verbose";

    @I18NMessage("gc")
    String GC = "PromptCommand.gc";

    @I18NMessage("gc [--dump] [--free] [--verbose={true|false}]")
    String GC_SYNTAX = "PromptCommand.gc.syntax";

    @I18NMessage("Helps free up memory by invoking the garbage collector")
    String GC_HELP = "PromptCommand.gc.help";

    @I18NMessage("Provides an interface to the garbage collector and memory subsystem.\\n\\\n"
        + "\\  -d, --dump : prints out information on current memory usage\\n\\\n"
        + "\\  -f, --free : attempts to free up memory and invokes the garbage collector\\n\\\n"
        + "\\  -v, --verbose={true|false} : enables/disables verbose gc messages")
    String GC_DETAILED_HELP = "PromptCommand.gc.detailed-help";

    @I18NMessage("Invoking the garbage collector")
    String GC_INVOKE = "PromptCommand.gc.invoke";

    @I18NMessage("{0} ({1}):\\n\\\n" //
        + "\\      init={2}\\n\\\n" //
        + "\\       max={3}\\n\\\n" //
        + "\\      used={4} ({5,number,##}% of committed)\\n\\\n" //
        + "\\    commit={6} ({7,number,##}% of max)")
    String GC_MEM_USAGE = "PromptCommand.gc.mem-usage";

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

    @I18NMessage("discovery [--plugin=<plugin name>] [--resourceType=<type name>]\\n\\\n" //
        + "\\                  [--resourceId=<id>] [--verbose]\\n\\\n" //
        + "\\        discovery --full [--verbose]\\n\\\n" //
        + "\\        discovery --blacklist={list|clear}")
    String DISCOVERY_SYNTAX = "PromptCommand.discovery.syntax";

    @I18NMessage("Asks a plugin to run a server scan discovery")
    String DISCOVERY_HELP = "PromptCommand.discovery.help";

    @I18NMessage("Asks a plugin to run a discovery scan. This is a way to determine what\\n\\\n"
        + "resources a plugin can actually find.  Note that this will run a server scan\\n\\\n"
        + "not a service scan (i.e. it will not try to discover child services for parent\\n\\\n"
        + "servers already in inventory) unless you use --full or --resourceId. Also\\n\\\n"
        + "note that only --full will actually send an inventory report to the server.\\n\\\n"
        + "The valid command line arguments are:\\n\\\n"
        + "\\  -f, --full : Runs a detailed discovery inside the plugin container.\\n\\\n"
        + "\\               This will update the actual agent inventory by sending\\n\\\n"
        + "\\               an inventory report to the server.\\n\\\n"
        + "\\               This ignores --plugin, --resourceType and --resourceId.\\n\\\n"
        + "\\  -p, --plugin=<name> : The name of the plugin whose discovery will run.\\n\\\n"
        + "\\                        If you do not specify a plugin, all plugins will\\n\\\n"
        + "\\                        run their discovery.\\n\\\n"
        + "\\  -r, --resourceType=<type> : specifies the specific resource type to be\\n\\\n"
        + "\\                              discovered.  If not specified, all resource\\n\\\n"
        + "\\                              types that a plugin supports will be\\n\\\n"
        + "\\                              discovered (and if no plugin was specified,\\n\\\n"
        + "\\                              then all resource types for all plugins will\\n\\\n"
        + "\\                              be discovered).\\n\\\n"
        + "\\  -i, --resourceId=<id> : specifies the specific resource ID whose\\n\\\n"
        + "\\                          services are to be discovered.  If specified,\\n\\\n"
        + "\\                          a service scan is performed as opposed to a\\n\\\n"
        + "\\                          server scan and the --plugin/--resourceType\\n\\\n"
        + "\\                          options are ignored.\\n\\\n"
        + "\\  -v, --verbose : If doing a non-full discovery, this prints the plugin\\n\\\n"
        + "\\                  configuration of each discovered resource. For --full\\n\\\n"
        + "\\                  scans, this will dump all resources and errors found.\\n\\\n"
        + "\\  -b, --blacklist={list|clear} : Operates on the blacklist which determines\\n\\\n"
        + "\\                                 which resource types are not discoverable.\\n\\\n"
        + "\\                                 (note that specifying this option will not\\n\\\n"
        + "\\                                 run an actual discovery scan)\\n\\\n"
        + "\\                                 'list' prints blacklisted resource types.\\n\\\n"
        + "\\                                 'clear' delists all resource types which\\n\\\n"
        + "\\                                 re-enables all types to be discoverable.")
    String DISCOVERY_DETAILED_HELP = "PromptCommand.discovery.detailed-help";

    @I18NMessage("WARNING: Discovery will not be run for the following ResourceTypes, because they are blacklisted: {0}")
    String DISCOVERY_BLACKLISTED_TYPES = "PromptCommand.discovery.blacklisted-types";

    @I18NMessage("A discovery scan is already in progress. Please wait for it to complete and then try again.")
    String DISCOVERY_SCAN_ALREADY_IN_PROGRESS = "PromptCommand.discovery.scan-already-in-progress";

    @I18NMessage("Full discovery run in [{0}] ms")
    String DISCOVERY_FULL_RUN = "PromptCommand.discovery.full-run";

    @I18NMessage("Service discovery run for resource [{0}]")
    String DISCOVERY_RESOURCE_SERVICES = "PromptCommand.discovery.resource-service-scan";

    @I18NMessage("The given resource ID [{0,number,#}] does not match any resource in inventory.")
    String DISCOVERY_RESOURCE_ID_INVALID = "PromptCommand.discovery.resource-id-invalid";

    @I18NMessage("=== {0} Inventory Report ===\\n\\\n" //
        + "Start Time:         {1,date,medium} {1,time,medium}\\n\\\n" //
        + "Finish Time:        {2,date,medium} {2,time,medium}\\n\\\n" //
        + "New Resource Count: {3}\\n\\\n")
    String DISCOVERY_INVENTORY_REPORT_SUMMARY = "PromptCommand.discovery.inventory-report-summary";

    @I18NMessage("Resource: {0}")
    String DISCOVERY_INVENTORY_REPORT_RESOURCE = "PromptCommand.discovery.inventory-report-resource";

    @I18NMessage("Error: {0}")
    String DISCOVERY_INVENTORY_REPORT_ERROR = "PromptCommand.discovery.inventory-report-error";

    @I18NMessage("Blacklist: {0}")
    String DISCOVERY_BLACKLIST_LIST = "PromptCommand.discovery.blacklist.list";

    @I18NMessage("Blacklist has been cleared. All resource types are re-enabled for discovery.")
    String DISCOVERY_BLACKLIST_CLEAR = "PromptCommand.discovery.blacklist.clear";

    @I18NMessage("You must first start the plugin container before attempting discovery.")
    String DISCOVERY_PC_NOT_STARTED = "PromptCommand.discovery.pc-not-started";

    @I18NMessage("NOTE: The Agent is not currently connected to the Server, so the discovery reports will not be sent to the Server.")
    String DISCOVERY_AGENT_NOT_CONNECTED_TO_SERVER = "PromptCommand.discovery.agent-not-connected-to-server";

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

    @I18NMessage("inventory [--sync] [--xml] [--export=<file>] [--norecurse]\\n\\\n"
        + "\\                  [--id=<#>] | --types | <inventory-binary-file>]")
    String INVENTORY_SYNTAX = "PromptCommand.inventory.syntax";

    @I18NMessage("Provides information about the current inventory of resources")
    String INVENTORY_HELP = "PromptCommand.inventory.help";

    @I18NMessage("This will allow you to view the resources currently in inventory.\\n\\\n"
        + "The valid command line arguments are:\\n\\\n"
        + "\\ -s, --sync : Purges the agent's inventory and re-syncs it with the server.\\n\\\n"
        + "\\              This forces the agent's plugin container to restart.\\n\\\n"
        + "\\              All other options are ignored if this is specified.\\n\\\n"
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

    @I18NMessage("Cannot sync inventory - not currently able to talk to the RHQ Server.")
    String INVENTORY_ERROR_NOT_SENDING = "PromptCommand.inventory.error-not-sending";

    @I18NMessage("Purged the persisted inventory found at [{0}], sync will occur when plugin container restarts")
    String INVENTORY_DATA_FILE_DELETED = "PromptCommand.inventory.data-file-deleted";

    @I18NMessage("Failed to purge the inventory data file [{0}], sync may fail.")
    String INVENTORY_DATA_FILE_DELETION_FAILURE = "PromptCommand.inventory.data-file-deletion-failure";

    @I18NMessage("avail")
    String AVAILABILITY = "PromptCommand.availability";

    @I18NMessage("avail [--changed] [--force] [--verbose|--quiet]")
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
        + "\\ -f, --force   : if specified, the report will force availability\\n\\\n"
        + "\\                 checks for all resources.\\n\\\n"
        + "\\ -v, --verbose : if specified, additional resource information is displayed.\\n\\\n"
        + "\\ -q, --quiet : if specified, don't show individual resource information.")
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

    @I18NMessage("Restarting the plugin container due to previous failure to merge the upgrade results with the server.")
    String RESTARTING_PLUGIN_CONTAINER_AFTER_UPGRADE_MERGE_FAILURE = "AgentMain.pc-conditional-restart";

    @I18NMessage("Restoring the original security token.")
    String RESTORING_SECURITY_TOKEN = "AgentMain.restoring-security-token";

    @I18NMessage("The config file already has a security token defined. The original security token will be thrown away.")
    String NOT_RESTORING_SECURITY_TOKEN = "AgentMain.not-restoring-security-token";

    @I18NMessage("Starting polling to determine sender status")
    String PING_EXECUTOR_STARTING_POLLING = "AgentMain.ping-executor.starting-polling";

    @I18NMessage("Stopping polling and resuming pinging")
    String PING_EXECUTOR_STOPPING_POLLING_RESUME_PING = "AgentMain.ping-executor.stop-polling-resume-ping";

    @I18NMessage("Starting polling to determine sender status (server ping failed)")
    String PING_EXECUTOR_STARTING_POLLING_AFTER_EXCEPTION = "AgentMain.ping-executor.start-polling-after-exception";

    @I18NMessage("Server ping failed")
    String PING_EXECUTOR_SERVER_PING_FAILED = "AgentMain.ping-executor.server-ping-failed";
}
