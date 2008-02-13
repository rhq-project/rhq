/*
 * JBoss, a division of Red Hat
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.rhq.core.db;

import mazz.i18n.annotation.I18NMessage;
import mazz.i18n.annotation.I18NMessages;
import mazz.i18n.annotation.I18NResourceBundle;

/**
 * I18N messages for the DB utilities.
 * 
 * ATTENTION: This file needs to be encoded in UTF-8 !
 *
 * @author John Mazzitelli
 *
 */
@I18NResourceBundle(baseName = "dbutils-messages", defaultLocale = "en")
public interface DbUtilsI18NResourceKeys {

    @I18NMessages( { @I18NMessage("Executing the following SQL: {0}"),
        @I18NMessage(value = "Führe das nachfolgnde SQL aus: {0}", locale = "de") })
    String EXECUTING_SQL = "DatabaseType.executing-sql";

    @I18NMessages( {
        @I18NMessage("The generic type [{0}] has an invalid mapping - there is an invalid attrib [{1}] with value [{2}]"),
        @I18NMessage(value = "Der generische Typ [{0}] hat ein ungülties Mapping: Es gibt ein ungültiges Attribut [{1}] mit Wert [{2}] ", locale = "de") })
    String INVALID_TYPE_MAP_ATTRIB = "TypeMap.invald-type-map-attrib";

    @I18NMessages( { @I18NMessage("Invalid typemap child element [{0}] - only [{1}] child elements are allowed"),
        @I18NMessage(value = "Ungültiges TypeMap-Kindelement [{0}]. Nur [{1}] Kinelemente sind erlaubt", locale = "de") })
    String INVALID_TYPE_MAP_CHILD = "TypeMap.invald-type-map-child";

    @I18NMessages( {
        @I18NMessage("Not a valid typemap - the attribute defining the generic type name [{0}] is missing"),
        @I18NMessage(value = "Keine gültige TypeMap: Das Attribut fehlt, das den generischen Namenstyp [{0}] benennt", locale = "de") })
    String MISSING_TYPE_MAP_GENERIC_TYPE = "TypeMap.missing-type-map-generic-type";

    @I18NMessages( { @I18NMessage("Node is not a valid TypeMap node: {0}"),
        @I18NMessage(value = "Knoten ist kein gültiger TypeMap-Knoten: {0}", locale = "de") })
    String NODE_NOT_VALID_TYPEMAP_NODE = "TypeMap.node-not-valid-typemap-node";

    @I18NMessages( { @I18NMessage("An invalid typemap definition was found. Cause: {0}"),
        @I18NMessage(value = "Eine ungültige TypeMap-Definition wurde gefunden. Der Grund ist: {0}", locale = "de") })
    String INVALID_TYPE_MAP = "TypeMap.invalid-type-map";

    @I18NMessages( { @I18NMessage("Error closing connection. Cause: {0}"),
        @I18NMessage(value = "Fehler beim Schließen der Datenbankverbindung. Der Grund ist {0}", locale = "de") })
    String DBTYPE_CLOSE_CONN_ERROR = "DatabaseType.close-conn-error";

    @I18NMessages( { @I18NMessage("Error closing statement. Cause: {0}"),
        @I18NMessage(value = "Fehler beim Schließen des Statements: {0}", locale = "de") })
    String DBTYPE_CLOSE_STATEMENT_ERROR = "DatabaseType.close-statement-error";

    @I18NMessages( { @I18NMessage("Error closing result set. Cause: {0}"),
        @I18NMessage(value = "Fehler beim Schließen des ResultSts. Der Grund ist: {0} ", locale = "de") })
    String DBTYPE_CLOSE_RESULTSET_ERROR = "DatabaseType.close-resultset-error";

    @I18NMessages( { @I18NMessage("Database connection is connected to database=[{0}], version=[{1}]"),
        @I18NMessage(value = "Verbunden mit Datenbank [{0}], Version [{1}] ", locale = "de") })
    String DB_CONNECTION_METADATA = "DatabaseTypeFactory.db-conn-metadata";

