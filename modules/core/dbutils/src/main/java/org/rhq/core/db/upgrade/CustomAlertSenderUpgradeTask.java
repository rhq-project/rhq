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
package org.rhq.core.db.upgrade;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.OracleDatabaseType;
import org.rhq.core.db.PostgresqlDatabaseType;

/**
 * The introduction of custom alert senders brought with it the denormalization of the AlertNotification schema.
 * Instead of the AlertNotification entity storing the notification-related data itself (through referential integrity)
 * it has been subsumed inside of configuration objects, which are then associated back to the entity.
 *
 * Each custom alert sender has full control over the structure within that configuration object.  This task represents
 * the work necessary to translate the first-class notification data (previously stored in the rhq_alert_notification
 * table itself) into appropriate configuration objects to be used by the custom senders that will be shipped with the
 * product by default.
 *
 * In particular, this task handles the upgrade tasks for alert notifications setup against the following types of data:
 *
 * <ul>
 *   <li>RHQ Subjects</li>
 *   <li>RHQ Roles</li>
 *   <li>Direct Email Addresses</li>
 *   <li>SNMP Trap Receivers</li>
 *   <li>Resource Operations</li>
 * </ul>
 *
 * @author Joseph Marques
 */
public class CustomAlertSenderUpgradeTask implements DatabaseUpgradeTask {

    private DatabaseType databaseType;
    private Connection connection;

    private final long NOW = System.currentTimeMillis();

    public void execute(DatabaseType databaseType, Connection connection) throws SQLException {
        this.databaseType = databaseType;
        this.connection = connection;

        // upgrade the notification audit trail
        upgradeSubjectNotificationLogs();
        upgradeRoleNotificationLogs();
        upgradeEmailNotificationLogs();
        upgradeOperationNotificationLogs();

        // upgrade the notification rules
        upgradeSubjectNotifications();
        upgradeRoleNotifications();
        upgradeEmailNotifications();
        upgradeSNMPNotifications();
        upgradeOperationNotifications();

        upgradeSNMPPreferences();
    }

    private void upgradeSubjectNotificationLogs() throws SQLException {
        /*
         * alert.alertNotificationLog.sender = "System Users"
         * alert.alertNotificationLog.result_state = "UNKNOWN" // success-failure is unknown for existing alerts
         * alert.alertNotificationLog.message = "Sending to subjects: [<alert.alertNotificationLog.subjects>]"
         */
        String field = "notif.subjects";
        String message = concat("'Sending to subjects: '", field);
        String insertSQL = getNotificationLogConversionSQL("'System Users'", "'UNKNOWN'", message, field);
        System.out.println("Executing: " + insertSQL);
        databaseType.executeSql(connection, insertSQL);
    }

    private void upgradeRoleNotificationLogs() throws SQLException {
        /*
         * alert.alertNotificationLog.sender = "System Roles"
         * alert.alertNotificationLog.result_state = "UNKNOWN" // success-failure is unknown for existing alerts
         * alert.alertNotificationLog.message = "Sending to roles: [<alert.alertNotificationLog.roles>]"
         */
        String field = "notif.roles";
        String message = concat("'Sending to roles: '", field);
        String insertSQL = getNotificationLogConversionSQL("'System Roles'", "'UNKNOWN'", message, field);
        System.out.println("Executing: " + insertSQL);
        databaseType.executeSql(connection, insertSQL);
    }

    private void upgradeEmailNotificationLogs() throws SQLException {
        /*
         * alert.alertNotificationLog.sender = "Direct Emails"
         * alert.alertNotificationLog.result_state = "UNKNOWN" // success-failure is unknown for existing alerts
         * alert.alertNotificationLog.message = "Sending to subjects: [<alert.alertNotificationLog.emails>]"
         */
        String field = "notif.emails";
        String message = concat("'Sending to addresses: '", field);
        String insertSQL = getNotificationLogConversionSQL("'Direct Emails'", "'UNKNOWN'", message, field);
        System.out.println("Executing: " + insertSQL);
        databaseType.executeSql(connection, insertSQL);
    }

