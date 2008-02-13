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
package org.rhq.core.db.ant;

import mazz.i18n.annotation.I18NMessage;
import mazz.i18n.annotation.I18NMessages;
import mazz.i18n.annotation.I18NResourceBundle;

/**
 * I18N messages for the ant tools.
 *
 * @author John Mazzitelli
 *
 */
@I18NResourceBundle(baseName = "dbutils-ant-messages", defaultLocale = "en")
public interface DbAntI18NResourceKeys {
    @I18NMessages( { @I18NMessage("There was no version specified for a schemaSpec"),
        @I18NMessage(value = "Es wurde für 'schemaSpec' keine Version angegeben", locale = "de") })
    String MISSING_SCHEMA_SPEC_VERSION = "SchemaSpec.missing-version";

    @I18NMessages( { @I18NMessage("Cannot add the task [{0}] - it is not of type [{1}]"),
        @I18NMessage(value = "Kann die Aufgabe [{0}] nicht hinzufügen - sie ist nicht vom Typ [{1}]", locale = "de") })
    String CANNOT_ADD_SCHEMA_SPEC_TASK = "SchemaSpec.cannot-add-schema-spec-task";

    @I18NMessages( { @I18NMessage("Cannot compare a non-SchemaSpec object [{0}]"),
        @I18NMessage(value = "Kann kein nicht-SchemaSpec-Objekt vergleichen [{0}]", locale = "de") })
    String CANNOT_COMPARE_NON_SCHEMA_SPEC = "SchemaSpec.cannot-compare-non-schema-spec";

    @I18NMessages( { @I18NMessage("Executing the task [{0}] in schema spec version [{1}]"),
        @I18NMessage(value = "Führe die Aufgabe [{0}] in Schema spec Version [{1}] aus", locale = "de") })
    String EXECUTING_SCHEMA_SPEC_TASK = "SchemaSpec.executing-schema-spec-task";

    @I18NMessages( {
        @I18NMessage("Error executing the task [{0}] in schema spec version [{1}]. Cause: {2}"),
        @I18NMessage(value = "Fehler beim Ausführen der Aufgabe [{0}] in Schema-Spec-Version [{1}]. Grund: {2}", locale = "de") })
    String ERROR_EXECUTING_SCHEMA_SPEC_TASK = "SchemaSpec.error-executing-schema-spec-task";

    @I18NMessages( {
        @I18NMessage("[{0}] is generic type name that has an unknown or unsupported JDBC SQL data type"),
        @I18NMessage(value = "[{0}] ist ein generischer Typname der einen unbekannten oder nicht unterstützten JDBC SQL Datentyp hat", locale = "de") })
    String INVALID_JDBC_SQL_TYPE = "SchemaSpecTask.invalid-jdbc-sql-type";

    @I18NMessages( {
        @I18NMessage("There is no database specific mapping of the generic type [{0}] for the database [{1}]"),
        @I18NMessage(value = "Es gibt kein datenbankspezifisches Mapping des generischen Typs [{0}] für die Datenbank [{1}]", locale = "de") })
    String NO_DB_SPECIFIC_TYPE_MAPPING = "SchemaSpecTask.no-db-specific-type-mapping";

    @I18NMessages( {
        @I18NMessage("The schema spec task [{0}] has an unsupported attribute [{1}]"),
        @I18NMessage(value = "Die Schema-Spec-Aufgabe [{0}] hat ein nicht unterstütztes Attribut [{1}]", locale = "de") })
    String SCHEMA_SPEC_TASK_UNSUPPORTED_ATTRIB = "SchemaSpecTask.unsupported-attrib";

    @I18NMessages( { @I18NMessage("The schema spec task [{0}] is missing the attribute [{1}]"),
        @I18NMessage(value = "Der Schema-Spec-Aufgabe [{0}] fehlt das Attribut [{1}]", locale = "de") })
    String SCHEMA_SPEC_TASK_MISSING_ATTRIB = "SchemaSpecTask.missing-attrib";

    @I18NMessages( { @I18NMessage("The schema spec task [{0}] is missing the child element [{1}]"),
        @I18NMessage(value = "Der Schema-Spec-Aufgabe [{0}] fehlt das Kindelement [{1}]", locale = "de") })
    String SCHEMA_SPEC_TASK_MISSING_CHILD_ELEMENT = "SchemaSpecTask.missing-child-element";

