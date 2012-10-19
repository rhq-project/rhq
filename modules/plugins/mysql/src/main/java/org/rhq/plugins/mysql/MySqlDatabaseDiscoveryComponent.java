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

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.database.DatabaseQueryUtility;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;

/**
 * @author Greg Hinkle
 * @author Steve Millidge
 */
public class MySqlDatabaseDiscoveryComponent implements ResourceDiscoveryComponent<MySqlComponent> {

    private Log logger = LogFactory.getLog(this.getClass());

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<MySqlComponent> context) {

        if (logger.isDebugEnabled()) {
            logger.debug("Database discovery started");
        }
        Set<DiscoveredResourceDetails> databases = new LinkedHashSet<DiscoveredResourceDetails>();
        Connection connection = context.getParentResourceComponent().getConnection();


        Statement statement = null;
        ResultSet resultSet = null;
        if (connection != null) {
            try {
                statement = connection.createStatement();
                resultSet = statement.executeQuery("SHOW DATABASES");

                while (resultSet.next()) {
                    String databaseName = resultSet.getString(1);
                    Configuration config = new Configuration();
                    config.put(new PropertySimple("databaseName",databaseName));
                    DiscoveredResourceDetails details =
                            new DiscoveredResourceDetails(
                            context.getResourceType(),
                            databaseName,
                            databaseName + " Database",
                            null,
                            "A MySql Database",
                            config,
                            null);
                    databases.add(details);
                }

            } catch (SQLException e) {
            } finally {
                DatabaseQueryUtility.close(statement, resultSet);
            }
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("No connection to MySQL obtained from connection manager");
            }
        }

        return databases;
    }
}
