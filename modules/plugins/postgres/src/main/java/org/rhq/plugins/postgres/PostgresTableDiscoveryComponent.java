/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

package org.rhq.plugins.postgres;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.database.DatabasePluginUtil;

/**
 * Discovers postgres tables
 *
 * @author Greg Hinkle
 */
public class PostgresTableDiscoveryComponent implements ResourceDiscoveryComponent<PostgresDatabaseComponent> {
    private static final Log LOG = LogFactory.getLog(PostgresTableDiscoveryComponent.class);

    public static final String TABLE_NAMES_QUERY = "select relname from pg_stat_user_tables";

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<PostgresDatabaseComponent> context)
        throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Discovering postgres tables for " + context.getParentResourceComponent().getDatabaseName()
                + "...");
        }
        Set<DiscoveredResourceDetails> discoveredTables = new HashSet<DiscoveredResourceDetails>();

        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            connection = context.getParentResourceComponent().getPooledConnectionProvider().getPooledConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(TABLE_NAMES_QUERY);
            while (resultSet.next()) {
                String tableName = resultSet.getString(1);
                DiscoveredResourceDetails service = new DiscoveredResourceDetails(context.getResourceType(), tableName,
                    tableName, null, null, null, null);
                service.getPluginConfiguration().put(new PropertySimple("tableName", tableName));
                discoveredTables.add(service);
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Found " + discoveredTables.size() + " tables in database "
                    + context.getParentResourceComponent().getDatabaseName());
            }
        } catch (SQLException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(
                    "Could not find tables in database " + context.getParentResourceComponent().getDatabaseName(), e);
            }
        } finally {
            DatabasePluginUtil.safeClose(connection, statement, resultSet);
        }

        return discoveredTables;
    }
}
