/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.modules.plugins.jbossas7;

import java.io.File;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceUpgradeReport;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeContext;
import org.rhq.core.system.ProcessInfo;
import org.rhq.modules.plugins.jbossas7.helper.AdditionalJavaOpts;
import org.rhq.modules.plugins.jbossas7.helper.HostPort;

/**
 * Discovery component for "JBossAS7 Standalone Server" Resources.
 *
 * @author Ian Springer
 */
public class StandaloneASDiscovery extends BaseProcessDiscovery {

    private static final Log log = LogFactory.getLog(BaseProcessDiscovery.class);

    private static final boolean OS_IS_WINDOWS = (File.separatorChar == '\\');

    private static final String SERVER_BASE_DIR_SYSPROP = "jboss.server.base.dir";
    private static final String SERVER_CONFIG_DIR_SYSPROP = "jboss.server.config.dir";
    private static final String SERVER_LOG_DIR_SYSPROP = "jboss.server.log.dir";

    private static final String HOME_DIR_PROP = "homeDir";
    private static final String JAVA_OPTS_ADDITIONAL_PROP = "javaOptsAdditional";

    @Override
    protected AS7Mode getMode() {
        return AS7Mode.STANDALONE;
    }

    @Override
    protected String getBaseDirSystemPropertyName() {
        return SERVER_BASE_DIR_SYSPROP;
    }

    @Override
    protected String getConfigDirSystemPropertyName() {
        return SERVER_CONFIG_DIR_SYSPROP;
    }

    @Override
    protected String getLogDirSystemPropertyName() {
        return SERVER_LOG_DIR_SYSPROP;
    }

    @Override
    protected String getDefaultBaseDirName() {
        return "standalone";
    }

    @Override
    protected String getLogFileName() {
        return "server.log";
    }

    @Override
    protected String buildDefaultResourceName(HostPort hostPort, HostPort managementHostPort,
        JBossProductType productType, String serverName) {
        if (serverName != null && !serverName.trim().isEmpty()) {
            return String.format("%s (%s %s:%d)", productType.SHORT_NAME, serverName, managementHostPort.host,
                managementHostPort.port);
        }

        return String.format("%s (%s:%d)", productType.SHORT_NAME, managementHostPort.host, managementHostPort.port);
    }

    @Override
    protected String buildDefaultResourceDescription(HostPort hostPort, JBossProductType productType) {
        return String.format("Standalone %s server", productType.FULL_NAME);
    }

    @Override
    protected ProcessInfo getPotentialStartScriptProcess(ProcessInfo process) {
        // If the server was started via standalone.sh/bat, its parent process will be standalone.sh/bat.
        return process.getParentProcess();
    }

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext discoveryContext) throws Exception {
        Set<DiscoveredResourceDetails> discoveredResources = super.discoverResources(discoveryContext);

        for (DiscoveredResourceDetails discoveredResource : discoveredResources) {
            discoverAdditionalJavaOpts(discoveredResource);
        }

        return discoveredResources;
    }

    @Override
    public DiscoveredResourceDetails discoverResource(Configuration pluginConfig, ResourceDiscoveryContext context)
        throws InvalidPluginConfigurationException {
        DiscoveredResourceDetails discoveredResource = super.discoverResource(pluginConfig, context);

        discoverAdditionalJavaOpts(discoveredResource);

        return discoveredResource;
    }

    @Override
    public ResourceUpgradeReport upgrade(ResourceUpgradeContext inventoriedResource) {
        // TODO Auto-generated method stub
        ResourceUpgradeReport resourceUpgradeReport = super.upgrade(inventoriedResource);

        return resourceUpgradeReport;
    }

    private void discoverAdditionalJavaOpts(DiscoveredResourceDetails discoveredResource) {
        File baseDirectory = new File(discoveredResource.getPluginConfiguration().getSimpleValue(HOME_DIR_PROP));
        File binDirectory = new File(baseDirectory, "bin");

        String javaOptsAdditionalValue = null;
        File configFile = null;
        AdditionalJavaOpts additionalJavaOptsConfig = null;

        if (OS_IS_WINDOWS) {
            configFile = new File(binDirectory, "standalone.conf.bat");
            additionalJavaOptsConfig = new AdditionalJavaOpts.WindowsConfiguration();
        }else {
            configFile = new File(binDirectory, "standalone.conf");
            additionalJavaOptsConfig = new AdditionalJavaOpts.LinuxConfiguration();
        }

        try {
            javaOptsAdditionalValue = additionalJavaOptsConfig.discover(configFile);
        } catch (Exception e) {
            log.error("Unable to discover additional JAVA_OPTS set via RHQ from configuration file.", e);
        }

        discoveredResource.getPluginConfiguration().setSimpleValue(JAVA_OPTS_ADDITIONAL_PROP, javaOptsAdditionalValue);
    }
}
