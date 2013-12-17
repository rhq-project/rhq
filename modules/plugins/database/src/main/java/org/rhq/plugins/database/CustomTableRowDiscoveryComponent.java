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
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * Discovery for a generic component that can read data out of a table for
 * monitoring purposes. Necessary configuration properties:
 * <li> table - the name of the table to search for during inventory
 * <li> keyColumn - the key of the table used to load metric data
 * <li> name - the name of the resource
 * <li> description - the description of the resource
 *
 * @author Greg Hinkle
 */
public class CustomTableRowDiscoveryComponent implements ResourceDiscoveryComponent<ResourceComponent<?>> {
    private static final Log LOG = LogFactory.getLog(CustomTableRowDiscoveryComponent.class);

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
        ResourceDiscoveryContext<ResourceComponent<?>> discoveryContext) throws InvalidPluginConfigurationException,
        Exception {

        ResourceComponent<?> parentComponent = discoveryContext.getParentResourceComponent();

        Configuration config = discoveryContext.getDefaultPluginConfiguration();
        String table = config.getSimpleValue("table", null);
        String keyColumn = config.getSimpleValue("keyColumn", null);
        ResourceType resourceType = discoveryContext.getResourceType();
        String resourceName = config.getSimpleValue("name", resourceType.getName());
        String resourceDescription = config.getSimpleValue("description", "");

        if (!canProvideConnection(parentComponent)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Parent component does not provide JDBC connections, cannot discover" + resourceName);
            }
            return Collections.emptySet();
        }

        if (resourceName == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("'name' property not set, cannot discover" + resourceName);
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
            resultSet = statement.executeQuery("SELECT * FROM " + table);

            Set<DiscoveredResourceDetails> found = new HashSet<DiscoveredResourceDetails>();
            while (resultSet.next()) {
                config = discoveryContext.getDefaultPluginConfiguration();
                String key = resultSet.getString(keyColumn);
                config.put(new PropertySimple("key", key));
                DiscoveredResourceDetails details = new DiscoveredResourceDetails(discoveryContext.getResourceType(),
                    key, formatMessage(resourceName, key), null, formatMessage(resourceDescription, key), config, null);
                found.add(details);
            }

            return found;

        } catch (SQLException e) {
            LOG.debug("table " + table + " column " + keyColumn, e);
        } finally {
            DatabasePluginUtil.safeClose(null, statement, resultSet);
            if (hasConnectionPoolingSupport(parentComponent)) {
                DatabasePluginUtil.safeClose(connection, statement, resultSet);
            }
        }

       return Collections.emptySet();
    }

    /**
     * Format a message with {<key>} formatted replacement keys.
     *
     * @param  message the message to format
     * @param  key to replace; if null returns the message as-is
     *
     * @return the formatted text with variables replaced
     */
    public static String formatMessage(String message, String key) {
        if (key == null)
            return message;
        key = Matcher.quoteReplacement(key);
        message = message.replaceAll("\\{key\\}", key);
        return message;
    }
}
