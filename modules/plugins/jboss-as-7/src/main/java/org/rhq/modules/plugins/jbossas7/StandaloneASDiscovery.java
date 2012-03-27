/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7;

import java.io.File;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;

/**
 * Discovery component for "JBossAS7 Standalone Server" Resources.
 *
 * @author Ian Springer
 */
public class StandaloneASDiscovery extends BaseProcessDiscovery {

    private static final String SERVER_BASE_DIR_SYSPROP = "jboss.server.base.dir";
    private static final String SERVER_CONFIG_DIR_SYSPROP = "jboss.server.config.dir";

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
    protected String getDefaultBaseDirName() {
        return "standalone";
    }

    @Override
    protected String getHostXmlFileName() {
        return "standalone.xml";
    }

    @Override
    protected String buildDefaultResourceName(HostPort hostPort, String configName, JBossProductType productType) {
        String hostName = findHostName();
        StringBuilder name = new StringBuilder();
        name.append(configName).append(" ").append(productType.SHORT_NAME);
        if ((hostName != null) && !hostName.isEmpty()) {
            name.append(" (").append(hostName).append(")");
        }
        return name.toString();
    }

    @Override
    protected String buildDefaultResourceDescription(HostPort hostPort, JBossProductType productType) {
        return "Standalone " + productType.FULL_NAME + " server";
    }

    @Override
    protected File getLogFile(ProcessInfo process) {
        String bootLogFile = getLogFileFromCommandLine(process.getCommandLine());
        File logDir = new File(bootLogFile).getParentFile();
        return new File(logDir, "server.log");
    }

    @Override
    protected HostPort getManagementPortFromHostXml(String[] commandLine) {
        HostPort managementPort = super.getManagementPortFromHostXml(commandLine);
        return checkForSocketBindingOffset(managementPort, commandLine);
    }

    @Override
    protected DiscoveredResourceDetails buildResourceDetails(ResourceDiscoveryContext discoveryContext,
                                                             ProcessScanResult psr) {
        DiscoveredResourceDetails details = super.buildResourceDetails(discoveryContext, psr);
        Configuration pluginConfig = details.getPluginConfiguration();       
        pluginConfig.put(new PropertySimple("config", getHostXmlFile(psr.getProcessInfo())));
        return details;
    }

}