    @I18NMessages( {
        @I18NMessage("The schema spec task [{0}] had an invalid attribute [{1}] with value [{2}]"),
        @I18NMessage(value = "Die Schema-Spec-Aufgabe [{0}] hat ein ungültiges Attribut [{1}] mit Wert [{2}]", locale = "de") })
    String SCHEMA_SPEC_TASK_INVALID_ATTRIB = "SchemaSpecTask.invalid-attrib";

    @I18NMessages( { @I18NMessage("The schema spec task [{0}] has encountered an error. Cause: {1}"),
        @I18NMessage(value = "Bei der Schema-Spec-Aufgabe [{0}] ist ein Fehler aufgetreten. Grund: {1}", locale = "de") })
    String SCHEMA_SPEC_TASK_FAILURE = "SchemaSpecTask.failure";

    @I18NMessages( {
        @I18NMessage("Cannot update column [{0}] - it does not exist in the table [{1}]"),
        @I18NMessage(value = "Kann die Spalte [{0}] nicht aktualisieren, da sie in der Tabelle [{1}] nicht existiert", locale = "de") })
    String ERROR_UPDATING_NONEXISTING_COLUMN = "SchemaSpecTask.updatecolumn.error-updating-nonexisting-column";

    @I18NMessages( {
        @I18NMessage("Error updating column [{0}] in the table [{1}]. Cause: {2}"),
        @I18NMessage(value = "Fehler bei Aktualisieren der Spalte [{0}] in der Tabelle [{1}]. Grund: {2}", locale = "de") })
    String ERROR_UPDATING_COLUMN = "SchemaSpecTask.updatecolumn.error-updating-column";

    @I18NMessages( { @I18NMessage("Updating column [{0}] in the table [{1}] with modify command of [{2}]"),
        @I18NMessage(value = "Aktualisiere Spalte [{0}] in der Tabelle [{1}] mit dem Kommando [{2}]", locale = "de") })
    String UPDATING_COLUMN = "SchemaSpecTask.updatecolumn.updating-column";

    @I18NMessages( { @I18NMessage("Error deleting column [{0}] in the table [{1}]. Cause: {2}"),
        @I18NMessage(value = "Fehler beim L�schen der Spalte [{0}] in der Tabelle [{1}]. Grund: {2}", locale = "de") })
    String ERROR_DELETING_COLUMN = "SchemaSpecTask.deletecolumn.error-deleting-column";

    @I18NMessages( { @I18NMessage("Deleting column [{0}] in the table [{1}]"),
        @I18NMessage(value = "L�schen Spalte [{0}] in der Tabelle [{1}]", locale = "de") })
    String DELETING_COLUMN = "SchemaSpecTask.deletecolumn.deleting-column";

    @I18NMessages( {
        @I18NMessage("Will not attempt to drop table [{0}] because it does not exist"),
        @I18NMessage(value = "Werde nicht versuchen, die Tabelle [{0}] zu löschen, da sie nicht existiert", locale = "de") })
    String DROP_TABLE_TABLE_DOES_NOT_EXIST = "SchemaSpecTask.droptable.table-does-not-exist";

    @I18NMessages( { @I18NMessage("Dropping table [{0}]"),
        @I18NMessage(value = "Tabelle [{0}] wird gelöscht", locale = "de") })
    String DROP_TABLE_EXECUTING = "SchemaSpecTask.droptable.executing";

    @I18NMessages( { @I18NMessage("Error occurred while attempting to drop table [{0}]. Cause: {1}"),
        @I18NMessage(value = "Beim Löschen der Tabelle [{0}] ist ein Fehler aufgetreten. Grund: {1}", locale = "de") })
    String DROP_TABLE_ERROR = "SchemaSpecTask.droptable.error";

    @I18NMessages( {
        @I18NMessage("Creating sequence with the name [{0}] having an initial value of [{1}] and an increment of [{2}]"),
        @I18NMessage(value = "Erstellen der Sequenz mit Namen [{0}], die einen Initialwert von {[1}] und ein Inkrement von [{2}] hat", locale = "de") })
    String CREATE_SEQUENCE_EXECUTING = "SchemaSpecTask.createsequence.executing";

    @I18NMessages( { @I18NMessage("Dropping sequence with the name [{0}]"),
        @I18NMessage(value = "Sequenz mit dem Namem [{0}] wird gelöscht", locale = "de") })
    String DROP_SEQUENCE_EXECUTING = "SchemaSpecTask.dropsequence.executing";

    @I18NMessages( { @I18NMessage("Inserting into the table [{0}] using the insert command of [{1}]"),
        @I18NMessage(value = "Einfügen in die Tabelle [{0}] mit dem Kommando [{1}]", locale = "de") })
    String INSERT_EXECUTING = "SchemaSpecTask.insert.executing";