    @I18NMessages( { @I18NMessage("Unknown or unsupported database: name=[{0}], version=[{1}]"),
        @I18NMessage(value = "Unbekannte oder nicht unterstützte Datenbank: Name=[{0}], Version=[{1}]", locale = "de") })
    String UNKNOWN_DATABASE = "DatabaseTypeFactory.unknown-database";

    @I18NMessages( { @I18NMessage("Cannot retrieve value from the sequence table using query [{0}]"),
        @I18NMessage(value = "Kann den Wert der Sequenztabelle nicht über die Abfrage [{0}] ermitteln", locale = "de") })
    String NOT_A_SEQUENCE = "DatabaseTypeFactory.not-a-sequence";

    @I18NMessages( { @I18NMessage("Could not find the known typemaps file named [{0}] in the classloader."),
        @I18NMessage(value = "Kann die Datei mit bekannten TypeMaps [{0}] nicht im Classloader finden", locale = "de") })
    String KNOWN_TYPEMAPS_XML_FILE_NOT_FOUND = "TypeMap.known-typemaps-xml-file-not-found";

    @I18NMessages( { @I18NMessage("Type mappings have been loaded: {0}"),
        @I18NMessage(value = "Typen-Mappings wurden geladen: {0}", locale = "de") })
    String LOADED_TYPE_MAPS = "TypeMap.loaded-typemaps";

    @I18NMessages( {
        @I18NMessage("Cannot load the JDBC driver [{0}] for JDBC URL [{1}] - connecting to your database will probably fail."),
        @I18NMessage(value = "Kann den JDBC-Treiber [{0}] für die JBDC URL [{1}] nicht laden. Ein Verbindungsversuch wird wahrscheinlich fehlschlagen", locale = "de") })
    String CANNOT_LOAD_JDBC_DRIVER = "DatabaseTypeFactory.cannot-load-jdbc-driver";

    @I18NMessages( {
        @I18NMessage("The source [{0}] does not appear to contain valid DBSetup XML content"),
        @I18NMessage(value = "Die Quelldatei [{0}] scheint keine gültigen gültigen DBSetup XML-Inhalt zu beinhalten", locale = "de") })
    String DBSETUP_SOURCE_NOT_VALID = "DBSetup.source-not-valid";

    @I18NMessages( {
        @I18NMessage("Cannot process <include> tag; the path is relative but the source is a stream. The full path cannot be determined from a stream"),
        @I18NMessage(value = "Kann den <include>-Tag nicht verarbeiten: Der Pfade ist relativ, aber die Quelle ist ein Stream. Der vollständige Pfad kann aus einem Stream nicht ermittelt werden.", locale = "de") })
    String DBSETUP_PATHS_NOT_RELATIVE_TO_STREAM = "DBSetup.path-not-relative-to-stream";

    @I18NMessages( {
        @I18NMessage("A fatal XML parsing error occurred on line [{0}], column [{1}]: {2}"),
        @I18NMessage(value = "Ein schwerwiegender Fehler beim Parsen des XML ist in Zeile [{0}], Spalte [{1}] aufgetreten: [{2}]", locale = "de") })
    String DBSETUP_SAX_FATAL = "DBSetup.sax-fatal";

    @I18NMessages( {
        @I18NMessage("An XML parsing error occurred on line [{0}], column [{1}]: {2}"),
        @I18NMessage(value = "Ein Fehler beim Parsen des XML ist in Zeile [{0}], Spalte [{1}] aufgetreten: [{2}]", locale = "de") })
    String DBSETUP_SAX_ERROR = "DBSetup.sax-error";

    @I18NMessages( {
        @I18NMessage("An XML parsing warning occurred on line [{0}], column [{1}]: {2}"),
        @I18NMessage(value = "Eine Warnung beim Parsen des XML ist in Zeile [{0}], Spalte [{1}] aufgetreten: [{2}]", locale = "de") })
    String DBSETUP_SAX_WARNING = "DBSetup.sax-warning";

    @I18NMessages( {
        @I18NMessage("Successfully connected to the database via JDBC URL [{0}] as user [{1}]"),
        @I18NMessage(value = "Verbindung zur Datenbank mit JDBC-URL [{0}] als Benutzer [{1}] war erfolgreich", locale = "de") })
    String DBSETUP_CONNECTED_TO_DB = "DBSetup.connected-to-db";

