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
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;
import org.rhq.plugins.database.DatabasePluginUtil;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 * @author Steve Millidge
 */
public class MySqlDiscoveryComponent implements ResourceDiscoveryComponent, ManualAddFacet {
    private static final Log LOG = LogFactory.getLog(MySqlDiscoveryComponent.class);

    public static final String HOST_CONFIGURATION_PROPERTY = "host";
    public static final String PORT_CONFIGURATION_PROPERTY = "port";
    public static final String DB_CONFIGURATION_PROPERTY = "db";
    public static final String PRINCIPAL_CONFIGURATION_PROPERTY = "principal";
    public static final String CREDENTIALS_CONFIGURATION_PROPERTY = "credentials";

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Resource Discovery Started");
        }
        Set<DiscoveredResourceDetails> servers = new LinkedHashSet<DiscoveredResourceDetails>();

        // Process any auto-discovered resources.
        List<ProcessScanResult> autoDiscoveryResults = context.getAutoDiscoveredProcesses();
        for (ProcessScanResult result : autoDiscoveryResults) {
            LOG.info("Discovered a mysql process: " + result);

            ProcessInfo procInfo = result.getProcessInfo();

            servers.add(createResourceDetails(context, context.getDefaultPluginConfiguration(), procInfo));
        }

        return servers;
    }

    public DiscoveredResourceDetails discoverResource(Configuration pluginConfiguration,
        ResourceDiscoveryContext resourceDiscoveryContext) throws InvalidPluginConfigurationException {
        ProcessInfo processInfo = null;
        DiscoveredResourceDetails resourceDetails = createResourceDetails(resourceDiscoveryContext,
            pluginConfiguration, processInfo);
        return resourceDetails;
    }

    protected static DiscoveredResourceDetails createResourceDetails(ResourceDiscoveryContext discoveryContext,
        Configuration pluginConfig, ProcessInfo processInfo) throws InvalidPluginConfigurationException {

        Connection conn = null;
        String version = "";
        try {
            conn = buildConnection(pluginConfig);
            version = conn.getMetaData().getDatabaseProductVersion();
        } catch (SQLException ex) {
            // ignore so we can still add to the inventory even though we can't currently connect
        } finally {
            DatabasePluginUtil.safeClose(conn);
        }
        String key = new StringBuilder().append("MySql:")
            .append(pluginConfig.getSimple(DB_CONFIGURATION_PROPERTY).getStringValue()).append(":")
            .append(pluginConfig.getSimple(HOST_CONFIGURATION_PROPERTY).getStringValue()).append(":")
            .append(pluginConfig.getSimple(PORT_CONFIGURATION_PROPERTY).getStringValue()).append("-")
            .append(pluginConfig.getSimple(PRINCIPAL_CONFIGURATION_PROPERTY).getStringValue()).toString();
        String name = new StringBuilder().append("MySql [")
            .append(pluginConfig.getSimple(DB_CONFIGURATION_PROPERTY).getStringValue()).append("]").toString();

        DiscoveredResourceDetails result = new DiscoveredResourceDetails(discoveryContext.getResourceType(), key, name,
            version, "MySql Server", pluginConfig, processInfo);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Discovered Database Server for MySQL Database " + buildConnectionURL(pluginConfig));
        }
        return result;

    }

    static String buildConnectionURL(Configuration pluginConfig) {
        return new StringBuilder().append("jdbc:mysql://")
            .append(pluginConfig.getSimple(HOST_CONFIGURATION_PROPERTY).getStringValue()).append(":")
            .append(pluginConfig.getSimple(PORT_CONFIGURATION_PROPERTY).getStringValue()).append("/")
            .append(pluginConfig.getSimple(DB_CONFIGURATION_PROPERTY).getStringValue()).toString();
    }

    static Connection buildConnection(Configuration pluginConfig) throws SQLException {
        String driverClass = "com.mysql.jdbc.Driver";
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            throw new InvalidPluginConfigurationException("Specified JDBC driver class (" + driverClass
                + ") not found.");
        }

        String url = buildConnectionURL(pluginConfig);
        String principal = pluginConfig.getSimple(PRINCIPAL_CONFIGURATION_PROPERTY).getStringValue();
        String credentials = pluginConfig.getSimple(CREDENTIALS_CONFIGURATION_PROPERTY).getStringValue();

        return DriverManager.getConnection(url, principal, credentials);
    }
}
