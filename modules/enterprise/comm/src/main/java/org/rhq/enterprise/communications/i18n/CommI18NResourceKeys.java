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
package org.rhq.enterprise.communications.i18n;

import mazz.i18n.annotation.I18NMessage;
import mazz.i18n.annotation.I18NMessages;
import mazz.i18n.annotation.I18NResourceBundle;

/**
 * I18N resource bundle keys that identify the messages needed by the communications module.
 *
 * @author John Mazzitelli
 */
@I18NResourceBundle(baseName = "comm-messages", defaultLocale = "en")
public interface CommI18NResourceKeys {
    @I18NMessages( { @I18NMessage("Added the polling listener [{0}]") })
    String SERVER_POLLING_THREAD_ADDED_POLLING_LISTENER = "ServerPollingThread.added-polling-listener";

    @I18NMessages( { @I18NMessage("Removed the polling listener [{0}]") })
    String SERVER_POLLING_THREAD_REMOVED_POLLING_LISTENER = "ServerPollingThread.removed-polling-listener";

    @I18NMessages( { @I18NMessage("Communicator is changing endpoint from [{0}] to [{1}]") })
    String COMMUNICATOR_CHANGING_ENDPOINT = "JBossRemotingRemoteCommunicator.changing-endpoint";

    @I18NMessages( { @I18NMessage("The initialize callback has finished. Callback to be invoked again? [{0}]") })
    String INITIALIZE_CALLBACK_DONE = "JBossRemotingRemoteCommunicator.init-callback-done";

    @I18NMessages( { @I18NMessage("The initialize callback has failed. It will be tried again. Cause: {0}") })
    String INITIALIZE_CALLBACK_FAILED = "JBossRemotingRemoteCommunicator.init-callback-failed";

    @I18NMessages( { @I18NMessage("The remote server did not reply with a valid command response. Reply was: [{0}]") })
    String COMM_CCE = "JBossRemotingRemoteCommunicator.comm-cce";

    @I18NMessages( { @I18NMessage("Invalid number of semaphore permits [{0}] configured for [{1}]. Defaulting to [{2}]") })
    String INVALID_PERMITS_CONFIG = "ConcurrencyManager.invalid-permits-config";

    @I18NMessages( { @I18NMessage("Confirmed that keystore already exists: [{0}]"),
        @I18NMessage(value = "Bestätigt, dass der Keystore schon besteht: [{0}]", locale = "de") })
    String KEYSTORE_EXISTS = "SecurityUtil.keystore-exists";

    @I18NMessages( { @I18NMessage("The keystore directory [{0}] does not exist and cannot be created") })
    String CANNOT_CREATE_KEYSTORE_DIR = "SecurityUtil.cannot-create-keystore-dir";

    @I18NMessages( { @I18NMessage("Keystore password must not be null"),
        @I18NMessage(value = "Das Keystore-Passwort darf nicht leer sein", locale = "de") })
    String KEYSTORE_PASSWORD_NULL = "SecurityUtil.keystore-password-null";

    @I18NMessages( { @I18NMessage("Keystore password must be at least 6 characters long ") })
    String KEYSTORE_PASSWORD_MIN_LENGTH = "SecurityUtil.keystore-password-min-length";

    @I18NMessages( {
        @I18NMessage("Key password is not specified; when creating the keystore [{0}], the key password will be the same as the keystore password"),
        @I18NMessage(value = "Das Passwort für den Schlüssel wurde beim Anlegen des Keystores [{0}] nicht angegeben, als Passwort für den Schlüssel wird das des Keystores verwendet", locale = "de") })
    String KEY_PASSWORD_UNSPECIFIED = "SecurityUtil.key-password-unspecified";

    @I18NMessages( { @I18NMessage("Keystore password must be at least 6 characters long"),
        @I18NMessage(value = "Das Password für den Keystore muss mindestens 6 Zeichen lang sein", locale = "de") })
    String KEYSTORE_PASSWORD_NOT_LONG_ENOUGH = "SecurityUtil.keystore-password-not-long-enough";

    @I18NMessages( { @I18NMessage("Key password must be at least 6 characters long"),
        @I18NMessage(value = "Das Passwort für den Schlüssel muss mindestens 6 Zeichen lang sein", locale = "de") })
    String KEY_PASSWORD_NOT_LONG_ENOUGH = "SecurityUtil.key-password-not-long-enough";

    @I18NMessages( { @I18NMessage("Failed to create keystore [{0}] with arguments [{1}]"),
        @I18NMessage(value = "Konte den Keystore [{0}] mit den Argumenten [{1}] nicht anlegen", locale = "de") })
    String KEYSTORE_CREATION_FAILURE = "SecurityUtil.keystore-creation-failure";

    @I18NMessages( { @I18NMessage("Keystore has been created at [{0}]"),
        @I18NMessage(value = "Keystore wurde unter [{0}] angelegt", locale = "de") })
    String KEYSTORE_CREATED = "SecurityUtil.keystore-created";

    @I18NMessages( { @I18NMessage("PING ACKNOWLEDGE"), @I18NMessage(value = "PING BESTÄTIGUNG", locale = "de") })
    String PING_ACK = "PingImpl.ping-ack";

    @I18NMessages( { @I18NMessage("The given pojo [{0}] does not implement the interface [{1}]"),
        @I18NMessage(value = "Das übergebene POJO [{0}] implmentiert die Schnittstelle [{1}] nicht", locale = "de") })
    String INVALID_POJO_INTERFACE = "RemotePojoInvocationCommandService.invalid-pojo-interface";

    @I18NMessages( { @I18NMessage("The given interface [{0}] is not valid. Cause: {1}"),
        @I18NMessage(value = "Die gegebene Schnittstelle ist ungültig. Grund: {1} ", locale = "de") })
    String INVALID_INTERFACE = "RemotePojoInvocationCommandService.invalid-interface";

    @I18NMessages( {
        @I18NMessage("There is no remote POJO that can service the method invocation request. Command: {0} "),
        @I18NMessage(value = "Es gibt kein entferntes Objekt, das die Anfrage bearbeiten kann. Kommando: {0}", locale = "de") })
    String NO_POJO_SERVICE = "RemotePojoInvocationCommandService.no-pojo-service";

    @I18NMessages( { @I18NMessage("Failed to execute a remote pojo invocation") })
    String REMOTE_POJO_EXECUTE_FAILURE = "RemotePojoInvocationCommandService.remote-pojo-execute-failure";

    @I18NMessages( { @I18NMessage("Command not permitted - server reached its limit of concurrent invocations [{0}]. Retry in [{1}]ms") })
    String COMMAND_NOT_PERMITTED = "CommandService.remote-pojo-execute-not-permitted";

    @I18NMessages( { @I18NMessage("Command does not define any parameter definitions and accepts all parameters") })
    String NO_PARAM_DEF_ACCEPTS_ALL = "AbstractCommand.no-param-def-accepts-all";

    @I18NMessages( { @I18NMessage("Parameter name must not be null"),
        @I18NMessage(value = "Der Name des Parameters darf nicht leer sein", locale = "de") })
    String NULL_PARAM_NAME = "AbstractCommand.null-param-name";

    @I18NMessages( { @I18NMessage("Cannot add a parameter whose name is null"),
        @I18NMessage(value = "Kann keinen Parameter hinzufügen dessen Name null ist", locale = "de") })
    String NULL_PARAM_NAME_SET = "AbstractCommand.null-param-name-set";

    @I18NMessages( { @I18NMessage("Cannot remove a parameter whose name is null"),
        @I18NMessage(value = "Kann keinen Parameter löschen dessen Name null ist", locale = "de") })
    String NULL_PARAM_NAME_REMOVE = "AbstractCommand.null-param-name-remove";

    @I18NMessages( { @I18NMessage("Missing required field [{0}]; command=[{1}]"),
        @I18NMessage(value = "Das Pflichtfeld [{0}] für das Kommando [{1}] fehlt", locale = "de") })
    String MISSING_REQUIRED_FIELD = "AbstractCommand.missing-required-field";

    @I18NMessages( {
        @I18NMessage("Required field [{0}] must be of type [{1}] with nullable set to [{2}] but was a [{3}]; command=[{4}]"),
        @I18NMessage(value = "Das Pflichtfeld [{0}] muss vom Typ [{1}] sein mit nullable auf [{2}] gesetzt, war aber [{3}]; Kommando [{4}]", locale = "de") })
    String BAD_REQUIRED_PARAM_TYPE = "AbstractCommand.bad-required-param-type";

    @I18NMessages( {
        @I18NMessage("Optional field [{0}] must be of type [{1}] with nullable set to [{2}] but was a [{3}]; command=[{4}]"),
        @I18NMessage(value = "Das optinale Feld [{0}] muss vom Typ [{1}] sein mit nullable auf [{2}] gesetzt, war aber [{3}]; Kommando [{4}]", locale = "de") })
    String BAD_OPTIONAL_PARAM_TYPE = "AbstractCommand.bad-optional-param-type";

    @I18NMessages( {
        @I18NMessage("Unexpected parameter with the name [{0}] has been specified; command=[{1}]"),
        @I18NMessage(value = "Es wurde ein unerwarteter Parameter mit Namen [{0}] angegeben; Kommando=[{1}]", locale = "de") })
    String UNEXPECTED_PARAM = "AbstractCommand.unexpected-param";

    @I18NMessages( { @I18NMessage("This should never have occurred"),
        @I18NMessage(value = "Dies hätte nie passieren dürfen", locale = "de") })
    String SHOULD_NOT_OCCUR = "AbstractCommand.should-not-occur";