    private String getNotificationLogConversionSQL(String sender, String resultState, String message,
        String notNullField) {
        if (databaseType instanceof PostgresqlDatabaseType) {
            return "INSERT INTO rhq_alert_notif_log ( id, alert_id, sender, result_state, message )" //
                + "      SELECT nextval('RHQ_ALERT_NOTIF_LOG_ID_SEQ'), " //
                + "             notif.alert_id AS notifAlertId, " //
                + "             " + sender + " AS notifSender, "//
                + "             " + resultState + " AS notifResultState, " //
                + "             " + message + " AS notifMessage "//
                + "        FROM rhq_alert_notif_log notif " //
                + "       WHERE " + notNullField + " IS NOT NULL";
        } else if (databaseType instanceof OracleDatabaseType) {
            return "INSERT INTO rhq_alert_notif_log ( id, alert_id, sender, result_state, message )" //
                + "      SELECT RHQ_ALERT_NOTIF_LOG_ID_SEQ.nextval, " //
                + "             notifAlertId, notifSender, notifResultState, notifMessage "
                + "        FROM ( SELECT notif.alert_id AS notifAlertId, " //
                + "                      " + sender + " AS notifSender, "//
                + "                      " + resultState + " AS notifResultState, " //
                + "                      " + message + " AS notifMessage "//
                + "                 FROM rhq_alert_notif_log notif " //
                + "                WHERE " + notNullField + " IS NOT NULL )";
        } else {
            throw new IllegalStateException(this.getClass().getSimpleName() + " does not support upgrades for "
                + databaseType.getName());
        }
    }

    private void upgradeOperationNotificationLogs() throws SQLException {
        /*
         * alert.alertNotificationLog.sender = "Resource Operations"
         * alert.alertNotificationLog.result_state = "SUCCESS"
         * alert.alertNotificationLog.message = "Executed '<alert.triggeredOperation>' on this resource"
         */
        String field = "alert.triggered_operation";
        String message = concat("'Executed \"'", field, "'\" on this resource'");
        String insertSQL = getNotificationLogInsertionSQL("'Resource Operations'", "'SUCCESS'", message, field);
        System.out.println("Executing: " + insertSQL);
        databaseType.executeSql(connection, insertSQL);
    }

    private String getNotificationLogInsertionSQL(String sender, String resultState, String message, String notNullField) {
        if (databaseType instanceof PostgresqlDatabaseType) {
            return "INSERT INTO rhq_alert_notif_log ( id, alert_id, sender, result_state, message )" //
                + "      SELECT nextval('RHQ_ALERT_NOTIF_LOG_ID_SEQ'), " //
                + "             alert.id AS notifAlertId, " //
                + "             " + sender + " AS notifSender, "//
                + "             " + resultState + " AS notifResultState, " //
                + "             " + message + " AS notifMessage "//
                + "        FROM rhq_alert alert " //
                + "       WHERE " + notNullField + " IS NOT NULL";
        } else if (databaseType instanceof OracleDatabaseType) {
            return "INSERT INTO rhq_alert_notif_log ( id, alert_id, sender, result_state, message )" //
                + "      SELECT RHQ_ALERT_NOTIF_LOG_ID_SEQ.nextval, " //
                + "             notifAlertId, notifSender, notifResultState, notifMessage "
                + "        FROM ( SELECT alert.id AS notifAlertId, " //
                + "                      " + sender + " AS notifSender, "//
                + "                      " + resultState + " AS notifResultState, " //
                + "                      " + message + " AS notifMessage "//
                + "                 FROM rhq_alert alert " //
                + "                WHERE " + notNullField + " IS NOT NULL )";
        } else {
            throw new IllegalStateException(this.getClass().getSimpleName() + " does not support upgrades for "
                + databaseType.getName());
        }
    }

    private String concat(String... elements) {
        StringBuilder builder = new StringBuilder();
        if (databaseType instanceof PostgresqlDatabaseType || databaseType instanceof OracleDatabaseType) {
            boolean first = true;
            for (String next : elements) {
                if (first) {
                    first = false;
                } else {
                    builder.append("||");
                }
                builder.append(next);
            }
        } else {
            throw new IllegalStateException(this.getClass().getSimpleName() + " does not support upgrades for "
                + databaseType.getName());
        }
        return builder.toString();
    }

    private void upgradeSubjectNotifications() throws SQLException {
        String dataMapSQL = "" //
            + "  SELECT notif.alert_definition_id, notif.subject_id "//
            + "    FROM rhq_alert_notification notif "//
            + "   WHERE notif.notification_type = 'SUBJECT' "//
            + "ORDER BY notif.alert_definition_id";
        List<Object[]> data = databaseType.executeSelectSql(connection, dataMapSQL);

        String propertyName = "subjectId";
        String senderName = "System Users";

        persist(data, propertyName, senderName, "|", true);
    }

    private void upgradeRoleNotifications() throws SQLException {
        String dataMapSQL = "" //
            + "  SELECT notif.alert_definition_id, notif.role_id "//
            + "    FROM rhq_alert_notification notif "//
            + "   WHERE notif.notification_type = 'ROLE' "//
            + "ORDER BY notif.alert_definition_id";

        List<Object[]> data = databaseType.executeSelectSql(connection, dataMapSQL);

        String propertyName = "roleId";
        String senderName = "System Roles";

        persist(data, propertyName, senderName, "|", true);
    }

