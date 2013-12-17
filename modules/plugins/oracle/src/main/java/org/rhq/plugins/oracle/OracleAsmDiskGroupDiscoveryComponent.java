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

package org.rhq.plugins.oracle;

import static org.rhq.plugins.database.DatabasePluginUtil.getConnectionFromComponent;
import static org.rhq.plugins.database.DatabasePluginUtil.hasConnectionPoolingSupport;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.database.DatabasePluginUtil;

/**
 * Discovery Oracle ASM Disk Groups.
 *
 * @author Richard Hensman
 */
public class OracleAsmDiskGroupDiscoveryComponent implements ResourceDiscoveryComponent<ResourceComponent<?>> {
    private static final Log LOG = LogFactory.getLog(OracleAsmDiskGroupDiscoveryComponent.class);

    public Set<DiscoveredResourceDetails> discoverResources(
        ResourceDiscoveryContext<ResourceComponent<?>> resourceDiscoveryContext)
        throws InvalidPluginConfigurationException, Exception {

        String table = "V$ASM_DISKGROUP";
        String keyColumn = "GROUP_NUMBER";
        String nameColumn = "NAME";
        String description = "Oracle ASM Disk Groups";

        ResourceComponent<?> parentComponent = resourceDiscoveryContext.getParentResourceComponent();

        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getConnectionFromComponent(parentComponent);
            statement = connection.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM " + table);

            Configuration config = null;
            Set<DiscoveredResourceDetails> found = new HashSet<DiscoveredResourceDetails>();
            while (resultSet.next()) {
                config = resourceDiscoveryContext.getDefaultPluginConfiguration();
                String key = resultSet.getString(keyColumn);
                String name = resultSet.getString(nameColumn);
                DiscoveredResourceDetails details = new DiscoveredResourceDetails(
                    resourceDiscoveryContext.getResourceType(), key, name, null, description, config, null);
                found.add(details);
            }

            return found;
        } catch (SQLException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("table " + table + " column " + keyColumn, e);
            }
        } finally {
            DatabasePluginUtil.safeClose(null, statement, resultSet);
            if (hasConnectionPoolingSupport(parentComponent)) {
                DatabasePluginUtil.safeClose(connection);
            }
        }

        return Collections.emptySet();
    }

}
