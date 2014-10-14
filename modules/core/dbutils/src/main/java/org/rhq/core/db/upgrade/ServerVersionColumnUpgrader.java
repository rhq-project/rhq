/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.core.db.upgrade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.util.jdbc.JDBCUtil;

/**
 * A tool to create the version column in the servers table.
 *
 * @author Thomas Segismont
 */
public class ServerVersionColumnUpgrader {

    /**
     * @param connection a connection to the DB (won't be closed by this method)
     * @param version the version to set for all rows
     * @return true if the column was not present and upgraded
     * @throws Exception
     */
    public boolean upgrade(Connection connection, String version) throws Exception {
        PreparedStatement preparedStatement = null;
        try {
            DatabaseType db = DatabaseTypeFactory.getDatabaseType(connection);
            if (!db.checkColumnExists(connection, "RHQ_SERVER", "VERSION")) {
                db.addColumn(connection, "RHQ_SERVER", "VERSION", "VARCHAR2", "255");
                preparedStatement = connection.prepareStatement("UPDATE RHQ_SERVER SET VERSION = ?");
                preparedStatement.setString(1, "PRE-" + version);
                preparedStatement.executeUpdate();
                db.closeStatement(preparedStatement);
                // set column not null after it's been set
                db.alterColumn(connection, "RHQ_SERVER", "VERSION", "VARCHAR2", null, "255", false, false);
                return true;
            }
        } finally {
            JDBCUtil.safeClose(null, preparedStatement, null);
        }
        return false;
    }

    /**
     * @param connection a connection to the DB (won't be closed by this method)
     * @param version the version to set for server with the specified <code>name</code>
     * @param name server name
     * @throws java.sql.SQLException
     */
    public int setVersionForServerWithName(Connection connection, String version, String name) throws SQLException {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement("UPDATE RHQ_SERVER SET VERSION = ? WHERE NAME = ?");
            preparedStatement.setString(1, version);
            preparedStatement.setString(2, name);
            return preparedStatement.executeUpdate();
        } finally {
            JDBCUtil.safeClose(null, preparedStatement, null);
        }
    }

    /**
     * @param connection a connection to the DB (won't be closed by this method)
     * @param version the version to set for all servers
     * @throws SQLException
     */
    public int setVersionForAllServers(Connection connection, String version) throws SQLException {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement("UPDATE RHQ_SERVER SET VERSION = ?");
            preparedStatement.setString(1, version);
            return preparedStatement.executeUpdate();
        } finally {
            JDBCUtil.safeClose(null, preparedStatement, null);
        }
    }
}