    private void upgradeEmailNotifications() throws SQLException {
        String dataMapSQL = "" //
            + "  SELECT notif.alert_definition_id, notif.email_address "//
            + "    FROM rhq_alert_notification notif "//
            + "   WHERE notif.notification_type = 'EMAIL' "//
            + "ORDER BY notif.alert_definition_id";

        List<Object[]> data = databaseType.executeSelectSql(connection, dataMapSQL);

        String propertyName = "emailAddress";
        String senderName = "Direct Emails";

        persist(data, propertyName, senderName, ",", false);
    }

    private void upgradeSNMPNotifications() throws SQLException {
        String dataMapSQL = "" //
            + "  SELECT notif.alert_definition_id, notif.snmp_host, notif.snmp_port, notif.snmp_oid "//
            + "    FROM rhq_alert_notification notif "//
            + "   WHERE notif.notification_type = 'SNMP' "//
            + "ORDER BY notif.alert_definition_id";

        List<Object[]> data = databaseType.executeSelectSql(connection, dataMapSQL);

        for (Object[] next : data) {
            int alertDefinitionId = ((Number) next[0]).intValue();
            String host = (String) next[1];
            String port = ((Number) next[2]).toString();
            String oid = (String) next[3];

            // buffer will be 0 the very first time, since definitionId is initially -1
            int configId = persistConfiguration("host", host, "port", port, "oid", oid);
            persistNotification(alertDefinitionId, configId, "SNMP Traps");
        }
    }

    private void upgradeOperationNotifications() throws SQLException {
        String dataMapSQL = "" //
            + "  SELECT def.id, def.operation_def_id" //
            + "    FROM rhq_alert_definition def" //
            + "   WHERE def.operation_def_id IS NOT NULL"; // not all alert definitions have operation notifications

        List<Object[]> data = databaseType.executeSelectSql(connection, dataMapSQL);

        for (Object[] next : data) {
            int alertDefinitionId = ((Number) next[0]).intValue();
            String operationDefinitionId = ((Number) next[1]).toString();

            // buffer will be 0 the very first time, since definitionId is initially -1
            int configId = persistConfiguration("operation-definition-id", operationDefinitionId, "selection-mode",
                "SELF");
            persistNotification(alertDefinitionId, configId, "Resource Operations");
        }
    }

    /**
     * Copy the system wide snmp preferences. This happens only on
     * a fresh migration from pre RHQ3 and only if the user has
     * actually changed the provided defaults.
     */
    private void upgradeSNMPPreferences() throws SQLException {
        String oldPrefsSQL = "" //
            + " SELECT property_key,property_value"
            + " FROM RHQ_SYSTEM_CONFIG"
            + " WHERE property_key LIKE 'SNMP%'";

        String[] keyToProp = { //
                "SNMP_AGENT_ADDRESS","agentAddress", //
                "SNMP_AUTH_PASSPHRASE","authPassphrase", //
                "SNMP_AUTH_PROTOCOL","authProtocol", //
                "SNMP_COMMUNITY","community",//
                "SNMP_CONTEXT_NAME","targetContext",//
                "SNMP_ENGINE_ID","engineId",//
                "SNMP_ENTERPRISE_OID","enterpriseOid",//
                "SNMP_GENERIC_ID","genericId",//
                "SNMP_PRIVACY_PROTOCOL","privacyProtocol",//
                "SNMP_PRIV_PASSPHRASE","privacyPassphrase",//
                "SNMP_SECURITY_NAME","securityName",//
                "SNMP_SPECIFIC_ID","specificId",//
                "SNMP_TRAP_OID","trapOid",//
                "SNMP_VERSION","snmpVersion"
        };

        /*
         * Check if there is already a config present.
         * Only run the copy on a fresh upgrade from a pre RHQ 3 version.
         */
        int configId = getPluginConfigurationId("alert-snmp");
        if (configId!=0) {
            System.out.println("Already found a snmp configuration, not copying the old one over.");
            return;
        }

        // Get the properties from the database
        List<Object[]> data = databaseType.executeSelectSql(connection, oldPrefsSQL);

        // check if the user actually did set up the snmp settings in the older version
        // If not, don't bother, as the plugin will set up its defaults later on.
        for (Object[] next : data) {
            String key = (String) next[0];
            if (key.equals("SNMP_VERSION")) {
                String val = (String) next[1];
                if (val==null || val.equals("")) {
                    System.out.println("No SNMP config set in old db version, so not copying");
                    return;
                }
            }
        }

        // We have work to do ...
        configId = databaseType.getNextSequenceValue(connection, "rhq_config", "id");
        String insertConfigSQL = getInsertConfigSQL(configId);
        databaseType.executeSql(connection, insertConfigSQL);

        for (Object[] next : data) {
            // find property
            String propertyName = null;
            for (int i = 0 ; i< keyToProp.length ; i++) {
                if (keyToProp[i].equals(next[0])) {
                    propertyName = keyToProp[i+1];
                    break;
                }
            }
            if (propertyName==null) {
                System.err.println("Input property " + next[0] + " is not encoded");
                System.err.println("Not copying the SNMP preferences");

            }

            String propertyValue = (String) next[1];

            int propertyId = databaseType.getNextSequenceValue(connection, "rhq_config_property", "id");
            String insertPropertySQL = getInsertPropertySQL(propertyId, configId, propertyName, propertyValue);
            databaseType.executeSql(connection, insertPropertySQL);
        }
    }