    @I18NMessages( {
        @I18NMessage("Could not insert into the table [{0}] - the data already exists. This task was told to ignore such errors. The insert will be rolled back"),
        @I18NMessage(value = "Konnte nicht in die Tabelle [{0}] einfügen - Das Datum exisitert bereits. Die Aufgabe ignoriert solche Fehler. Das Einfügen wird zurückgerollt", locale = "de") })
    String INSERT_IGNORE_DUPLICATE = "SchemaSpecTask.insert.ignore-dup";

    @I18NMessages( {
        @I18NMessage("Could not rollback after the attempt to insert duplicate data. Cause: {0}"),
        @I18NMessage(value = "Konnte nach dem Versuch, doppelte Daten einzufügen nicht zurückrollen. Grund: {0}", locale = "de") })
    String INSERT_ROLLBACK_ERROR = "SchemaSpecTask.insert.rollback-error";

    @I18NMessages( {
        @I18NMessage("The task was to be executed on a [{0}] database, but this is a [{1}] database; task will be skipped"),
        @I18NMessage(value = "Die Aufgabe sollte auf einer [{0}] Datenbank ausgeführt werden. Dies ist aber eine [{1}] Datenbank. Die Aufgabe wird übersprungen", locale = "de") })
    String SCHEMA_SPEC_TASK_VENDOR_MISMATCH = "SchemaSpecTask.vendor-mismatch";

    @I18NMessages( {
        @I18NMessage("The task was to be executed on a [{0}] database of version [{1}], but this database is at version [{2}]; task will be skipped"),
        @I18NMessage(value = "Die Aufgabe sollte auf einer [{0}] Datenbank mit Version [{1}] ausgeführt werden. Die Version dieser Datenbank ist allerdings [{2}]. Die Aufgabe wird übersprungen", locale = "de") })
    String SCHEMA_SPEC_TASK_VERSION_MISMATCH = "SchemaSpecTask.version-mismatch";

    @I18NMessages( { @I18NMessage("Executing direct SQL. Description=[{0}] : SQL=[{1}]"),
        @I18NMessage(value = "Führe direktes SQL aus. Beschreibung=[{0}] : SQL=[{1}]s", locale = "de") })
    String DIRECTSQL_EXECUTING = "SchemaSpecTask.directsql.executing";

    @I18NMessages( {
        @I18NMessage("The task [{0}] defines a target DB version [{1}] without specifying a database vendor"),
        @I18NMessage(value = "Die Aufgabe [{0}] definiert die Zieldatenbankversion [{1}] ohne Angabe eines Herstellers", locale = "de") })
    String SCHEMA_SPEC_TASK_TARGET_VERSION_WITHOUT_VENDOR = "SchemaSpecTask.invalid-target-db";

    @I18NMessages( {
        @I18NMessage("Cannot update data in the column [{0}] - it does not exist in the table [{1}]"),
        @I18NMessage(value = "Kann die Daten in Spalte [{0}] nicht aktualisieren - sie existiert nicht in Tabelle [{1}]", locale = "de") })
    String UPDATE_COLUMN_DOES_NOT_EXIST = "SchemaSpecTask.update.column-does-not-exist";

    @I18NMessages( {
        @I18NMessage("Updating data of type [{0}] (JDBC type=[{1}]) in table.column of [{2}.{3}]. The new value will be [{4}]. Optional where clause=[{5}]"),
        @I18NMessage(value = "Aktualisieren der Date mit Typ [{0}] (JDBC-Typ=[{1}]) in Tabelle.Spalte [{2}.{3}]. Der neue Wert wird [{4}]. Optionale WHERE-Bedingung=[{5}]", locale = "de") })
    String UPDATE_EXECUTING = "SchemaSpecTask.update.executing";

    @I18NMessages( {
        @I18NMessage("Could not update data in table.column of [{0}.{1}]. Cause: {2}"),
        @I18NMessage(value = "Kann die Daten in Tabelle.Spalte [{0}.{1}] nicht akutualisieren. Grund: {2}", locale = "de") })
    String UPDATE_ERROR = "SchemaSpecTask.update.error";

    @I18NMessages( {
        @I18NMessage("There are multiple initializers defined. You are allowed to define at most one initializer"),
        @I18NMessage(value = "Es sind mehrere Initialisierungen definiert. Sie dürfen maximal eine angeben", locale = "de") })
    String MULTIPLE_INITIALIZERS_NOT_ALLOWED = "SchemaSpecTask.multiple-initializers-not-allowed";

