/*
 * RHQ Management Platform
 * Copyright (C) 2013 Red Hat, Inc.
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
import org.rhq.core.util.obfuscation.Obfuscator;

/**
 * @author Lukas Krejci
 * @since 4.10
 */
public class SystemSettingsPasswordObfuscationTask implements DatabaseUpgradeTask {

    @Override
    public void execute(DatabaseType type, Connection connection) throws SQLException {
        updatePasswordField(type, connection, "CAM_LDAP_BIND_PW", "CAM_HELP_PASSWORD");
    }

    private void updatePasswordField(DatabaseType type, Connection connection, String... names) throws SQLException {
        String sql = "SELECT id, property_value, default_property_value FROM rhq_system_config WHERE property_key IN (";
        for(String name : names) {
            sql += "'" + name + "', ";
        }

        if (names.length > 0) {
            sql = sql.substring(0, sql.length() - 2);
        }

        sql += ")";

        List<Object[]> results = type.executeSelectSql(connection, sql);

        for(Object[] row : results) {
            int settingId = (Integer) row[0];
            String value = (String) row[1];
            String defaultValue = (String) row[2];

            sql = "UPDATE rhq_system_config SET ";
            try {
                if (value != null) {
                    value = Obfuscator.encode(value);
                    sql += "property_value = '" + value + "'";
                }

                if (defaultValue != null) {
                    defaultValue = Obfuscator.encode(defaultValue);

                    sql += (value != null ? ", " : "") + "default_property_value = '" + defaultValue + "'";
                }
            } catch (Exception e) {
                throw new SQLException("Failed to obfuscate password system settings.", e);
            }

            sql += "WHERE id = " + settingId;

            type.executeSql(connection, sql);
        }
    }
}
