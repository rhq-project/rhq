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
package org.rhq.enterprise.installer.i18n;

import mazz.i18n.Msg;
import mazz.i18n.annotation.I18NMessage;
import mazz.i18n.annotation.I18NMessages;
import mazz.i18n.annotation.I18NResourceBundle;

/**
 * NOTE THAT THIS FILE NEEDS TO BE ISO-8859-1 / ISO-8859-15 encoded !!!!!
 *
 * @author Mazz
 * @author Heiko W. Rupp
 */
@I18NResourceBundle(baseName = "InstallerMessages", defaultLocale = "en")
public interface InstallerI18NResourceKeys {
    Msg.BundleBaseName BUNDLE_BASE_NAME = new Msg.BundleBaseName("InstallerMessages");

    @I18NMessages( { @I18NMessage(locale = "en", value = "The {0} property value was an invalid number [{1}]"),
        @I18NMessage(locale = "de", value = "[{1}] ist ein ungültiger Zahlenwert für die Eigenschaft {0}") })
    String INVALID_NUMBER = "invalidNumber";

    @I18NMessages( {
        @I18NMessage(locale = "en", value = "The {0} boolean property value must be either 'true' or 'false' but was [{1}]"),
        @I18NMessage(locale = "de", value = "Die boolesche Eigenschaft muss entweder 'true' oder 'false' sein, war aber [{1}]") })
    String INVALID_BOOLEAN = "invalidBoolean";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Failed to save properties and fully deploy - RHQ Server will not function properly\\n\\\nCause: {0}") })
    String SAVE_FAILURE = "saveFailure";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Error"), @I18NMessage(locale = "de", value = "Fehler") })
    String ERROR_LABEL = "errorLabel";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Could not save the settings for some reason."),
        @I18NMessage(locale = "de", value = "Kann die Einstellungen nicht sichern") })
    String SAVE_ERROR = "saveError";

    @I18NMessages( {
        @I18NMessage(locale = "en", value = "Could not connect to the database with the given database settings. \\n\\\n"
            + "Please check the database settings and make sure your database is running."),
        @I18NMessage(locale = "de", value = "Eine Verbindung zur Datenbank ist mit den gebenen Einstellungen für die Datenbank nicht möglich. \\n\\\n"
            + "Bitte überprüfen Sie die Einstellungen und stellen Sie sicher, dass die Datenbak aktiv ist") })
    String INVALID_DATABASE_SETTINGS = "invalidDatabaseSettings";

    @I18NMessages( { @I18NMessage(locale = "en", value = "This page shows you the current configuration property settings \\n\\\n"
        + "for this RHQ Server installation.  You may change some, all or none \\n\\\n"
        + "of these as per your custom requirements.  Note that changes to \\n\\\n"
        + "some of these settings will not take effect until you restart the \\n\\\n"
        + "server.  If you change any of those settings, you will have to \\n\\\n"
        + "immediately shutdown and restart the server to pick up those changes.") })
    String SET_PROPERTIES_INSTRUCTIONS = "setPropertiesInstructions";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Install RHQ Server!"),
        @I18NMessage(locale = "de", value = "RHQ Server installieren!") })
    String SAVE = "save";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Yes"), @I18NMessage(locale = "de", value = "Ja") })
    String YES_STRING = "yesString";

    @I18NMessages( { @I18NMessage(locale = "en", value = "No"), @I18NMessage(locale = "de", value = "Nein") })
    String NO_STRING = "noString";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Property Name"),
        @I18NMessage(locale = "de", value = "Eigenschaft") })
    String PROPERTY_NAME = "propertyName";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Value"), @I18NMessage(locale = "de", value = "Wert") })
    String VALUE = "value";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Requires Restart?"),
        @I18NMessage(locale = "de", value = "Neustart erforderlich?") })
    String REQUIRES_RESTART = "requiresRestart";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Welcome to RHQ!"),
        @I18NMessage(locale = "de", value = "Willkommen bei RHQ!") })
    String WELCOME_TITLE = "welcomeTitle";

    @I18NMessages( {
        @I18NMessage(locale = "en", value = "You have reached the RHQ Installer. You will use this page \\n\\\n"
            + "to complete the installation and configuration of the RHQ Server. \\n\\\n"
            + "Once complete, you will be able to log on and begin using RHQ"),
        @I18NMessage(locale = "de", value = "Willkommen beim Installationsprogramm von RHQ. Mit diesem können Sie \\n\\\n"
            + "die Installation und Konfiguration von RHQ komplettieren. \\n\\\n"
            + "Nachdem dies geschehen ist, können Sie sich einloggen und mit RHQ arbeiten") })
    String WELCOME_MESSAGE = "welcomeMessage";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Starting up, please wait..."),
        @I18NMessage(locale = "de", value = "Bitte warten Sie bis der Server gestartet ist") })
    String STARTING = "starting";

    @I18NMessages( { @I18NMessage(locale = "en", value = "RHQ will be installed!"),
        @I18NMessage(locale = "de", value = "RHQ wird installiert") })
    String ALREADY_INSTALLED = "alreadyInstalled";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Done! Click here to get started!"),
        @I18NMessage(locale = "de", value = "Fertig! Klicken Sie hier, um fortzufahren!") })
    String ALREADY_INSTALLED_STARTED_LINK = "alreadyInstalledStartedLink";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Click here to continue the installation"),
        @I18NMessage(locale = "de", value = "Klicken Sie hier, um mit der Installation fortzufahren") })
    String START_INSTALLING_LINK = "startInstallingLink";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Back to configuration page"),
        @I18NMessage(locale = "de", value = "Zurück zur Seite mit den Einstellungen") })
    String BACK_TO_SETTINGS_LINK = "backToSettingsLink";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Show Advanced Settings"),
        @I18NMessage(locale = "de", value = "Erweiterte Einstellungen anzeigen") })
    String SHOW_ADVANCED_SETTINGS = "showAdvancedSettings";

    // Below are the localized names of all the properties that can be configured
    // The _HELP I18NMessage strings refer to links under the RHQ Server Install Guide

    @I18NMessages( { @I18NMessage(locale = "en", value = "Database Type"),
        @I18NMessage(locale = "de", value = "Datenbanktyp") })
    String PROP_DATABASE_TYPE = "propertyDatabaseType";
    @I18NMessage("#d.JONServerInstallationGuide-DatabaseType")
    String PROP_DATABASE_TYPE_HELP = "propertyDatabaseTypeHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Database Connection URL"),
        @I18NMessage(locale = "de", value = "URL der Datenbankverbindung") })
    String PROP_DATABASE_CONNECTION_URL = "propertyDatabaseConnectionUrl";
    @I18NMessage("#d.JONServerInstallationGuide-DatabaseConnectionURL")
    String PROP_DATABASE_CONNECTION_URL_HELP = "propertyDatabaseConnectionUrlHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Database JDBC Driver Class"),
        @I18NMessage(locale = "de", value = "Klassnname des JDBC-Datenbanktreibers") })
    String PROP_DATABASE_DRIVER_CLASS = "propertyDatabaseDriverClass";
    @I18NMessage("#d.JONServerInstallationGuide-DatabaseJDBCDriverClass")
    String PROP_DATABASE_DRIVER_CLASS_HELP = "propertyDatabaseDriverClassHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Database User Name"),
        @I18NMessage(locale = "de", value = "Name des Datenbankbenutzers") })
    String PROP_DATABASE_USERNAME = "propertyDatabaseUserName";
    @I18NMessage("#d.JONServerInstallationGuide-DatabaseUserName")
    String PROP_DATABASE_USERNAME_HELP = "propertyDatabaseUserNameHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Database Password"),
        @I18NMessage(locale = "de", value = "Password des Datenbankbenutzers") })
    String PROP_DATABASE_PASSWORD = "propertyDatabasePassword";
    @I18NMessage("#d.JONServerInstallationGuide-DatabasePassword")
    String PROP_DATABASE_PASSWORD_HELP = "propertyDatabasePasswordHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Server Bind Address"),
        @I18NMessage(locale = "de", value = "IP-Adresse an die der Sever sich binden soll") })
    String PROP_SERVER_BIND_ADDRESS = "propertyBindAddress";
    @I18NMessage("#d.JONServerInstallationGuide-ServerBindAddress")
    String PROP_SERVER_BIND_ADDRESS_HELP = "propertyBindAddressHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "HTTP Port") })
    String PROP_HTTP_PORT = "propertyHttpPort";
    @I18NMessage("#d.JONServerInstallationGuide-HTTPPort")
    String PROP_HTTP_PORT_HELP = "propertyHttpPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Secure HTTPS Port"),
        @I18NMessage(locale = "de", value = "HTTPS Port") })
    String PROP_HTTPS_PORT = "propertyHttpsPort";
    @I18NMessage("#d.JONServerInstallationGuide-SecureHTTPSPort")
    String PROP_HTTPS_PORT_HELP = "propertyHttpsPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Web Service Port") })
    String PROP_WEB_SERVICE_PORT = "propertyWebServicePort";
    @I18NMessage("#d.JONServerInstallationGuide-WebServicePort")
    String PROP_WEB_SERVICE_PORT_HELP = "propertyWebServicePortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Naming Service Port") })
    String PROP_NAMING_SERVICE_PORT = "propertyNamingServicePort";
    @I18NMessage("#d.JONServerInstallationGuide-NamingServicePort")
    String PROP_NAMING_SERVICE_PORT_HELP = "propertyNamingServicePortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Naming Service RMI Port") })
    String PROP_NAMING_SERVICE_RMI_PORT = "propertyNamingServiceRmiPort";
    @I18NMessage("#d.JONServerInstallationGuide-NamingServiceRMIPort")
    String PROP_NAMING_SERVICE_RMI_PORT_HELP = "propertyNamingServiceRmiPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "JRMP Invoker RMI Port") })
    String PROP_JRMP_INVOKER_RMI_PORT = "propertyJrmpInvokerRmiPort";
    @I18NMessage("#d.JONServerInstallationGuide-JRMPInvokerRMIPort")
    String PROP_JRMP_INVOKER_RMI_PORT_HELP = "propertyJrmpInvokerRmiPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Pooled Invoker RMI Port") })
    String PROP_POOLED_INVOKER_RMI_PORT = "propertyPooledInvokerRmiPort";
    @I18NMessage("#d.JONServerInstallationGuide-PooledInvokerRMIPort")
    String PROP_POOLED_INVOKER_RMI_PORT_HELP = "propertyPooledInvokerRmiPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "AJP Port") })
    String PROP_AJP_PORT = "propertyAjpPort";
    @I18NMessage("#d.JONServerInstallationGuide-AJPPort")
    String PROP_AJP_PORT_HELP = "propertyAjpPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Unified Invoker Port") })
    String PROP_UNIFIED_INVOKER_PORT = "propertyUnifiedInvokerPort";
    @I18NMessage("#d.JONServerInstallationGuide-UnifiedInvokerPort")
    String PROP_UNIFIED_INVOKER_PORT_HELP = "propertyUnifiedInvokerPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "RHQ Console Keystore") })
    String PROP_TOMCAT_KEYSTORE_FILENAME = "propertyTomcatKeystoreFilename";
    @I18NMessage("#d.JONServerInstallationGuide-JONConsoleKeystore")
    String PROP_TOMCAT_KEYSTORE_FILENAME_HELP = "propertyTomcatKeystoreFilenameHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "RHQ Console Keystore Password") })
    String PROP_TOMCAT_KEYSTORE_PASSWORD = "propertyTomcatKeystorePassword";
    @I18NMessage("#d.JONServerInstallationGuide-JONConsoleKeystorePassword")
    String PROP_TOMCAT_KEYSTORE_PASSWORD_HELP = "propertyTomcatKeystorePasswordHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "RHQ Console SSL Protocol") })
    String PROP_TOMCAT_SSL_PROTOCOL = "propertyTomcatSslProtocol";
    @I18NMessage("#d.JONServerInstallationGuide-JONConsoleSSLProtocol")
    String PROP_TOMCAT_SSL_PROTOCOL_HELP = "propertyTomcatSslProtocolHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Agent Communications Transport") })
    String PROP_CONNECTOR_TRANSPORT = "propertyConnectorTransport";
    @I18NMessage("#d.JONServerInstallationGuide-IncomingAgentCommunicationsTransport")
    String PROP_CONNECTOR_TRANSPORT_HELP = "propertyConnectorTransportHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Agent Communications Bind Address") })
    String PROP_CONNECTOR_BIND_ADDRESS = "propertyConnectorBindAddress";
    @I18NMessage("#d.JONServerInstallationGuide-IncomingAgentCommunicationsBindAddress")
    String PROP_CONNECTOR_BIND_ADDRESS_HELP = "propertyConnectorBindAddressHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Agent Communications Port") })
    String PROP_CONNECTOR_BIND_PORT = "propertyConnectorBindPort";
    @I18NMessage("#d.JONServerInstallationGuide-IncomingAgentCommunicationsPort")
    String PROP_CONNECTOR_BIND_PORT_HELP = "propertyConnectorBindPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Agent Communications Transport Parameters") })
    String PROP_CONNECTOR_TRANSPORT_PARAMS = "propertyConnectorTransportParams";
    @I18NMessage("#d.JONServerInstallationGuide-IncomingAgentCommunicationsTransportParameters")
    String PROP_CONNECTOR_TRANSPORT_PARAMS_HELP = "propertyConnectorTransportParamsHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Agent Multicast Detector Enabled") })
    String PROP_AGENT_MULTICAST_DETECTOR_ENABLED = "propertyAgentMulticastDetectorEnabled";
    @I18NMessage("#d.JONServerInstallationGuide-AgentMulticastDetectorEnabled")
    String PROP_AGENT_MULTICAST_DETECTOR_ENABLED_HELP = "propertyAgentMulticastDetectorEnabledHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Agent Multicast Detector Bind Address") })
    String PROP_AGENT_MULTICAST_DETECTOR_BIND_ADDRESS = "propertyAgentMulticastDetectorBindAddress";
    @I18NMessage("#d.JONServerInstallationGuide-AgentMulticastDetectorBindAddress")
    String PROP_AGENT_MULTICAST_DETECTOR_BIND_ADDRESS_HELP = "propertyAgentMulticastDetectorBindAddressHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Agent Multicast Detector Multicast Address") })
    String PROP_AGENT_MULTICAST_DETECTOR_MULTICAST_ADDRESS = "propertyAgentMulticastDetectorMulticastAddress";
    @I18NMessage("#d.JONServerInstallationGuide-AgentMulticastDetectorMulticastAddress")
    String PROP_AGENT_MULTICAST_DETECTOR_MULTICAST_ADDRESS_HELP = "propertyAgentMulticastDetectorMulticastAddressHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Agent Multicast Detector Port") })
    String PROP_AGENT_MULTICAST_DETECTOR_PORT = "propertyAgentMulticastDetectorPort";
    @I18NMessage("#d.JONServerInstallationGuide-AgentMulticastDetectorPort")
    String PROP_AGENT_MULTICAST_DETECTOR_PORT_HELP = "propertyAgentMulticastDetectorPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Security - Secure Socket Protocol") })
    String PROP_SECURITY_SERVER_SECURE_SOCKET_PROTOCOL = "propertySecurityServerSecureSocketProtocol";
    @I18NMessage("#d.JONServerInstallationGuide-IncomingSecureSocketProtocol")
    String PROP_SECURITY_SERVER_SECURE_SOCKET_PROTOCOL_HELP = "propertySecurityServerSecureSocketProtocolHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Security - Keystore File") })
    String PROP_SECURITY_SERVER_KEYSTORE_FILE = "propertySecurityServerKeystoreFile";
    @I18NMessage("#d.JONServerInstallationGuide-IncomingKeystoreFile")
    String PROP_SECURITY_SERVER_KEYSTORE_FILE_HELP = "propertySecurityServerKeystoreFileHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Security - Keystore Algorithm") })
    String PROP_SECURITY_SERVER_KEYSTORE_ALGORITHM = "propertySecurityServerKeystoreAlgorithm";
    @I18NMessage("#d.JONServerInstallationGuide-IncomingKeystoreAlgorithm")
    String PROP_SECURITY_SERVER_KEYSTORE_ALGORITHM_HELP = "propertySecurityServerKeystoreAlgorithmHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Security - Keystore Type") })
    String PROP_SECURITY_SERVER_KEYSTORE_TYPE = "propertySecurityServerKeystoreType";
    @I18NMessage("#d.JONServerInstallationGuide-IncomingKeystoreType")
    String PROP_SECURITY_SERVER_KEYSTORE_TYPE_HELP = "propertySecurityServerKeystoreTypeHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Security - Keystore Password") })
    String PROP_SECURITY_SERVER_KEYSTORE_PASSWORD = "propertySecurityServerKeystorePassword";
    @I18NMessage("#d.JONServerInstallationGuide-IncomingKeystorePassword")
    String PROP_SECURITY_SERVER_KEYSTORE_PASSWORD_HELP = "propertySecurityServerKeystorePasswordHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Security - Keystore Key Password") })
    String PROP_SECURITY_SERVER_KEYSTORE_KEY_PASSWORD = "propertySecurityServerKeystoreKeyPassword";
    @I18NMessage("#d.JONServerInstallationGuide-IncomingKeystoreKeyPassword")
    String PROP_SECURITY_SERVER_KEYSTORE_KEY_PASSWORD_HELP = "propertySecurityServerKeystoreKeyPasswordHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Security - Keystore Alias") })
    String PROP_SECURITY_SERVER_KEYSTORE_ALIAS = "propertySecurityServerKeystoreAlias";
    @I18NMessage("#d.JONServerInstallationGuide-IncomingKeystoreAlias")
    String PROP_SECURITY_SERVER_KEYSTORE_ALIAS_HELP = "propertySecurityServerKeystoreAliasHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Security - Truststore File") })
    String PROP_SECURITY_SERVER_TRUSTSTORE_FILE = "propertySecurityServerTruststoreFile";
    @I18NMessage("#d.JONServerInstallationGuide-IncomingTruststoreFile")
    String PROP_SECURITY_SERVER_TRUSTSTORE_FILE_HELP = "propertySecurityServerTruststoreFileHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Security - Truststore Algorithm") })
    String PROP_SECURITY_SERVER_TRUSTSTORE_ALGORITHM = "propertySecurityServerTruststoreAlgorithm";
    @I18NMessage("#d.JONServerInstallationGuide-IncomingTruststoreAlgorithm")
    String PROP_SECURITY_SERVER_TRUSTSTORE_ALGORITHM_HELP = "propertySecurityServerTruststoreAlgorithmHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Security - Truststore Type") })
    String PROP_SECURITY_SERVER_TRUSTSTORE_TYPE = "propertySecurityServerTruststoreType";
    @I18NMessage("#d.JONServerInstallationGuide-IncomingTruststoreType")
    String PROP_SECURITY_SERVER_TRUSTSTORE_TYPE_HELP = "propertySecurityServerTruststoreTypeHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Security - Truststore Password") })
    String PROP_SECURITY_SERVER_TRUSTSTORE_PASSWORD = "propertySecurityServerTruststorePassword";
    @I18NMessage("#d.JONServerInstallationGuide-IncomingTruststorePassword")
    String PROP_SECURITY_SERVER_TRUSTSTORE_PASSWORD_HELP = "propertySecurityServerTruststorePasswordHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Security - Client Authentication Mode") })
    String PROP_SECURITY_SERVER_CLIENT_AUTH_MODE = "propertySecurityServerClientAuthMode";
    @I18NMessage("#d.JONServerInstallationGuide-IncomingClientAuthenticationMode")
    String PROP_SECURITY_SERVER_CLIENT_AUTH_MODE_HELP = "propertySecurityServerClientAuthModeHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Outgoing Security - Secure Socket Protocol") })
    String PROP_SECURITY_CLIENT_SECURE_SOCKET_PROTOCOL = "propertySecurityClientSecureSocketProtocol";
    @I18NMessage("#d.JONServerInstallationGuide-OutgoingSecureSocketProtocol")
    String PROP_SECURITY_CLIENT_SECURE_SOCKET_PROTOCOL_HELP = "propertySecurityClientSecureSocketProtocolHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Outgoing Security - Keystore File") })
    String PROP_SECURITY_CLIENT_KEYSTORE_FILE = "propertySecurityClientKeystoreFile";
    @I18NMessage("#d.JONServerInstallationGuide-OutgoingKeystoreFile")
    String PROP_SECURITY_CLIENT_KEYSTORE_FILE_HELP = "propertySecurityClientKeystoreFileHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Outgoing Security - Keystore Algorithm") })
    String PROP_SECURITY_CLIENT_KEYSTORE_ALGORITHM = "propertySecurityClientKeystoreAlgorithm";
    @I18NMessage("#d.JONServerInstallationGuide-OutgoingKeystoreAlgorithm")
    String PROP_SECURITY_CLIENT_KEYSTORE_ALGORITHM_HELP = "propertySecurityClientKeystoreAlgorithmHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Outgoing Security - Keystore Type") })
    String PROP_SECURITY_CLIENT_KEYSTORE_TYPE = "propertySecurityClientKeystoreType";
    @I18NMessage("#d.JONServerInstallationGuide-OutgoingKeystoreType")
    String PROP_SECURITY_CLIENT_KEYSTORE_TYPE_HELP = "propertySecurityClientKeystoreTypeHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Outgoing Security - Keystore Password") })
    String PROP_SECURITY_CLIENT_KEYSTORE_PASSWORD = "propertySecurityClientKeystorePassword";
    @I18NMessage("#d.JONServerInstallationGuide-OutgoingKeystorePassword")
    String PROP_SECURITY_CLIENT_KEYSTORE_PASSWORD_HELP = "propertySecurityClientKeystorePasswordHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Outgoing Security - Keystore Key Password") })
    String PROP_SECURITY_CLIENT_KEYSTORE_KEY_PASSWORD = "propertySecurityClientKeystoreKeyPassword";
    @I18NMessage("#d.JONServerInstallationGuide-OutgoingKeystoreKeyPassword")
    String PROP_SECURITY_CLIENT_KEYSTORE_KEY_PASSWORD_HELP = "propertySecurityClientKeystoreKeyPasswordHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Outgoing Security - Keystore Alias") })
    String PROP_SECURITY_CLIENT_KEYSTORE_ALIAS = "propertySecurityClientKeystoreAlias";
    @I18NMessage("#d.JONServerInstallationGuide-OutgoingKeystoreAlias")
    String PROP_SECURITY_CLIENT_KEYSTORE_ALIAS_HELP = "propertySecurityClientKeystoreAliasHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Outgoing Security - Truststore File") })
    String PROP_SECURITY_CLIENT_TRUSTSTORE_FILE = "propertySecurityClientTruststoreFile";
    @I18NMessage("#d.JONServerInstallationGuide-OutgoingTruststoreFile")
    String PROP_SECURITY_CLIENT_TRUSTSTORE_FILE_HELP = "propertySecurityClientTruststoreFileHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Outgoing Security - Truststore Algorithm") })
    String PROP_SECURITY_CLIENT_TRUSTSTORE_ALGORITHM = "propertySecurityClientTruststoreAlgorithm";
    @I18NMessage("#d.JONServerInstallationGuide-OutgoingTruststoreAlgorithm")
    String PROP_SECURITY_CLIENT_TRUSTSTORE_ALGORITHM_HELP = "propertySecurityClientTruststoreAlgorithmHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Outgoing Security - Truststore Type") })
    String PROP_SECURITY_CLIENT_TRUSTSTORE_TYPE = "propertySecurityClientTruststoreType";
    @I18NMessage("#d.JONServerInstallationGuide-OutgoingTruststoreType")
    String PROP_SECURITY_CLIENT_TRUSTSTORE_TYPE_HELP = "propertySecurityClientTruststoreTypeHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Outgoing Security - Truststore Password") })
    String PROP_SECURITY_CLIENT_TRUSTSTORE_PASSWORD = "propertySecurityClientTruststorePassword";
    @I18NMessage("#d.JONServerInstallationGuide-OutgoingTruststorePassword")
    String PROP_SECURITY_CLIENT_TRUSTSTORE_PASSWORD_HELP = "propertySecurityClientTruststorePasswordHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Outgoing Security - Server Authentication Mode Enabled") })
    String PROP_SECURITY_CLIENT_SERVER_AUTH_MODE_ENABLED = "propertySecurityClientServerAuthModeEnabled";
    @I18NMessage("#d.JONServerInstallationGuide-OutgoingServerAuthenticationModeEnabled")
    String PROP_SECURITY_CLIENT_SERVER_AUTH_MODE_ENABLED_HELP = "propertySecurityClientServerAuthModeEnabledHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Embedded RHQ Agent Enabled"),
        @I18NMessage(locale = "de", value = "Eingebetteten RHQ Agent verwenden") })
    String PROP_EMBEDDED_JON_AGENT_ENABLED = "propertyEmbeddedJonAgentEnabled";
    @I18NMessage("#d.JONServerInstallationGuide-EmbeddedJONAgentEnabled")
    String PROP_EMBEDDED_JON_AGENT_ENABLED_HELP = "propertyEmbeddedJonAgentEnabledHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Embedded RHQ Agent Name"),
        @I18NMessage(locale = "de", value = "Name des eingebetteten RHQ Agent") })
    String PROP_EMBEDDED_JON_AGENT_NAME = "propertyEmbeddedJonAgentName";
    @I18NMessage("#d.JONServerInstallationGuide-EmbeddedJONAgentName")
    String PROP_EMBEDDED_JON_AGENT_NAME_HELP = "propertyEmbeddedJonAgentNameHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Embedded RHQ Agent Disable Native System") })
    String PROP_EMBEDDED_JON_AGENT_DISABLE_NATIVE_SYSTEM = "propertyEmbeddedJonAgentDisableNativeSystem";
    @I18NMessage("#d.JONServerInstallationGuide-EmbeddedJONAgentDisableNativeSystem")
    String PROP_EMBEDDED_JON_AGENT_DISABLE_NATIVE_SYSTEM_HELP = "propertyEmbeddedJonAgentDisableNativeSystemHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Embedded RHQ Agent Reset Configuration") })
    String PROP_EMBEDDED_JON_AGENT_RESET_CONFIGURATION = "propertyEmbeddedJonAgentResetConfiguration";
    @I18NMessage("#d.JONServerInstallationGuide-EmbeddedJONAgentResetConfiguration")
    String PROP_EMBEDDED_JON_AGENT_RESET_CONFIGURATION_HELP = "propertyEmbeddedJonAgentResetConfigurationHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Email SMTP Hostname") })
    String PROP_EMAIL_SMTP_HOST = "propertyEmailSmtpHost";
    @I18NMessage("#d.JONServerInstallationGuide-EmailSMTPHostname")
    String PROP_EMAIL_SMTP_HOST_HELP = "propertyEmailSmtpHostHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Email SMTP Port") })
    String PROP_EMAIL_SMTP_PORT = "propertyEmailSmtpPort";
    @I18NMessage("#d.JONServerInstallationGuide-EmailSMTPPort")
    String PROP_EMAIL_SMTP_PORT_HELP = "propertyEmailSmtpPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Email From Address"),
        @I18NMessage(locale = "de", value = "E-Mail Absenderadresse") })
    String PROP_EMAIL_FROM_ADDRESS = "propertyEmailFromAddress";
    @I18NMessage("#d.JONServerInstallationGuide-EmailFromAddress")
    String PROP_EMAIL_FROM_ADDRESS_HELP = "propertyEmailFromAddressHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Operation Invocation Default Timeout") })
    String PROP_OPERATION_TIMEOUT = "propertyOperationTimeout";
    @I18NMessage("#d.JONServerInstallationGuide-OperationInvocationDefaultTimeout")
    String PROP_OPERATION_TIMEOUT_HELP = "propertyOperationTimeoutHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Cluster - Partition Name") })
    String PROP_CLUSTER_PARTITION_NAME = "propertyClusterPartitionName";
    @I18NMessage("#d.JONServerInstallationGuide-PartitionName")
    String PROP_CLUSTER_PARTITION_NAME_HELP = "propertyClusterPartitionNameHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Cluster - Partition Bind Address") })
    String PROP_CLUSTER_PARTITION_BIND_ADDRESS = "propertyClusterPartitionBindAddress";
    @I18NMessage("#d.JONServerInstallationGuide-PartitionBindAddress")
    String PROP_CLUSTER_PARTITION_BIND_ADDRESS_HELP = "propertyClusterPartitionBindAddressHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Cluster - Partition UDP Multicast Group IP Address") })
    String PROP_CLUSTER_UDP_GROUP = "propertyClusterUdpGroup";
    @I18NMessage("#d.JONServerInstallationGuide-PartitionUDPMulticastGroupIPAddress")
    String PROP_CLUSTER_UDP_GROUP_HELP = "propertyClusterUdpGroupHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Cluster - Partition UDP Multicast Port") })
    String PROP_CLUSTER_HAPARTITION_PORT = "propertyClusterHaPartitionPort";
    @I18NMessage("#d.JONServerInstallationGuide-PartitionUDPMulticastPort")
    String PROP_CLUSTER_HAPARTITION_PORT_HELP = "propertyClusterHaPartitionPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Cluster - Partition UDP EJB3 Entity Cache Multicast Port") })
    String PROP_CLUSTER_EJB3CACHE_PORT = "propertyClusterEjb3CachePort";
    @I18NMessage("#d.JONServerInstallationGuide-PartitionUDPEJB3EntityCacheMulticastPort")
    String PROP_CLUSTER_EJB3CACHE_PORT_HELP = "propertyClusterEjb3CachePortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Cluster - Partition UDP Alert Cache Multicast Port") })
    String PROP_CLUSTER_ALERTCACHE_PORT = "propertyClusterAlertCachePort";
    @I18NMessage("#d.JONServerInstallationGuide-PartitionUDPAlertCacheMulticastPort")
    String PROP_CLUSTER_ALERTCACHE_PORT_HELP = "propertyClusterAlertCachePortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Cluster - Partition UDP Loopback") })
    String PROP_CLUSTER_UDP_LOOPBACK = "propertyClusterUdpLoopback";
    @I18NMessage("#d.JONServerInstallationGuide-PartitionUDPLoopback")
    String PROP_CLUSTER_UDP_LOOPBACK_HELP = "propertyClusterUdpLoopbackHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Cluster - HA JNDI Port") })
    String PROP_CLUSTER_HAJNDI_PORT = "propertyClusterHaJndiPort";
    @I18NMessage("#d.JONServerInstallationGuide-HAJNDIPort")
    String PROP_CLUSTER_HAJNDI_PORT_HELP = "propertyClusterHaJndiPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Cluster - HA JNDI RMI Port") })
    String PROP_CLUSTER_HAJNDI_RMIPORT = "propertyClusterHaJndiRmiPort";
    @I18NMessage("#d.JONServerInstallationGuide-HAJNDIRMIPort")
    String PROP_CLUSTER_HAJNDI_RMIPORT_HELP = "propertyClusterHaJndiRmiPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Cluster - HA JNDI Auto Discovery Group Port") })
    String PROP_CLUSTER_HAJNDI_AUTODISCOVERPORT = "propertyClusterHaJndiAutoDiscoveryPort";
    @I18NMessage("#d.JONServerInstallationGuide-HAJNDIAutoDiscoveryGroupPort")
    String PROP_CLUSTER_HAJNDI_AUTODISCOVERPORT_HELP = "propertyClusterHaJndiAutoDiscoveryPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Cluster - HA JRMP Invoker RMI Port") })
    String PROP_CLUSTER_HAJRMPINVOKER_RMIPORT = "propertyClusterHaJrmpInvokerRmiPort";
    @I18NMessage("#d.JONServerInstallationGuide-HAJRMPInvokerRMIPort")
    String PROP_CLUSTER_HAJRMPINVOKER_RMIPORT_HELP = "propertyClusterHaJrmpInvokerRmiPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Cluster - HA Pooled Invoker Port") })
    String PROP_CLUSTER_HAPOOLEDINVOKER_PORT = "propertyClusterHaPooledInvokerPort";
    @I18NMessage("#d.JONServerInstallationGuide-HAPooledInvokerPort")
    String PROP_CLUSTER_HAPOOLEDINVOKER_PORT_HELP = "propertyClusterHaPooledInvokerPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Cluster - JGroups UDP IP Time To Live") })
    String PROP_CLUSTER_JGROUPS_UDP_IP_TTL = "propertyClusterJGroupsUdpIpTtl";
    @I18NMessage("#d.JONServerInstallationGuide-JGroupsUDPIPTimeToLive")
    String PROP_CLUSTER_JGROUPS_UDP_IP_TTL_HELP = "propertyClusterJGroupsUdpIpTtlHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Concurrency Limit - Maximum Web Connections") })
    String PROP_CONCURRENCY_LIMIT_WEBCONNS = "propertyConcurrencyLimitWebConns";
    @I18NMessage("#d.JONServerInstallationGuide-WebConnections")
    String PROP_CONCURRENCY_LIMIT_WEBCONNS_HELP = "propertyConcurrencyLimitWebConnsHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Concurrency Limit - Global") })
    String PROP_CONCURRENCY_LIMIT_GLOBAL = "propertyConcurrencyLimitGlobal";
    @I18NMessage("#d.JONServerInstallationGuide-GlobalConcurrencyLimit")
    String PROP_CONCURRENCY_LIMIT_GLOBAL_HELP = "propertyConcurrencyLimitGlobalHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Concurrency Limit - Inventory Reports") })
    String PROP_CONCURRENCY_LIMIT_INV_REPORT = "propertyConcurrencyLimitInventoryReport";
    @I18NMessage("#d.JONServerInstallationGuide-InventoryReport")
    String PROP_CONCURRENCY_LIMIT_INV_REPORT_HELP = "propertyConcurrencyLimitInventoryReportHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Concurrency Limit - Availability Reports") })
    String PROP_CONCURRENCY_LIMIT_AVAIL_REPORT = "propertyConcurrencyLimitAvailabilityReport";
    @I18NMessage("#d.JONServerInstallationGuide-AvailabilityReport")
    String PROP_CONCURRENCY_LIMIT_AVAIL_REPORT_HELP = "propertyConcurrencyLimitAvailabilityReportHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Concurrency Limit - Inventory Synchronizations") })
    String PROP_CONCURRENCY_LIMIT_INV_SYNC = "propertyConcurrencyLimitInventorySync";
    @I18NMessage("#d.JONServerInstallationGuide-InventorySynchronization")
    String PROP_CONCURRENCY_LIMIT_INV_SYNC_HELP = "propertyConcurrencyLimitInventorySyncHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Concurrency Limit - Content Reports") })
    String PROP_CONCURRENCY_LIMIT_CONTENT_REPORT = "propertyConcurrencyLimitContentReport";
    @I18NMessage("#d.JONServerInstallationGuide-ContentReport")
    String PROP_CONCURRENCY_LIMIT_CONTENT_REPORT_HELP = "propertyConcurrencyLimitContentReportHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Concurrency Limit - Content Downloads") })
    String PROP_CONCURRENCY_LIMIT_CONTENT_DOWNLOAD = "propertyConcurrencyLimitContentDownload";
    @I18NMessage("#d.JONServerInstallationGuide-ContentDownload")
    String PROP_CONCURRENCY_LIMIT_CONTENT_DOWNLOAD_HELP = "propertyConcurrencyLimitContentDownloadHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Concurrency Limit - Measurement Reports") })
    String PROP_CONCURRENCY_LIMIT_MEAS_REPORT = "propertyConcurrencyLimitMeasurementReport";
    @I18NMessage("#d.JONServerInstallationGuide-MeasurementReport")
    String PROP_CONCURRENCY_LIMIT_MEAS_REPORT_HELP = "propertyConcurrencyLimitMeasurementReportHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Concurrency Limit - Measurement Schedule Requests") })
    String PROP_CONCURRENCY_LIMIT_MEASSCHED_REQ = "propertyConcurrencyLimitMeasurementScheduleRequest";
    @I18NMessage("#d.JONServerInstallationGuide-MeasurementScheduleRequest")
    String PROP_CONCURRENCY_LIMIT_MEASSCHED_REQ_HELP = "propertyConcurrencyLimitMeasurementScheduleRequestHelp";

    // Help Documentation - links to the wiki

    @I18NMessages( {
        @I18NMessage(locale = "en", value = "Please review the documentation linked below to learn more about RHQ:"),
        @I18NMessage(locale = "de", value = "Die unten aufgeführten Links enthalten weitere Informationen zu RHQ:") })
    String INTRODUCE_HELP_DOCS = "introduceHelpDocs";

    @I18NMessage("d.+JON+Server+Installation+Guide")
    String HELP_DOC_JON_SERVER_INSTALL_GUIDE = "helpDocJonServerInstallGuide";
    @I18NMessages( { @I18NMessage(locale = "en", value = "RHQ Server Installation Guide"),
        @I18NMessage(locale = "de", value = "RHQ Server Installationsanleitung") })
    String HELP_DOC_JON_SERVER_INSTALL_GUIDE_LABEL = "helpDocJonServerInstallGuideLabel";

    @I18NMessage("JON+GUI+Console+Users+Guide")
    String HELP_DOC_JON_GUI_CONSOLE_USERS_GUIDE = "helpDocJonGuiConsoleUsersGuide";
    @I18NMessages( { @I18NMessage(locale = "en", value = "RHQ GUI Console Users Guide"),
        @I18NMessage(locale = "de", value = "Benutzerhandbuch RHQ GUI") })
    String HELP_DOC_JON_GUI_CONSOLE_USERS_GUIDE_LABEL = "helpDocJonGuiConsoleUsersGuideLabel";

    @I18NMessage("JON+Server+Users+Guide")
    String HELP_DOC_JON_SERVER_USERS_GUIDE = "helpDocJonServerUsersGuide";
    @I18NMessages( { @I18NMessage(locale = "en", value = "RHQ Server Users Guide"),
        @I18NMessage(locale = "de", value = "Benutzerhandbuch RHQ Server") })
    String HELP_DOC_JON_SERVER_USERS_GUIDE_LABEL = "helpDocJonServerUsersGuideLabel";

    @I18NMessage("JON+Agent+Users+Guide")
    String HELP_DOC_JON_AGENT_USERS_GUIDE = "helpDocJonAgentUsersGuide";
    @I18NMessages( { @I18NMessage(locale = "en", value = "RHQ Agent Users Guide"),
        @I18NMessage(locale = "de", value = "Benutzerhandbuch RHQ Agent") })
    String HELP_DOC_JON_AGENT_USERS_GUIDE_LABEL = "helpDocJonAgentUsersGuideLabel";

    @I18NMessage("FAQ")
    String HELP_DOC_FAQ = "helpDocFaq";
    @I18NMessages( { @I18NMessage(locale = "en", value = "Frequently Asked Questions"),
        @I18NMessage(locale = "de", value = "Häufig gestellte Fragen") })
    String HELP_DOC_FAQ_LABEL = "helpDocFaqLabel";

    // as we translate our documentation - point to each language's root doc location
    @I18NMessages( { @I18NMessage(locale = "en", value = "https://network.jboss.com/confluence/display/JON2/") })
    String HELP_DOC_ROOT = "helpDocRoot";
}