    @I18NMessages( { @I18NMessage("Command type must not be null"),
        @I18NMessage(value = "Der Typ des Kommandos darf nicht null sein", locale = "de") })
    String NULL_COMMAND_TYPE = "AbstractCommand.null-command-type";

    @I18NMessages( { @I18NMessage("Cannot have mutiple parameter definitions with the same name [{0}]"),
        @I18NMessage(value = "Mehrere Parameter mit dem selben Namen [{0}] sind nicht erlaubt", locale = "de") })
    String DUP_DEFS = "AbstractCommand.dup-defs";

    @I18NMessages( {
        @I18NMessage("Command type name must be 1 or more non-space characters"),
        @I18NMessage(value = "Der Name des Kommandotyps muss einen oder mehrere Zeichen enthalten, die keine Leerzeichen sind", locale = "de") })
    String EMPTY_NAME = "CommandType.empty-name";

    @I18NMessages( { @I18NMessage("Command type name/version string must not be null"),
        @I18NMessage(value = "Der Name oder die Version des Kommandotyps darf nicht null sein", locale = "de") })
    String NULL_NAME_VERSION = "CommandType.null-name-version";

    @I18NMessages( { @I18NMessage("Command type name/version string was invalid [{0}]"),
        @I18NMessage(value = "Der Name oder die Version des Kommandotyps war ungültig [{0}]", locale = "de") })
    String INVALID_NAME_VERSION = "CommandType.invalid-name-version";

    @I18NMessages( { @I18NMessage("Command type name/version string is missing the name [{0}]"),
        @I18NMessage(value = "Dem Kommandotyp fehlt der name [{0}]", locale = "de") })
    String NAME_MISSING = "CommandType.name-missing";

    @I18NMessages( { @I18NMessage("Command type name/version string has an invalid version number [{0}]"),
        @I18NMessage(value = "Der Kommandotyp hat eine ungültige Versionsnummer [{0}]", locale = "de") })
    String VERSION_INVALID = "CommandType.version-invalid";

    @I18NMessages( { @I18NMessage("Cannot compare command types with different names: [{0}] != [{1}]") })
    String CANNOT_COMPARE_DIFF_NAMES = "CommandType.cannot-compare-diff-names";

    @I18NMessages( { @I18NMessage("Failed to notify callback of response [{0}]") })
    String CALLBACK_FAILED = "ClientCommandSenderTask.callback-failed";

    @I18NMessages( { @I18NMessage("Failed to send command [{0}]. Cause: {1}"),
        @I18NMessage(value = "Das Kommando [{0}] konnte nicht gesendet werden. Grund: {1}", locale = "de") })
    String SEND_FAILED = "ClientCommandSenderTask.send-failed";

    @I18NMessages( {
        @I18NMessage("Failed to ping the agent at [{0}], because we could not connect to it."),
        @I18NMessage(locale = "de", value = "Ein Ping zum Agent unter [{0}] ist fehlgeschlagen, da der Agent nicht kontaktiert werden konnte") })
    String AGENT_PING_FAILED = "ClientCommandSenderTask.agent-ping-failed";

    @I18NMessages( { @I18NMessage("The command that failed has its guaranteed-delivery flag set so it is being queued again") })
    String QUEUING_FAILED_COMMAND = "ClientCommandSenderTask.queuing-failed-command";

    @I18NMessages( { @I18NMessage("Failed to re-queue the command - it is lost: [{0}]") })
    String CLIENT_COMMAND_SENDER_TASK_REQUEUE_FAILED = "ClientCommandSenderTask.requeue-failed";

    @I18NMessages( { @I18NMessage("Failed to send command"),
        @I18NMessage(value = "Das Kommando konnte nicht gesendet werden", locale = "de") })
    String SEND_FAILED_NO_ARG = "ClientCommandSenderTask.send-failed-no-arg";

    @I18NMessages( { @I18NMessage("Server polling thread has started and will poll for server status every [{0}] milliseconds") })
    String SERVER_POLLING_THREAD_STARTED = "ServerPollingThread.started";

    @I18NMessages( {
        @I18NMessage("The server has come back online; client has been told to start sending commands again"),
        @I18NMessage(value = "Der Server ist wieder online. Der Client wird wieder Kommandos senden", locale = "de") })
    String SERVER_POLLING_THREAD_SERVER_ONLINE = "ServerPollingThread.server-online";

    @I18NMessages( {
        @I18NMessage("The server has gone offline; client has been told to stop sending commands"),
        @I18NMessage(value = "Der Server ist offline gegangen. Der Client wird keine weitere Kommandos senden", locale = "de") })
    String SERVER_POLLING_THREAD_SERVER_OFFLINE = "ServerPollingThread.server-offline";

    @I18NMessages( { @I18NMessage("Failed to successfully poll the server. This is normally due to the server not being up yet. You can usually ignore this message since it will be tried again later, however, you should ensure this failure was not really caused by a misconfiguration. Cause: {0}") })
    String SERVER_POLL_FAILURE = "ServerPollingThread.server-poll-failure";

    @I18NMessages( { @I18NMessage("Server polling thread has been stopped") })
    String SERVER_POLLING_THREAD_STOPPED = "ServerPollingThread.stopped";

    @I18NMessages( { @I18NMessage("Send throttling is configured for [{0}] max commands and quiet periods of [{1}] milliseconds. These will take effect while send throttling is enabled.") })
    String SEND_THROTTLE_CONFIGURED = "SendThrottle.configured";

    @I18NMessages( { @I18NMessage("Send throttling is now enabled - a maximum of [{0}] commands are allowed to be sent in between quiet periods of [{1}] milliseconds.") })
    String SEND_THROTTLE_ENABLED = "SendThrottle.enabled";

    @I18NMessages( { @I18NMessage("Send throttling is disabled - commands will be sent as fast as possible") })
    String SEND_THROTTLE_DISABLED = "SendThrottle.disabled";

    @I18NMessages( { @I18NMessage("Cannot kill the send throttle quiet period thread") })
    String SEND_THROTTLE_CANNOT_KILL = "SendThrottle.cannot-kill";

    @I18NMessages( { @I18NMessage("The purge percentage [{0}] must be between 0 and 99") })
    String INVALID_PURGE_PERCENTAGE = "PersistentFifo.invalid-purge-percentage";

    @I18NMessages( { @I18NMessage("The max size [{0}] must be greater than or equal to [{1}]"),
        @I18NMessage(value = "Die Maximalgrüße [{0}] muss größer oder gleich [{1}] sein", locale = "de") })
    String INVALID_MAX_SIZE = "PersistentFifo.invalid-max-size";

    @I18NMessages( { @I18NMessage("Failed to execute remote POJO method [{0}]. Cause: {1}") })
    String CLIENT_REMOTE_POJO_INVOKER_EXECUTION_FAILURE = "ClientRemotePojoFactory.execution-failure";

    @I18NMessages( { @I18NMessage("Command queue configuration must not be null"),
        @I18NMessage(value = "Die Konfiguration für die Warteschlange darf nicht leer sein", locale = "de") })
    String COMMAND_QUEUE_NULL_CONFIG = "CommandQueue.null-config";

    @I18NMessages( { @I18NMessage("Queue throttling is configured for [{0}] max commands in burst periods of [{1}] milliseconds.  These will take effect when queue throttling is enabled.") })
    String COMMAND_QUEUE_CONFIGURED = "CommandQueue.configured";

    @I18NMessages( { @I18NMessage("Command Queue is now throttling - a maximum of [{0}] commands are allowed to be dequeued every [{1}] milliseconds") })
    String COMMAND_QUEUE_ENABLED = "CommandQueue.enabled";

    @I18NMessages( { @I18NMessage("Command Queue is not throttling - commands will be dequeued as fast as possible") })
    String COMMAND_QUEUE_DISABLED = "CommandQueue.disabled";

    @I18NMessages( { @I18NMessage("Command response: {0}"),
        @I18NMessage(value = "Antwort auf das Kommando: {0}", locale = "de") })
    String CMDLINE_CLIENT_RESPONSE = "CmdlineClient.response";

    @I18NMessages( { @I18NMessage("Failed to execute command: {0}"),
        @I18NMessage(value = "Konnte das Kommando {0} nicht ausführen ", locale = "de") })
    String CMDLINE_CLIENT_EXECUTE_FAILURE = "CmdlineClient.execute-failure";

    @I18NMessages( { @I18NMessage("Failed to process the command line arguments"),
        @I18NMessage(value = "Konnte die Kommandozeilenparameter nicht verarbeiten", locale = "de") })
    String CMDLINE_CLIENT_PROCESS_ARGS_FAILURE = "CmdlineClient.process-args-failure";

    @I18NMessages( { @I18NMessage("Using generic command client to issue an unknown command: [{0}]") })
    String CMDLINE_CLIENT_USING_GENERIC_CLIENT = "CmdlineClient.using-generic-client";

    @I18NMessages( { @I18NMessage("Locator URI must not be null"),
        @I18NMessage(value = "Die URI darf nicht leer sein", locale = "de") })
    String CMDLINE_CLIENT_NULL_URI = "CmdlineClient.null-uri";

