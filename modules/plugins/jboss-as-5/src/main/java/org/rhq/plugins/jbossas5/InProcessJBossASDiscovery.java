/*
 * Jopr Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jbossas5.connection.LocalProfileServiceConnectionProvider;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnection;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnectionProvider;
import org.rhq.plugins.jbossas5.helper.JBossInstallationInfo;
import org.rhq.plugins.jbossas5.helper.JBossProductType;
import org.rhq.plugins.jbossas5.util.ManagedComponentUtils;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;

/**
 * A helper class for discovering an in-process JBoss app server.
 */
public class InProcessJBossASDiscovery {
    private final Log log = LogFactory.getLog(this.getClass());

    private static final String JAVA_HOME_ENV_VAR = "JAVA_HOME";

    /**
     * Attempts to discover an in-process JBoss AS or EAP instance. If successful, returns the Resource details,
     * otherwise, returns null.
     *
     * @param discoveryContext the Resource discovery context
     *
     * @return if successful, the Resource details, otherwise, null
     */
    @Nullable
    public DiscoveredResourceDetails discoverInProcessJBossAS(ResourceDiscoveryContext discoveryContext) {
        ProfileServiceConnectionProvider connectionProvider = new LocalProfileServiceConnectionProvider();
        ProfileServiceConnection connection;
        try {
            connection = connectionProvider.connect();
        } catch (Exception e) {
            // This most likely just means we're not embedded inside a JBoss 5.x or 6.x app server instance.
            log.debug("Unable to connect to in-process ProfileService.", e);
            return null;
        }

        ManagementView managementView = connection.getManagementView();
        ManagedComponent serverConfigComponent = ManagedComponentUtils.getSingletonManagedComponent(managementView,
            new ComponentType("MCBean", "ServerConfig"));
        String serverName = (String) ManagedComponentUtils.getSimplePropertyValue(serverConfigComponent, "serverName");

        // serverHomeDir is the full path to the instance's configuration dir, e.g. "/opt/jboss-5.1.0.GA/server/default";
        // That's guaranteed to be unique among JBAS instances on the same machine, so we'll use it as the Resource key.
        String serverHomeDir = (String) ManagedComponentUtils.getSimplePropertyValue(serverConfigComponent,
            "serverHomeDir");
        String resourceKey = serverHomeDir;

        // homeDir is the full path to the JBoss installation dir used by this instance, e.g. "/opt/jboss-5.1.0.GA".
        String homeDir = (String) ManagedComponentUtils.getSimplePropertyValue(serverConfigComponent, "homeDir");
        // Figure out if the instance is AS or EAP, and reflect that in the Resource name.
        JBossInstallationInfo installInfo;
        try {
            installInfo = new JBossInstallationInfo(new File(homeDir));
        } catch (IOException e) {
            throw new InvalidPluginConfigurationException(e);
        }

        JBossProductType productType = installInfo.getProductType();
        String resourceName = productType.NAME;
        resourceName += " " + installInfo.getMajorVersion();
        resourceName += " (" + serverName + ")";

        String description = productType.DESCRIPTION;

        String version = (String) ManagedComponentUtils.getSimplePropertyValue(serverConfigComponent,
            "specificationVersion");

        // TODO (ips): Perhaps we should use the java.home sysprop instead of the JAVA_HOME env var, since it may be 
        //             slightly more reliable.
        String javaHome = System.getenv(JAVA_HOME_ENV_VAR);

        Configuration pluginConfig = discoveryContext.getDefaultPluginConfiguration();
        pluginConfig.put(new PropertySimple(ApplicationServerPluginConfigurationProperties.HOME_DIR, homeDir));
        pluginConfig.put(new PropertySimple(ApplicationServerPluginConfigurationProperties.SERVER_HOME_DIR,
                serverHomeDir));
        pluginConfig.put(new PropertySimple(ApplicationServerPluginConfigurationProperties.SERVER_NAME, serverName));
        pluginConfig.put(new PropertySimple(ApplicationServerPluginConfigurationProperties.JAVA_HOME, javaHome));

        return new DiscoveredResourceDetails(discoveryContext.getResourceType(), resourceKey, resourceName, version,
                description, pluginConfig, null);
    }
}
