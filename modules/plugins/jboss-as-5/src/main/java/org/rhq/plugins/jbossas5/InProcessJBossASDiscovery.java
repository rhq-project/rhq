/*
 * Jopr Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
import org.rhq.plugins.jbossas5.util.ManagedComponentUtils;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;

public class InProcessJBossASDiscovery {

    private final Log log = LogFactory.getLog(this.getClass());

    private static final String DEFAULT_RESOURCE_DESCRIPTION_AS = "JBoss Application Server (AS)";
    private static final String DEFAULT_RESOURCE_DESCRIPTION_EAP = "JBoss Enterprise Application Platform (EAP)";
    private static final String JBMANCON_DEBUG_SYSPROP = "jbmancon.debug";

    @Nullable
    public DiscoveredResourceDetails discoverInProcessJBossAS(ResourceDiscoveryContext discoveryContext) {
        ProfileServiceConnectionProvider connectionProvider = new LocalProfileServiceConnectionProvider();
        ProfileServiceConnection connection;
        try {
            connection = connectionProvider.connect();
        } catch (Exception e) {
            // This most likely just means we're not embedded inside a JBoss AS 5.x instance.
            log.debug("Unable to connect to in-process ProfileService: " + e);
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
        String resourceName = "JBoss ";
        resourceName += installInfo.isEap() ? "EAP " : "AS ";
        resourceName += installInfo.getMajorVersion();
        resourceName += " (" + serverName + ")";

        String description = installInfo.isEap() ? DEFAULT_RESOURCE_DESCRIPTION_EAP : DEFAULT_RESOURCE_DESCRIPTION_AS;

        String version = (String) ManagedComponentUtils.getSimplePropertyValue(serverConfigComponent,
            "specificationVersion");

        Configuration pluginConfig = discoveryContext.getDefaultPluginConfiguration();
        pluginConfig.put(new PropertySimple(PluginConfigUtil.HOME_DIR, homeDir));
        pluginConfig.put(new PropertySimple(PluginConfigUtil.SERVER_HOME_DIR, serverHomeDir));
        pluginConfig.put(new PropertySimple(PluginConfigUtil.SERVER_NAME, serverName));

        boolean debug = Boolean.getBoolean(JBMANCON_DEBUG_SYSPROP);
        if (debug) {
            //new UnitTestRunner().runUnitTests(connection);
        }

        return new DiscoveredResourceDetails(discoveryContext.getResourceType(), resourceKey, resourceName, version,
                description, pluginConfig, null);
    }
}