    @I18NMessages( { @I18NMessage("[options] [paramName[=[paramValue]]\\n\\\n"
        + "\\n\\\n"
        + "options:\\n\\\n"
        + "    -h, --help                         Show this help message\\n\\\n"
        + "    --                                 Stop processing options\\n\\\n"
        + "    -p, --pkgs=<packages>              Defines command client class locations (colon-separated)\\n\\\n"
        + "    -c, --cmd=<command name>           Set the name of the command to issue\\n\\\n"
        + "    -v, --version                      Set the version of the command to issue\\n\\\n"
        + "    -l, --class=<classname>            Defines the command client class to use; this ignores --pkgs\\n\\\n"
        + "    -u, --uri=<URI>                    Sets the locator URI used to connect to the server\\n\\\n"
        + "    -s, --subsystem=<subsystem name>   Sets the subsystem name of the server's command processor handler\\n\\\n"
        + "\\n\\\n"
        + "paramName                              Sets the name of a command parameter\\n\\\n"
        + "paramValue                             Sets the value of the command parameter, 'true' if unspecified\\n\\\n"
        + "\\n\\\n"
        + "Note that -c and -u are required options in order to be able to issue any commands to the command processor.\\n\\\n"
        + "If the command type's specific command client can not be found, will attempt with a generic client.\\n") })
    String CMDLINE_CLIENT_USAGE = "CmdlineClient.usage";

    @I18NMessages( { @I18NMessage("{0}: command parameter: {1}={2}"),
        @I18NMessage(value = "{0}: Kommandoparameter {1}={2}", locale = "de") })
    String CMDLINE_CLIENT_CMDLINE_PARAM = "CmdlineClient.cmdline-param";

    @I18NMessages( { @I18NMessage("Command version [{0}] is invalid. Cause: {1}"),
        @I18NMessage(value = "Kommandoversion [{0}] ist ungültig. Grund: {1}", locale = "de") })
    String CMDLINE_CLIENT_INVALID_CMD_VERSION = "CmdlineClient.invalid-cmd-version";

    @I18NMessages( { @I18NMessage("{0}: command class packages: {1}") })
    String CMDLINE_CLIENT_PACKAGES = "CmdlineClient.packages";

    @I18NMessages( { @I18NMessage("{0}: command client class name: {1}") })
    String CMDLINE_CLIENT_CLASSNAME = "CmdlineClient.classname";

    @I18NMessages( { @I18NMessage("{0}: command: {1}"), @I18NMessage(value = "{0}: Kommando: {1}", locale = "de") })
    String CMDLINE_CLIENT_COMMAND = "CmdlineClient.command";

    @I18NMessages( { @I18NMessage("{0}: locator uri: {1}") })
    String CMDLINE_CLIENT_LOCATOR_URI = "CmdlineClient.locator-uri";

    @I18NMessages( { @I18NMessage("{0}: subsystem: {1}"), @I18NMessage(value = "{0}: Subsystem: {1}", locale = "de") })
    String CMDLINE_CLIENT_SUBSYSTEM = "CmdlineClient.subsystem";

    @I18NMessages( { @I18NMessage("unhandled option code: {0}") })
    String CMDLINE_CLIENT_UNHANDLED_OPTION = "CmdlineClient.unhandled-option";

    @I18NMessages( { @I18NMessage("cannot find command client class - no command specified") })
    String CMDLINE_CLIENT_CANNOT_FIND_CLIENT = "CmdlineClient.cannot-find-client";

    @I18NMessages( { @I18NMessage("cannot find command client class - searched for command client here") })
    String CMDLINE_CLIENT_CANNOT_FIND_CLIENT_SEARCHED = "CmdlineClient.cannot-find-client-searched";

    @I18NMessages( { @I18NMessage("Added the command client sender state listener [{0}]; sender is sending=[{1}]; notify listener immediately=[{2}]") })
    String CLIENT_COMMAND_SENDER_ADDED_STATE_LISTENER = "ClientCommandSender.added-state-listener";

    @I18NMessages( { @I18NMessage("Removed the command client sender state listener [{0}]") })
    String CLIENT_COMMAND_SENDER_REMOVED_STATE_LISTENER = "ClientCommandSender.removed-state-listener";

    @I18NMessages( { @I18NMessage("Notifying command client sender state listeners of a state change - sender is sending=[{0}]") })
    String CLIENT_COMMAND_SENDER_NOTIFYING_STATE_LISTENERS = "ClientCommandSender.notifying-state-listeners";

    @I18NMessages( { @I18NMessage("Listener [{0}] threw an exception: {1}"),
        @I18NMessage(value = "Listener [{0}] hat eine Exception geworfen: {1} ", locale = "de") })
    String CLIENT_COMMAND_SENDER_STATE_LISTENER_EXCEPTION = "ClientCommandSender.state-listener-exception";

    @I18NMessages( { @I18NMessage("Remote communicator must not be null") })
    String CLIENT_COMMAND_SENDER_NULL_REMOTE_COMM = "ClientCommandSender.null-remote-comm";

    @I18NMessages( { @I18NMessage("Sender configuration must not be null"),
        @I18NMessage(value = "Die Senderkonfiguration darf nicht leer sein", locale = "de") })
    String CLIENT_COMMAND_SENDER_NULL_CONFIG = "ClientCommandSender.null-config";

    @I18NMessages( { @I18NMessage("Cannot access the command spool file [{0}] - cannot guarantee the delivery of commands to [{1}]. Cause: {2}") })
    String CLIENT_COMMAND_SENDER_COMMAND_SPOOL_ACCESS_ERROR = "ClientCommandSender.command-spool-access-error";

    @I18NMessages( { @I18NMessage("Failed to queue up the previously queued tasks - some commands are lost for [{0}]") })
    String CLIENT_COMMAND_SENDER_REQUEUE_FAILED = "ClientCommandSender.requeue-failed";

    @I18NMessages( { @I18NMessage("A new client sender has re-queued [{0}] commands for [{1}]") })
    String CLIENT_COMMAND_SENDER_REQUEUE = "ClientCommandSender.requeue";

    @I18NMessages( { @I18NMessage("Queue is full - the command will not be sent: [{0}]"),
        @I18NMessage(value = "Die Warteschlange ist voll. Das Kommando wird nicht gesendet: [{0}]", locale = "de") })
    String CLIENT_COMMAND_SENDER_FULL_QUEUE = "ClientCommandSender.full-queue";

    @I18NMessages( {
        @I18NMessage("Failed to queue the command - it will not be sent now [{0}]"),
        @I18NMessage(value = "Konnte das Kommando nicht in die Warteschlange einstellen - es wird jetzt nicht gesendet [{0}]", locale = "de") })
    String CLIENT_COMMAND_SENDER_QUEUE_FAILED = "ClientCommandSender.queue-failed";

    @I18NMessages( { @I18NMessage("The sender object is currently not sending commands now. Command not sent: [{0}]") })
    String CLIENT_COMMAND_SENDER_CANNOT_SEND_NOT_SENDING = "ClientCommandSender.cannot-send-not-sending";

    @I18NMessages( { @I18NMessage("Interrupted while draining the queue of commands preparing to send them - some commands may be lost for [{0}]") })
    String CLIENT_COMMAND_SENDER_DRAIN_INTERRUPTED = "ClientCommandSender.drain-interrupted";

    @I18NMessages( { @I18NMessage("Interrupted while re-queuing commands during start - some commands may be lost for [{0}]") })
    String CLIENT_COMMAND_SENDER_REQUEUE_INTERRUPTED = "ClientCommandSender.requeue-interrupted";

    @I18NMessages( { @I18NMessage("Client is now going to actively send commands"),
        @I18NMessage(value = "Der Client wird nun aktiv Kommandos senden", locale = "de") })
    String CLIENT_COMMAND_SENDER_SENDING = "ClientCommandSender.sending";

    @I18NMessages( { @I18NMessage("Client is no longer actively sending commands to [{0}]") })
    String CLIENT_COMMAND_SENDER_NO_LONGER_SENDING = "ClientCommandSender.no-longer-sending";

    @I18NMessages( { @I18NMessage("Failed to drain the queued commands from the sender, thread was interrupted") })
    String CLIENT_COMMAND_SENDER_DRAIN_METHOD_INTERRUPTED = "ClientCommandSender.drain-method-interrupted";

    @I18NMessages( { @I18NMessage("Could not retry command - failed to acquire the read lock.  The command is lost: [{0}]") })
    String CLIENT_COMMAND_SENDER_RETRY_READ_LOCK_ACQUIRE_FAILURE = "ClientCommandSender.retry-read-lock-acquire-failure";

    @I18NMessages( { @I18NMessage("Failed to retry the command to [{0}], it is lost: [{1}]") })
    String CLIENT_COMMAND_SENDER_RETRY_FAILURE = "ClientCommandSender.retry-failure";

    @I18NMessages( { @I18NMessage("Command defines an invalid timeout [{0}], using the default of [{1}]. Command is [{2}]") })
    String CLIENT_COMMAND_SENDER_INVALID_TIMEOUT = "ClientCommandSender.invalid-timeout";

    @I18NMessages( { @I18NMessage("Loading [{0}] commands from the spool file and queueing them up to be sent to [{1}]") })
    String CLIENT_COMMAND_SENDER_LOADING_COMMAND_SPOOL = "ClientCommandSender.loading-command-spool";

    @I18NMessages( { @I18NMessage("[{0}] commands were unspooled and will be resent to [{1}]") })
    String CLIENT_COMMAND_SENDER_UNSPOOLED = "ClientCommandSender.unspooled";