    @I18NMessages( { @I18NMessage("Dropped view [{0}]"),
        @I18NMessage(value = "Datenbank-View [{0}] gelöscht", locale = "de") })
    String DBSETUP_DROPPED_VIEW = "DBSetup.dropped-view";

    @I18NMessages( { @I18NMessage("Failed to drop view [{0}]. Cause: {1}"),
        @I18NMessage(value = "Datenbank-View [{0}] konnte nicht gelöscht werden. Der Grund ist: [{1}]", locale = "de") })
    String DBSETUP_DROPPED_VIEW_ERROR = "DBSetup.dropped-view-error";

    @I18NMessages( { @I18NMessage("Dropped table [{0}] and its associated sequences"),
        @I18NMessage(value = "Tabelle [{0}] und ihre zugehörigen Sequenzen gelöscht", locale = "de") })
    String DBSETUP_DROPPED_TABLE = "DBSetup.dropped-table";

    @I18NMessages( {
        @I18NMessage("Failed to drop table [{0}] or one of its sequences. Cause: {1}"),
        @I18NMessage(value = "Konnte die Tabelle [{0}] oder eine ihrer Sequenzen nicht löschen. Der Grund ist: [{1}]", locale = "de") })
    String DBSETUP_DROPPED_TABLE_ERROR = "DBSetup.dropped-table-error";

    @I18NMessages( { @I18NMessage("[{0}] views dropped successfully."),
        @I18NMessage(value = "[{0}] Datenbank-Views erfolgreich gelöscht ", locale = "de") })
    String DBSETUP_UNINSTALL_DROPPED_VIEWS = "DBSetup.uninstall.views.dropped";

    @I18NMessages( { @I18NMessage("[{0}] views failed to drop."),
        @I18NMessage(value = "[{0}] Datenbank-Views konnten nicht gelöscht werden ", locale = "de") })
    String DBSETUP_UNINSTALL_FAILED_VIEWS = "DBSetup.uninstall.views.failed";

    @I18NMessages( { @I18NMessage("[{0}] tables dropped successfully."),
        @I18NMessage(value = "[{0}] Tabellen erfolgreich gelöscht ", locale = "de") })
    String DBSETUP_UNINSTALL_DROPPED_TABLES = "DBSetup.uninstall.tables.dropped";

    @I18NMessages( { @I18NMessage("[{0}] tables failed to drop."),
        @I18NMessage(value = "[{0}] Tabellen konnten nicht gelöscht werden ", locale = "de") })
    String DBSETUP_UNINSTALL_FAILED_TABLES = "DBSetup.uninstall.tables.failed";

    @I18NMessages( { @I18NMessage("Setting up table [{0}]"),
        @I18NMessage(value = "Setze Tabelle [{0}] auf", locale = "de") })
    String DBSETUP_SETTING_UP_TABLE = "DBSetup.setting-up-table";

    @I18NMessages( { @I18NMessage("Setting up view [{0}]"), @I18NMessage(value = "Setze View [{0}] auf", locale = "de") })
    String DBSETUP_SETTING_UP_VIEW = "DBSetup.setting-up-view";

    @I18NMessages( { @I18NMessage("Created table [{0}]"), @I18NMessage(value = "Tabelle [{0}] angelegt", locale = "de") })
    String DBSETUP_CREATED_TABLE = "DBSetup.created-table";

    @I18NMessages( { @I18NMessage("Failed to create table [{0}]"),
        @I18NMessage(value = "Konnte die Tabelle [{0}] nicht anlegen", locale = "de") })
    String DBSETUP_CREATED_TABLE_ERROR = "DBSetup.created-table-error";

    @I18NMessages( { @I18NMessage("Created index [{0}] for table [{1}]"),
        @I18NMessage(value = "Index [{0}] für Tabelle [{1}] angelegt", locale = "de") })
    String DBSETUP_CREATED_INDEX = "DBSetup.created-index";

