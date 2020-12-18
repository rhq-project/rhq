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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.ProcExe;
import org.mc4j.ems.connection.support.metadata.J2SE5ConnectionTypeDescriptor;
import org.rhq.cassandra.util.ConfigEditor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceUpgradeReport;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeContext;
import org.rhq.core.system.ProcessInfo;
import org.rhq.plugins.jmx.JMXDiscoveryComponent;

/**
 * @author John Sanda
 */
public class CassandraNodeDiscoveryComponent extends JMXDiscoveryComponent {

    private static final Log log = LogFactory.getLog(CassandraNodeDiscoveryComponent.class);

    protected static final String HOST_PROPERTY = "host";
    protected static final String CLUSTER_NAME_PROPERTY = "clusterName";
    protected static final String JMX_PORT_PROPERTY = "jmxPort";
    protected static final String AUTHENTICATOR_PROPERTY = "authenticator";
    protected static final String YAML_PROPERTY = "yamlConfiguration";
    protected static final String BASEDIR_PROPERTY = "baseDir";

    protected static final String DEFAULT_RHQ_CLUSTER = "rhq";

    private static final String RESOURCE_NAME = "Cassandra";


    @SuppressWarnings({ "rawtypes" })
    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
        Set<DiscoveredResourceDetails> cassandraNodes = new HashSet<DiscoveredResourceDetails>();

        for (DiscoveredResourceDetails discoveredResource : this.scanForResources(context)) {
            if (isCassandraNode(discoveredResource)) {
                cassandraNodes.add(discoveredResource);
            }
        }

        return cassandraNodes;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Set<DiscoveredResourceDetails> scanForResources(ResourceDiscoveryContext context) {
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

    protected boolean isCassandraNode(DiscoveredResourceDetails discoveredResource) {
        if (DEFAULT_RHQ_CLUSTER.equals(discoveredResource.getPluginConfiguration()
            .getSimpleValue(CLUSTER_NAME_PROPERTY))) {
            return false;
        }

        return true;
    }

    @SuppressWarnings({ "deprecation" })
    private DiscoveredResourceDetails getDetails(ResourceDiscoveryContext<?> context,
        ProcessScanResult processScanResult) {

        Configuration pluginConfig = context.getDefaultPluginConfiguration();

        String jmxPort = null;
        StringBuilder commandLineBuilder = new StringBuilder(400);
        int classpathIndex = -1;

        ProcessInfo processInfo = processScanResult.getProcessInfo();
        String[] arguments = processInfo.getCommandLine();
        for (int i = 0; i < arguments.length; i++) {
            String arg = arguments[i];

            if (arg.startsWith("-Dcassandra.jmx.local.port") ||
                arg.startsWith("-Dcom.sun.management.jmxremote.port")) {

                String[] jmxPortArg = arg.split("=");
                jmxPort = jmxPortArg[1];
            }
            if (arg.startsWith("-cp") || (arg.startsWith("-classpath"))) {
                classpathIndex = i;
            }

            commandLineBuilder.append(arg);
            commandLineBuilder.append(' ');
        }

        ProcExe exec = processInfo.priorSnaphot().getExecutable();
        String cwd;
        String baseDir;
        if (exec == null) {
            cwd = null;
            baseDir = "/";
        } else {
            cwd = exec.getCwd();
            baseDir = new File(cwd).getParentFile().getAbsolutePath();
        }

        pluginConfig.put(new PropertySimple(COMMAND_LINE_CONFIG_PROPERTY, commandLineBuilder.toString()));

        if (classpathIndex != -1 && classpathIndex + 1 < arguments.length) {
            String[] classpathEntries = arguments[classpathIndex + 1].split(File.pathSeparator);

            File yamlConfigurationPath = null;
            for (String classpathEntry : classpathEntries) {
                if (classpathEntry.endsWith("conf")) {
                    yamlConfigurationPath = new File(classpathEntry);
                    if (!yamlConfigurationPath.isAbsolute()) {
                        try {
                            //relative path, use process CWD to find absolute path of the conf directory
                            yamlConfigurationPath = new File(cwd, classpathEntry);
                        } catch (Exception e) {
                            log.error("Error creating path for yaml file.", e);
                        }
                    }
                } else if (classpathEntry.contains("/lib/apache-cassandra-") && baseDir == null) {
                    baseDir = new File(classpathEntry).getParentFile().getAbsolutePath();
                }
            }

            if (yamlConfigurationPath != null) {
                File yamlConfigurationFile = new File(yamlConfigurationPath, "cassandra.yaml");
                ConfigEditor yamlEditor = new ConfigEditor(yamlConfigurationFile);
                yamlEditor.load();

                pluginConfig.put(new PropertySimple(YAML_PROPERTY, yamlConfigurationFile.getAbsolutePath()));
                pluginConfig.put(new PropertySimple(CLUSTER_NAME_PROPERTY, yamlEditor.getClusterName()));
                pluginConfig.put(new PropertySimple(HOST_PROPERTY, yamlEditor.getListenAddress()));
                pluginConfig.put(new PropertySimple(AUTHENTICATOR_PROPERTY, yamlEditor.getAuthenticator()));
            }
        }

        if (jmxPort != null) {
            pluginConfig.put(new PropertySimple(JMX_PORT_PROPERTY, jmxPort));

            pluginConfig.put(new PropertySimple(JMXDiscoveryComponent.CONNECTION_TYPE,
                J2SE5ConnectionTypeDescriptor.class.getName()));
            pluginConfig.put(new PropertySimple(JMXDiscoveryComponent.CONNECTOR_ADDRESS_CONFIG_PROPERTY,
                "service:jmx:rmi:///jndi/rmi://" + pluginConfig.getSimpleValue(HOST_PROPERTY) + ":" + jmxPort
                    + "/jmxrmi"));
        }

        String resourceKey = "Cassandra (" + pluginConfig.getSimpleValue(HOST_PROPERTY) + ") " + jmxPort;
        String resourceName = RESOURCE_NAME;

        pluginConfig.put(new PropertySimple(BASEDIR_PROPERTY, baseDir));

        return new DiscoveredResourceDetails(context.getResourceType(), resourceKey, resourceName, null, null,
            pluginConfig, processInfo);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public ResourceUpgradeReport upgrade(ResourceUpgradeContext inventoriedResource) {
        // don't use super's impl because the resource key is not a JvmResourceKey
        return null;
    }

}