    @I18NMessages( { @I18NMessage("A command failed to be unspooled or the spool file was corrupted.  [{0}] commands were unspooled successfully; however, [{1}] were not and are lost for [{2}]") })
    String CLIENT_COMMAND_SENDER_UNSPOOL_CNC_FAILURE = "ClientCommandSender.unspool-failure";

    @I18NMessages( { @I18NMessage("An exception occurred while attempting to queue up commands that were persisted - no more commands will be unspooled, those that were unspooled will remained queued and ready to be sent to [{0}]") })
    String CLIENT_COMMAND_SENDER_UNSPOOL_FAILURE = "ClientCommandSender.unspool-and-queue-failure";

    @I18NMessages( { @I18NMessage("Persisting commands with guaranteed delivery to the spool file"),
        @I18NMessage(value = "Schreibe Kommandos mit garantierter Zustellung in die SpoolDatei", locale = "de") })
    String CLIENT_COMMAND_SENDER_SPOOLING = "ClientCommandSender.spooling";

    @I18NMessages( { @I18NMessage("Failed to spool commands found in the queue - some commands will be lost! [{0}]") })
    String CLIENT_COMMAND_SENDER_SPOOL_FAILURE = "ClientCommandSender.spool-failure";

    @I18NMessages( { @I18NMessage("Persisted [{0}] commands to the spool file for later delivery to [{2}]; [{1}] volatile commands remain in the queue and will be lost if the sender does not start again") })
    String CLIENT_COMMAND_SENDER_SPOOL_DONE = "ClientCommandSender.spool-done";

    @I18NMessages( { @I18NMessage("The callback may not be serializable; serializing only the command. Cause: {0}") })
    String CLIENT_COMMAND_SENDER_CALLBACK_NOT_SERIALIZABLE = "ClientCommandSender.callback-not-serializable";

    @I18NMessages( { @I18NMessage("Failed to persist a command/callback pair.  Command=[{0}]") })
    String CLIENT_COMMAND_SENDER_PERSIST_FAILURE = "ClientCommandSender.persist-failure";

    @I18NMessages( { @I18NMessage("Failed to take a command/callback pair from the spool file for [{0}].") })
    String CLIENT_COMMAND_SENDER_COMMAND_STORE_TAKE_FAILURE = "ClientCommandSender.command-store-take-failure";

    @I18NMessages( { @I18NMessage("Command sender is starting - queuing the startup command [{0}].") })
    String CLIENT_COMMAND_SENDER_QUEUING_STARTUP_COMMAND = "ClientCommandSender.queuing-startup-cmd";

    @I18NMessages( { @I18NMessage("The endpoint does not know what type it is"),
        @I18NMessage(value = "Der Endpunkt weiss nicht, welchen Typs er ist", locale = "de") })
    String IDENTIFY_COMMAND_SERVICE_UNKNOWN_ENDPOINT = "IdentifyCommandService.unknown-endpoint";

    @I18NMessages( { @I18NMessage("Describes the invocation to be made on the remote POJO"),
        @I18NMessage(value = "Beschreibt die durchzuführende Aktion auf dem entfernten POJO", locale = "de") })
    String REMOTE_POJO_INVOCATION_COMMAND_INVOCATION = "RemotePojoInvocationCommand.invocation";

    @I18NMessages( { @I18NMessage("The remote POJO interface that is being invoked"),
        @I18NMessage(value = "Die entfernte POJO-Schnittstelle, die verwendet werden soll", locale = "de") })
    String REMOTE_POJO_INVOCATION_COMMAND_TARGET_INTERFACE_NAME = "RemotePojoInvocationCommand.target-interface-name";

    @I18NMessages( { @I18NMessage("The message to echo back to the client"),
        @I18NMessage(value = "Die Nachricht, die an den Client zurückgesandt wird", locale = "de") })
    String ECHO_COMMAND_MESSAGE = "EchoCommand.message";

    @I18NMessages( {
        @I18NMessage("A string prepended to the message when echoed back"),
        @I18NMessage(value = "Eine Zeichenkette, die der Nachricht vorangestellt wird, die zurückgesendet wird", locale = "de") })
    String ECHO_COMMAND_PREFIX = "EchoCommand.prefix";

    @I18NMessages( { @I18NMessage("Name of this program start configuration"),
        @I18NMessage(value = "Name der Startkonfiguration des Progamms", locale = "de") })
    String START_COMMAND_PROGRAM_TITLE = "StartCommand.program-title";

    @I18NMessages( { @I18NMessage("Name of the program's executable"),
        @I18NMessage(value = "Name des Executables", locale = "de") })
    String START_COMMAND_PROGRAM_EXECUTABLE = "StartCommand.program-executable";

    @I18NMessages( { @I18NMessage("Full directory pathname to the program's executable"),
        @I18NMessage(value = "Voller Verzeichnispfad zum Executable", locale = "de") })
    String START_COMMAND_PROGRAM_DIRECTORY = "StartCommand.program-directory";

    @I18NMessages( { @I18NMessage("Array of arguments to pass to the program executable; by default, no arguments are passed") })
    String START_COMMAND_ARGUMENTS = "StartCommand.arguments";

    @I18NMessages( { @I18NMessage("Environment variable in the format 'name=value'; null (the default) allows subprocess to inherit parent process environment") })
    String START_COMMAND_ENVIRONMENT_VARIABLES = "StartCommand.environment-variables";

    @I18NMessages( { @I18NMessage("The current working directory of the new process; null (the default) allows subprocess to inherit directory from parent process") })
    String START_COMMAND_WORKING_DIRECTORY = "StartCommand.working-directory";

    @I18NMessages( {
        @I18NMessage("The directory where the program's output log file will be written; default is a tmp directory"),
        @I18NMessage(value = "Das Verzeichnis, in das die Ausgabe des Programms geschieben wird. Standard ist ein Temporärverzeichnis", locale = "de") })
    String START_COMMAND_OUTPUT_DIRECTORY = "StartCommand.output-directory";

    @I18NMessages( { @I18NMessage("The file (to be placed in the output directory) where the program's output will be written; default is auto-generated") })
    String START_COMMAND_OUTPUT_FILE = "StartCommand.output-file";

    @I18NMessages( { @I18NMessage("The directory containing a file whose contents is input data to be fed into the program's stdin input stream") })
    String START_COMMAND_INPUT_DIRECTORY = "StartCommand.input-directory";

    @I18NMessages( { @I18NMessage("A file containing input data to be fed into the program's stdin input stream") })
    String START_COMMAND_INPUT_FILE = "StartCommand.input-file";

    @I18NMessages( { @I18NMessage("Time (ms) for the server to wait for the process to exit before sending back the response; 0 or less means do not wait before sending back a response") })
    String START_COMMAND_WAIT_FOR_EXIT = "StartCommand.wait-for-exit";

    @I18NMessages( { @I18NMessage("If true, the started process' output will be written to the output file; false (the default) means no output file is created") })
    String START_COMMAND_CAPTURE_OUTPUT = "StartCommand.capture-output";

    @I18NMessages( { @I18NMessage("If false (the default), any previous output file will be overwritten; if true, previous output files will be renamed with a date/timestamp") })
    String START_COMMAND_BACKUP_OUTPUT_FILE = "StartCommand.backup-output-file";

    @I18NMessages( { @I18NMessage("Environment variable is not in the form name=value: {0}"),
        @I18NMessage(value = "Die Umgebungsvariable ist nicht in der Form Name=Wert: {0}", locale = "de") })
    String START_COMMAND_ENV_VAR_BAD_FORMAT = "StartCommand.env-var-bad-format";

    @I18NMessages( { @I18NMessage("Must specify either both or none of the input directory and input file parameters: {0}") })
    String START_COMMAND_BAD_INPUT_PARAMS = "StartCommand.bad-input-params";

    @I18NMessages( { @I18NMessage("Not using parameter label from ResourceBundle since no key was specified.") })
    String NOT_USING_LABEL_KEY = "ParameterRenderingInformation.not-using-label-key";

    @I18NMessages( { @I18NMessage("Not using parameter description from ResourceBundle since no key was specified.") })
    String NOT_USING_DESC_KEY = "ParameterRenderingInformation.not-using-desc-key";

    @I18NMessages( { @I18NMessage("No ResourceBundle was specified for looking up parameter labels and descriptions, but keys were specified in the rendering information. Either specify a ResourceBundle or do not specify keys") })
    String PARAMETER_RENDING_INFORMATION_NO_RESOURCE_BUNDLE = "ParameterRenderingInformation.no-resource-bundle";

    @I18NMessages( { @I18NMessage("Not using parameter option lables from ResourceBundle since no keys were specified.") })
    String NOT_USING_OPTION_LABELS_KEY = "OptionListRenderingInformation.not-using-option-labels-key";

    @I18NMessages( { @I18NMessage("No ResourceBundle was specified for looking up option list labels, but keys were specified in the rendering information. Either specify a ResourceBundle or do not specify keys") })
    String OPTION_LIST_RENDING_INFORMATION_NO_RESOURCE_BUNDLE = "OptionListRenderingInformation.no-resource-bundle";

    @I18NMessages( { @I18NMessage("Array types are not allowed to have fixed values") })
    String NO_ARRAY_TYPES_ALLOWED = "FixedValuesParameterDefinition.no-array-types-allowed";

    @I18NMessages( { @I18NMessage("Invalid parameter type [{0}]; cannot determine if allowed values are OK. Cause: {1}") })
    String INVALID_PARAM_TYPE = "FixedValuesParameterDefinition.invalid-param-type";