    @I18NMessages( { @I18NMessage("Failed to create index [{0}] for table [{1}]"),
        @I18NMessage(value = "Konnte den Index [{0}] für Tabelle [{1}] nicht anlegen", locale = "de") })
    String DBSETUP_CREATED_INDEX_ERROR = "DBSetup.created-index-error";

    @I18NMessages( { @I18NMessage("Created view [{0}]"), @I18NMessage(value = "View [{0}] angelegt", locale = "de") })
    String DBSETUP_CREATED_VIEW = "DBSetup.created-view";

    @I18NMessages( { @I18NMessage("Failed to create view [{0}]"),
        @I18NMessage(value = "Konte den View [{0}] nicht anlegen", locale = "de") })
    String DBSETUP_CREATED_VIEW_ERROR = "DBSetup.created-view-error";

    @I18NMessages( { @I18NMessage("Deleted all rows from table [{0}]"),
        @I18NMessage(value = "Alle Zeilen von Tabelle [{0}] wurden gelöscht", locale = "de") })
    String DBSETUP_CLEARED_TABLE = "DBSetup.cleared-table";

    @I18NMessages( {
        @I18NMessage("Could not delete all rows from table [{0}]. This will be retried in the second pass. Cause: {1}"),
        @I18NMessage(value = "Konnte nicht alle Zeilen der Tabelle [{0}] löschen. Dies wird in einem zweiten Durchgang erneut versucht. Der Grund ist: {1}", locale = "de") })
    String DBSETUP_CLEARED_TABLE_ERROR_FIRST_PASS = "DBSetup.cleared-table-error-first-pass";

    @I18NMessages( { @I18NMessage("Failed to deleted all rows from table [{0}]. Cause: {1}"),
        @I18NMessage(value = "Konnte nicht alle Zeilen der Tabelle [{0}] löschen. Der Grund ist: {1}", locale = "de") })
    String DBSETUP_CLEARED_TABLE_ERROR_SECOND_PASS = "DBSetup.cleared-table-error-second-pass";

    @I18NMessages( {
        @I18NMessage("The first pass had [{0}] failed attempts to delete rows - a second pass will be performed to try them again"),
        @I18NMessage(value = "Der erste Durchgang hatte [{0}] vergebliche Versuche, Zeilen zu löschen. In einem zweiten Durchgang wird dies erneut versucht", locale = "de") })
    String DBSETUP_CLEAR_SECOND_PASS = "DBSetup.clear-second-pass";

    @I18NMessages( { @I18NMessage("[{0}] attempts to clear table data were successful."),
        @I18NMessage(value = "[{0}] Versuche Daten aus der Tabelle zu löschen, waren erfolgreich", locale = "de") })
    String DBSETUP_CLEAR_CLEARED_TABLES = "DBSetup.clear.tables.cleared";

    @I18NMessages( { @I18NMessage("[{0}] attempts to clear table data failed."),
        @I18NMessage(value = "[{0}] Versuche die Daten der Tabelle zu löschen, sind fehlgeschlagen", locale = "de") })
    String DBSETUP_CLEAR_FAILED_TABLES = "DBSetup.clear.tables.failed";

    @I18NMessages( { @I18NMessage("{0}"), @I18NMessage(value = "{0}", locale = "de") })
    String DBSETUP_FATAL_SQL_EXCEPTION = "DBSetup.fata-sql-exception";

    @I18NMessages( { @I18NMessage("Created [{0}] rows in table [{1}]"),
        @I18NMessage(value = "[{0}] Zeilen in Tabelle [{1}] angelegt", locale = "de") })
    String DBSETUP_CREATED_ROWS = "DBSetup.created-rows";

    @I18NMessages( { @I18NMessage("Failed to create rows in table [{0}]"),
        @I18NMessage(value = "Konnte keine Zeilen in Tabelle [{0}] anlegen", locale = "de") })
    String DBSETUP_CREATED_ROWS_ERROR = "DBSetup.created-rows-error";

    @I18NMessages( { @I18NMessage("[{0}] tables created successfully"),
        @I18NMessage(value = "[{0}] Tabellen erfolgreich angelegt", locale = "de") })
    String DBSETUP_SETUP_TABLES = "DBSetup.setup.tables.created";

