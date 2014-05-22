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
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.resource.ResourceUpgradeReport;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeFacet;
import org.rhq.plugins.database.DatabasePluginUtil;
import org.rhq.plugins.database.PooledConnectionProvider;

import static org.rhq.plugins.database.DatabasePluginUtil.*;

/**
 * Discovers Postgres Users though shouldn't need super user access
 *
 * @author Greg Hinkle
 */
public class PostgresUserDiscoveryComponent implements ResourceDiscoveryComponent<PostgresServerComponent<?>>,
    ResourceUpgradeFacet<PostgresServerComponent<?>> {

    private static final Log LOG = LogFactory.getLog(PostgresUserDiscoveryComponent.class);

    static final String OID_PREFIX = "oid: ";
    static final String FIND_ALL_ROLES = "select * from pg_roles";
    static final String FIND_ROLE_BY_NAME = "select * from pg_roles where rolname = ?";

    /**
     * @deprecated as of RHQ 4.12. Shouldn't have been exposed.
     */
    @Deprecated
    public static final String USERS_QUERY = "select * from pg_roles";

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<PostgresServerComponent<?>> context)
        throws Exception {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Discovering postgres users for " + context.getParentResourceComponent().getJDBCUrl() + "...");
        }

        Set<DiscoveredResourceDetails> discoveredUsers = new HashSet<DiscoveredResourceDetails>();

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = context.getParentResourceComponent().getPooledConnectionProvider().getPooledConnection();
            statement = connection.prepareStatement(FIND_ALL_ROLES);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                long userOid = resultSet.getLong("oid");
                String userName = resultSet.getString("rolname");
                DiscoveredResourceDetails service = new DiscoveredResourceDetails(context.getResourceType(),
                    createResourceKey(userOid), userName, null, "A Postgres user", null, null);
                discoveredUsers.add(service);
            }
        } finally {
            safeClose(connection, statement, resultSet);
        }

        return discoveredUsers;
    }

    static String createResourceKey(long userOid) {
        return OID_PREFIX + userOid;
    }

    @Override
    public ResourceUpgradeReport upgrade(ResourceUpgradeContext<PostgresServerComponent<?>> inventoriedResource) {
        String currentResourceKey = inventoriedResource.getResourceKey();

        if (!currentResourceKey.startsWith(OID_PREFIX)) {
            // currentResourceKey is the user name
            ResourceUpgradeReport report = new ResourceUpgradeReport();
            try {
                long userOid = getUserOid(currentResourceKey, inventoriedResource.getParentResourceComponent()
                    .getPooledConnectionProvider());
                if (userOid == -1) {
                    LOG.warn("Could not find oid of user [" + currentResourceKey + "]");
                    return null;
                }
                report.setNewResourceKey(createResourceKey(userOid));
                return report;
            } catch (SQLException e) {
                LOG.warn("Exception thrown while searching oid of user [" + currentResourceKey + "]", e);
            }
        }

        return null;
    }

    static long getUserOid(String user, PooledConnectionProvider pooledConnectionProvider) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = pooledConnectionProvider.getPooledConnection();
            statement = connection.prepareStatement(FIND_ROLE_BY_NAME);
            statement.setString(1, user);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getLong("oid");
            }
            return -1;
        } finally {
            safeClose(connection, statement, resultSet);
        }
    }
}