    @I18NMessages( { @I18NMessage("Must define at least one fixed value for this parameter definition: [{0}]") })
    String NEED_AT_LEAST_ONE_FIXED_VALUE = "FixedValuesParameterDefinition.need-at-least-one";

    @I18NMessages( { @I18NMessage("All allowed values must be of type (or convertible to type) [{0}]. Cause: {1}") })
    String ALLOWED_VALUE_INVALID_TYPE = "FixedValuesParameterDefinition.allowed-value-invalid-type";

    @I18NMessages( { @I18NMessage("Value of parameter [{0}] must be one of the allowed values [{1}]") })
    String FIXED_VALUES_PARAMETER_DEFINITION_INVALID_VALUE = "FixedValuesParameterDefinition.invalid-value";

    @I18NMessages( { @I18NMessage("Object to convert is null but the parameter definition [{0}] does not allow for null values") })
    String PARAMETER_DEFINITION_NOT_NULLABLE = "ParameterDefinition.not-nullable";

    @I18NMessages( { @I18NMessage("For some reason, the newly converted parameter value (now of type [{0}]) is still not valid") })
    String PARAMETER_DEFINITION_STILL_NOT_VALID = "ParameterDefinition.still-not-valid";

    @I18NMessages( { @I18NMessage("Class [{0}] cannot be found"),
        @I18NMessage(value = "ie Klasse [{0}] kann nicht gefunden werden", locale = "de") })
    String CLASS_NOT_FOUND = "ParameterDefinition.class-not-found";

    @I18NMessages( { @I18NMessage("Cannot convert to a primitive type [{0}]"),
        @I18NMessage(value = "Kann nicht in den primitiven Datentyp [{0}] konvertieren", locale = "de") })
    String CANNOT_CONVERT_PRIMITIVE = "ParameterDefinition.cannot-convert-primitive";

    @I18NMessages( { @I18NMessage("Failed to convert to type [{0}]; an exception occurred in its constructor") })
    String PARAMETER_DEFINITION_CANNOT_CONVERT = "ParameterDefinition.cannot-convert";

    @I18NMessages( { @I18NMessage("Cannot convert to type [{0}];  it does not have a constructor that takes a single parameter of type [{1}]") })
    String PARAMETER_DEFINITION_CANNOT_CONVERT_NO_CONSTRUCTOR = "ParameterDefinition.cannot-convert-no-constructor";

    @I18NMessages( { @I18NMessage("There is no parameter named [{0}]"),
        @I18NMessage(value = "Es gibt keinen Parameter mit Namen [{0}]", locale = "de") })
    String NO_PARAMETER = "ParametersImpl.no-parameter";

    @I18NMessages( { @I18NMessage("Object must be either a String or Parameter"),
        @I18NMessage(value = "Das Objekt muss ein String oder eine Zeichenkette sein", locale = "de") })
    String MUST_BE_STRING_OR_PARAM = "ParametersImpl.must-be-string-or-param";

    @I18NMessages( { @I18NMessage("Parameter [{0}] is not of the proper visibility [{1}] to add to this collection") })
    String PARAMETERS_IMPL_HIDDEN = "ParametersImpl.hidden";

    @I18NMessages( { @I18NMessage("The command service [{0}] must have a key property named [{1}] whose value must be [{2}]") })
    String INVALID_CMD_SERVICE_NAME = "CommandService.invalid-cmd-service-name";

    @I18NMessages( { @I18NMessage("Cannot execute unknown command type: [{0}]"),
        @I18NMessage(value = "Kann den unbekannten Kommandotyp [{0}] nicht ausführen", locale = "de") })
    String UNKNOWN_COMMAND_TYPE = "MultipleCommandService.unknown-command-type";

    @I18NMessages( { @I18NMessage("[{0}] must be a non-abstract concrete type of [{1}], while not derived of type [{2}]") })
    String INVALID_EXECUTOR_CLASS = "MultipleCommandService.invalid-executor-class";

    @I18NMessages( { @I18NMessage("Executor instance is of type [{0}]; it must not be derived from [{1}]") })
    String INVALID_EXECUTOR_INSTANCE = "MultipleCommandService.invalid-executor-instance";

    @I18NMessages( { @I18NMessage("Failed to instantiate new command executor") })
    String CANNOT_CREATE_EXECUTOR = "MultipleCommandService.cannot-create-executor";

    @I18NMessages( { @I18NMessage("Command processor assigned an authenticator for command security checks: [{0}]") })
    String COMMAND_PROCESSOR_AUTHENTICATOR_SET = "CommandProcessor.authenticator-set";

    @I18NMessages( { @I18NMessage("Command failed to be authenticated!  This command will be ignored and not processed: {0}") })
    String COMMAND_PROCESSOR_FAILED_AUTHENTICATION = "CommandProcessor.failed-authentication";

    @I18NMessages( { @I18NMessage("Executing command [{0}]"),
        @I18NMessage(value = "Führe das Kommnado [{0}] aus", locale = "de") })
    String COMMAND_PROCESSOR_EXECUTING = "CommandProcessor.executing";

    @I18NMessages( { @I18NMessage("Failed to post-process command [{0}] - returning response anyway") })
    String COMMAND_PROCESSOR_POST_PROCESSING_FAILURE = "CommandProcessor.post-processing-failure";

    @I18NMessages( { @I18NMessage("Executed command - response is [{0}]"),
        @I18NMessage(value = "Kommando ausgeführt. Die Anwort ist [{0}]", locale = "de") })
    String COMMAND_PROCESSOR_EXECUTED = "CommandProcessor.executed";

    @I18NMessages( { @I18NMessage("A remote invocation was received but there is no command to execute") })
    String COMMAND_PROCESSOR_MISSING_COMMAND = "CommandProcessor.no-command";

    @I18NMessages( { @I18NMessage("There is no command service instance in subsystem [{0}] that provides support for the command type [{1}]") })
    String COMMAND_PROCESSOR_UNSUPPORTED_COMMAND_TYPE = "CommandProcessor.unsupported-command-type";

    @I18NMessages( { @I18NMessage("Command listener threw an exception when command was received: {0}") })
    String COMMAND_PROCESSOR_LISTENER_ERROR_RECEIVED = "CommandProcessor.listener-error.received";

    @I18NMessages( { @I18NMessage("Command listener threw an exception after command was processed: {0}") })
    String COMMAND_PROCESSOR_LISTENER_ERROR_PROCESSED = "CommandProcessor.listener-error.processed";

    @I18NMessages( { @I18NMessage("Cannot find the command service directory using the query [{0}]") })
    String COMMAND_PROCESSOR_NO_DIRECTORY = "CommandProcessor.no-directory";

    @I18NMessages( { @I18NMessage("The command directory [{0}] must have a key property named [{1}] whose value must be [{2}]") })
    String COMMAND_SERVICE_DIRECTORY_INVALID_SELF_NAME = "CommandServiceDirectory.invalid-self-name";

    @I18NMessages( { @I18NMessage("Failed to start the directory service"),
        @I18NMessage(value = "Konnte den Verzeichnisdiesnt nicht starten", locale = "de") })
    String COMMAND_SERVICE_DIRECTORY_FAILED_TO_START = "CommandServiceDirectory.failed-to-start";

    @I18NMessages( { @I18NMessage("Received an unknown type of notification - should have been an MBeanServerNotification: [{0}]") })
    String COMMAND_SERVICE_DIRECTORY_UNKNOWN_NOTIF = "CommandServiceDirectory.unknown-notif";

    @I18NMessages( { @I18NMessage("Command Service Directory has been allowed to perform dynamic discovery") })
    String COMMAND_SERVICE_DIRECTORY_DYNAMIC_DISCOVERY_ALLOWED = "CommandServiceDirectory.dynamic-discovery-allowed";

    @I18NMessages( { @I18NMessage("failed to remove the command services directory as a listener to the MBeanServer delegate") })
    String COMMAND_SERVICE_DIRECTORY_LISTENER_REMOVAL_FAILURE = "CommandServiceDirectory.listener-removal-failure";

    @I18NMessages( { @I18NMessage("Command [{0}] is now supported by [{1}]"),
        @I18NMessage(value = "Kommado [{0}] ist nun durch [{1}] unterstützt", locale = "de") })
    String COMMAND_SERVICE_DIRECTORY_NEW_SUPPORTED_COMMAND = "CommandServiceDirectory.new-supported-command";

    @I18NMessages( { @I18NMessage("Command [{0}] is no longer supported; removed [{1}]"),
        @I18NMessage(value = "Kommando [{0}] wird nicht mehr unterstützt. Es wird gelöscht [{1}]", locale = "de") })
    String COMMAND_SERVICE_DIRECTORY_REMOVED_COMMAND_SUPPORT = "CommandServiceDirectory.removed-command-support";

    @I18NMessages( { @I18NMessage("Asked to remove a supported command type [{0}], but the given service [{1}] was not its provider [{2}]") })
    String COMMAND_SERVICE_DIRECTORY_REMOVAL_FAILURE = "CommandServiceDirectory.removal-failure";

    @I18NMessages( { @I18NMessage("A new command service has been detected [{0}]; however, dynamic discovery has been disabled.  This new command has not been added to the directory") })
    String COMMAND_SERVICE_DIRECTORY_DETECTED_BUT_NOT_ADDED = "CommandServiceDirectory.detected-but-not-added";

    @I18NMessages( { @I18NMessage("Failed to stop listening to the previous Network Registry - will continue receiving notifications from it!") })
    String FAILED_TO_STOP_LISTENING = "AutoDiscovery.failed-to-stop-listening";

