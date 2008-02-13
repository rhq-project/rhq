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
package org.rhq.plugins.postgres;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.util.jdbc.JDBCUtil;

/**
 * Discovers postgres tables
 *
 * @author Greg Hinkle
 */
public class PostgresTableDiscoveryComponent implements ResourceDiscoveryComponent<PostgresDatabaseComponent> {
    private static final Log log = LogFactory.getLog(PostgresTableDiscoveryComponent.class);

    public static final String TABLE_NAMES_QUERY = "select relname from pg_stat_user_tables";

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<PostgresDatabaseComponent> context)
        throws Exception {
        log.debug("Discovering postgres tables for " + context.getParentResourceComponent().getDatabaseName());
        Set<DiscoveredResourceDetails> discoveredTables = new HashSet<DiscoveredResourceDetails>();

        Connection connection = context.getParentResourceComponent().getConnection();
        if (connection == null) // For databases we don't have access to don't find the tables
        {
            return discoveredTables;
        }

        ResultSet resultSet = null;
        Statement statement = connection.createStatement();
        resultSet = statement.executeQuery(TABLE_NAMES_QUERY);
        while (resultSet.next()) {
            String tableName = resultSet.getString(1);
            DiscoveredResourceDetails service = new DiscoveredResourceDetails(context.getResourceType(), tableName,
                tableName, null, null, null, null);
            service.getPluginConfiguration().put(new PropertySimple("tableName", tableName));
            discoveredTables.add(service);
        }

        JDBCUtil.safeClose(statement, resultSet);

        return discoveredTables;
    }
}