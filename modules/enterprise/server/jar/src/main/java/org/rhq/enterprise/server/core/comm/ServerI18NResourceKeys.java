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
package org.rhq.enterprise.server.core.comm;

import mazz.i18n.annotation.I18NMessage;
import mazz.i18n.annotation.I18NMessages;
import mazz.i18n.annotation.I18NResourceBundle;

/**
 * I18N resource bundle keys that identify the messages needed by the server module.
 *
 * @author John Mazzitelli
 */
@I18NResourceBundle(baseName = "server-messages", defaultLocale = "en")
public interface ServerI18NResourceKeys {
    @I18NMessages( { @I18NMessage("Failed to remove remote API invocation handler. Cause: {0}") })
    String REMOTE_API_REMOVAL_FAILURE = "ServerCommunicationsService.remote-api-removal-failure";

    @I18NMessages( { @I18NMessage("Failed to ping endpoint [{0}]. Cause: {1}") })
    String PING_FAILED = "ServerCommunicationsService.ping-failed";

    @I18NMessages( { @I18NMessage("Failed to save server property [{0}] with value [{1}] to file [{2}]") })
    String SERVER_PROPERTY_SAVE_FAILED = "ServerCommunicationsService.server-property-save-failed";

    @I18NMessages( { @I18NMessage("New concurrency limit: [{0}]=[{1}]") })
    String NEW_CONCURRENCY_LIMIT = "ServerCommunicationsService.new-concurrency-limit";

    @I18NMessages( { @I18NMessage("Cannot find the server configuration file [{0}]"),
        @I18NMessage(value = "Kann die Server-Konfigurationsdatei [{0}] nicht finden ", locale = "de") })
    String CANNOT_FIND_CONFIG_FILE = "ServerCommunicationsService.cannot-find-config-file";

    @I18NMessages( { @I18NMessage("Loading server configuration file [{0}]"),
        @I18NMessage(value = "Lade die Server-Konfigurationsdatei [{0}]", locale = "de") })
    String LOADING_CONFIG_FILE = "ServerCommunicationsService.loading-config-file";

    @I18NMessages( {
        @I18NMessage("Found preferences already loaded in persisted store [{0}]; using those for the server configuration"),
        @I18NMessage(value = "Habe Voreinstellungen gefunden, die bereits im Speicher [{0}] abgelegt waren; letztere werden für die Serverkonfiguration herangezogen.", locale = "de") })
    String PREFERENCES_ALREADY_EXIST = "ServerCommunicationsService.preferences-already-exist";

    @I18NMessages( { @I18NMessage("Server preferences node name is [{0}]"),
        @I18NMessage(value = "Der Name des Voreinstellungs-Knotens ist [{0}]", locale = "de") })
    String PREFERENCES_NODE_NAME = "ServerCommunicationsService.pref-node-name";

    @I18NMessages( {
        @I18NMessage("The server configuration file [{0}] does not have preferences under the node name [{1}]; use a different config file or set the preferences node name appropriately"),
        @I18NMessage(value = "Die Serverkonfigurationsdatei [{0}] hat keine Voreinstellungen unter dem Knoten mit Namen [{1}]. Verwenden Sie eine andere Konfigurationsdate oder setzen Sie den Knotennamen auf einen passenden Wert", locale = "de") })
    String BAD_NODE_NAME_IN_CONFIG_FILE = "ServerCommunicationsService.bad-node-name-in-config-file";

    @I18NMessages( { @I18NMessage("Server configuration file loaded [{0}]"),
        @I18NMessage(value = "Server-Konfigurationsdatei [{0}] geladen", locale = "de") })
    String LOADED_CONFIG_FILE = "ServerCommunicationsService.loaded-config-file";

    @I18NMessages( { @I18NMessage("Server configuration preferences have been loaded [{0}]"),
        @I18NMessage(value = "Voreinstellungen für die Serverkonfiguration wurden geladen [{0}]", locale = "de") })
    String CONFIG_PREFERENCES = "ServerCommunicationsService.config-preferences";

    @I18NMessages( {
        @I18NMessage("A server configuration preference has been overridden: [{0}]=[{1}]"),
        @I18NMessage(value = "Eine Voresinstellung der Serverkonfiguration wurde überschrieben: [{0}]=[{1}]", locale = "de") })
    String CONFIG_PREFERENCE_OVERRIDE = "ServerCommunicationsService.config-preference-override";

    @I18NMessages( { @I18NMessage("The [{0}] preference value specified is invalid [{1}] - it must be greater than 0; will use the default of [{2}]") })
    String PREF_MUST_BE_GREATER_THAN_0 = "ServerConfiguration.pref-must-be-greater-than-0";