    @I18NMessages( { @I18NMessage("The given Network Registry name is not valid.  Cause: {0}") })
    String INVALID_REGISTRY_NAME = "AutoDiscovery.invalid-registry-name";

    @I18NMessages( { @I18NMessage("Auto-discovery will look to the Network Registry named [{0}]") })
    String REGISTRY_NAME = "AutoDiscovery.registry-name";

    @I18NMessages( { @I18NMessage("Failed to start listening to the Network Registry named [{0}]") })
    String FAILED_TO_START_LISTENING = "AutoDiscovery.failed-to-start-listening";

    @I18NMessages( { @I18NMessage("Received an unknown network registry notification type [{0}]") })
    String UNKNOWN_NOTIF_TYPE = "AutoDiscovery.unknown-notif-type";

    @I18NMessages( { @I18NMessage("Received an unknown network registry notification [{0}]") })
    String UNKNOWN_NOTIF = "AutoDiscovery.unknown-notif";

    @I18NMessages( { @I18NMessage("Failed to unregister our listener from the Network Registry [{0}]") })
    String UNREGISTER_FAILURE = "AutoDiscovery.unregister-failure";

    @I18NMessages( { @I18NMessage("Got a network registry notification of type [{0}]") })
    String GOT_NOTIF = "ServiceContainerNetworkNotificationListener.got-notif";

    @I18NMessages( { @I18NMessage("A new server has come online: [{0}]"),
        @I18NMessage(value = "Ein neuer Server kam online: [{0}]", locale = "de") })
    String SERVICE_CONTAINER_NETWORK_NOTIF_LISTENER_SERVER_ONLINE = "ServiceContainerNetworkNotificationListener.server-online";

    @I18NMessages( { @I18NMessage("Failed to process the newly detected server [{0}]") })
    String SERVICE_CONTAINER_NETWORK_NOTIF_LISTENER_ONLINE_PROCESSING_FAILURE = "ServiceContainerNetworkNotificationListener.online-processing-failure";

    @I18NMessages( { @I18NMessage("A remote server has gone down: [{0}]"),
        @I18NMessage(value = "Ein entfernter Server ging offline", locale = "de") })
    String SERVICE_CONTAINER_NETWORK_NOTIF_LISTENER_SERVER_OFFLINE = "ServiceContainerNetworkNotificationListener.server-offline";

    @I18NMessages( { @I18NMessage("Failed to process the detected offline server [{0}]") })
    String SERVICE_CONTAINER_NETWORK_NOTIF_LISTENER_OFFLINE_PROCESSING_FAILURE = "ServiceContainerNetworkNotificationListener.offline-processing-failure";

    @I18NMessages( { @I18NMessage("The [{0}] preference specified is invalid [{1}] - it must be one of [{2}, {3}, {4}]. Setting it to [{5}]") })
    String SERVICE_CONTAINER_CONFIGURATION_INVALID_CLIENT_AUTH = "ServiceContainerConfiguration.invalid-client-auth";

    @I18NMessages( { @I18NMessage("The [{0}] preference specified is invalid [{1}] - it must be one of [{2}, {3}] (case sensitive).") })
    String SERVICE_CONTAINER_CONFIGURATION_INVALID_TOMCAT_CLIENT_AUTH = "ServiceContainerConfiguration.invalid-tomcat-client-auth";

    @I18NMessages( { @I18NMessage("<unknown>"), @I18NMessage(value = "<unbekannt>", locale = "de") })
    String SERVICE_CONTAINER_CONFIGURATION_UNKNOWN = "ServiceContainerConfiguration.unknown";

    @I18NMessages( { @I18NMessage("cannot get preferences: {0}"),
        @I18NMessage(value = "Kann die Voreinstellung {0} nicht laden", locale = "de") })
    String SERVICE_CONTAINER_CONFIGURATION_CANNOT_GET_PREFS = "ServiceContainerConfiguration.cannot-get-prefs";

    @I18NMessages( { @I18NMessage("Service container started - ready to accept incoming commands"),
        @I18NMessage(value = "Der Service Container ist gestarter und wartet auf eingehende Kommandos", locale = "de") })
    String SERVICE_CONTAINER_STARTED = "ServiceContainer.started";

    @I18NMessages( { @I18NMessage("Service container is configured to disable communications - incoming commands will not be accepted") })
    String SERVICE_CONTAINER_DISABLED = "ServiceContainer.disabled";

    @I18NMessages( { @I18NMessage("Global concurrency limit has been set - no more than [{0}] incoming commands will be accepted at the same time") })
    String GLOBAL_CONCURRENCY_LIMIT_SET = "ServiceContainer.global-concurrency-limit-set";

    @I18NMessages( { @I18NMessage("Global concurrency limit has been disabled - there is no limit to the number of incoming commands allowed") })
    String GLOBAL_CONCURRENCY_LIMIT_DISABLED = "ServiceContainer.global-concurrency-limit-disabled";

    @I18NMessages( { @I18NMessage("Service container shutting down..."),
        @I18NMessage(value = "Der Service Container fährt herunter ...", locale = "de") })
    String SERVICE_CONTAINER_SHUTTING_DOWN = "ServiceContainer.shutting-down";

    @I18NMessages( { @I18NMessage("Failed to stop the connector; this will be ignored and the shutdown will continue") })
    String SERVICE_CONTAINER_SHUTDOWN_CONNECTOR_FAILURE = "ServiceContainer.shutdown-connector-failure";

    @I18NMessages( { @I18NMessage("Failed to stop multicast detector; this will be ignored and the shutdown will continue") })
    String SERVICE_CONTAINER_SHUTDOWN_DETECTOR_FAILURE = "ServiceContainer.shutdown-detector-failure";

    @I18NMessages( { @I18NMessage("Failed to unregister discovery listener from registry; this will be ignored and the shutdown will continue") })
    String SERVICE_CONTAINER_UNREGISTER_LISTENER_FAILURE = "ServiceContainer.unregister-listener-failure";

    @I18NMessages( { @I18NMessage("Failed to stop the SSL socket factory service; this will be ignored and the shutdown will continue") })
    String SERVICE_CONTAINER_SHUTDOWN_SSL_FACTORY_SERVICE_FAILURE = "ServiceContainer.shutdown-ssl-factory-service-failure";

    @I18NMessages( { @I18NMessage("Failed to stop the remote POJO service; this will be ignored and the shutdown will continue") })
    String SERVICE_CONTAINER_SHUTDOWN_REMOTE_POJO_SERVICE_FAILURE = "ServiceContainer.shutdown-remote-pojo-service-failure";

    @I18NMessages( { @I18NMessage("Failed to stop the remote input stream service; this will be ignored and the shutdown will continue") })
    String SERVICE_CONTAINER_SHUTDOWN_REMOTE_STREAM_SERVICE_FAILURE = "ServiceContainer.shutdown-remote-instream-service-failure";

    @I18NMessages( { @I18NMessage("Failed to stop the remote output stream service; this will be ignored and the shutdown will continue") })
    String SERVICE_CONTAINER_SHUTDOWN_REMOTE_OUTSTREAM_SERVICE_FAILURE = "ServiceContainer.shutdown-remote-outstream-service-failure";

    @I18NMessages( { @I18NMessage("Failed to clean up the MBeanServer; this will be ignored and the shutdown will continue.") })
    String SERVICE_CONTAINER_SHUTDOWN_MBS_FAILURE = "ServiceContainer.shutdown-mbs-failure";

    @I18NMessages( {
        @I18NMessage("Service container shut down - no longer accepting incoming commands"),
        @I18NMessage(value = "Der Service Container ist heruntergefahren und akzeptiert keine eingehenden Kommandos mehr", locale = "de") })
    String SERVICE_CONTAINER_SHUTDOWN = "ServiceContainer.shutdown";

    @I18NMessages( { @I18NMessage("Found an existing MBeanServer [{0}] - will use that"),
        @I18NMessage(value = "Habe einen existierenen MBeanServer [{0}] gefunden - werde ihn verwenden", locale = "de") })
    String SERVICE_CONTAINER_USING_EXISTING_MBS = "ServiceContainer.using-existing-mbs";

    @I18NMessages( { @I18NMessage("Creating a new MBeanServer [{0}]"),
        @I18NMessage(value = "Lege einen neuen MBeanServer [{0}] an", locale = "de") })
    String SERVICE_CONTAINER_CREATING_MBS = "ServiceContainer.creating-mbs";

    @I18NMessages( { @I18NMessage("Network registry has been created [{0}]") })
    String SERVICE_CONTAINER_REGISTRY_CREATED = "ServiceContainer.registry-created";

    @I18NMessages( { @I18NMessage("The machine hostname does not seem to be resolvable - generating our own identity. Cause: {0}") })
    String SERVICE_CONTAINER_IDENTITY_FAILURE = "ServiceContainer.identity-failure";

    @I18NMessages( { @I18NMessage("Failed to register the network registry. The machine hostname probably is not resolvable to "
        + "an IP address but should be. Because of this, multicast detection is disabled. Cause: {0}") })
    String SERVICE_CONTAINER_NETWORK_REGISTRY_FAILURE = "ServiceContainer.network-registry-failure";