    @I18NMessages( { @I18NMessage("[{0}] indexes created successfully"),
        @I18NMessage(value = "[{0}] Indexe erfolgreich angelegt", locale = "de") })
    String DBSETUP_SETUP_INDEXES = "DBSetup.setup.indexes.created";

    @I18NMessages( { @I18NMessage("[{0}] views created successfully"),
        @I18NMessage(value = "[{0}] Views erfolgreich angelegt", locale = "de") })
    String DBSETUP_SETUP_VIEWS = "DBSetup.setup.views.created";

    @I18NMessages( { @I18NMessage("Getting ready to execute SQL: {0}"),
        @I18NMessage(value = "Werde nun das folgende SQL ausführen: {0}", locale = "de") })
    String DBSETUP_DO_SQL = "DBSetup.do-sql";

    @I18NMessages( {
        @I18NMessage("\\n\\\n{0}\\n\\\n"
            + "DBSetup -op=export|setup|clear|uninstall|uninstallsetup [-log=none|all|sql]\\n\\\n"
            + "        -jdbcurl=<db-url> [-jdbcuser=<username>] [-jdbcpassword=<password>]\\n\\\n"
            + "        -file=<dbsetup-xml-file>"),
        @I18NMessage(value = "\\n\\\n{0}\\n\\\n"
            + "DBSetup -op=export|setup|clear|uninstall|uninstallsetup [-log=none|all|sql]\\n\\\n"
            + "        -jdbcurl=<db-url> [-jdbcuser=<Benutzername>] [-jdbcpassword=<Passwort>]\\n\\\n"
            + "        -file=<dbsetup-xml-Datei>", locale = "de") })
    String DBSETUP_CMDLINE_USAGE = "DBSetup.cmdline.usage";

    @I18NMessages( {
        @I18NMessage("The operation is [{0}] - it must be one of:\\n\\\n"
            + "''export'', ''setup'', ''clear'', ''uninstall'' or ''uninstallsetup''"),
        @I18NMessage(value = "Die Operation ist [{0}] - sie muss eine der folgenden sein:\\n\\\n"
            + "''export'', ''setup'', ''clear'', ''uninstall'' oder ''uninstallsetup''", locale = "de") })
    String DBSETUP_CMDLINE_BAD_OP = "DBSetup.cmdline.bad-op";

    @I18NMessages( {
        @I18NMessage("The log argument is invalid [{0}] - it must be one of ''none'', ''all'' or ''sql''"),
        @I18NMessage(value = "Das Argument für das Logging ist ungültig [{0}]. Es muss eines der folgenden sein: ''none'', ''all'' or ''sql''", locale = "de") })
    String DBSETUP_CMDLINE_BAD_LOG = "DBSetup.cmdline.bad-log";

    @I18NMessages( { @I18NMessage("The path to the DBSetup XML file is invalid [{0}]"),
        @I18NMessage(value = "Der Pfad zur DBSetup-XML-Datei ist ungültig [{0}]", locale = "de") })
    String DBSETUP_CMDLINE_BAD_DBSETUPFILE = "DBSetup.cmdline.bad-dbsetup-file";

    @I18NMessages( { @I18NMessage("You must specify a JDBC URL (-jdbcurl)"),
        @I18NMessage(value = "Sie müssen eine JDBC URL angegben (-jdbcurl)", locale = "de") })
    String DBSETUP_CMDLINE_BAD_JDBCURL = "DBSetup.cmdline.bad-jdbcurl";

    @I18NMessages( { @I18NMessage("Invalid argument specified [{0}]"),
        @I18NMessage(value = "Ungültiges Argument: [{0}]", locale = "de") })
    String DBSETUP_CMDLINE_BAD_ARG = "DBSetup.cmdline.bad-arg";

    @I18NMessages( {
        @I18NMessage("DBSetup: provides a mechanism to create, clear and uninstall\\n\\\n"
            + "database schema and data as defined in an XML definition file"),
        @I18NMessage(value = "DBSetup: bietet einen Mechanismus um Datenbankschemata und -daten zu erzeugen,\\n\\\n"
            + "zu säubern und zu löschen.\\n\\\n" + "Dies wird in einer XMLDatei definiert", locale = "de") })
    String DBSETUP_CMDLINE_HELP = "DBSetup.cmdline.help";

