/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.iis;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Win32Exception;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.OperatingSystemType;

/**
 *
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class IISServerDiscoveryComponent implements ResourceDiscoveryComponent {

    private static final String REG_INET_SERVICE = "SYSTEM\\CurrentControlSet\\Services\\W3SVC";
    private static final String REG_INET = "SOFTWARE\\Microsoft\\InetStp";
    private static final String REG_INET_MAJORVER = "MajorVersion";
    private static final String REG_INET_MINORVER = "MinorVersion";

    private Log log = LogFactory.getLog(IISServerDiscoveryComponent.class);

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext discoveryContext)
        throws InvalidPluginConfigurationException, Exception {

        if (discoveryContext.getSystemInformation().getOperatingSystemType() != OperatingSystemType.WINDOWS)
            return null;

        String path = null;
        String imagePath = null;
        String version = null;

        try {
            RegistryKey w3svcKey = RegistryKey.LocalMachine.openSubKey(REG_INET_SERVICE);

            imagePath = w3svcKey.getStringValue("ImagePath").trim();
            path = imagePath.substring(0, imagePath.lastIndexOf(File.separator));

            RegistryKey versionInfo = RegistryKey.LocalMachine.openSubKey(REG_INET);
            int majorVersion = versionInfo.getIntValue(REG_INET_MAJORVER);
            int minorVersion = versionInfo.getIntValue(REG_INET_MINORVER);

            version = majorVersion + "." + minorVersion;
        } catch (Win32Exception w32e) {
            log.debug("Could not find a valid installation of IIS");
            return null;
        }

        log.debug("IIS installation found. Path: " + path + " Version: " + version);

        Configuration pluginConfig = discoveryContext.getDefaultPluginConfiguration();
        DiscoveredResourceDetails details = new DiscoveredResourceDetails(discoveryContext.getResourceType(),
            imagePath, "IIS", version, "IIS Server on "
                + discoveryContext.getSystemInformation().getHostname(), pluginConfig, null);

        return Collections.singleton(details);
    }
}
