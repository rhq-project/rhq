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
package org.rhq.plugins.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Set;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.util.jdbc.JDBCUtil;

/**
 * Discovery for a generic component that can read data out of a table for monitoring purposes. Neccessary configuration
 * properties table - the name of the table to search for during inventory metricQuery - the query run to load metric
 * data name - the name of the resource description - the description of the resource
 *
 * @author Greg Hinkle
 */
public class CustomTableDiscoveryComponent implements ResourceDiscoveryComponent<DatabaseComponent> {
    public Set<DiscoveredResourceDetails> discoverResources(
        ResourceDiscoveryContext<DatabaseComponent> resourceDiscoveryContext)
        throws InvalidPluginConfigurationException, Exception {
        Statement statement = null;
        try {
            Connection conn = resourceDiscoveryContext.getParentResourceComponent().getConnection();

            Configuration config = resourceDiscoveryContext.getDefaultPluginConfiguration();
            String table = config.getSimpleValue("table", null);
            String resourceName = config.getSimpleValue("name", null);
            String resourceDescription = config.getSimpleValue("description", null);

            statement = conn.createStatement();
            statement.executeQuery("SELECT COUNT(*) FROM " + table);

            DiscoveredResourceDetails details = new DiscoveredResourceDetails(resourceDiscoveryContext
                .getResourceType(), table, resourceName, null, resourceDescription, config, null);

            return Collections.singleton(details);
        } catch (SQLException e) {
            // table not found, don't inventory
        } finally {
            JDBCUtil.safeClose(statement);
        }

        return Collections.emptySet();
    }
}