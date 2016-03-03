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
package org.rhq.modules.plugins.wildfly10;

import org.rhq.core.system.ProcessInfo;
import org.rhq.modules.plugins.wildfly10.helper.HostPort;

/**
 * Discovery component for "JBossAS7 Host Controller" Resources.
 *
 * @author Ian Springer
 */
public class HostControllerDiscovery extends BaseProcessDiscovery {

    private static final String DOMAIN_BASE_DIR_SYSPROP = "jboss.domain.base.dir";
    private static final String DOMAIN_CONFIG_DIR_SYSPROP = "jboss.domain.config.dir";
    private static final String DOMAIN_LOG_DIR_SYSPROP = "jboss.domain.log.dir";

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
    protected String getLogFileName() {
        return "host-controller.log";
    }

    @Override
    protected String buildDefaultResourceName(HostPort hostPort, HostPort managementHostPort,
        JBossProductType productType, String serverName) {
        boolean isDomainController = hostPort.isLocal;
        String instanceType = (isDomainController) ? "Domain Controller" : "Host Controller";

        if (serverName != null && !serverName.trim().isEmpty()) {
            return String.format("%s %s (%s %s:%d)", productType.NAME, instanceType, serverName,
                managementHostPort.host, managementHostPort.port);
        }

        return String.format("%s %s (%s:%d)", productType.NAME, instanceType, managementHostPort.host,
            managementHostPort.port);
    }

    @Override
    protected String buildDefaultResourceDescription(HostPort hostPort, JBossProductType productType) {
        boolean isDomainController = hostPort.isLocal;
        String prefix = (isDomainController) ? "Domain controller" : "Host controller";
        String suffix = (isDomainController) ? "domain" : "host";
        return String.format("%s for a %s %s", prefix, productType.FULL_NAME, suffix);
    }

    @Override
    protected ProcessInfo getPotentialStartScriptProcess(ProcessInfo serverProcess) {
        // If the server was started via domain.sh/bat, its parent process will be the process controller JVM, and the
        // process controller JVM's parent process will be domain.sh/bat.
        return serverProcess.getParentProcess().getParentProcess();
    }

}