    @I18NMessages( { @I18NMessage("Multicast Detector configuration:\\n\\\n" + "Multicast Detector address: {0}\\n\\\n"
        + "Multicast Detector port: {1,number,#}\\n\\\n" + "Multicast Detector bind address: {2}\\n\\\n"
        + "Multicast Detector default IP: {3}\\n\\\n" + "Multicast Detector default time delay: {4}\\n\\\n"
        + "Multicast Detector heartbeat time delay: {5}") })
    String SERVICE_CONTAINER_MULTICAST_DETECTOR_CONFIG = "ServiceContainer.multicast-detector-config";

    @I18NMessages( { @I18NMessage("Multicast detector has been created [{0}] and is listening for new remote servers to come online") })
    String SERVICE_CONTAINER_MULTICAST_DETECTOR_CREATED = "ServiceContainer.multicast-detector-created";

    @I18NMessages( { @I18NMessage("Multicast detector has not been enabled in configuration - it will not be started and auto-discovery will not be supported") })
    String SERVICE_CONTAINER_MULTICAST_DETECTOR_DISABLED = "ServiceContainer.multicast-detector-disabled";

    @I18NMessages( { @I18NMessage("Multicast detector failed to start.  It will be disabled. Cause: {0}") })
    String SERVICE_CONTAINER_MULTICAST_DETECTOR_START_ERROR = "ServiceContainer.multicast-detector-start-error";

    @I18NMessages( { @I18NMessage("The rhqtype [{0}] must be either [{1}] or [{2}]; defaulting to the latter [{2}]") })
    String SERVICE_CONTAINER_INVALID_RHQTYPE = "ServiceContainer.invalid-rhqtype";

    @I18NMessages( { @I18NMessage("Connector is using locator URI of [{0}]") })
    String SERVICE_CONTAINER_CONNECTOR_URI = "ServiceContainer.connector-uri";

    @I18NMessages( { @I18NMessage("The server transport [{0}] requires security services - initializing them now") })
    String SERVICE_CONTAINER_NEEDS_SECURITY_SERVICES = "ServiceContainer.needs-security-services";

    @I18NMessages( { @I18NMessage("The connector lease period is [{0}]") })
    String SERVICE_CONTAINER_CONNECTOR_LEASE_PERIOD = "ServiceContainer.connector-lease-period";

    @I18NMessages( { @I18NMessage("Command processor is now ready to begin accepting commands from clients") })
    String SERVICE_CONTAINER_PROCESSOR_READY = "ServiceContainer.processor-ready";

    @I18NMessages( { @I18NMessage("Added command service [{0}] with an id of [{1}]") })
    String SERVICE_CONTAINER_ADDED_COMMAND_SERVICE = "ServiceContainer.added-command-service";

    @I18NMessages( { @I18NMessage("The communications configuration is invalid - remote POJOs are defined as a comma-separated list of pojo-class-name:remote-interface-name pairs via the preference [{0}]. The misconfigured element is: [{1}]. The full preference value was [{2}]") })
    String SERVICE_CONTAINER_REMOTE_POJO_CONFIG_INVALID = "ServiceContainer.remote-pojo-config-invalid";

    @I18NMessages( { @I18NMessage("Added remote POJO of type [{0}] with a remote interface of [{1}]") })
    String SERVICE_CONTAINER_ADDED_REMOTE_POJO = "ServiceContainer.added-remote-pojo";

    @I18NMessages( { @I18NMessage("Command service directory has been created [{0}]") })
    String SERVICE_CONTAINER_DIRECTORY_CREATED = "ServiceContainer.directory-created";

    @I18NMessages( { @I18NMessage("Failed to set truststore but it is probably going to be needed to perform client authentication") })
    String SERVICE_CONTAINER_TRUSTSTORE_FAILURE = "ServiceContainer.truststore-failure";

    @I18NMessages( { @I18NMessage("SSL server socket factory service has been created [{0}]") })
    String SERVICE_CONTAINER_SSL_SOCKET_FACTORY_CREATED = "ServiceContainer.ssl-socket-factory-created";

    @I18NMessages( { @I18NMessage("Input stream with ID [{0}] is being accessed from a remote client - the method being invoked is [{1}]") })
    String INVOKING_STREAM_FROM_REMOTE_CLIENT = "RemoteInputStreamCommandService.invoking-stream-from-remote-client";

    @I18NMessages( { @I18NMessage("Output stream with ID [{0}] is being accessed from a remote client - the method being invoked is [{1}]") })
    String INVOKING_OUTSTREAM_FROM_REMOTE_CLIENT = "RemoteOutputStreamCommandService.invoking-stream-from-remote-client";

    @I18NMessages( { @I18NMessage("Input stream being accessed from a remote client encountered an exception - the method invoked was [{0}]. The command was: [{1}]") })
    String FAILED_TO_INVOKE_STREAM_METHOD = "RemoteInputStreamCommandService.invoke-stream-failure";

    @I18NMessages( { @I18NMessage("Output stream being accessed from a remote client encountered an exception - the method invoked was [{0}]. The command was: [{1}]") })
    String FAILED_TO_INVOKE_OUTSTREAM_METHOD = "RemoteOutputStreamCommandService.invoke-stream-failure";

    @I18NMessages( { @I18NMessage("An invalid name or signature for an InputStream method was specified - this is very bad - you cannot remotely access a stream.") })
    String INVALID_INPUT_STREAM_METHOD = "ClientRemoteInputStream.invalid-method-definition";

    @I18NMessages( { @I18NMessage("An invalid name or signature for an OututStream method was specified - this is very bad - you cannot remotely access a stream.") })
    String INVALID_OUTPUT_STREAM_METHOD = "ClientRemoteOutputStream.invalid-method-definition";

    @I18NMessages( { @I18NMessage("A command service with an ID of [{0}] and name of [{1}] could not be removed because it was never registered in the first place. Cause: [{2}]") })
    String CANNOT_REMOVE_UNREGISTERED_CMDSERVICE = "ServiceContainer.cannot-remove-unregistered-command-service";

    @I18NMessages( { @I18NMessage("A command service with an ID of [{0}] and name of [{1}] could not be removed - an error occurred in its deregister code. Cause: [{2}]") })
    String CANNOT_REMOVE_CMDSERVICE = "ServiceContainer.cannot-remove-command-service";

    @I18NMessages( { @I18NMessage("A remote input stream with an ID of [{0}] and server endpoint of [{1}] has not yet been assigned a sender object - cannot access the stream") })
    String REMOTE_INPUT_STREAM_HAS_NO_SENDER = "RemoteInputStream.remote-input-stream-missing-sender";

    @I18NMessages( { @I18NMessage("A remote output stream with an ID of [{0}] and server endpoint of [{1}] has not yet been assigned a sender object - cannot access the stream") })
    String REMOTE_OUTPUT_STREAM_HAS_NO_SENDER = "RemoteOutputStream.remote-output-stream-missing-sender";

    @I18NMessages( { @I18NMessage("Failed to create a command sender for [{0}]") })
    String FAILED_TO_CREATE_SENDER = "ServiceContainer.failed-to-create-sender";

    @I18NMessages( { @I18NMessage("Remote input stream with ID [{0}] has been idle and needs to be removed but failed to do so. Cause: [{1}]") })
    String TIMER_TASK_CANNOT_REMOVE_STREAM = "RemoteInputStreamCommandService.timer-task-cannot-remove-stream";

    @I18NMessages( { @I18NMessage("Remote output stream with ID [{0}] has been idle and needs to be removed but failed to do so. Cause: [{1}]") })
    String TIMER_TASK_CANNOT_REMOVE_OUTSTREAM = "RemoteOutputStreamCommandService.timer-task-cannot-remove-stream";

    @I18NMessages( { @I18NMessage("This instance of the client command sender does not have a command spool filename defined; no commands will be spooled and thus guaranteed delivery is implicitly disabled for [{0}]") })
    String CLIENT_COMMAND_SENDER_NO_COMMAND_SPOOL_FILENAME = "ClientCommandSender.no-spool-file-defined";

    @I18NMessages( { @I18NMessage("The command preprocessor class [{0}] cannot be instantiated - the sender will not be able to perform any command preprocessing for [{1}]") })
    String CLIENT_COMMAND_SENDER_INVALID_PREPROCESSOR = "ClientCommandSender.invalid-preprocessor";

    @I18NMessages( { @I18NMessage("A command asked to access an unknown input stream with ID of [{0}].  The command was: [{1}]") })
    String INVALID_STREAM_ID = "RemoteInputStreamCommandService.invalid-stream-id";

    @I18NMessages( { @I18NMessage("A command asked to access an unknown output stream with ID of [{0}].  The command was: [{1}]") })
    String INVALID_OUTSTREAM_ID = "RemoteOutputStreamCommandService.invalid-stream-id";

    @I18NMessages( { @I18NMessage("An input stream has been remoted (ID=[{0}]) and can now be accessed by remote clients") })
    String ADDED_REMOTE_STREAM = "RemoteInputStreamCommandService.added-stream";

    @I18NMessages( { @I18NMessage("An output stream has been remoted (ID=[{0}]) and can now be accessed by remote clients") })
    String ADDED_REMOTE_OUTSTREAM = "RemoteOutputStreamCommandService.added-stream";

    @I18NMessages( { @I18NMessage("An input stream with ID=[{0}] has been removed and is no longer accessible by remote clients") })
    String REMOVED_REMOTE_STREAM = "RemoteInputStreamCommandService.removed-stream";

    @I18NMessages( { @I18NMessage("An output stream with ID=[{0}] has been removed and is no longer accessible by remote clients") })
    String REMOVED_REMOTE_OUTSTREAM = "RemoteOutputStreamCommandService.removed-stream";

