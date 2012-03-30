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
package org.rhq.modules.plugins.jbossas7;

import java.io.File;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;

/**
 * Discovery component for "JBossAS7 Host Controller" Resources.
 *
 * @author Ian Springer
 */
public class HostControllerDiscovery extends BaseProcessDiscovery {

    private static final String DOMAIN_BASE_DIR_SYSPROP = "jboss.domain.base.dir";
    private static final String DOMAIN_CONFIG_DIR_SYSPROP = "jboss.domain.config.dir";
    private static final String DOMAIN_LOG_DIR_SYSPROP = "jboss.domain.log.dir";

    private AS7CommandLineOption HOST_CONFIG_OPTION = new AS7CommandLineOption(null, "host-config");

    @Override
    protected AS7Mode getMode() {
        return AS7Mode.DOMAIN;
    }

    @Override
    protected String getBaseDirSystemPropertyName() {
        return DOMAIN_BASE_DIR_SYSPROP;
    }

    @Override
    protected String getConfigDirSystemPropertyName() {
        return DOMAIN_CONFIG_DIR_SYSPROP;
    }

    @Override
    protected String getLogDirSystemPropertyName() {
        return DOMAIN_LOG_DIR_SYSPROP;
    }

    @Override
    protected String getDefaultBaseDirName() {
        return "domain";
    }

    @Override
    protected AS7CommandLineOption getHostXmlFileNameOption() {
        return HOST_CONFIG_OPTION;
    }

    @Override
    protected String getDefaultHostXmlFileName() {
        return "host.xml";
    }

    @Override
    protected String getLogFileName() {
        return "host-controller.log";
    }

    @Override
    protected String buildDefaultResourceName(HostPort hostPort, String configName, JBossProductType productType) {
        boolean isDomainController = hostPort.isLocal;
        String suffix = (isDomainController) ? "Domain Controller" : "Host Controller";
        return configName + " " + productType.NAME + " " + suffix;
    }

    @Override
    protected String buildDefaultResourceDescription(HostPort hostPort, JBossProductType productType) {
        boolean isDomainController = hostPort.isLocal;
        String prefix = (isDomainController) ? "Domain controller" : "Host controller";
        String suffix = (isDomainController) ? "domain" : "host";
        return prefix + " for a " + productType.FULL_NAME + " " + suffix;
    }

    @Override
    protected DiscoveredResourceDetails buildResourceDetails(ResourceDiscoveryContext discoveryContext,
                                                             ProcessScanResult psr) throws Exception {
        DiscoveredResourceDetails details = super.buildResourceDetails(discoveryContext, psr);
        ProcessInfo process = psr.getProcessInfo();
        Configuration pluginConfig = details.getPluginConfiguration();
        String domainConfig = getServerConfigFromCommandLine(psr.getProcessInfo().getCommandLine(), getMode());
        pluginConfig.put(new PropertySimple("domainConfig", domainConfig));
        pluginConfig.put(new PropertySimple("hostConfig", getHostXmlFileName(process)));
        return details;
    }

}
