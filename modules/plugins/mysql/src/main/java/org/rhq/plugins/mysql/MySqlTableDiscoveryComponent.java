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
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.database.DatabasePluginUtil;

/**
 * Discovers MySQL tables.
 * @author Steve Millidge (C2B2 Consulting Limited)
 */
public class MySqlTableDiscoveryComponent implements ResourceDiscoveryComponent {
    private static final Log LOG = LogFactory.getLog(MySqlTableDiscoveryComponent.class);

    private static final String TABLE_DISCOVERY = "tableDiscovery";

    @Override
    public Set discoverResources(ResourceDiscoveryContext rdc) throws InvalidPluginConfigurationException, Exception {
        Set<DiscoveredResourceDetails> set = new HashSet<DiscoveredResourceDetails>();
        MySqlDatabaseComponent parent = (MySqlDatabaseComponent) rdc.getParentResourceComponent();
        Configuration pconfig = rdc.getParentResourceContext().getPluginConfiguration();
        // If the user has disabled table discovery on the parent, we don't autodiscover
        // them, as we may hit temporary ones that go away any time soon again
        // See BZ-797356
        if (!Boolean.parseBoolean(pconfig.getSimpleValue(TABLE_DISCOVERY, "true"))) {
            LOG.debug("table discovery disabled");
            return set;
        }

        Connection jdbcConnection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            jdbcConnection = parent.getPooledConnectionProvider().getPooledConnection();
            statement = jdbcConnection.createStatement();
            resultSet = statement.executeQuery("show tables from " + parent.getName());
            while (resultSet.next()) {
                String tableName = resultSet.getString(1);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Discovered Table " + tableName);
                }
                Configuration config = new Configuration();
                config.put(new PropertySimple("tableName", tableName));
                DiscoveredResourceDetails details = new DiscoveredResourceDetails(rdc.getResourceType(), tableName,
                    tableName + " Table", null, tableName + " MySql Table", config, null);
                set.add(details);
            }
        } catch (SQLException se) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Unable to Discover Tables", se);
            }

        } finally {
            DatabasePluginUtil.safeClose(jdbcConnection, statement, resultSet);
        }
        return set;
    }
}
