/*
 * JBoss, a division of Red Hat.
 * Copyright 2007, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.plugins.iis;

import org.hyperic.sigar.win32.RegistryKey;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.system.OperatingSystemType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Set;
import java.util.Collections;
import java.io.File;

/**
 *
 * @author Greg Hinkle
 */
public class IISServerDiscoveryComponent implements ResourceDiscoveryComponent {


    private static final String REG_INET_SERVICE = "SYSTEM\\CurrentControlSet\\Services\\W3SVC";
    private static final String REG_INET = "SOFTWARE\\Microsoft\\InetStp";
    private static final String REG_INET_MAJORVER = "MajorVersion";
    private static final String REG_INET_MINORVER = "MinorVersion";

    private Log log = LogFactory.getLog(IISServerDiscoveryComponent.class);

    
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext discoveryContext) throws InvalidPluginConfigurationException, Exception {

        if (discoveryContext.getSystemInformation().getOperatingSystemType() != OperatingSystemType.WINDOWS.WINDOWS)
            return null;

        RegistryKey w3svcKey = RegistryKey.LocalMachine.openSubKey(REG_INET_SERVICE);

        String imagePath = w3svcKey.getStringValue("ImagePath").trim();

        String path = imagePath.substring(0, imagePath.lastIndexOf(File.separator));

        RegistryKey versionInfo = RegistryKey.LocalMachine.openSubKey(REG_INET);
        int majorVersion = versionInfo.getIntValue(REG_INET_MAJORVER);
        int minorVersion = versionInfo.getIntValue(REG_INET_MINORVER);

        String version = majorVersion + "." + minorVersion;

        log.debug("IIS installation found. Path: " + path + " Version: " + version);

        Configuration pluginConfig = discoveryContext.getDefaultPluginConfiguration();
        DiscoveredResourceDetails details =
                new DiscoveredResourceDetails(
                        discoveryContext.getResourceType(),
                        imagePath,
                        discoveryContext.getSystemInformation().getHostname() + " IIS Server",
                version,
                        "IIS Server on " + discoveryContext.getSystemInformation().getHostname(),
                        pluginConfig, null);

        return Collections.singleton(details);
    }


    /*
    SVN Project - Server
      SVN Tree - Trunk

    RHQ Project                      [SVN Project]
      trunk                          [SVN Tree]
      branches/API-REFACTORING       [SVN Tree]
      ghinkle                        [SVN User]
    */

    public static void main(String[] args) throws Exception {
        new IISServerDiscoveryComponent().discoverResources(null);
    }
}
