/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.plugins.postgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceUpgradeReport;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeFacet;
import org.rhq.plugins.database.DatabasePluginUtil;

/**
 * Discovers postgres tables
 *
 * @author Greg Hinkle
 */
public class PostgresTableDiscoveryComponent implements ResourceDiscoveryComponent<PostgresDatabaseComponent>,
    ResourceUpgradeFacet<PostgresDatabaseComponent> {

    private static final Log LOG = LogFactory.getLog(PostgresTableDiscoveryComponent.class);

    private static final String DISCOVERY_QUERY = "select schemaname, relname from pg_stat_user_tables where schemaname NOT LIKE 'pg_temp%'";
    private static final String UPGRADE_QUERY = "select schemaname from pg_stat_user_tables where relname = ?";

    static final String SCHEMA_SEPARATOR = ".";

    /**
     * @deprecated as of RHQ4.11. No longer used (and shouldn't have been exposed anyway).
     */
    @Deprecated
    public static final String TABLE_NAMES_QUERY = "select relname from pg_stat_user_tables";

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<PostgresDatabaseComponent> context)
        throws Exception {

        PostgresDatabaseComponent postgresDatabaseComponent = context.getParentResourceComponent();

        if (LOG.isTraceEnabled()) {
            LOG.trace("Discovering postgres tables for " + postgresDatabaseComponent.getDatabaseName() + "...");
        }

        Set<DiscoveredResourceDetails> discoveredTables = new HashSet<DiscoveredResourceDetails>();

        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            connection = postgresDatabaseComponent.getPooledConnectionProvider().getPooledConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(DISCOVERY_QUERY);

            while (resultSet.next()) {
                String schemaName = resultSet.getString(1);
                String tableName = resultSet.getString(2);

                discoveredTables.add(new DiscoveredResourceDetails(context.getResourceType(), createNewResourceKey(
                    schemaName, tableName), createNewResourceName(schemaName, tableName), null, null,
                    createNewPluginConfiguration(schemaName, tableName), null));
            }

            if (LOG.isTraceEnabled()) {
                LOG.trace("Found " + discoveredTables.size() + " tables in database "
                    + postgresDatabaseComponent.getDatabaseName());
            }
        } catch (SQLException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Could not find tables in database " + postgresDatabaseComponent.getDatabaseName(), e);
            }
        } finally {
            DatabasePluginUtil.safeClose(connection, statement, resultSet);
        }

        return discoveredTables;
    }

    private String createNewResourceKey(String schemaName, String tableName) {
        return schemaName + SCHEMA_SEPARATOR + tableName;
    }

    private String createNewResourceName(String schemaName, String tableName) {
        return "public".equalsIgnoreCase(schemaName) ? tableName : schemaName + SCHEMA_SEPARATOR + tableName;
    }

    private Configuration createNewPluginConfiguration(String schemaName, String tableName) {
        Configuration newPluginConfig = new Configuration();
        newPluginConfig.setSimpleValue("schemaName", schemaName);
        newPluginConfig.setSimpleValue("tableName", tableName);
        return newPluginConfig;
    }

    @Override
    public ResourceUpgradeReport upgrade(ResourceUpgradeContext<PostgresDatabaseComponent> upgradeContext) {
        String inventoriedResourceKey = upgradeContext.getResourceKey();
        if (inventoriedResourceKey.contains(SCHEMA_SEPARATOR)) {
            // Already in latest format
            return null;
        }

        String schemaName = null;
        String tableName = inventoriedResourceKey;

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = upgradeContext.getParentResourceComponent().getPooledConnectionProvider()
                .getPooledConnection();
            statement = connection.prepareStatement(UPGRADE_QUERY);
            statement.setString(1, inventoriedResourceKey);
            resultSet = statement.executeQuery();

            if (!resultSet.next()) {
                LOG.warn("Could not upgrade " + upgradeContext.getResourceDetails()
                    + ". The table was not found in the dabatase");
                return null;
            }

            schemaName = resultSet.getString(1);

            if (resultSet.next()) {
                Set<String> allSchemas = new HashSet<String>();
                allSchemas.add(schemaName);
                allSchemas.add(resultSet.getString(1));
                while (resultSet.next()) {
                    allSchemas.add(resultSet.getString(1));
                }
                LOG.warn("Could not upgrade " + upgradeContext.getResourceDetails()
                    + ". The table was found in more than one schema: " + allSchemas);
                return null;
            }
        } catch (SQLException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Exception while upgrading " + upgradeContext.getResourceDetails(), e);
            }
        } finally {
            DatabasePluginUtil.safeClose(connection, statement, resultSet);
        }

        ResourceUpgradeReport report = new ResourceUpgradeReport();
        report.setNewResourceKey(createNewResourceKey(schemaName, tableName));
        report.setNewPluginConfiguration(createNewPluginConfiguration(schemaName, tableName));

        return report;
    }
}
