/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.plugins.server;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.ClassLoaderFacet;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * Just returns the singleton RHQ Server Subsystems parent resource as a container for the
 * subsystem resources.
 * 
 * @author Jay Shaughnessy
 * @author John Mazzitelli
 */
public class JBossAS7JMXDiscoveryComponent<T extends ResourceComponent<JBossAS7JMXComponent<?>>> implements
    ResourceDiscoveryComponent<T>, ClassLoaderFacet<ResourceComponent<JBossAS7JMXComponent<?>>> {

    private static final Log LOG = LogFactory.getLog(JBossAS7JMXDiscoveryComponent.class);

    /**
     * @deprecated as of RHQ4.12, this discovery component should no longer implement the
     * {@link org.rhq.core.pluginapi.inventory.ClassLoaderFacet}
     */
    @Override
    @Deprecated
    public List<URL> getAdditionalClasspathUrls(
        ResourceDiscoveryContext<ResourceComponent<JBossAS7JMXComponent<?>>> context, DiscoveredResourceDetails details)
        throws Exception {
        return Collections.emptyList();
    }

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<T> context) {
        ResourceContext<?> parentResourceContext = context.getParentResourceContext();
        Configuration parentPluginConfig = parentResourceContext.getPluginConfiguration();

        File rhqServerFile = null;
        File clientJarDir = null;

        String homeDirStr = parentPluginConfig.getSimpleValue("homeDir");
        if (homeDirStr != null) {

            File homeDirFile = new File(homeDirStr);
            if (homeDirFile.exists()) {
                clientJarDir = new File(homeDirFile, "bin/client");
                if (!clientJarDir.exists()) {
                    LOG.warn("The client jar location [" + clientJarDir.getAbsolutePath()
                        + "] does not exist - will not be able to connect to the AS7 instance");
                }

                rhqServerFile = new File(homeDirFile, "../bin/rhq-server.sh");
            }
        }

        // If the parent is not an RHQ server then just return.
        // If clientJar was not found then just return
        if (null == rhqServerFile || !rhqServerFile.exists() || !clientJarDir.exists()) {
            return Collections.emptySet();
        }

        String clientJarLocation = clientJarDir.getAbsolutePath();

        String key = "RHQServerSubsystems";
        String name = "RHQ Server Subsystems";
        String version = parentResourceContext.getVersion();
        String description = "Container for RHQ Server Subsystem services";

        Configuration pluginConfig = context.getDefaultPluginConfiguration();

        String hostname = parentPluginConfig.getSimpleValue(JBossAS7JMXComponent.PLUGIN_CONFIG_HOSTNAME, "127.0.0.1");
        pluginConfig.setSimpleValue(JBossAS7JMXComponent.PLUGIN_CONFIG_HOSTNAME, hostname);

        String port = parentPluginConfig.getSimpleValue(JBossAS7JMXComponent.PLUGIN_CONFIG_PORT,
            JBossAS7JMXComponent.DEFAULT_PLUGIN_CONFIG_PORT);
        if (!JBossAS7JMXComponent.DEFAULT_PLUGIN_CONFIG_PORT.equals(port)) {
            port = String.valueOf(Integer.valueOf(port) + 9);
        }
        pluginConfig.setSimpleValue(JBossAS7JMXComponent.PLUGIN_CONFIG_PORT, port);

        String user = parentPluginConfig.getSimpleValue(JBossAS7JMXComponent.PLUGIN_CONFIG_USERNAME, "rhqadmin");
        pluginConfig.setSimpleValue(JBossAS7JMXComponent.PLUGIN_CONFIG_USERNAME, user);

        String password = parentPluginConfig.getSimpleValue(JBossAS7JMXComponent.PLUGIN_CONFIG_PASSWORD, "rhqadmin");
        pluginConfig.setSimpleValue(JBossAS7JMXComponent.PLUGIN_CONFIG_PASSWORD, password);

        pluginConfig.setSimpleValue(JBossAS7JMXComponent.PLUGIN_CONFIG_CLIENT_JAR_LOCATION, clientJarLocation);

        DiscoveredResourceDetails resource = new DiscoveredResourceDetails(context.getResourceType(), key, name,
            version, description, pluginConfig, null);

        return Collections.singleton(resource);
    }
}
