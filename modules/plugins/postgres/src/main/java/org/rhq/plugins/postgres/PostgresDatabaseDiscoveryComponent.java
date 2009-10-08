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

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.util.jdbc.JDBCUtil;

/**
 * @author Greg Hinkle
 */
public class PostgresDatabaseDiscoveryComponent implements ResourceDiscoveryComponent<PostgresServerComponent> {
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<PostgresServerComponent> context)
        throws Exception {
        Set<DiscoveredResourceDetails> databases = new HashSet<DiscoveredResourceDetails>();

        Statement statement = null;
        ResultSet resultSet = null;
        try {
            statement = context.getParentResourceComponent().getConnection().createStatement();
            resultSet = statement
                .executeQuery("SELECT *, pg_database_size(datname) FROM pg_database where datistemplate = false");
            while (resultSet.next()) {
                String databaseName = resultSet.getString("datname");
                DiscoveredResourceDetails database = new DiscoveredResourceDetails(context.getResourceType(),
                    databaseName, databaseName + " Database", null, "The " + databaseName
                        + " Postgres Database Instance", null, null);
                database.getPluginConfiguration().put(new PropertySimple("databaseName", databaseName));
                databases.add(database);
            }
        } finally {
            JDBCUtil.safeClose(statement);
        }

        return databases;
    }
}