    @I18NMessages( {
        @I18NMessage("There are multiple foreign key constraints defined. You are allowed to define at most one foreign key constraint"),
        @I18NMessage(value = "Es sind mehrere Fremdschlüsselbedingungen definiert. Sie dürfen maximal eine definieren", locale = "de") })
    String MULTIPLE_FOREIGN_KEYS_NOT_ALLOWED = "SchemaSpecTask.multiple-foreign-keys-not-allowed";

    @I18NMessages( {
        @I18NMessage("Adding new column: table=[{0}], column=[{1}], columnType=[{2}], precision=[{3}]"),
        @I18NMessage(value = "Hinzufügen einer neuen Spalte: Tablle=[{0}], Spalte=[{1}], Spaltentyp=[{2}], Genauigkeit=[{3}]", locale = "de") })
    String ADD_COLUMN_EXECUTING = "SchemaSpecTask.addcolumn.executing";

    @I18NMessages( { @I18NMessage("Failed to add the new column. Cause: {0}"),
        @I18NMessage(value = "Konnte die neue Spalte nicht hinzufügen. Grund: {0}", locale = "de") })
    String ADD_COLUMN_ERROR = "SchemaSpecTask.addcolumn.error";

    @I18NMessages( { @I18NMessage("Cannot alter column - it does not exist: [{0}.{1}]"),
        @I18NMessage(value = "Kann die Spalte nicht verändern - sie existiert nicht: [{0}.{1}]", locale = "de") })
    String ALTER_COLUMN_DOES_NOT_EXIST = "SchemaSpecTask.altercolumn.column-does-not-exist";

    @I18NMessages( { @I18NMessage("Failed to alter column. Cause: {0}"),
        @I18NMessage(value = "Konnte die Spalte nicht verändern. Grund: {0}", locale = "de") })
    String ALTER_COLUMN_ERROR = "SchemaSpecTask.altercolumn.error";

    @I18NMessages( { @I18NMessage("No 'jdbcUrl' attribute specified."),
        @I18NMessage(value = "Es ist kein 'jdbcUrl' Attribut angegeben", locale = "de") })
    String DBUPGRADE_NO_JDBC_URL = "DBUpgrader.no-jdbc-url";

    @I18NMessages( { @I18NMessage("Failed to load type mapping from file [{0}]. Cause: {1}"),
        @I18NMessage(value = "Kann das Typen-Mapping nicht aus der Datei [{0}] laden. Grund: {1}", locale = "de") })
    String DBUPGRADE_TYPE_MAP_FILE_ERROR = "DBUpgrader.type-map-file-error";

    @I18NMessages( {
        @I18NMessage("No 'targetSchemaVersion' attribute was specified. Do not know what version to ugprade to"),
        @I18NMessage(value = "Es wurde keine ‘targetSchemaVersion‘ angegeben. Die Version auf die aktualisiert werden soll ist unbekannt", locale = "de") })
    String DBUPGRADE_NO_VERSION = "DBUpgrader.no-version";

    @I18NMessages( { @I18NMessage("Invalid 'targetSchemaVersion' attribute was specified. Cause: {0}"),
        @I18NMessage(value = "Eine ungültige 'targetSchemaVersion' wurde angegeben. Grund: {0}", locale = "de") })
    String DBUPGRADE_INVALID_VERSION = "DBUpgrader.invalid-version";

    @I18NMessages( { @I18NMessage("Error updating schema version to [{0}]. Cause: {1}"),
        @I18NMessage(value = "Kann die Schemavrsion nicht auf [{0}] erhöhen. Grund: {1}", locale = "de") })
    String DBUPGRADE_ERROR_UPDATING_VERSION = "DBUpgrader.error-updating-version";

    @I18NMessages( { @I18NMessage("Error loading the starting schema version string. Cause: {0}"),
        @I18NMessage(value = "Fehler beim Laden des Versionsstrings. Grund… {0}", locale = "de") })
    String DBUPGRADE_ERROR_LOADING_START_VERSION = "DBUpgrader.error-loading-start-version";

    @I18NMessages( { @I18NMessage("Cannot find the starting schema version string in [{0}.{1}.{2}]"),
        @I18NMessage(value = "Kann die zugrundegelete Schemaversion nicht in [{0}.{1}.{2}] finden", locale = "de") })
    String DBUPGRADE_ERROR_MISSING_VERSION = "DBUpgrader.error-missing-version";

    @I18NMessages( { @I18NMessage("Found multiple starting schema version strings in [{0}.{1}.{2}]"),
        @I18NMessage(value = "Habe mehrere zugrundelegende Schemaversionen in [{0}.{1}.{2}] gefunden", locale = "de") })
    String DBUPGRADE_ERROR_DUPLICATE_VERSION = "DBUpgrader.error-duplicate-version";

