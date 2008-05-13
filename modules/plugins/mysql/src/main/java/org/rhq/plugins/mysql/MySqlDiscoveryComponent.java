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
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class MySqlDiscoveryComponent implements ResourceDiscoveryComponent {
    private static final Log log = LogFactory.getLog(MySqlDiscoveryComponent.class);

    public static final String DRIVER_CONFIGURATION_PROPERTY = "driverClass";
    public static final String HOST_CONFIGURATION_PROPERTY = "host";
    public static final String PORT_CONFIGURATION_PROPERTY = "port";
    public static final String DB_CONFIGURATION_PROPERTY = "db";
    public static final String PRINCIPAL_CONFIGURATION_PROPERTY = "principal";
    public static final String CREDENTIALS_CONFIGURATION_PROPERTY = "credentials";

    private static final String DEFAULT_RESOURCE_DESCRIPTION = "Mysql relational database server";

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
        Set<DiscoveredResourceDetails> servers = new LinkedHashSet<DiscoveredResourceDetails>();

        // Process any auto-discovered resources.
        List<ProcessScanResult> autoDiscoveryResults = context.getAutoDiscoveredProcesses();
        for (ProcessScanResult result : autoDiscoveryResults) {
            log.info("Discovered a mysql process: " + result);

            ProcessInfo procInfo = result.getProcessInfo();
        }

        // Process any manually-added resources.
        List<Configuration> contextPluginConfigurations = context.getPluginConfigurations();
        for (Configuration pluginConfiguration : contextPluginConfigurations) {
            ProcessInfo processInfo = null;
            DiscoveredResourceDetails resourceDetails = createResourceDetails(context, pluginConfiguration, processInfo);
            servers.add(resourceDetails);
        }

        return servers;
    }

    protected static DiscoveredResourceDetails createResourceDetails(ResourceDiscoveryContext discoveryContext,
        Configuration pluginConfiguration,
        ProcessInfo processInfo) {
        String key = buildUrl(pluginConfiguration);
        String db = pluginConfiguration.getSimple(DB_CONFIGURATION_PROPERTY).getStringValue();
        String name = "MySql [" + db + "]";
        String version = getVersion(pluginConfiguration);
        return new DiscoveredResourceDetails(discoveryContext.getResourceType(), key, name, version,
            DEFAULT_RESOURCE_DESCRIPTION, pluginConfiguration, processInfo);
    }

    protected static String buildUrl(Configuration config) {
        String host = config.getSimple(HOST_CONFIGURATION_PROPERTY).getStringValue();
        String port = config.getSimple(PORT_CONFIGURATION_PROPERTY).getStringValue();
        String user = config.getSimple(PRINCIPAL_CONFIGURATION_PROPERTY).getStringValue();
        String pass = config.getSimple(CREDENTIALS_CONFIGURATION_PROPERTY).getStringValue();
        String url = "jdbc:mysql://" + host + "?user=" + user + "&password=" + pass;
        return url;
    }

    protected static String getVersion(Configuration config) {
        String version = null;
        try {
            Connection conn = buildConnection(config);
            version = conn.getMetaData().getDatabaseProductVersion();
        } catch (SQLException e) {
            // TODO GH: How to put this back to the server while inventorying this resource in an unconfigured state
            log.info("Exception detecting mysql instance version", e);
        }
        return version;
    }

    public static Connection buildConnection(Configuration configuration) throws SQLException {
        String driverClass = configuration.getSimple(DRIVER_CONFIGURATION_PROPERTY).getStringValue();
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            throw new InvalidPluginConfigurationException("Specified JDBC driver class (" + driverClass
                + ") not found.");
        }

        String url = buildUrl(configuration);

        return DriverManager.getConnection(url);
    }
}