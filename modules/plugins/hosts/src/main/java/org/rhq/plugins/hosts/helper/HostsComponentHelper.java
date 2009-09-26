/*
 * JBoss, a division of Red Hat.
 * Copyright 2006, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.plugins.hosts.helper;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.plugins.hosts.HostsComponent;

import java.io.File;

/**
 * @author Ian Springer
 */
public class HostsComponentHelper {
    public static File getHostsFile(Configuration pluginConfig) {
        String hostsFilePath = pluginConfig.getSimple(HostsComponent.PATH_PROP).getStringValue();
        return new File(hostsFilePath);
    }

    public static void validateHostFileExists(File hostsFile) throws InvalidPluginConfigurationException {
        if (!hostsFile.exists()) {
            throw new InvalidPluginConfigurationException("Location specified by '" + HostsComponent.PATH_PROP
                    + "' connection property does not exist.");
        }
        if (hostsFile.isDirectory()) {
            throw new InvalidPluginConfigurationException("Location specified by '" + HostsComponent.PATH_PROP
                    + "' connection property is a directory, not a regular file.");
        }
    }

    private HostsComponentHelper() {
    }
}