    @I18NMessages( { @I18NMessage("Found multiple schema specs defined with the same version [{0}]"),
        @I18NMessage(value = "Habe mehrere Schemas mit der selben Versin [{0}] gefunden", locale = "de") })
    String DBUPGRADE_ERROR_DUPLICATE_SCHEMA_SPECS = "DBUpgrader.error-duplicate-schema-specs";

    @I18NMessages( { @I18NMessage("Found schema spec defined out of order [{0}]"), // TODO broken or wrong ordering?
        @I18NMessage(value = "Gefundene Schema-Spec ist in falscher Reihenfolge [{0}] ", locale = "de") })
    String DBUPGRADE_ERROR_SCHEMA_SPECS_OUT_OF_ORDER = "DBUpgrader.error-schema-specs-out-of-order";

    @I18NMessages( { @I18NMessage("Database schema is in an inconsistent state: version=[{0}]"),
        @I18NMessage(value = "Datenbankschema ist in einem inkonsistenten Zustang: Version=[{0}]", locale = "de") })
    String DBUPGRADE_ERROR_INCONSISTENT_STATE = "DBUpgrader.inconsistent-state";

    @I18NMessages( { @I18NMessage("Starting database schema upgrade: [{0}] -> [{1}]"),
        @I18NMessage(value = "Beginne mit der Aktualisierung des Datenbankschemas: [{0}] -> [{1}]", locale = "de") })
    String DBUPGRADE_STARTING = "DBUpgrader.starting";

    @I18NMessages( { @I18NMessage("Upgrading: [{0}] -> [{1}]"),
        @I18NMessage(value = "Aktualisiere: [{0}] -> [{1}]", locale = "de") })
    String DBUPGRADE_UPGRADE_STEP = "DBUpgrader.upgrade-step";

    @I18NMessages( { @I18NMessage("Finished upgrade: [{0}] -> [{1}] OK"),
        @I18NMessage(value = "Aktualisierung [{0}] -> [{1}] erfolgreich beendet ", locale = "de") })
    String DBUPGRADE_UPGRADE_STEP_DONE = "DBUpgrader.upgrade-step-done";

    @I18NMessages( { @I18NMessage("Cannot downgrade schema from [{0}] to [{1}]"),
        @I18NMessage(value = "Kann das Schema nicht von [{0}] auf [{1}] downgraden", locale = "de") // TODO better word for downgrade
    })
    String DBUPGRADE_ERROR_DOWNGRADING = "DBUpgrader.error-downgrading";

    @I18NMessages( {
        @I18NMessage("Failed to upgrade - error in spec version [{0}]. Cause: {1}"),
        @I18NMessage(value = "Konnte nicht aktualisieren - Fehler in der Spec-Version [{0}]. Grund: {1}", locale = "de") })
    String DBUPGRADE_UPGRADE_STEP_ERROR = "DBUpgrader.upgrade-step-error";

    @I18NMessages( { @I18NMessage("DATABASE SUCCESSFULLY UPGRADED TO [{0}]"),
        @I18NMessage(value = "DATENBANK ERFOLGREICH auf [{0}] AKTUALISIERT", locale = "de") })
    String DBUPGRADE_SUCCESS = "DBUpgrader.success";

    @I18NMessages( { @I18NMessage("At least one file must be provided."),
        @I18NMessage(value = "Mindestens eine Datei muss angegeben werden", locale = "de") })
    String BASEFILESET_NEED_A_FILE = "BaseFileSetTask.need-a-file";

    @I18NMessages( { @I18NMessage("File [{0}] does not exist."),
        @I18NMessage(value = "Die Datei [{0}] existiert nicht", locale = "de") })
    String BASEFILESET_FILE_DOES_NOT_EXIST = "BaseFileSetTask.file-does-not-exist";

    @I18NMessages( { @I18NMessage("The task [{0}] is missing the required attribute [{1}]"),
        @I18NMessage(value = "Der Aufgabe [{0}] fehlt das Pflichtattribut [{1}]", locale = "de") })
    String TASK_MISSING_ATTRIB = "Task.missing-attrib";

    @I18NMessages( { @I18NMessage("Conditions on indexes are not supported for database {0}"),
        @I18NMessage(value = "Die Datenbank {0} unterstützt keine Bedingungen für Indexe", locale = "de") })
    String INDEX_CONDITION_NOT_SUPPORTED = "DBUpgrader.index-condition-not-supported";
}