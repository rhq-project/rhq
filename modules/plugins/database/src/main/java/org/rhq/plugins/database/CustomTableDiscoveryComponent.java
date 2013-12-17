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

package org.rhq.plugins.database;

import static org.rhq.plugins.database.DatabasePluginUtil.canProvideConnection;
import static org.rhq.plugins.database.DatabasePluginUtil.getConnectionFromComponent;
import static org.rhq.plugins.database.DatabasePluginUtil.hasConnectionPoolingSupport;

import java.sql.Connection;
import java.sql.ResultSet;
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
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

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
public class CustomTableDiscoveryComponent implements ManualAddFacet<ResourceComponent<?>>,
    ResourceDiscoveryComponent<ResourceComponent<?>> {

    protected Log log = LogFactory.getLog(getClass());

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
        ResourceDiscoveryContext<ResourceComponent<?>> discoveryContext) throws InvalidPluginConfigurationException,
        Exception {

        ResourceComponent<?> parentComponent = discoveryContext.getParentResourceComponent();

        Configuration config = discoveryContext.getDefaultPluginConfiguration();
        String table = config.getSimpleValue("table", "");
        ResourceType resourceType = discoveryContext.getResourceType();
        String resourceName = config.getSimpleValue("name", resourceType.getName());
        String resourceDescription = config.getSimpleValue("description", resourceType.getDescription());

        if (!canProvideConnection(parentComponent)) {
            if (log.isDebugEnabled()) {
                log.debug("Parent component does not provide JDBC connections, cannot discover" + resourceName);
            }
            return Collections.emptySet();
        }

        if (table.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("'table' value not set, cannot discover " + resourceName);
            }
            return Collections.emptySet();
        }

        Statement statement = null;
        Connection connection = null;
        ResultSet resultSet = null;
        try {
            connection = getConnectionFromComponent(parentComponent);
            if (connection == null) {
                throw new InvalidPluginConfigurationException("cannot obtain connection from parent");
            }
            statement = connection.createStatement();
            statement.setMaxRows(1);
            statement.setFetchSize(1);
            // This is more efficient than 'count(*)'
            // unless the JDBC driver fails to support setMaxRows or doesn't stream results
            resultSet = statement.executeQuery("SELECT * FROM " + table);

            DiscoveredResourceDetails details = new DiscoveredResourceDetails(discoveryContext.getResourceType(), table
                + resourceName, resourceName, null, resourceDescription, config, null);

            if (log.isDebugEnabled()) {
                log.debug("discovered " + details);
            }
            return Collections.singleton(details);

        } catch (SQLException e) {
            if (log.isDebugEnabled()) {
                log.debug("discovery failed " + e + " for " + table);
            }
            // table not found, don't inventory
        } finally {
            DatabasePluginUtil.safeClose(null, statement, resultSet);
            if (hasConnectionPoolingSupport(parentComponent)) {
                DatabasePluginUtil.safeClose(connection, statement, resultSet);
            }
        }

        return Collections.emptySet();
    }

    @Override
    public DiscoveredResourceDetails discoverResource(Configuration pluginConfiguration,
        ResourceDiscoveryContext<ResourceComponent<?>> discoveryContext) throws InvalidPluginConfigurationException {

        Configuration config = pluginConfiguration;
        String table = config.getSimpleValue("table", null);
        String resourceName = config.getSimpleValue("name", table);
        String resourceDescription = config.getSimpleValue("description", discoveryContext.getResourceType()
            .getDescription());
        String resourceVersion = config.getSimpleValue("version", null);

        DiscoveredResourceDetails details = new DiscoveredResourceDetails(discoveryContext.getResourceType(), table
            + resourceName, resourceName, resourceVersion, resourceDescription, config, null);

        return details;
    }
}
