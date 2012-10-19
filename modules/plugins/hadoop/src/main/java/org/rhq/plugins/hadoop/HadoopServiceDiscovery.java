/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.plugins.hadoop;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.support.metadata.InternalVMTypeDescriptor;
import org.mc4j.ems.connection.support.metadata.J2SE5ConnectionTypeDescriptor;
import org.mc4j.ems.connection.support.metadata.LocalVMTypeDescriptor;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jmx.JMXDiscoveryComponent;

/**
 * Discover individual hadoop processes
 * @author Heiko W. Rupp
 */
public class HadoopServiceDiscovery implements ResourceDiscoveryComponent {

    private final Log log = LogFactory.getLog(HadoopServiceDiscovery.class);
    private static final String HADOOP_VERSION_MATCH = ".*hadoop-([0-9\\.]+)-core.jar.*";
    private Pattern hadoopPattern = Pattern.compile(HADOOP_VERSION_MATCH);

    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext resourceDiscoveryContext) throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();

        @SuppressWarnings("unchecked")
        List<ProcessScanResult> parentProcessScans = resourceDiscoveryContext.getParentResourceContext().getNativeProcessesForType();
        ResourceType resourceType = resourceDiscoveryContext.getResourceType();
        String rtName = resourceType.getName();

        for (ProcessScanResult psr : parentProcessScans) {


            if (psr.getProcessScan().getName().equals(rtName)) {

                String[] commandLineArgs = psr.getProcessInfo().getCommandLine();
                String version = getVersion(commandLineArgs);
                String javaClazz = getClazzFromCommandLine(commandLineArgs);

                Configuration pluginConfiguration = resourceDiscoveryContext.getDefaultPluginConfiguration();
                DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                        resourceType, // ResourceType
                        rtName+":"+psr.getProcessInfo().getCurrentWorkingDirectory(), // ResourceKey
                        rtName, // resource name
                        version, // Version
                        "Hadoop " + rtName + " ( "+  psr.getProcessInfo().getCurrentWorkingDirectory() +" )", // description
                        pluginConfiguration,
                        psr.getProcessInfo() // process info
                );


                /*
                 * We'll connect to the discovered VM on the local host, so set the jmx connection
                 * properties accordingly. This may only work on JDK6+, but then JDK5 is deprecated
                 * anyway.
                 */
                pluginConfiguration.put(new PropertySimple(JMXDiscoveryComponent.COMMAND_LINE_CONFIG_PROPERTY,
                        javaClazz));
                pluginConfiguration.put(new PropertySimple(JMXDiscoveryComponent.CONNECTION_TYPE,
                    LocalVMTypeDescriptor.class.getName()));

                log.info("Discovered " + detail);
                details.add(detail);
            }
        }

        return details;
    }

    /**
     * Get the full class name of the java class that 'jps -l' would list
     * @param commandLineArgs Command line args for the java executable
     * @return full class name for the server class
     */
    private String getClazzFromCommandLine(String[] commandLineArgs) {
        for (String line : commandLineArgs) {
            if (line.startsWith("org.apache.hadoop."))
                return line;
        }
        return "-not found-";
    }

    /**
     * Get hadoop version
     * from command line by looking at haoop-core-xx-core.jar
     * @param commandLine Command line args for the java executable
     * @return hdoop version string or null if it can not be determined
     */
    private String getVersion(String[] commandLine) {

        for (String line : commandLine) {
            Matcher m = hadoopPattern.matcher(line);
            if (m.matches()) {
                String result = m.group(1);
                return result;
            }
        }

        return null;
    }


}
