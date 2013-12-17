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
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.database.DatabasePluginUtil;

/**
 * @author Steve Millidge (C2B2 Consulting Limited)
 */
public class MySqlUserDiscoveryComponent implements ResourceDiscoveryComponent {

    public Set discoverResources(ResourceDiscoveryContext rdc) throws InvalidPluginConfigurationException, Exception {
        Set<DiscoveredResourceDetails> set = new HashSet<DiscoveredResourceDetails>();
        MySqlComponent parent = (MySqlComponent) rdc.getParentResourceComponent();
        Connection jdbcConnection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            jdbcConnection = parent.getPooledConnectionProvider().getPooledConnection();
            statement = jdbcConnection.createStatement();
            resultSet = statement.executeQuery("select User,Host from mysql.user");
            while (resultSet.next()) {
                String user = resultSet.getString(1);
                String host = resultSet.getString(2);
                String userName = user + "@" + host;
                Configuration config = new Configuration();
                config.put(new PropertySimple("userName", user));
                config.put(new PropertySimple("host", host));
                DiscoveredResourceDetails discoveredUser = new DiscoveredResourceDetails(rdc.getResourceType(),
                    userName, userName, null, "A MySql User", config, null);
                set.add(discoveredUser);
            }
        } catch (Exception e) {
        } finally {
            DatabasePluginUtil.safeClose(jdbcConnection, statement, resultSet);
        }
        return set;
    }
}
