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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.database.DatabaseQueryUtility;

/**
 *
 * @author Steve Millidge (C2B2 Consulting Limited)
 */
public class MySqlTableDiscoveryComponent implements ResourceDiscoveryComponent {

    private Log log = LogFactory.getLog(this.getClass());

    @Override
    public Set discoverResources(ResourceDiscoveryContext rdc) throws InvalidPluginConfigurationException, Exception {

        HashSet<DiscoveredResourceDetails> set = new HashSet<DiscoveredResourceDetails>();
        MySqlDatabaseComponent parent = (MySqlDatabaseComponent)rdc.getParentResourceComponent();
        Connection conn = parent.getConnection();

        if (conn != null) {
            Statement stmt = null;
            ResultSet rs = null;
            try {
                stmt = conn.createStatement();
                rs = stmt.executeQuery("show tables from " + parent.getName());
                while (rs.next()) {
                    String tableName = rs.getString(1);
                     if (log.isDebugEnabled()) {
                        log.debug("Discovered Table "+ tableName);
                    }
                    Configuration config = new Configuration();
                    config.put(new PropertySimple("tableName",tableName));
                    DiscoveredResourceDetails details = new DiscoveredResourceDetails(
                            rdc.getResourceType(),
                            tableName,
                            tableName + " Table",
                            null,
                            tableName + " MySql Table", config, null);
                    set.add(details);
                }
            } catch(SQLException se) {
                if (log.isDebugEnabled()) {
                    log.debug("Unable to Discover Tables",se);
                }

            }finally {
                DatabaseQueryUtility.close(stmt, rs);
            }
        }
        return set;
    }

}
