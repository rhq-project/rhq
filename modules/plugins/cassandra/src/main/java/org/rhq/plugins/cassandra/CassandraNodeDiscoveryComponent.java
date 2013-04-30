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
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.support.metadata.J2SE5ConnectionTypeDescriptor;
import org.yaml.snakeyaml.Yaml;

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

    private static final Log log = LogFactory.getLog(CassandraNodeDiscoveryComponent.class);

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

    @SuppressWarnings({ "unchecked", "deprecation" })
    private DiscoveredResourceDetails getDetails(ResourceDiscoveryContext<?> context,
        ProcessScanResult processScanResult) {
        ProcessInfo processInfo = processScanResult.getProcessInfo();

        Configuration pluginConfig = new Configuration();

        String jmxPort = null;


        String[] arguments = processInfo.getCommandLine();
        int classpathIndex = -1;
        for (int i = 0; i < arguments.length; i++) {
            String arg = arguments[i];

            if (arg.startsWith("-Dcom.sun.management.jmxremote.port")) {
                String[] jmxPortArg = arg.split("=");
                jmxPort = jmxPortArg[1];
            }
            if (arg.startsWith("-cp")) {
                classpathIndex = i;
            }
        }

        if (classpathIndex != -1 && classpathIndex + 1 < arguments.length) {
            String[] classpathEntries = arguments[classpathIndex + 1].split(":");

            String yamlConfigurationPath = null;
            for (String classpathEntry : classpathEntries) {
                if (classpathEntry.endsWith("conf")) {
                    yamlConfigurationPath = processInfo.getExecutable().getCwd() + "/" + classpathEntry;
                }
            }

            if (yamlConfigurationPath != null) {

                InputStream inputStream = null;
                try {
                    inputStream = new FileInputStream(new File(yamlConfigurationPath + "/cassandra.yaml"));
                    Yaml yaml = new Yaml();
                    Map<String, String> parsedProperties = (Map<String, String>) yaml.load(inputStream);

                    if (parsedProperties.get("cluster_name") != null) {
                        pluginConfig.put(new PropertySimple("clusterName", parsedProperties.get("cluster_name")));
                    }

                    if (parsedProperties.get("listen_address") != null) {
                        pluginConfig.put(new PropertySimple("host", parsedProperties.get("listen_address")));
                    }

                    if (parsedProperties.get("native_transport_port") != null) {
                        pluginConfig.put(new PropertySimple("nativeTransportPort", parsedProperties
                            .get("native_transport_port")));
                    }
                } catch (Exception e) {
                    log.error("YAML Configuration load exception ", e);
                } finally {
                    try {
                        if ( inputStream != null){
                            inputStream.close();
                        }
                    } catch (Exception e) {
                        log.error("Unable to close stream for yaml configuration", e);
                    }
                }
            }
        }

        if (jmxPort != null) {
            pluginConfig.put(new PropertySimple(JMXDiscoveryComponent.CONNECTION_TYPE,
                J2SE5ConnectionTypeDescriptor.class.getName()));
            pluginConfig.put(new PropertySimple(JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY,
                "service:jmx:rmi:///jndi/rmi://" + pluginConfig.getSimpleValue("host") + ":" + jmxPort + "/jmxrmi"));
        }

        String resourceKey = "Cassandra (" + pluginConfig.getSimpleValue("host") + ") " + jmxPort;
        String resourceName = "Cassandra (" + pluginConfig.getSimpleValue("host") + ")";

        String path = processInfo.getExecutable().getCwd();
        pluginConfig.put(new PropertySimple("baseDir", new File(path).getParentFile().getAbsolutePath()));

        pluginConfig.put(new PropertySimple("username", "cassandra"));
        pluginConfig.put(new PropertySimple("password", "cassandra"));

        return new DiscoveredResourceDetails(context.getResourceType(), resourceKey, resourceName, null, null,
            pluginConfig, processInfo);
    }
}