    @I18NMessages( {
        @I18NMessage("Command spool file maximum size must be equal to or greater than 10000"),
        @I18NMessage(value = "Die Maximalgröße für die Kommando-Spool-Date muss mindestens 10000 betragen", locale = "de") })
    String COMMAND_SPOOL_INVALID_MAX_SIZE = "ServerConfiguration.command-spool-invalid-max-size";

    @I18NMessages( { @I18NMessage("Command spool file purge percentage must be between 0 and 99") })
    String COMMAND_SPOOL_INVALID_PURGE_PERCENTAGE = "ServerConfiguration.command-spool-invalid-purge-percentage";

    @I18NMessages( { @I18NMessage("Command spool file params format does not specify the parameters in the proper format") })
    String COMMAND_SPOOL_INVALID_FORMAT = "ServerConfiguration.command-spool-invalid-format";

    @I18NMessages( { @I18NMessage("The [{0}] preference value specified is invalid [{1}] - it must be in the form \"max-file-size:purge-percentage\". Cause: [{2}]") })
    String BAD_COMMAND_SPOOL_PREF = "ServerConfiguration.bad-command-spool-pref";

    @I18NMessages( { @I18NMessage("Send throttling max commands must be larger than 0") })
    String SEND_THROTTLE_INVALID_MAX = "ServerConfiguration.send-throttle-invalid-max";

    @I18NMessages( { @I18NMessage("Send throttling quiet period must be equal to or greater than [{0}]") })
    String SEND_THROTTLE_INVALID_QUIET_PERIOD = "ServerConfiguration.send-throttle-invalid-quiet-period";

    @I18NMessages( { @I18NMessage("Send throttling format does not specify the throttling parameters in the proper format") })
    String SEND_THROTTLE_INVALID_FORMAT = "ServerConfiguration.send-throttle-invalid-format";

    @I18NMessages( { @I18NMessage("The [{0}] preference value specified is invalid [{1}] - it must be in the form \"max-commands:quiet-period-milliseconds\". Send throttling configuration will be disabled. Cause: [{2}]") })
    String BAD_SEND_THROTTLE_PREF = "ServerConfiguration.bad-send-throttle-pref";

    @I18NMessages( { @I18NMessage("Queue throttling max commands must be larger than 0") })
    String QUEUE_THROTTLE_INVALID_MAX = "ServerConfiguration.queue-throttle-invalid-max";

    @I18NMessages( {
        @I18NMessage("Queue throttling burst period must be equal to or greater than [{0}]"),
        @I18NMessage(value = "Die Zeit für das schnelle Senden in der Sendewarteschlange muss größer oder gleich [{0}] sein", locale = "de") })
    String QUEUE_THROTTLE_INVALID_BURST_PERIOD = "ServerConfiguration.queue-throttle-invalid-burst-period";

    @I18NMessages( {
        @I18NMessage("Queue throttling format does not specify the throttling parameters in the proper format"),
        @I18NMessage(value = "Für die Verlangsamung der Sendeschlange sind die Parameter nicht im richtigen Format", locale = "de") })
    String QUEUE_THROTTLE_INVALID_FORMAT = "ServerConfiguration.queue-throttle-invalid-format";

    @I18NMessages( {
        @I18NMessage("The [{0}] preference value specified is invalid [{1}] - it must be in the form \"max-commands-per-burst:burst-period-milliseconds\". Queue throttling configuration will be disabled. Cause: [{2}]"),
        @I18NMessage(value = "Der Voreinstellungswert [{0}] ist ungültig: [{1}] - er muss im Format \"max-commands-per-burst:burst-period-milliseconds\" sein. Die Konfiguration für das Verlangsamen der Warteschlagen wird abgeschaltet. Grund: [{2}]", locale = "de") // TODO what?
    })
    String BAD_QUEUE_THROTTLE_PREF = "ServerConfiguration.bad-queue-throttle-pref";

    @I18NMessages( { @I18NMessage("<cannot get preferences: {0}>"),
        @I18NMessage(value = "<Kann die Voreinstellung: {0} nicht laden>", locale = "de") })
    String CANNOT_GET_PREFERENCES = "ServerConfiguration.cannot-get-preferences";

    @I18NMessages( { @I18NMessage("<unknown>"), @I18NMessage(value = "<unbekannt>", locale = "de") })
    String UNKNOWN = "ServerConfiguration.unknown";

    @I18NMessages( { @I18NMessage("A new agent coming online has been detected: [{0}]"),
        @I18NMessage(value = "Ein neuer Agent wurde als verfügbar erkannt: [{0}]", locale = "de") })
    String AUTO_DETECTED_NEW_AGENT = "ServerAutoDiscovery.new-agent";

    @I18NMessages( { @I18NMessage("An agent has been detected going offline: [{0}]"),
        @I18NMessage(value = "Es wurde erkannt, dass ein Agent offline ging: [{0}]", locale = "de") })
    String AUTO_DETECTED_DOWNED_AGENT = "ServerAutoDiscovery.downed-agent";
}