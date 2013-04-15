/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.plugins.cassandra;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mc4j.ems.connection.support.metadata.J2SE5ConnectionTypeDescriptor;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;
import org.rhq.plugins.jmx.JMXDiscoveryComponent;

/**
 * @author John Sanda
 */
public class CassandraNodeDiscoveryComponent extends JMXDiscoveryComponent {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();
        List<ProcessScanResult> processScanResults = context.getAutoDiscoveredProcesses();

        for (ProcessScanResult processScanResult : processScanResults) {
            DiscoveredResourceDetails discoveredDetails = getDetails(context, processScanResult);
            if (discoveredDetails != null) {
                details.add(discoveredDetails);
            }
        }

        return details;
    }

    private DiscoveredResourceDetails getDetails(ResourceDiscoveryContext<?> context,
        ProcessScanResult processScanResult) {
        ProcessInfo processInfo = processScanResult.getProcessInfo();
        String jmxPort = null;

        for (String arg : processInfo.getCommandLine()) {
            if (arg.startsWith("-Dcom.sun.management.jmxremote.port")) {
                String[] jmxPortArg = arg.split("=");
                jmxPort = jmxPortArg[1];
                break;
            }
        }

        if (jmxPort == null) {
            return null;
        }

        String resourceKey = "CassandraDaemon:" + jmxPort;
        String resourceName = "CassandraDaemon";

        Configuration pluginConfig = new Configuration();
        pluginConfig.put(new PropertySimple(JMXDiscoveryComponent.CONNECTION_TYPE,
            J2SE5ConnectionTypeDescriptor.class.getName()));
        pluginConfig.put(new PropertySimple(JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY,
            "service:jmx:rmi:///jndi/rmi://127.0.0.1:" + jmxPort + "/jmxrmi"));

        String path = processInfo.getExecutable().getCwd();
        pluginConfig.put(new PropertySimple("baseDir", new File(path).getParentFile().getAbsolutePath()));

        pluginConfig.put(new PropertySimple("username", "cassandra"));
        pluginConfig.put(new PropertySimple("password", "cassandra"));

        return new DiscoveredResourceDetails(context.getResourceType(), resourceKey, resourceName, null, null,
            pluginConfig, processInfo);
    }
}
