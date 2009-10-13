/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.hosts;

import java.io.File;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.augeas.AugeasConfigurationDiscoveryComponent;

/**
 * The ResourceDiscoveryComponent for the "Hosts File" ResourceType.
 *
 * @author Ian Springer
 */
public class HostsDiscoveryComponent extends AugeasConfigurationDiscoveryComponent {
    private static final boolean IS_WINDOWS = (File.separatorChar == '\\');

    private final Log log = LogFactory.getLog(this.getClass());

    @Override
    public Set discoverResources(ResourceDiscoveryContext discoveryContext) throws InvalidPluginConfigurationException,
            Exception
    {
        return super.discoverResources(discoveryContext);
    }

    @Override
    public DiscoveredResourceDetails discoverResource(Configuration pluginConfig,
                                                      ResourceDiscoveryContext discoveryContext) throws InvalidPluginConfigurationException
    {
        return super.discoverResource(pluginConfig, discoveryContext);
    }

    @Override
    protected File getConfigurationFile(ResourceDiscoveryContext discoveryContext)
    {
        File hostsFile;
        if (IS_WINDOWS) {
            File windowsDir = getWindowsDir();
            hostsFile = new File(windowsDir, "system32/drivers/etc/hosts");
        } else {
            hostsFile = new File("/etc/hosts");
        }
        return hostsFile;
    }

    private File getWindowsDir() {
        File windowsDir = null;
        String windowsDirPath = System.getenv().get("SystemRoot");
        if (windowsDirPath != null) {
            windowsDir = new File(windowsDirPath);
            if (!windowsDir.isDirectory()) {
                windowsDirPath = System.getenv().get("WinDir");
                if (windowsDirPath != null) {
                    windowsDir = new File(windowsDirPath);
                    if (!windowsDir.isDirectory()) {
                        windowsDir = new File("C:\\WINDOWS");
                        if (!windowsDir.isDirectory()) {
                            windowsDir = new File("C:\\WINNT");
                            if (!windowsDir.isDirectory()) {
                                windowsDir = null;
                            }
                        }
                    }
                }            
            }
        }
        if (windowsDir == null) {
            throw new IllegalStateException("Failed to determine Windows directory.");
        }
        log.debug("Windows directory: " + windowsDir);
        return windowsDir;
    }
}
