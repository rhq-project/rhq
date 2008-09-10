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

    @I18NMessage("${product.shortName}")
    String PRODUCT_SHORTNAME = "product.shortName";
    @I18NMessage("${product.name}")
    String PRODUCT_NAME = "product.name";
    @I18NMessage("${product.fullName}")
    String PRODUCT_FULLNAME = "product.fullName";
    @I18NMessage("${product.url}")
    String PRODUCT_URL = "product.url";
    @I18NMessage("${product.sales.email}")
    String PRODUCT_SALES_EMAIL = "product.sales.email";
    @I18NMessage("${product.support.email}")
    String PRODUCT_SUPPORT_EMAIL = "product.support.email";

    // as we translate our documentation - point to each language's root doc location
    @I18NMessages( { @I18NMessage(locale = "en", value = "${product.help.doc.root}") })
    String HELP_DOC_ROOT = "helpDocRoot";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Create Database/User"),
        @I18NMessage(locale = "de", value = "Datenbank mit -benutzer anlegen") })
    String CREATE_DATABASE_USER_TITLE = "createDatabaseUserTitle";

    @I18NMessages( {
        @I18NMessage(locale = "en", value = "Enter your database administrator's information here and press the button to create a database and user."),
        @I18NMessage(locale = "de", value = "Geben Sie die Administratoreninformation hier ein, und clicken Sie den Button, um eine Datenbank mit Benutzer anzulegen.") })
    String CREATE_DATABASE_USER_HELP = "createDatabaseUserHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "DB Connection URL"),
        @I18NMessage(locale = "de", value = "URL der Datenbankverbindung") })
    String ADMIN_CONNECTION_URL = "adminConnectionUrl";

    @I18NMessages( { @I18NMessage(locale = "en", value = "DB Admin Username"),
        @I18NMessage(locale = "de", value = "Benutzername des Datenbankadministrators") })
    String ADMIN_USERNAME = "adminUsername";

    @I18NMessages( { @I18NMessage(locale = "en", value = "DB Admin Password"),
        @I18NMessage(locale = "de", value = "Passwort des Datenbankadministrators") })
    String ADMIN_PASSWORD = "adminPassword";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Create a database/user if needed"),
        @I18NMessage(locale = "de", value = "DB mit -benutzer anlegen, falls nötig") })
    String CREATE_DATABASE_NOTE = "createDatabaseNote";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Create Database"),
        @I18NMessage(locale = "de", value = "Datenbank anlegen") })
    String CREATE_DATABASE_BUTTON = "createDatabaseButton";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Confirm database settings"),
        @I18NMessage(locale = "de", value = "Überprüfen Sie, dass die Einstellungen für die Datenbank gültig sind") })
    String TEST_DATABASE_NOTE = "testDatabaseNote";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Test Connection"),
        @I18NMessage(locale = "de", value = "Verbindung testen") })
    String TEST_DATABASE_BUTTON = "testDatabaseButton";

    @I18NMessages( { @I18NMessage(locale = "en", value = "A database schema already exists. What do you want to do?"),
        @I18NMessage(locale = "de", value = "Das gewünschte Datenbankschema existiert schon. Was wollen Sie machen?") })
    String EXISTING_SCHEMA_QUESTION = "existingSchemaQuestion";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Overwrite (lose existing data)"),
        @I18NMessage(locale = "de", value = "Überschreiben (vorhandene Daten gehen verloren)") })
    String EXISTING_SCHEMA_OPTION_OVERWRITE = "existingSchemaAnswerOverwrite";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Keep (maintain existing data)"),
        @I18NMessage(locale = "de", value = "Behalten (existierende Daten werden aktualisiert)") })
    String EXISTING_SCHEMA_OPTION_KEEP = "existingSchemaAnswerUpgrade";

    @I18NMessages( { @I18NMessage(locale = "en", value = "The {0} property value was an invalid number [{1}]"),
        @I18NMessage(locale = "de", value = "[{1}] ist ein ungültiger Zahlenwert für die Eigenschaft {0}") })
    String INVALID_NUMBER = "invalidNumber";

    @I18NMessages( {
        @I18NMessage(locale = "en", value = "The {0} boolean property value must be either 'true' or 'false' but was [{1}]"),
        @I18NMessage(locale = "de", value = "Die boolesche Eigenschaft {0} muss entweder 'true' oder 'false' sein, war aber [{1}]") })
    String INVALID_BOOLEAN = "invalidBoolean";

    @I18NMessages( { @I18NMessage(locale = "en", value = "The {0} property value must be set."),
        @I18NMessage(locale = "de", value = "Die Eigenschaft {0} muss gesetzt werden.") })
    String INVALID_STRING = "invalidString";

    @I18NMessages( {
        @I18NMessage(locale = "en", value = "Failed to save properties and fully deploy - ${product.shortName} Server will not function properly\\n\\\nCause: {0}"),
        @I18NMessage(locale = "de", value = "Konnte die Einstellungen nicht speichern und den Server vollständig starten.\\n\\\n"
            + "Der ${product.shortName} Server wird nicht korrekt funktionieren.\\n\\\nGrund: {0}") })
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

    @I18NMessages( {
        @I18NMessage(locale = "en", value = "These are the configuration settings for this ${product.shortName} Server installation. \\n\\\n"
            + "You may customize some, all or none as required.  Click on the setting name for setting-specific help. Changes to settings marked as \"Restart Required\" \\n\\\n"
            + "will not take effect until after restarting the server. In this case restart the server immediately after successful installation."),
        @I18NMessage(locale = "de", value = "Diese Seite zeigt Ihnen die Konfigurationseinstellungen \\n\\\n"
            + "für die Installation des ${product.shortName} Servers. Sie können keinen, einige oder alle\\n\\\n"
            + "dieser Werte nach Ihren Anforderungen ändern. Sie können auf den Namen einer Einstellung klicken, um direkt weitere Informationen zu erhalten.\\n\\\n"
            + "Bitte beachten sie, dass \\n\\\n"
            + "einige dieser Änderungen erst nach einem Neustart des Servers aktiv werden. \\n\\\n"
            + "Wenn Sie diese Einstellungen ändern, müssen Sie den Server sofort neu starten, damit \\n\\\n"
            + "sie aufgegriffen werden.") })
    String SET_PROPERTIES_INSTRUCTIONS = "setPropertiesInstructions";

    @I18NMessages( {
        @I18NMessage(locale = "en", value = "Click this checkbox to see both standard and advanced configuration settings."),
        @I18NMessage(locale = "de", value = "Markieren Sie diese Checkbox, um die standardmäßigen und weiterführende Einstellungen anzuzeigen.") })
    String ADVANCED_SETTINGS_INSTRUCTIONS = "advancedSettingsInstructions";

    @I18NMessages( {
        @I18NMessage(locale = "en", value = "Database Settings define the database configured for this installation. All are required. Use the \"Test Connection\" button to validate the settings."),
        @I18NMessage(locale = "de", value = "Die Datenbankeinstellungen definieren die Datenbank für diese Installation. Alle einstellungen werden benötigt.\\n\\\n"
            + "Klicken Sie auf den \"Verbindung testen\" Button, um die Einstellungen zu validieren.") })
    String DATABASE_SETTINGS_INSTRUCTIONS = "databaseSettingsInstructions";

    @I18NMessages( {
        @I18NMessage(locale = "en", value = "Installation Settings define the required server endpoint for this installation."),
        @I18NMessage(locale = "de", value = "Diese Einstellungen definieren den notwendigen Server-Endpunkt für diese Installation.") })
    String INSTALL_SETTINGS_INSTRUCTIONS = "installSettingsInstructions";

    @I18NMessages( {
        @I18NMessage(locale = "en", value = "If performing an upgrade or re-installation then select from the list of registered servers: "),
        @I18NMessage(locale = "de", value = "Wählen Sie aus der Liste der registrierten Server, wenn Sie ein Upgrade oder eine Neuinstallation durchführen.") })
    String INSTALL_SETTINGS_NOTE_1 = "installSettingsNote1";

    @I18NMessages( {
        @I18NMessage(locale = "en", value = "If installing a new server enter the server endpoint settings below:"),
        @I18NMessage(locale = "de", value = "Geben Sie den Serverendpunkt unten ein, wenn Sie eine Neuinstallation durchführen:") })
    String INSTALL_SETTINGS_NOTE_2 = "installSettingsNote2";

    @I18NMessages( {
        @I18NMessage(locale = "en", value = "Server Settings configure the server for this installation. All server settings are required."),
        @I18NMessage(locale = "de", value = "Diese Servereinstellungen konfigurieren den Server für diese Installation. Alle Einstellungen werden benötigt.") })
    String SERVER_SETTINGS_INSTRUCTIONS = "serverSettingsInstructions";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Install ${product.shortName} Server!"),
        @I18NMessage(locale = "de", value = "${product.shortName} Server installieren!") })
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

    @I18NMessages( { @I18NMessage(locale = "en", value = "Welcome to ${product.shortName}!"),
        @I18NMessage(locale = "de", value = "Willkommen bei ${product.shortName}!") })
    String WELCOME_TITLE = "welcomeTitle";

    @I18NMessages( {
        @I18NMessage(locale = "en", value = "You have reached the ${product.shortName} Installer. Use this page \\n\\\n"
            + "to install and configure the ${product.shortName} Server. \\n\\\n"
            + "Once complete you will be able to log on and use ${product.shortName}."),
        @I18NMessage(locale = "de", value = "Willkommen beim Installationsprogramm von ${product.shortName}. Mit diesem können Sie \\n\\\n"
            + "${product.shortName} installieren und konfigurieren. \\n\\\n"
            + "Nachdem dies geschehen ist, können Sie sich einloggen und mit ${product.shortName} arbeiten") })
    String WELCOME_MESSAGE = "welcomeMessage";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Starting up, please wait..."),
        @I18NMessage(locale = "de", value = "Bitte warten Sie bis der Server gestartet ist") })
    String STARTING = "starting";

    @I18NMessages( { @I18NMessage(locale = "en", value = "${product.shortName} will be installed!"),
        @I18NMessage(locale = "de", value = "${product.shortName} wird installiert") })
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
    // The _HELP I18NMessage strings refer to links under HELP_DOC_RHQ_SERVER_PROP_PARENT_PAGE

    @I18NMessages( { @I18NMessage(locale = "en", value = "Database Type"),
        @I18NMessage(locale = "de", value = "Datenbanktyp") })
    String PROP_DATABASE_TYPE = "propertyDatabaseType";
    @I18NMessage("-DatabaseType")
    String PROP_DATABASE_TYPE_HELP = "propertyDatabaseTypeHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Database Connection URL"),
        @I18NMessage(locale = "de", value = "URL der Datenbankverbindung") })
    String PROP_DATABASE_CONNECTION_URL = "propertyDatabaseConnectionUrl";
    @I18NMessage("-DatabaseConnectionURL")
    String PROP_DATABASE_CONNECTION_URL_HELP = "propertyDatabaseConnectionUrlHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Database JDBC Driver Class"),
        @I18NMessage(locale = "de", value = "Klassnname des JDBC-Datenbanktreibers") })
    String PROP_DATABASE_DRIVER_CLASS = "propertyDatabaseDriverClass";
    @I18NMessage("-DatabaseJDBCDriverClass")
    String PROP_DATABASE_DRIVER_CLASS_HELP = "propertyDatabaseDriverClassHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Database User Name"),
        @I18NMessage(locale = "de", value = "Name des Datenbankbenutzers") })
    String PROP_DATABASE_USERNAME = "propertyDatabaseUserName";
    @I18NMessage("-DatabaseUserName")
    String PROP_DATABASE_USERNAME_HELP = "propertyDatabaseUserNameHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Database Password"),
        @I18NMessage(locale = "de", value = "Password des Datenbankbenutzers") })
    String PROP_DATABASE_PASSWORD = "propertyDatabasePassword";
    @I18NMessage("-DatabasePassword")
    String PROP_DATABASE_PASSWORD_HELP = "propertyDatabasePasswordHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Server Bind Address"),
        @I18NMessage(locale = "de", value = "IP-Adresse an die der Sever sich binden soll") })
    String PROP_SERVER_BIND_ADDRESS = "propertyBindAddress";
    @I18NMessage("-ServerBindAddress")
    String PROP_SERVER_BIND_ADDRESS_HELP = "propertyBindAddressHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "HTTP Port") })
    String PROP_HTTP_PORT = "propertyHttpPort";
    @I18NMessage("-HTTPPort")
    String PROP_HTTP_PORT_HELP = "propertyHttpPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Secure HTTPS Port"),
        @I18NMessage(locale = "de", value = "HTTPS Port") })
    String PROP_HTTPS_PORT = "propertyHttpsPort";
    @I18NMessage("-SecureHTTPSPort")
    String PROP_HTTPS_PORT_HELP = "propertyHttpsPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Web Service Port") })
    String PROP_WEB_SERVICE_PORT = "propertyWebServicePort";
    @I18NMessage("-WebServicePort")
    String PROP_WEB_SERVICE_PORT_HELP = "propertyWebServicePortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Naming Service Port") })
    String PROP_NAMING_SERVICE_PORT = "propertyNamingServicePort";
    @I18NMessage("-NamingServicePort")
    String PROP_NAMING_SERVICE_PORT_HELP = "propertyNamingServicePortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Naming Service RMI Port") })
    String PROP_NAMING_SERVICE_RMI_PORT = "propertyNamingServiceRmiPort";
    @I18NMessage("-NamingServiceRMIPort")
    String PROP_NAMING_SERVICE_RMI_PORT_HELP = "propertyNamingServiceRmiPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "JRMP Invoker RMI Port") })
    String PROP_JRMP_INVOKER_RMI_PORT = "propertyJrmpInvokerRmiPort";
    @I18NMessage("-JRMPInvokerRMIPort")
    String PROP_JRMP_INVOKER_RMI_PORT_HELP = "propertyJrmpInvokerRmiPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Pooled Invoker RMI Port") })
    String PROP_POOLED_INVOKER_RMI_PORT = "propertyPooledInvokerRmiPort";
    @I18NMessage("-PooledInvokerRMIPort")
    String PROP_POOLED_INVOKER_RMI_PORT_HELP = "propertyPooledInvokerRmiPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "AJP Port") })
    String PROP_AJP_PORT = "propertyAjpPort";
    @I18NMessage("-AJPPort")
    String PROP_AJP_PORT_HELP = "propertyAjpPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Unified Invoker Port") })
    String PROP_UNIFIED_INVOKER_PORT = "propertyUnifiedInvokerPort";
    @I18NMessage("-UnifiedInvokerPort")
    String PROP_UNIFIED_INVOKER_PORT_HELP = "propertyUnifiedInvokerPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Aspect Deployer Port") })
    String PROP_ASPECT_DEPLOYER_PORT = "propertyAspectDeployerPort";
    @I18NMessage("-AspectDeployerPort")
    String PROP_ASPECT_DEPLOYER_PORT_HELP = "propertyAspectDeployerPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "RHQ Console Keystore") })
    String PROP_TOMCAT_KEYSTORE_FILENAME = "propertyTomcatKeystoreFilename";
    @I18NMessage("-RHQConsoleKeystore")
    String PROP_TOMCAT_KEYSTORE_FILENAME_HELP = "propertyTomcatKeystoreFilenameHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "RHQ Console Keystore Password") })
    String PROP_TOMCAT_KEYSTORE_PASSWORD = "propertyTomcatKeystorePassword";
    @I18NMessage("-RHQConsoleKeystorePassword")
    String PROP_TOMCAT_KEYSTORE_PASSWORD_HELP = "propertyTomcatKeystorePasswordHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "RHQ Console SSL Protocol") })
    String PROP_TOMCAT_SSL_PROTOCOL = "propertyTomcatSslProtocol";
    @I18NMessage("-RHQConsoleSSLProtocol")
    String PROP_TOMCAT_SSL_PROTOCOL_HELP = "propertyTomcatSslProtocolHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Agent Communications Transport") })
    String PROP_CONNECTOR_TRANSPORT = "propertyConnectorTransport";
    @I18NMessage("-IncomingAgentCommunicationsTransport")
    String PROP_CONNECTOR_TRANSPORT_HELP = "propertyConnectorTransportHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Agent Communications Bind Address") })
    String PROP_CONNECTOR_BIND_ADDRESS = "propertyConnectorBindAddress";
    @I18NMessage("-IncomingAgentCommunicationsBindAddress")
    String PROP_CONNECTOR_BIND_ADDRESS_HELP = "propertyConnectorBindAddressHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Agent Communications Port") })
    String PROP_CONNECTOR_BIND_PORT = "propertyConnectorBindPort";
    @I18NMessage("-IncomingAgentCommunicationsPort")
    String PROP_CONNECTOR_BIND_PORT_HELP = "propertyConnectorBindPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Agent Communications Transport Parameters") })
    String PROP_CONNECTOR_TRANSPORT_PARAMS = "propertyConnectorTransportParams";
    @I18NMessage("-IncomingAgentCommunicationsTransportParameters")
    String PROP_CONNECTOR_TRANSPORT_PARAMS_HELP = "propertyConnectorTransportParamsHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Agent Multicast Detector Enabled") })
    String PROP_AGENT_MULTICAST_DETECTOR_ENABLED = "propertyAgentMulticastDetectorEnabled";
    @I18NMessage("-AgentMulticastDetectorEnabled")
    String PROP_AGENT_MULTICAST_DETECTOR_ENABLED_HELP = "propertyAgentMulticastDetectorEnabledHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Agent Multicast Detector Bind Address") })
    String PROP_AGENT_MULTICAST_DETECTOR_BIND_ADDRESS = "propertyAgentMulticastDetectorBindAddress";
    @I18NMessage("-AgentMulticastDetectorBindAddress")
    String PROP_AGENT_MULTICAST_DETECTOR_BIND_ADDRESS_HELP = "propertyAgentMulticastDetectorBindAddressHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Agent Multicast Detector Multicast Address") })
    String PROP_AGENT_MULTICAST_DETECTOR_MULTICAST_ADDRESS = "propertyAgentMulticastDetectorMulticastAddress";
    @I18NMessage("-AgentMulticastDetectorMulticastAddress")
    String PROP_AGENT_MULTICAST_DETECTOR_MULTICAST_ADDRESS_HELP = "propertyAgentMulticastDetectorMulticastAddressHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Agent Multicast Detector Port") })
    String PROP_AGENT_MULTICAST_DETECTOR_PORT = "propertyAgentMulticastDetectorPort";
    @I18NMessage("-AgentMulticastDetectorPort")
    String PROP_AGENT_MULTICAST_DETECTOR_PORT_HELP = "propertyAgentMulticastDetectorPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Security - Secure Socket Protocol") })
    String PROP_SECURITY_SERVER_SECURE_SOCKET_PROTOCOL = "propertySecurityServerSecureSocketProtocol";
    @I18NMessage("-IncomingSecureSocketProtocol")
    String PROP_SECURITY_SERVER_SECURE_SOCKET_PROTOCOL_HELP = "propertySecurityServerSecureSocketProtocolHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Security - Keystore File") })
    String PROP_SECURITY_SERVER_KEYSTORE_FILE = "propertySecurityServerKeystoreFile";
    @I18NMessage("-IncomingKeystoreFile")
    String PROP_SECURITY_SERVER_KEYSTORE_FILE_HELP = "propertySecurityServerKeystoreFileHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Security - Keystore Algorithm") })
    String PROP_SECURITY_SERVER_KEYSTORE_ALGORITHM = "propertySecurityServerKeystoreAlgorithm";
    @I18NMessage("-IncomingKeystoreAlgorithm")
    String PROP_SECURITY_SERVER_KEYSTORE_ALGORITHM_HELP = "propertySecurityServerKeystoreAlgorithmHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Security - Keystore Type") })
    String PROP_SECURITY_SERVER_KEYSTORE_TYPE = "propertySecurityServerKeystoreType";
    @I18NMessage("-IncomingKeystoreType")
    String PROP_SECURITY_SERVER_KEYSTORE_TYPE_HELP = "propertySecurityServerKeystoreTypeHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Security - Keystore Password") })
    String PROP_SECURITY_SERVER_KEYSTORE_PASSWORD = "propertySecurityServerKeystorePassword";
    @I18NMessage("-IncomingKeystorePassword")
    String PROP_SECURITY_SERVER_KEYSTORE_PASSWORD_HELP = "propertySecurityServerKeystorePasswordHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Security - Keystore Key Password") })
    String PROP_SECURITY_SERVER_KEYSTORE_KEY_PASSWORD = "propertySecurityServerKeystoreKeyPassword";
    @I18NMessage("-IncomingKeystoreKeyPassword")
    String PROP_SECURITY_SERVER_KEYSTORE_KEY_PASSWORD_HELP = "propertySecurityServerKeystoreKeyPasswordHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Security - Keystore Alias") })
    String PROP_SECURITY_SERVER_KEYSTORE_ALIAS = "propertySecurityServerKeystoreAlias";
    @I18NMessage("-IncomingKeystoreAlias")
    String PROP_SECURITY_SERVER_KEYSTORE_ALIAS_HELP = "propertySecurityServerKeystoreAliasHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Security - Truststore File") })
    String PROP_SECURITY_SERVER_TRUSTSTORE_FILE = "propertySecurityServerTruststoreFile";
    @I18NMessage("-IncomingTruststoreFile")
    String PROP_SECURITY_SERVER_TRUSTSTORE_FILE_HELP = "propertySecurityServerTruststoreFileHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Security - Truststore Algorithm") })
    String PROP_SECURITY_SERVER_TRUSTSTORE_ALGORITHM = "propertySecurityServerTruststoreAlgorithm";
    @I18NMessage("-IncomingTruststoreAlgorithm")
    String PROP_SECURITY_SERVER_TRUSTSTORE_ALGORITHM_HELP = "propertySecurityServerTruststoreAlgorithmHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Security - Truststore Type") })
    String PROP_SECURITY_SERVER_TRUSTSTORE_TYPE = "propertySecurityServerTruststoreType";
    @I18NMessage("-IncomingTruststoreType")
    String PROP_SECURITY_SERVER_TRUSTSTORE_TYPE_HELP = "propertySecurityServerTruststoreTypeHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Security - Truststore Password") })
    String PROP_SECURITY_SERVER_TRUSTSTORE_PASSWORD = "propertySecurityServerTruststorePassword";
    @I18NMessage("-IncomingTruststorePassword")
    String PROP_SECURITY_SERVER_TRUSTSTORE_PASSWORD_HELP = "propertySecurityServerTruststorePasswordHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Incoming Security - Client Authentication Mode") })
    String PROP_SECURITY_SERVER_CLIENT_AUTH_MODE = "propertySecurityServerClientAuthMode";
    @I18NMessage("-IncomingClientAuthenticationMode")
    String PROP_SECURITY_SERVER_CLIENT_AUTH_MODE_HELP = "propertySecurityServerClientAuthModeHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Outgoing Security - Secure Socket Protocol") })
    String PROP_SECURITY_CLIENT_SECURE_SOCKET_PROTOCOL = "propertySecurityClientSecureSocketProtocol";
    @I18NMessage("-OutgoingSecureSocketProtocol")
    String PROP_SECURITY_CLIENT_SECURE_SOCKET_PROTOCOL_HELP = "propertySecurityClientSecureSocketProtocolHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Outgoing Security - Keystore File") })
    String PROP_SECURITY_CLIENT_KEYSTORE_FILE = "propertySecurityClientKeystoreFile";
    @I18NMessage("-OutgoingKeystoreFile")
    String PROP_SECURITY_CLIENT_KEYSTORE_FILE_HELP = "propertySecurityClientKeystoreFileHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Outgoing Security - Keystore Algorithm") })
    String PROP_SECURITY_CLIENT_KEYSTORE_ALGORITHM = "propertySecurityClientKeystoreAlgorithm";
    @I18NMessage("-OutgoingKeystoreAlgorithm")
    String PROP_SECURITY_CLIENT_KEYSTORE_ALGORITHM_HELP = "propertySecurityClientKeystoreAlgorithmHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Outgoing Security - Keystore Type") })
    String PROP_SECURITY_CLIENT_KEYSTORE_TYPE = "propertySecurityClientKeystoreType";
    @I18NMessage("-OutgoingKeystoreType")
    String PROP_SECURITY_CLIENT_KEYSTORE_TYPE_HELP = "propertySecurityClientKeystoreTypeHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Outgoing Security - Keystore Password") })
    String PROP_SECURITY_CLIENT_KEYSTORE_PASSWORD = "propertySecurityClientKeystorePassword";
    @I18NMessage("-OutgoingKeystorePassword")
    String PROP_SECURITY_CLIENT_KEYSTORE_PASSWORD_HELP = "propertySecurityClientKeystorePasswordHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Outgoing Security - Keystore Key Password") })
    String PROP_SECURITY_CLIENT_KEYSTORE_KEY_PASSWORD = "propertySecurityClientKeystoreKeyPassword";
    @I18NMessage("-OutgoingKeystoreKeyPassword")
    String PROP_SECURITY_CLIENT_KEYSTORE_KEY_PASSWORD_HELP = "propertySecurityClientKeystoreKeyPasswordHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Outgoing Security - Keystore Alias") })
    String PROP_SECURITY_CLIENT_KEYSTORE_ALIAS = "propertySecurityClientKeystoreAlias";
    @I18NMessage("-OutgoingKeystoreAlias")
    String PROP_SECURITY_CLIENT_KEYSTORE_ALIAS_HELP = "propertySecurityClientKeystoreAliasHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Outgoing Security - Truststore File") })
    String PROP_SECURITY_CLIENT_TRUSTSTORE_FILE = "propertySecurityClientTruststoreFile";
    @I18NMessage("-OutgoingTruststoreFile")
    String PROP_SECURITY_CLIENT_TRUSTSTORE_FILE_HELP = "propertySecurityClientTruststoreFileHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Outgoing Security - Truststore Algorithm") })
    String PROP_SECURITY_CLIENT_TRUSTSTORE_ALGORITHM = "propertySecurityClientTruststoreAlgorithm";
    @I18NMessage("-OutgoingTruststoreAlgorithm")
    String PROP_SECURITY_CLIENT_TRUSTSTORE_ALGORITHM_HELP = "propertySecurityClientTruststoreAlgorithmHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Outgoing Security - Truststore Type") })
    String PROP_SECURITY_CLIENT_TRUSTSTORE_TYPE = "propertySecurityClientTruststoreType";
    @I18NMessage("-OutgoingTruststoreType")
    String PROP_SECURITY_CLIENT_TRUSTSTORE_TYPE_HELP = "propertySecurityClientTruststoreTypeHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Outgoing Security - Truststore Password") })
    String PROP_SECURITY_CLIENT_TRUSTSTORE_PASSWORD = "propertySecurityClientTruststorePassword";
    @I18NMessage("-OutgoingTruststorePassword")
    String PROP_SECURITY_CLIENT_TRUSTSTORE_PASSWORD_HELP = "propertySecurityClientTruststorePasswordHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Outgoing Security - Server Authentication Mode Enabled") })
    String PROP_SECURITY_CLIENT_SERVER_AUTH_MODE_ENABLED = "propertySecurityClientServerAuthModeEnabled";
    @I18NMessage("-OutgoingServerAuthenticationModeEnabled")
    String PROP_SECURITY_CLIENT_SERVER_AUTH_MODE_ENABLED_HELP = "propertySecurityClientServerAuthModeEnabledHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Embedded Agent Enabled"),
        @I18NMessage(locale = "de", value = "Eingebetteten Agent verwenden") })
    String PROP_EMBEDDED_RHQ_AGENT_ENABLED = "propertyEmbeddedRHQAgentEnabled";
    @I18NMessage("-EmbeddedRHQAgentEnabled")
    String PROP_EMBEDDED_RHQ_AGENT_ENABLED_HELP = "propertyEmbeddedRHQAgentEnabledHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Embedded Agent Name"),
        @I18NMessage(locale = "de", value = "Name des eingebetteten Agent") })
    String PROP_EMBEDDED_RHQ_AGENT_NAME = "propertyEmbeddedRHQAgentName";
    @I18NMessage("-EmbeddedRHQAgentName")
    String PROP_EMBEDDED_RHQ_AGENT_NAME_HELP = "propertyEmbeddedRHQAgentNameHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Embedded Agent Disable Native System") })
    String PROP_EMBEDDED_RHQ_AGENT_DISABLE_NATIVE_SYSTEM = "propertyEmbeddedRHQAgentDisableNativeSystem";
    @I18NMessage("-EmbeddedRHQAgentDisableNativeSystem")
    String PROP_EMBEDDED_RHQ_AGENT_DISABLE_NATIVE_SYSTEM_HELP = "propertyEmbeddedRHQAgentDisableNativeSystemHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Embedded Agent Reset Configuration") })
    String PROP_EMBEDDED_RHQ_AGENT_RESET_CONFIGURATION = "propertyEmbeddedRHQAgentResetConfiguration";
    @I18NMessage("-EmbeddedRHQAgentResetConfiguration")
    String PROP_EMBEDDED_RHQ_AGENT_RESET_CONFIGURATION_HELP = "propertyEmbeddedRHQAgentResetConfigurationHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Email SMTP Hostname"),
        @I18NMessage(locale = "de", value = "Hostname des SMTP Servers") })
    String PROP_EMAIL_SMTP_HOST = "propertyEmailSmtpHost";
    @I18NMessage("-EmailSMTPHostname")
    String PROP_EMAIL_SMTP_HOST_HELP = "propertyEmailSmtpHostHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Email SMTP Port") })
    String PROP_EMAIL_SMTP_PORT = "propertyEmailSmtpPort";
    @I18NMessage("-EmailSMTPPort")
    String PROP_EMAIL_SMTP_PORT_HELP = "propertyEmailSmtpPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Email From Address"),
        @I18NMessage(locale = "de", value = "E-Mail Absenderadresse") })
    String PROP_EMAIL_FROM_ADDRESS = "propertyEmailFromAddress";
    @I18NMessage("-EmailFromAddress")
    String PROP_EMAIL_FROM_ADDRESS_HELP = "propertyEmailFromAddressHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Operation Invocation Default Timeout") })
    String PROP_OPERATION_TIMEOUT = "propertyOperationTimeout";
    @I18NMessage("-OperationInvocationDefaultTimeout")
    String PROP_OPERATION_TIMEOUT_HELP = "propertyOperationTimeoutHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Cluster - Partition Name") })
    String PROP_CLUSTER_PARTITION_NAME = "propertyClusterPartitionName";
    @I18NMessage("-PartitionName")
    String PROP_CLUSTER_PARTITION_NAME_HELP = "propertyClusterPartitionNameHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Cluster - Partition Bind Address") })
    String PROP_CLUSTER_PARTITION_BIND_ADDRESS = "propertyClusterPartitionBindAddress";
    @I18NMessage("-PartitionBindAddress")
    String PROP_CLUSTER_PARTITION_BIND_ADDRESS_HELP = "propertyClusterPartitionBindAddressHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Cluster - Partition UDP Multicast Group IP Address") })
    String PROP_CLUSTER_UDP_GROUP = "propertyClusterUdpGroup";
    @I18NMessage("-PartitionUDPMulticastGroupIPAddress")
    String PROP_CLUSTER_UDP_GROUP_HELP = "propertyClusterUdpGroupHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Cluster - Partition UDP Multicast Port") })
    String PROP_CLUSTER_HAPARTITION_PORT = "propertyClusterHaPartitionPort";
    @I18NMessage("-PartitionUDPMulticastPort")
    String PROP_CLUSTER_HAPARTITION_PORT_HELP = "propertyClusterHaPartitionPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Cluster - Partition UDP EJB3 Entity Cache Multicast Port") })
    String PROP_CLUSTER_EJB3CACHE_PORT = "propertyClusterEjb3CachePort";
    @I18NMessage("-PartitionUDPEJB3EntityCacheMulticastPort")
    String PROP_CLUSTER_EJB3CACHE_PORT_HELP = "propertyClusterEjb3CachePortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Cluster - Partition UDP Alert Cache Multicast Port") })
    String PROP_CLUSTER_ALERTCACHE_PORT = "propertyClusterAlertCachePort";
    @I18NMessage("-PartitionUDPAlertCacheMulticastPort")
    String PROP_CLUSTER_ALERTCACHE_PORT_HELP = "propertyClusterAlertCachePortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Cluster - Partition UDP Loopback") })
    String PROP_CLUSTER_UDP_LOOPBACK = "propertyClusterUdpLoopback";
    @I18NMessage("-PartitionUDPLoopback")
    String PROP_CLUSTER_UDP_LOOPBACK_HELP = "propertyClusterUdpLoopbackHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Cluster - HA JNDI Port") })
    String PROP_CLUSTER_HAJNDI_PORT = "propertyClusterHaJndiPort";
    @I18NMessage("-HAJNDIPort")
    String PROP_CLUSTER_HAJNDI_PORT_HELP = "propertyClusterHaJndiPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Cluster - HA JNDI RMI Port") })
    String PROP_CLUSTER_HAJNDI_RMIPORT = "propertyClusterHaJndiRmiPort";
    @I18NMessage("-HAJNDIRMIPort")
    String PROP_CLUSTER_HAJNDI_RMIPORT_HELP = "propertyClusterHaJndiRmiPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Cluster - HA JNDI Auto Discovery Group Port") })
    String PROP_CLUSTER_HAJNDI_AUTODISCOVERPORT = "propertyClusterHaJndiAutoDiscoveryPort";
    @I18NMessage("-HAJNDIAutoDiscoveryGroupPort")
    String PROP_CLUSTER_HAJNDI_AUTODISCOVERPORT_HELP = "propertyClusterHaJndiAutoDiscoveryPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Cluster - HA JRMP Invoker RMI Port") })
    String PROP_CLUSTER_HAJRMPINVOKER_RMIPORT = "propertyClusterHaJrmpInvokerRmiPort";
    @I18NMessage("-HAJRMPInvokerRMIPort")
    String PROP_CLUSTER_HAJRMPINVOKER_RMIPORT_HELP = "propertyClusterHaJrmpInvokerRmiPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Cluster - HA Pooled Invoker Port") })
    String PROP_CLUSTER_HAPOOLEDINVOKER_PORT = "propertyClusterHaPooledInvokerPort";
    @I18NMessage("-HAPooledInvokerPort")
    String PROP_CLUSTER_HAPOOLEDINVOKER_PORT_HELP = "propertyClusterHaPooledInvokerPortHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Cluster - JGroups UDP IP Time To Live") })
    String PROP_CLUSTER_JGROUPS_UDP_IP_TTL = "propertyClusterJGroupsUdpIpTtl";
    @I18NMessage("-JGroupsUDPIPTimeToLive")
    String PROP_CLUSTER_JGROUPS_UDP_IP_TTL_HELP = "propertyClusterJGroupsUdpIpTtlHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Concurrency Limit - Maximum Web Connections") })
    String PROP_CONCURRENCY_LIMIT_WEBCONNS = "propertyConcurrencyLimitWebConns";
    @I18NMessage("-WebConnections")
    String PROP_CONCURRENCY_LIMIT_WEBCONNS_HELP = "propertyConcurrencyLimitWebConnsHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Concurrency Limit - Global") })
    String PROP_CONCURRENCY_LIMIT_GLOBAL = "propertyConcurrencyLimitGlobal";
    @I18NMessage("-GlobalConcurrencyLimit")
    String PROP_CONCURRENCY_LIMIT_GLOBAL_HELP = "propertyConcurrencyLimitGlobalHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Concurrency Limit - Inventory Reports") })
    String PROP_CONCURRENCY_LIMIT_INV_REPORT = "propertyConcurrencyLimitInventoryReport";
    @I18NMessage("-InventoryReport")
    String PROP_CONCURRENCY_LIMIT_INV_REPORT_HELP = "propertyConcurrencyLimitInventoryReportHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Concurrency Limit - Availability Reports") })
    String PROP_CONCURRENCY_LIMIT_AVAIL_REPORT = "propertyConcurrencyLimitAvailabilityReport";
    @I18NMessage("-AvailabilityReport")
    String PROP_CONCURRENCY_LIMIT_AVAIL_REPORT_HELP = "propertyConcurrencyLimitAvailabilityReportHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Concurrency Limit - Inventory Synchronizations") })
    String PROP_CONCURRENCY_LIMIT_INV_SYNC = "propertyConcurrencyLimitInventorySync";
    @I18NMessage("-InventorySynchronization")
    String PROP_CONCURRENCY_LIMIT_INV_SYNC_HELP = "propertyConcurrencyLimitInventorySyncHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Concurrency Limit - Content Reports") })
    String PROP_CONCURRENCY_LIMIT_CONTENT_REPORT = "propertyConcurrencyLimitContentReport";
    @I18NMessage("-ContentReport")
    String PROP_CONCURRENCY_LIMIT_CONTENT_REPORT_HELP = "propertyConcurrencyLimitContentReportHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Concurrency Limit - Content Downloads") })
    String PROP_CONCURRENCY_LIMIT_CONTENT_DOWNLOAD = "propertyConcurrencyLimitContentDownload";
    @I18NMessage("-ContentDownload")
    String PROP_CONCURRENCY_LIMIT_CONTENT_DOWNLOAD_HELP = "propertyConcurrencyLimitContentDownloadHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Concurrency Limit - Measurement Reports") })
    String PROP_CONCURRENCY_LIMIT_MEAS_REPORT = "propertyConcurrencyLimitMeasurementReport";
    @I18NMessage("-MeasurementReport")
    String PROP_CONCURRENCY_LIMIT_MEAS_REPORT_HELP = "propertyConcurrencyLimitMeasurementReportHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Concurrency Limit - Measurement Schedule Requests") })
    String PROP_CONCURRENCY_LIMIT_MEASSCHED_REQ = "propertyConcurrencyLimitMeasurementScheduleRequest";
    @I18NMessage("-MeasurementScheduleRequest")
    String PROP_CONCURRENCY_LIMIT_MEASSCHED_REQ_HELP = "propertyConcurrencyLimitMeasurementScheduleRequestHelp";

    // Help Documentation - links to the wiki

    @I18NMessages( {
        @I18NMessage(locale = "en", value = "Please review the documentation linked below to learn more about ${product.shortName}:"),
        @I18NMessage(locale = "de", value = "Die unten aufgeführten Links enthalten weitere Informationen zu ${product.shortName}:") })
    String INTRODUCE_HELP_DOCS = "introduceHelpDocs";

    @I18NMessage("${product.help.installation}")
    String HELP_DOC_RHQ_SERVER_INSTALL_GUIDE = "helpDocRHQServerInstallGuide";
    @I18NMessages( { @I18NMessage(locale = "en", value = "${product.shortName} Server Installation Guide"),
        @I18NMessage(locale = "de", value = "${product.shortName} Server Installationsanleitung") })
    String HELP_DOC_RHQ_SERVER_INSTALL_GUIDE_LABEL = "helpDocRHQServerInstallGuideLabel";

    @I18NMessage("GUI+Console+Users+Guide")
    String HELP_DOC_RHQ_GUI_CONSOLE_USERS_GUIDE = "helpDocRHQGuiConsoleUsersGuide";
    @I18NMessages( { @I18NMessage(locale = "en", value = "${product.shortName} GUI Console Users Guide"),
        @I18NMessage(locale = "de", value = "Benutzerhandbuch ${product.shortName} GUI") })
    String HELP_DOC_RHQ_GUI_CONSOLE_USERS_GUIDE_LABEL = "helpDocRHQGuiConsoleUsersGuideLabel";

    @I18NMessage("${product.help.guide.server}")
    String HELP_DOC_RHQ_SERVER_USERS_GUIDE = "helpDocRHQServerUsersGuide";
    @I18NMessages( { @I18NMessage(locale = "en", value = "${product.shortName} Server Users Guide"),
        @I18NMessage(locale = "de", value = "Benutzerhandbuch ${product.shortName} Server") })
    String HELP_DOC_RHQ_SERVER_USERS_GUIDE_LABEL = "helpDocRHQServerUsersGuideLabel";

    @I18NMessage("${product.help.guide.agent}")
    String HELP_DOC_RHQ_AGENT_USERS_GUIDE = "helpDocRHQAgentUsersGuide";
    @I18NMessages( { @I18NMessage(locale = "en", value = "${product.shortName} Agent Users Guide"),
        @I18NMessage(locale = "de", value = "Benutzerhandbuch ${product.shortName} Agent") })
    String HELP_DOC_RHQ_AGENT_USERS_GUIDE_LABEL = "helpDocRHQAgentUsersGuideLabel";

    @I18NMessage("${product.help.FAQ}")
    String HELP_DOC_FAQ = "helpDocFaq";
    @I18NMessages( { @I18NMessage(locale = "en", value = "Frequently Asked Questions"),
        @I18NMessage(locale = "de", value = "Häufig gestellte Fragen") })
    String HELP_DOC_FAQ_LABEL = "helpDocFaqLabel";

    // the page that contains all the config props help text
    // this is under the help doc root
    @I18NMessage("${product.help.config.props}")
    String HELP_DOC_RHQ_SERVER_PROP_PARENT_PAGE = "helpDocRHQServerPropParentPage";

    @I18NMessages( { @I18NMessage(locale = "en", value = "New Server"),
        @I18NMessage(locale = "de", value = "Neuer Server") })
    String NEW_SERVER_SELECT_ITEM = "newServerSelectItem";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Registered Servers:"),
        @I18NMessage(locale = "de", value = "Registrierte Server:") })
    String REGISTERED_SERVERS_LABEL = "registeredServersLabel";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Server Name"),
        @I18NMessage(locale = "de", value = "Servername") })
    String PROP_HIGH_AVAILABILITY_NAME = "propertyHighAvailabilityName";
    @I18NMessage("-ServerName")
    String PROP_HIGH_AVAILABILITY_NAME_HELP = "propertyHighAvailabilityNameHelp";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Server Public Address"),
        @I18NMessage(locale = "de", value = "HA-Endpunkt-Adresse des Servers") })
    String PROP_HIGH_AVAILABILITY_ENDPOINT_ADDRESS = "propertyHighAvailabilityEndpointAddress";

    @I18NMessages( { @I18NMessage(locale = "en", value = "Server Affinity Group Name"),
        @I18NMessage(locale = "de", value = "Name der zugehörigen Servergruppe") })
    String PROP_HIGH_AVAILABILITY_AFFINITY_GROUP = "propertyHighAvailabilityAffinityGroup";
}