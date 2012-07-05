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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.util.jdbc.JDBCUtil;

/**
 * Discovery for a generic component that can read data out of a table for
 * monitoring purposes. Necessary configuration properties
 * <li> table - the name of the table to search for during inventory. Note: If absent only
 * supports manual adding.
 * <li> metricQuery - the query run to load metric data
 * <li> name - the name of the resource
 * <li> description - the description of the resource
 *
 * @author Greg Hinkle
 */
public class CustomTableDiscoveryComponent implements ManualAddFacet<DatabaseComponent<?>>,
    ResourceDiscoveryComponent<DatabaseComponent<?>> {

    protected Log log = LogFactory.getLog(getClass());

    public Set<DiscoveredResourceDetails> discoverResources(
        ResourceDiscoveryContext<DatabaseComponent<?>> resourceDiscoveryContext)
        throws InvalidPluginConfigurationException, Exception {

        Configuration config = resourceDiscoveryContext.getDefaultPluginConfiguration();
        String table = config.getSimpleValue("table", "");
        ResourceType rt = resourceDiscoveryContext.getResourceType();
        String resourceName = config.getSimpleValue("name", rt.getName());
        String resourceDescription = config.getSimpleValue("description", rt.getDescription());

        if (table.isEmpty()) {
            log.debug("'table' value not set, cannot discover " + resourceName);
            return Collections.emptySet();
        }

        Statement statement = null;
        try {
            Connection conn = resourceDiscoveryContext.getParentResourceComponent().getConnection();
            if (conn == null)
                throw new InvalidPluginConfigurationException("cannot obtain connection from parent");

            statement = conn.createStatement();
            statement.setMaxRows(1);
            statement.setFetchSize(1);
            // This is more efficient than 'count(*)'
            // unless the JDBC driver fails to support setMaxRows or doesn't stream results
            statement.executeQuery("SELECT * FROM " + table).close();
            DiscoveredResourceDetails details = new DiscoveredResourceDetails(
                resourceDiscoveryContext.getResourceType(), table + resourceName, resourceName, null,
                resourceDescription, config, null);

            log.debug("discovered " + details);
            return Collections.singleton(details);
        } catch (SQLException e) {
            log.debug("discovery failed " + e + " for " + table);
            // table not found, don't inventory
        } finally {
            JDBCUtil.safeClose(statement);
        }

        return Collections.emptySet();
    }

    public DiscoveredResourceDetails discoverResource(Configuration pluginConfiguration,
        ResourceDiscoveryContext<DatabaseComponent<?>> discoveryContext) throws InvalidPluginConfigurationException {

        Configuration config = pluginConfiguration;
        String table = config.getSimpleValue("table", null);
        String resourceName = config.getSimpleValue("name", table);
        String resourceDescription = config.getSimpleValue("description",
                discoveryContext.getResourceType().getDescription());
        String resourceVersion = config.getSimpleValue("version", null);

        DiscoveredResourceDetails details = new DiscoveredResourceDetails(discoveryContext.getResourceType(), table
            + resourceName, resourceName, resourceVersion, resourceDescription, config, null);

        return details;
    }
}
