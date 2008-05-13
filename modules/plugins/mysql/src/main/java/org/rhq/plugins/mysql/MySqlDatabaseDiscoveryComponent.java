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

package org.rhq.plugins.mysql;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.plugins.database.DatabaseQueryUtility;

import java.util.Set;
import java.util.LinkedHashSet;
import java.util.List;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * @author Greg Hinkle
 */
public class MySqlDatabaseDiscoveryComponent implements ResourceDiscoveryComponent<MySqlComponent> {



    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<MySqlComponent> context) {
        Set<DiscoveredResourceDetails> tables = new LinkedHashSet<DiscoveredResourceDetails>();


        Connection connection = context.getParentResourceComponent().getConnection();


        Statement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.createStatement();
            resultSet = statement.executeQuery("SHOW DATABASES");

            while (resultSet.next()) {
                String databaseName = resultSet.getString(1);
                DiscoveredResourceDetails details =
                        new DiscoveredResourceDetails(
                                context.getResourceType(),
                                databaseName,
                                databaseName + " Database",
                                null,
                                "A MySql Database",
                                null,
                                null);
                tables.add(details);
            }

        } catch (SQLException e) {
            DatabaseQueryUtility.close(statement, resultSet);
        }

        return tables;
    }

}