    @I18NMessages( { @I18NMessage("Input stream with ID=[{0}] was removed because it was idle for longer than its allowed max of [{1}] milliseconds") })
    String TIMER_TASK_REMOVED_IDLE_STREAM = "RemoteInputStreamCommandService.removed-idle-stream";

    @I18NMessages( { @I18NMessage("Output stream with ID=[{0}] was removed because it was idle for longer than its allowed max of [{1}] milliseconds") })
    String TIMER_TASK_REMOVED_IDLE_OUTSTREAM = "RemoteOutputStreamCommandService.removed-idle-stream";

    @I18NMessages( { @I18NMessage("Removed command service whose id was [{0}] and name was [{1}] - it is no longer accessible") })
    String SERVICE_CONTAINER_REMOVED_COMMAND_SERVICE = "ServiceContainer.removed-command-service";

    @I18NMessages( { @I18NMessage("Removed remote POJO with the remote interface of [{0}] - it is no longer accessible") })
    String SERVICE_CONTAINER_REMOVED_REMOTE_POJO = "ServiceContainer.removed-remote-pojo";

    @I18NMessages( { @I18NMessage("The preferences configuration [{0}] is getting upgraded from version [{1,number,#}] to [{2,number,#}]") })
    String CONFIG_SCHEMA_VERSION_STEP_UPGRADING = "PreferencesUpgrade.step-upgrading";

    @I18NMessages( { @I18NMessage("The preferences configuration [{0}] was step-upgraded to version [{1,number,#}]") })
    String CONFIG_SCHEMA_VERSION_STEP_UPGRADED = "PreferencesUpgrade.step-upgraded";

    @I18NMessages( { @I18NMessage("The preferences configuration [{0}] has been fully upgraded and is now at version [{1,number,#}]") })
    String CONFIG_SCHEMA_VERSION_UPGRADED = "PreferencesUpgrade.upgrade-done";

    @I18NMessages( { @I18NMessage("The preferences configuration [{0}] is up-to-date at version [{1,number,#}]") })
    String CONFIG_SCHEMA_VERSION_UPTODATE = "PreferencesUpgrade.already-uptodate";

    @I18NMessages( { @I18NMessage("The preferences configuration [{0}] could not be upgraded fully to version [{1,number,#}] - it only was upgraded to version [{2,number,#}]. More upgrade steps need to be implemented.") })
    String CONFIG_SCHEMA_VERSION_NOT_UPTODATE = "PreferencesUpgrade.still-not-uptodate";

    @I18NMessages( { @I18NMessage("Setup is now preprocessing the instruction [{0}]") })
    String SETUP_PREPROCESS_INSTRUCTION = "Setup.preprocess-instruction";

    @I18NMessages( { @I18NMessage("Setup is now postprocessing the instruction [{0}]") })
    String SETUP_POSTPROCESS_INSTRUCTION = "Setup.postprocess-instruction";

    @I18NMessages( { @I18NMessage("Setup is finished - the preferences are now setup: {0}") })
    String SETUP_COMPLETE_WITH_DUMP = "Setup.complete-with-dump";

    @I18NMessages( { @I18NMessage("The setup has been stopped early - those values that have been setup are now in the preferences at node [{0}].") })
    String SETUP_USER_STOP = "Setup.user-stop";

    @I18NMessages( { @I18NMessage("The setup has been canceled - the original values have been restored back into the preferences at node [{0}].") })
    String SETUP_USER_CANCEL = "Setup.user-cancel";

    @I18NMessages( { @I18NMessage("The setup has been completed for the preferences at node [{0}].") })
    String SETUP_COMPLETE = "Setup.complete";

    @I18NMessages( { @I18NMessage("The new user-entered preference is [{0}]=[{1}]") })
    String SETUP_NEW_VALUE = "Setup.new-value";

    @I18NMessages( { @I18NMessage("Setup instruction will not prompt for value - the new preference is [{0}]=[{1}]") })
    String SETUP_NEW_VALUE_NO_PROMPT = "Setup.new-value-no-prompt";

    @I18NMessages( { @I18NMessage("That is not a valid integer: {0}"),
        @I18NMessage(value = "Dies ist keine gültige Ganzzahl: {0}", locale = "de") })
    String SETUP_NOT_AN_INTEGER = "Setup.integer-checker.not-an-integer";

    @I18NMessages( { @I18NMessage("That is not a valid number: {0}"),
        @I18NMessage(value = "Dies ist keine gültige Zahl: {0}", locale = "de") })
    String SETUP_NOT_A_LONG = "Setup.long-checker.not-a-long";

    @I18NMessages( { @I18NMessage("That is not a valid floating point number: {0}"),
        @I18NMessage(value = "Dies ist keine gültige Zahl: {0}", locale = "de") })
    String SETUP_NOT_A_FLOAT = "Setup.float-checker.not-a-float";

    @I18NMessages( { @I18NMessage("That is not a valid URL: {0}"),
        @I18NMessage(value = "Dies ist keine gültige URL: {0}", locale = "de") })
    String SETUP_NOT_A_URL = "Setup.float-checker.not-a-url";

    @I18NMessages( {
        @I18NMessage("That new value [{0}] is too low - it must be greater than or equal to [{1}]"),
        @I18NMessage(value = "Der neue Wert [{0}] ist zu niedrig - er muss größer oder gleich [{1}] sein", locale = "de") })
    String SETUP_NUMBER_TOO_LOW = "Setup.number-checker.too-low";

    @I18NMessages( { @I18NMessage("That new value [{0}] is too high - it must be less than or equal to [{1}]"),
        @I18NMessage(value = "Der neue Wert [{0}] ist zu hoch - er muss kleiner oder gleich [{1}] sein", locale = "de") })
    String SETUP_NUMBER_TOO_HIGH = "Setup.number-checker.too-high";

    @I18NMessages( {
        @I18NMessage("That new value [{0}] is not a valid IP address or hostname. Cause: {1}"),
        @I18NMessage(value = "Der neue Wert [{0}] ist keine gültige IP-Adresse oder Hostname. Grund: {1}", locale = "de") })
    String SETUP_NOT_AN_IP_ADDRESS_OR_HOSTNAME = "Setup.ip-checker.not-valid-ip";

    @I18NMessages( { @I18NMessage("That new value [{0}] is not a valid remote endpoint locator URI. Cause: {1}") })
    String SETUP_NOT_A_LOCATOR_URI = "Setup.locator-uri-checker.not-valid-locator";

    @I18NMessages( { @I18NMessage("That new value [{0}] is not valid remote endpoint locator URI parameters. Cause: {1}") })
    String SETUP_NOT_LOCATOR_URI_PARAMS = "Setup.locator-uri-checker.not-valid-locator-params";

    @I18NMessages( { @I18NMessage("- After each prompt, a default value will appear in square brackets.\\n\\\n"
        + "\\  If you press the ENTER key without providing any value,\\n\\\n"
        + "\\  the new preference value will be set to that default value.\\n\\\n"
        + "- If you wish to rely on the system internal default value and\\n\\\n"
        + "\\  not define any preference value, enter \'!*\'.\\n\\\n"
        + "- If you wish to stop before finishing all the questions but still\\n\\\n"
        + "\\  retain those preferences you already set, enter \'!+\'.\\n\\\n"
        + "- If you wish to cancel before finishing all the questions and revert\\n\\\n"
        + "\\  all preferences back to their original values, enter \'!-\'.\\n\\\n"
        + "- If you need help for a particular preference, enter \'!?\'.\\n") })
    String SETUP_STANDARD_INTRO = "Setup.standard-intro";

    @I18NMessages({ @I18NMessage("Transport must be one of the following: servlet, sslservlet, socket, sslsocket") })
    String NOT_SERVLET_TRANSPORT = "Setup.not-servlet-transport";

    @I18NMessage("Command was not serializable [{0}]. Cause: {1}")
    String TRACE_NOT_SERIALIZABLE_COMMAND = "trace.not-serializable.command";

    @I18NMessage("Command response was not serializable [{0}]. Cause: {1}")
    String TRACE_NOT_SERIALIZABLE_COMMAND_RESPONSE = "trace.not-serializable.command-response";

    @I18NMessage("Command [{0}] has size of [{1}] which exceeds the threshold")
    String TRACE_SIZE_THRESHOLD_EXCEEDED_COMMAND = "trace.size-threshold-exceeded.command";

    @I18NMessage("Command response [{0}] has size of [{1}] which exceeds the threshold")
    String TRACE_SIZE_THRESHOLD_EXCEEDED_COMMAND_RESPONSE = "trace.size-threshold-exceeded.command-response";

    @I18NMessage("==>{0}|{1}")
    String TRACE_OUTGOING_COMMAND_START = "send.initiate";

    @I18NMessage("=>>{0}|{1}|{2}")
    String TRACE_OUTGOING_COMMAND_FINISH = "send.complete";

    @I18NMessage("<=={0}|{1}")
    String TRACE_INCOMING_COMMAND_START = "recv.initiate";

    @I18NMessage("<<={0}|{1}|{2}")
    String TRACE_INCOMING_COMMAND_FINISH = "recv.complete";

    @I18NMessage("Failed to store preference key [{0}]. Cause: {1}")
    String CANNOT_STORE_PREFERENCES = "ServiceContainerConfiguration.cannot-store-preferences";

    @I18NMessage("Exception occurred: {0}")
    String EXCEPTION = "Misc.exception";
}