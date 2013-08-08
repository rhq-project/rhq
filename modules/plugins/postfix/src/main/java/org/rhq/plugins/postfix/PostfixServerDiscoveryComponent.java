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
package org.rhq.plugins.postfix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.plugins.augeas.AugeasConfigurationComponent;
import org.rhq.plugins.augeas.AugeasConfigurationDiscoveryComponent;

public class PostfixServerDiscoveryComponent extends AugeasConfigurationDiscoveryComponent {

    private static final Log LOG = LogFactory.getLog(PostfixServerDiscoveryComponent.class);

    private static final Pattern HOSTNAME_PATTERN = Pattern.compile("[\\s]*myhostname[\\s]*=[\\s]*([^$].*)[\\s]*");

    public Set discoverResources(ResourceDiscoveryContext resourceDiscoveryContext)
        throws InvalidPluginConfigurationException, Exception {
        List discoveredProcesses = resourceDiscoveryContext.getAutoDiscoveredProcesses();
        if (discoveredProcesses.isEmpty()) {
            return Collections.emptySet();
        }
        if (discoveredProcesses.size() != 1) {
            LOG.warn("Found more than one Postfix process running");
            return Collections.emptySet();
        }
        Set<DiscoveredResourceDetails> resources = super.discoverResources(resourceDiscoveryContext);
        for (DiscoveredResourceDetails detail : resources) {
            Configuration config = detail.getPluginConfiguration();
            PropertySimple property = (PropertySimple) config.get(AugeasConfigurationComponent.INCLUDE_GLOBS_PROP);
            String configFilePath = property.getStringValue();
            String resourceName;

            try {
                resourceName = findHostName(configFilePath);
            } catch (Exception e) {
                resourceName = resourceDiscoveryContext.getSystemInformation().getHostname();
            }
            detail.setResourceName(resourceName);
            detail.setProcessInfo(((ProcessScanResult) discoveredProcesses.get(0)).getProcessInfo());
        }
        return resources;
    }

    private String findHostName(String includeFile) throws Exception {
        try {
            File file = new File(includeFile);
            if (file.exists()) {
                FileInputStream fstream = new FileInputStream(file);
                BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
                try {
                    String strLine;
                    while ((strLine = br.readLine()) != null) {
                        Matcher m = HOSTNAME_PATTERN.matcher(strLine);
                        if (m.matches()) {
                            String glob = m.group(1);

                            return glob;
                        }
                    }
                } finally {
                    StreamUtil.safeClose(br);
                }
            }
        } catch (Exception e) {
            throw new Exception("NetBios name was not found in configuration file " + includeFile + " cause:", e);
        }
        throw new Exception("NetBios name was not found in configuration file " + includeFile);
    }
}
