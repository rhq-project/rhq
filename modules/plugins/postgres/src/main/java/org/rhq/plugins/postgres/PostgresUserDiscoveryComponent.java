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
 * Discovers Postgres Users though shouldn't need super user access
 *
 * @author Greg Hinkle
 */
public class PostgresUserDiscoveryComponent implements ResourceDiscoveryComponent<PostgresServerComponent> {
    private static final Log log = LogFactory.getLog(PostgresTableDiscoveryComponent.class);

    public static final String USERS_QUERY = "select * from pg_roles";

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<PostgresServerComponent> context)
        throws Exception {
        log.info("Discovering postgres users");
        Set<DiscoveredResourceDetails> discoveredUsers = new HashSet<DiscoveredResourceDetails>();

        Connection connection = context.getParentResourceComponent().getConnection();
        if (connection == null) // For databases we don't have access to don't find the tables
        {
            return discoveredUsers;
        }

        Statement statement = null;
        ResultSet resultSet = null;

        try {
            statement = connection.createStatement();
            resultSet = statement.executeQuery(USERS_QUERY);
            while (resultSet.next()) {
                String userName = resultSet.getString("rolname");
                DiscoveredResourceDetails service = new DiscoveredResourceDetails(context.getResourceType(), userName,
                    userName + " User", null, "A Postgres user", null, null);
                service.getPluginConfiguration().put(new PropertySimple("userName", userName));
                discoveredUsers.add(service);
            }
        } finally {
            JDBCUtil.safeClose(statement, resultSet);
        }

        return discoveredUsers;
    }
}