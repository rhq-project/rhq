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
package org.rhq.plugins.lsof;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.plugins.script.ScriptDiscoveryComponent;

/**
 * Discovers a lsof tool.
 *
 * @author John Mazzitelli
 */
public class LsofDiscoveryComponent extends ScriptDiscoveryComponent {
    private final Log log = LogFactory.getLog(LsofDiscoveryComponent.class);

    private static final String DEFAULT_NAME = "Network Resource Detector";
    private static final String DEFAULT_DESCRIPTION = "A resource that is used to detect network resources.";

    @Override
    public DiscoveredResourceDetails discoverResource(Configuration pluginConfig,
        ResourceDiscoveryContext discoveryContext) throws InvalidPluginConfigurationException {

        DiscoveredResourceDetails details;

        DetectionMechanism detectionMechanism = getDetectionMechanism(pluginConfig);
        log.debug("resource will have detection mechanism of [" + detectionMechanism + "]");

        switch (detectionMechanism) {
        case INTERNAL:
            if (!SystemInfoFactory.isNativeSystemInfoAvailable()) {
                throw new InvalidPluginConfigurationException(
                    "The native system is not available - cannot use the internal detection mechanism");
            }
            if (SystemInfoFactory.isNativeSystemInfoDisabled()) {
                throw new InvalidPluginConfigurationException(
                    "The native system is disabled - cannot use the internal detection mechanism");
            }
            String version = LsofComponent.VERSION;
            details = new DiscoveredResourceDetails(discoveryContext.getResourceType(), "*lsof-internal*",
                DEFAULT_NAME, version, DEFAULT_DESCRIPTION, pluginConfig, null);
            break;
        case EXTERNAL:
            // do all that we can to make sure we have a valid full path to the executable
            PropertySimple executable = pluginConfig.getSimple(LsofComponent.PLUGINCONFIG_EXECUTABLE);
            executable.setStringValue(findExecutable(executable.getStringValue()));

            details = super.discoverResource(pluginConfig, discoveryContext);
            details.setResourceName(DEFAULT_NAME);
            break;
        default:
            throw new InvalidPluginConfigurationException("Unknown detection mechanism: " + detectionMechanism);
        }

        return details;
    }

    private String findExecutable(String executable) {
        File findIt = new File(executable);
        if (!findIt.isAbsolute()) {
            // the typical locations where lsof can usually be found
            String[] possibleLocations = { "/usr/bin", "/usr/sbin", "/bin", "/sbin", "/usr/local/bin" };
            for (String possibleLocation : possibleLocations) {
                findIt = new File(possibleLocation, executable);
                if (findIt.exists()) {
                    executable = findIt.getAbsolutePath();
                    break;
                }
            }
        }
        return executable;
    }

    @Override
    protected String determineDescription(ResourceDiscoveryContext context, Configuration pluginConfig) {
        return DEFAULT_DESCRIPTION; // we don't use the discovery tool to get us a description - we hard code one
    }

    public static DetectionMechanism getDetectionMechanism(Configuration pluginConfig) {
        String mechanism = pluginConfig.getSimpleValue(LsofComponent.PLUGINCONFIG_DETECTION_MECHANISM, "external");
        return DetectionMechanism.valueOf(mechanism);
    }
}