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
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.system.ProcessInfo;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 * @author Steve Millidge
 */
public class MySqlDiscoveryComponent implements ResourceDiscoveryComponent, ManualAddFacet {
    private static final Log log = LogFactory.getLog(MySqlDiscoveryComponent.class);

    public static final String HOST_CONFIGURATION_PROPERTY = "host";
    public static final String PORT_CONFIGURATION_PROPERTY = "port";
    public static final String DB_CONFIGURATION_PROPERTY = "db";
    public static final String PRINCIPAL_CONFIGURATION_PROPERTY = "principal";
    public static final String CREDENTIALS_CONFIGURATION_PROPERTY = "credentials";

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {

        if (log.isDebugEnabled()) {
            log.debug("Resource Discovery Started");
        }
        Set<DiscoveredResourceDetails> servers = new LinkedHashSet<DiscoveredResourceDetails>();

        // Process any auto-discovered resources.
        List<ProcessScanResult> autoDiscoveryResults = context.getAutoDiscoveredProcesses();
        for (ProcessScanResult result : autoDiscoveryResults) {
            log.info("Discovered a mysql process: " + result);

            ProcessInfo procInfo = result.getProcessInfo();

            servers.add(createResourceDetails(context,context.getDefaultPluginConfiguration(),procInfo));
        }

        return servers;
    }

    public DiscoveredResourceDetails discoverResource(Configuration pluginConfiguration,
                                                      ResourceDiscoveryContext resourceDiscoveryContext)
            throws InvalidPluginConfigurationException {
        ProcessInfo processInfo = null;
        DiscoveredResourceDetails resourceDetails = createResourceDetails(resourceDiscoveryContext, pluginConfiguration,
                processInfo);
        return resourceDetails;
    }

    protected static DiscoveredResourceDetails createResourceDetails(ResourceDiscoveryContext discoveryContext,
        Configuration pluginConfiguration,
        ProcessInfo processInfo) throws InvalidPluginConfigurationException {

        MySqlConnectionInfo ci = buildConnectionInfo(pluginConfiguration);
        Connection conn;
        String version = "";
        try {
            conn = MySqlConnectionManager.getConnectionManager().getConnection(ci);
            version = conn.getMetaData().getDatabaseProductVersion();
        } catch (SQLException ex) {
            // ignore so we can still add to the inventory even though we can't currently connect
        }
       String key = new StringBuilder().append("MySql:")
                .append(ci.getDb())
                .append(":")
                .append(ci.getHost())
                .append(":")
                .append(ci.getPort())
                .append("-")
                .append(ci.getUser()).toString();
        String name = new StringBuilder().append("MySql [")
                .append(ci.getDb())
                .append("]").toString();

        DiscoveredResourceDetails result = new DiscoveredResourceDetails(
                discoveryContext.getResourceType(),
                key,
                name,
                version,
                "MySql Server",
                pluginConfiguration,
                processInfo);

        if (log.isDebugEnabled()) {
           log.debug("Discovered Database Server for MySQL Database " + ci.buildURL());
        }
        return result;

    }

    static MySqlConnectionInfo buildConnectionInfo(Configuration configuration) {
        // build the Discovered Resource from the configuration
        String host = configuration.getSimple(HOST_CONFIGURATION_PROPERTY).getStringValue();
        String port = configuration.getSimple(PORT_CONFIGURATION_PROPERTY).getStringValue();
        String user = configuration.getSimple(PRINCIPAL_CONFIGURATION_PROPERTY).getStringValue();
        String pass = configuration.getSimple(CREDENTIALS_CONFIGURATION_PROPERTY).getStringValue();
        String db   = configuration.getSimple(DB_CONFIGURATION_PROPERTY).getStringValue();
        return new MySqlConnectionInfo(host, port, db, user, pass);
   }
}