    int getPluginConfigurationId(String pluginName) throws SQLException {
        String getConfigIdSQL = "" //
            + " SELECT plugin_config_id " //
            + " FROM rhq_plugin"  //
            + " WHERE name = '" + pluginName + "'";
        List<Object[]> data = databaseType.executeSelectSql(connection, getConfigIdSQL);

        if (data==null || data.size()==0)
            return 0;
        Object[] idos = data.get(0);
        return (Integer)idos[0];

    }


    private void persist(List<Object[]> data, String propertyName, String sender, String delimiter,
        boolean bufferWithDelimiter) throws SQLException {
        int definitionId = -1;
        StringBuilder buffer = new StringBuilder();
        for (Object[] next : data) {
            int nextDefinitionId = ((Number) next[0]).intValue();
            String nextData = String.valueOf(next[1]);
            if (nextDefinitionId != definitionId) {
                if (buffer.length() != 0) {
                    // buffer will be 0 the very first time, since definitionId is initially -1
                    String bufferedData = bufferWithDelimiter ? (delimiter + buffer.toString() + delimiter) : buffer
                        .toString();
                    int configId = persistConfiguration(propertyName, bufferedData);
                    persistNotification(definitionId, configId, sender);
                }
                definitionId = nextDefinitionId;
                buffer = new StringBuilder(); // reset for the next definitionId
            }

            if (buffer.length() != 0) {
                // elements are already in the list, always add <delimiter> between them
                buffer.append(delimiter);
            }
            buffer.append(nextData);
        }

        if (buffer.length() != 0) {
            // always add <delimiter> to both side of the buffer -- this will enable searches for data
            // using the JPQL fragment notification.configuration.value = <delimiter><data><delimiter>'
            String bufferedData = bufferWithDelimiter ? (delimiter + buffer.toString() + delimiter) : buffer.toString();
            int configId = persistConfiguration(propertyName, bufferedData);
            persistNotification(definitionId, configId, sender);
        }
    }

    private int persistConfiguration(String... propertyNameValues) throws SQLException {
        int configId = databaseType.getNextSequenceValue(connection, "rhq_config", "id");
        String insertConfigSQL = getInsertConfigSQL(configId);
        databaseType.executeSql(connection, insertConfigSQL);

        for (int i = 0; i < propertyNameValues.length; i += 2) {
            String propertyName = propertyNameValues[i];
            String propertyValue = propertyNameValues[i + 1];

            int propertyId = databaseType.getNextSequenceValue(connection, "rhq_config_property", "id");
            String insertPropertySQL = getInsertPropertySQL(propertyId, configId, propertyName, propertyValue);
            databaseType.executeSql(connection, insertPropertySQL);
        }

        return configId;
    }

    private void persistNotification(int definitionId, int configId, String sender) throws SQLException {
        int notificationId = databaseType.getNextSequenceValue(connection, "rhq_alert_notification", "id");
        String insertNotificationSQL = getInsertNotificationSQL(notificationId, definitionId, configId, sender);

        databaseType.executeSql(connection, insertNotificationSQL);
    }

    private String getInsertConfigSQL(int id) {
        return "INSERT INTO rhq_config ( id, version, ctime, mtime )" //
            + "      VALUES ( " + id + ", 0, " + NOW + ", " + NOW + " ) ";
    }

    private String getInsertPropertySQL(int id, int configId, String name, String value) {
        return "INSERT INTO rhq_config_property ( id, configuration_id, name, string_value, dtype )" //
            + "      VALUES ( " + id + ", " + configId + ", '" + name + "', '" + value + "', 'property' ) ";
    }

    private String getInsertNotificationSQL(int id, int definitionId, int configId, String sender) {
        return "INSERT INTO rhq_alert_notification ( id, alert_definition_id, sender_config_id, sender_name )" //
            + "      VALUES ( " + id + ", " + definitionId + ", " + configId + ", '" + sender + "' ) ";
    }

}
