/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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

import org.rhq.core.db.DatabaseType;
import org.rhq.core.util.obfuscation.Obfuscator;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * We had a buggy version of the password obfuscation code borrowed from JBoss AS.
 * It was susceptible to these two issues:
 * https://issues.jboss.org/browse/SECURITY-344
 * https://issues.jboss.org/browse/SECURITY-563
 *
 * Unfortunately this means an information loss in the encoded value and therefore we have no way of recovering the
 * original passwords. This upgrade task therefore checks if an obfuscated value in a database can be decoded and if not,
 * it clears it out.
 *
 * @author Lukas Krejci
 */
public class ConfigurationObfuscationCorrectionUpgradeTask implements DatabaseUpgradeTask {

    @Override
    public void execute(DatabaseType type, Connection connection) throws SQLException {
        String sql = "SELECT id, string_value FROM rhq_config_property WHERE dtype = 'obfuscated'";

        List<Object[]> results = type.executeSelectSql(connection, sql);

        for(Object[] row : results) {
            String value = (String) row[1];

            //try to decode the value
            try {
                Obfuscator.decode(value);
            } catch (Exception e) {
                int id = ((Number) row[0]).intValue();

                type.executeSql(connection, "UPDATE rhq_config_property SET string_value = NULL WHERE id = " + id);
            }
        }
    }
}
