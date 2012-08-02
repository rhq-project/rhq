/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.support.metadata.LocalVMTypeDescriptor;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jmx.JMXDiscoveryComponent;

/**
 * Discover individual hadoop processes
 * @author Heiko W. Rupp
 * @author Lukas Krejci
 */
public class HadoopServerDiscovery implements ResourceDiscoveryComponent<ResourceComponent<?>> {

    private final Log log = LogFactory.getLog(HadoopServerDiscovery.class);
    private static final String HADOOP_VERSION_MATCH = "hadoop-core-([0-9\\.]+)\\.jar";
    private static final Pattern HADOOP_VERSION_PATTERN = Pattern.compile(HADOOP_VERSION_MATCH);
    private static final String MAIN_CLASS_PROPERTY = "_mainClass";
    public static final String HOME_DIR_PROPERTY = "hadoop.home.dir";
    private static final String HOME_DIR_OPTION = "-Dhadoop.home.dir";

    public Set<DiscoveredResourceDetails> discoverResources(
        ResourceDiscoveryContext<ResourceComponent<?>> resourceDiscoveryContext)
        throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();

        List<ProcessScanResult> processScans = resourceDiscoveryContext.getAutoDiscoveredProcesses();
        ResourceType resourceType = resourceDiscoveryContext.getResourceType();
        String rtName = resourceType.getName();

        for (ProcessScanResult psr : processScans) {

            String cwd = psr.getProcessInfo().getCurrentWorkingDirectory();

            String homeDir = getHadoopHomeDirIfAvailable(psr.getProcessInfo().getCommandLine());
            if (homeDir == null) {
                homeDir = cwd;
            }

            String version = getVersion(homeDir);

            Configuration pluginConfiguration = resourceDiscoveryContext.getDefaultPluginConfiguration();

            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(resourceType, // ResourceType
                homeDir, // ResourceKey
                rtName, // resource name
                version, // Version
                rtName + " ( " + cwd + " )", // description
                pluginConfiguration, psr.getProcessInfo() // process info
                );

            /*
             * We'll connect to the discovered VM on the local host, so set the jmx connection
             * properties accordingly. This may only work on JDK6+, but then JDK5 is deprecated
             * anyway.
             */
            pluginConfiguration.put(new PropertySimple(JMXDiscoveryComponent.COMMAND_LINE_CONFIG_PROPERTY,
                pluginConfiguration.getSimpleValue(MAIN_CLASS_PROPERTY, null)));
            pluginConfiguration.put(new PropertySimple(JMXDiscoveryComponent.CONNECTION_TYPE,
                LocalVMTypeDescriptor.class.getName()));

            pluginConfiguration.put(new PropertySimple(HOME_DIR_PROPERTY, homeDir));

            log.debug("Discovered " + detail);

            details.add(detail);
        }

        return details;
    }

    /**
     * Get hadoop version
     * from command line by looking at haoop-core-xx-core.jar
     * @param commandLine Command line args for the java executable
     * @return hdoop version string or null if it can not be determined
     */
    private String getVersion(String hadoopHomeDir) {

        File homeDir = new File(hadoopHomeDir);
        if (homeDir.isDirectory() && homeDir.canRead()) {
            String[] foundCoreJars = homeDir.list(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return HADOOP_VERSION_PATTERN.matcher(name).matches();
                }
            });

            if (foundCoreJars == null || foundCoreJars.length == 0) {
                return null;
            }

            Matcher matcher = HADOOP_VERSION_PATTERN.matcher(foundCoreJars[0]);
            if (matcher.matches()) {
                return matcher.group(1);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private String getHadoopHomeDirIfAvailable(String[] cmdline) {
        for (int i = 0; i < cmdline.length; ++i) {
            String cmd = cmdline[i];
            if (cmd.startsWith(HOME_DIR_OPTION)) {
                int eqPos = cmd.indexOf('=');
                if (eqPos > 0) {
                    return cmd.substring(eqPos + 1);
                }
            }
        }

        return null;
    }
}