    @I18NMessages( { @I18NMessage("DBSetup encountered an error! Cause: {0}"),
        @I18NMessage(value = "TODO", locale = "de") })
    String DBSETUP_CMDLINE_ERROR = "DBSetup.cmdline.error";

    @I18NMessages( {
        @I18NMessage("DBSetup failed to perform some SQL.  Please view the logs for more information."),
        @I18NMessage(value = "DBetup konnte einige SQL-Statements nicht erfolgreich ausführen. Bitte konsultieren Sie die Log-Datei für mehr Informationen", locale = "de") })
    String DBSETUP_CMDLINE_NOT_OK = "DBSetup.cmdline.not-ok";

    @I18NMessages( { @I18NMessage("DBSetup is done."), @I18NMessage(value = "DBSetup ist fertig", locale = "de") })
    String DBSETUP_CMDLINE_OK = "DBSetup.cmdline.ok";

    @I18NMessages( {
        @I18NMessage("Running DBSetup with the following options:\\n\\\n" + "Operation: {0}\\n\\\n"
            + "JDBC Logging?: {1}\\n\\\n" + "JDBC URL: {2}\\n\\\n" + "JDBC User: {3}\\n\\\n" + "DBSetup XML File: {4}"),
        @I18NMessage(value = "Führe DBSetup mit den folgenden Optionen aus::\\n\\\n" + "Operation: {0}\\n\\\n"
            + "JDBC Logging?: {1}\\n\\\n" + "JDBC URL: {2}\\n\\\n" + "JDBC Benutzer: {3}\\n\\\n"
            + "DBSetup-XML-Datei: {4}", locale = "de") })
    String DBSETUP_CMDLINE_OPTIONS = "DBSetup.cmdline.options";

    @I18NMessages( { @I18NMessage("Exporting definition of the found table [{0}]"),
        @I18NMessage(value = "Eportiere die Definition der gefundenen Tabelle [{0}]", locale = "de") })
    String DBSETUP_EXPORT_FOUND_TABLE = "DBSetup.export.found-table";

    @I18NMessages( { @I18NMessage("Exporting definition of the found column [{0}.{1}]"),
        @I18NMessage(value = "Exportiere die Definition der gefundenen Spalte [{0}.{1}]", locale = "de") })
    String DBSETUP_EXPORT_FOUND_COLUMN = "DBSetup.export.found-column";

    @I18NMessages( { @I18NMessage("[{0}] tables exported successfully"),
        @I18NMessage(value = "[{0}] Tabellen erfolgreich exportiert", locale = "de") })
    String DBSETUP_EXPORT_CREATED_TABLES = "DBSetup.export.tables-created";

    @I18NMessages( {
        @I18NMessage("\\n\\\nNOTE: You cannot use this generated XML file for input into DBSetup.\\n\\\n"
            + "This exported XML file is only for reference purposes; it is not\\n\\\n"
            + "generated in a way that can be used to recreate the DB.\\n\\\n"
            + "Use the backup/restore utilities provided by your database vendor\\n\\\n"
            + "to export your database for backup and recovery.\\n\\\n"
            + "\\n\\\nGenerated on: {0,date,long} {0,time,long}\\n\\\n"),
        @I18NMessage(value = "\\n\\\nACHTUNG: Sie können diese erzeugt XML-Datei nicht als Eingabe für DBSetup nutzen.\\n\\\n"
            + "Diese XML-Datei dient nur als Referenz. Sie ist in keiner Form,\\n\\\n"
            + "um die Datenbank zu restaurieren.\\n\\\n"
            + "Verwenden Sie die Backup-/Restore-Werkzeuge des Datenbankherstellers,\\n\\\n"
            + "um Backups zu erzeugen und einzuspielen.\\n\\\n"
            + "\\n\\\nErzeugt am: {0,date,long} um {0,time,long}\\n\\\n", locale = "de") })
    String DBSETUP_EXPORT_WARNING_NOTICE = "DBSetup.export.warning-notice";

}