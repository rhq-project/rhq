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

package org.rhq.plugins.mysql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.database.DatabasePluginUtil;

/**
 * @author Greg Hinkle
 * @author Steve Millidge
 */
public class MySqlDatabaseDiscoveryComponent implements ResourceDiscoveryComponent<MySqlComponent> {
    private static final Log LOG = LogFactory.getLog(MySqlDatabaseDiscoveryComponent.class);

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<MySqlComponent> context) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Database discovery started");
        }
        Connection jdbcConnection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            jdbcConnection = context.getParentResourceComponent().getPooledConnectionProvider().getPooledConnection();
            statement = jdbcConnection.createStatement();
            resultSet = statement.executeQuery("SHOW DATABASES");
            Set<DiscoveredResourceDetails> databases = new LinkedHashSet<DiscoveredResourceDetails>();
            while (resultSet.next()) {
                String databaseName = resultSet.getString(1);
                Configuration config = context.getDefaultPluginConfiguration();
                config.put(new PropertySimple("databaseName", databaseName));
                DiscoveredResourceDetails details = new DiscoveredResourceDetails(context.getResourceType(),
                    databaseName, databaseName + " Database", null, "A MySql Database", config, null);
                databases.add(details);
            }
            return databases;
        } catch (SQLException ignore) {
            return Collections.emptySet();
        } finally {
            DatabasePluginUtil.safeClose(jdbcConnection, statement, resultSet);
        }
    }
}
