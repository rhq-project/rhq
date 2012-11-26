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
package org.rhq.plugins.oracle;

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
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.plugins.database.DatabaseComponent;

/**
 * Discovery Oracle ASM Disk Groups.
 *
 * @author Richard Hensman
 */
public class OracleAsmDiskGroupDiscoveryComponent implements ResourceDiscoveryComponent<DatabaseComponent<?>> {

    private final Log log = LogFactory.getLog(getClass());

    public Set<DiscoveredResourceDetails> discoverResources(
        ResourceDiscoveryContext<DatabaseComponent<?>> resourceDiscoveryContext)
        throws InvalidPluginConfigurationException, Exception {
        Statement statement = null;
        ResultSet resultSet = null;
        
        String table = "V$ASM_DISKGROUP";
        String keyColumn = "GROUP_NUMBER";
        String nameColumn = "NAME";
        String description = "Oracle ASM Disk Groups";
        
        try {
            Connection conn = resourceDiscoveryContext.getParentResourceComponent().getConnection();

            statement = conn.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM " + table);

            Configuration config = null;
            Set<DiscoveredResourceDetails> found = new HashSet<DiscoveredResourceDetails>();
            while (resultSet.next()) {
            	config = resourceDiscoveryContext.getDefaultPluginConfiguration();
                String key = resultSet.getString(keyColumn);
                String name = resultSet.getString(nameColumn);
                DiscoveredResourceDetails details =
                        new DiscoveredResourceDetails(
                                resourceDiscoveryContext.getResourceType(),
                                key,
                                name,
                                null,
                                description, config, null);
                found.add(details);
            }

            return found;
        } catch (SQLException e) {
            log.debug("table " + table + " column " + keyColumn, e);
        } finally {
            JDBCUtil.safeClose(resultSet);
            JDBCUtil.safeClose(statement);
        }

		return Collections.emptySet();
	}

}