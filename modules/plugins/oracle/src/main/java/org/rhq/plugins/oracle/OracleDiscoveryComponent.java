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
import java.sql.DatabaseMetaData;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.system.ProcessInfo;


/**
 * @author Greg Hinkle
 */
public class OracleDiscoveryComponent implements ResourceDiscoveryComponent, ManualAddFacet {
    private static Log log = LogFactory.getLog(OracleDiscoveryComponent.class);

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext resourceDiscoveryContext)
        throws InvalidPluginConfigurationException, Exception {
        Set<DiscoveredResourceDetails> found = new HashSet<DiscoveredResourceDetails>();
        List<ProcessScanResult> autoDiscoveryResults = resourceDiscoveryContext.getAutoDiscoveredProcesses();
        for (ProcessScanResult process : autoDiscoveryResults) {
            String sid = process.getProcessInfo().getEnvironmentVariable("ORACLE_SID");
            if ((sid == null) || (sid.length() == 0)) {
                log.info("Unable to discover Oracle instance SID. Use manual inventory to complete setup.");
                continue;
            }

            String pwd = process.getProcessInfo().getExecutable().getCwd();

            String version = null;
            if (pwd.contains("10.2.0")) {
                version = "10.2.0";
            }

            Configuration pluginConfig = resourceDiscoveryContext.getDefaultPluginConfiguration().deepCopy();
            pluginConfig.put(new PropertySimple("sid", sid));

            DiscoveredResourceDetails details = createResourceDetails(resourceDiscoveryContext, pluginConfig, version,
                process.getProcessInfo());
            found.add(details);
        }

        return found;
    }

    public DiscoveredResourceDetails discoverResource(Configuration pluginConfig,
                                                      ResourceDiscoveryContext resourceDiscoveryContext)
            throws InvalidPluginConfigurationException {
        try {
            Connection connection = OracleServerComponent.buildConnection(pluginConfig);
            DatabaseMetaData dbmd = connection.getMetaData();
            String version = dbmd.getDatabaseMajorVersion() + "." + dbmd.getDatabaseMinorVersion();
            DiscoveredResourceDetails details = createResourceDetails(resourceDiscoveryContext, pluginConfig,
                version, null);
            return details;
        } catch (Exception e) {
            log.warn("Could not connect to oracle with supplied configuration", e);
            throw new InvalidPluginConfigurationException("Unable to connect to Oracle",e);
        }
    }

    private static DiscoveredResourceDetails createResourceDetails(ResourceDiscoveryContext discoveryContext,
        Configuration pluginConfig, String version, @Nullable
        ProcessInfo processInfo) {
        String key = pluginConfig.getSimpleValue("sid", "XE");
        String name = key;
        String description = "Oracle " + version + " (" + key + ")";
        return new DiscoveredResourceDetails(discoveryContext.getResourceType(), key, name, version, description,
            pluginConfig, processInfo);
    }
}