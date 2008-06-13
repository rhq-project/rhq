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
package org.rhq.plugins.hosts;

import java.io.File;
import java.util.List;
import java.util.Date;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.plugins.augeas.Augeas;

/**
 * @author Jason Dobies
 */
public class HostsComponent implements ResourceComponent, ConfigurationFacet {

    private ResourceContext resourceContext;
    private File hostsFile;

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;

        Configuration pluginConfiguration = resourceContext.getPluginConfiguration();

        PropertySimple hostsPathProperty = pluginConfiguration.getSimple("hosts-path");

        if (hostsPathProperty == null) {
            throw new InvalidPluginConfigurationException("Hosts path not found in the plugin configuration, cannot start resource component");
        }

        String hostsPath = hostsPathProperty.getStringValue();

        hostsFile = new File(hostsPath);

        if (!hostsFile.exists()) {
            throw new InvalidPluginConfigurationException("Hosts file not found at specified location: " + hostsPath);
        }
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        if (hostsFile == null) {
            return AvailabilityType.DOWN;
        }

        return hostsFile.exists() ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    public Configuration loadResourceConfiguration() throws Exception {
        Configuration pluginConfiguration = resourceContext.getPluginConfiguration();

        return loadResourceConfiguration(pluginConfiguration);
    }

    public Configuration loadResourceConfiguration(Configuration pluginConfiguration) throws Exception {
        // Gather data necessary to create the Augeas hook
        PropertySimple lensesPathProperty = pluginConfiguration.getSimple("lenses-path");

        if (lensesPathProperty == null) {
            throw new Exception("Lenses path not found in plugin configuration, cannot retrieve configuration");
        }

        PropertySimple rootPathProperty = pluginConfiguration.getSimple("root-path");

        if (rootPathProperty == null) {
            throw new Exception("Root path not found in plugin configuration, cannot retrieve configuration");
        }

        String lensesPath = lensesPathProperty.getStringValue();
        String rootPath = rootPathProperty.getStringValue();

        Augeas augeas = new Augeas(rootPath, lensesPath);

        // Find out where to look for the hosts tree
        PropertySimple augeasTreeNodeProperty = pluginConfiguration.getSimple("augeas-hosts-path");

        if (augeasTreeNodeProperty == null) {
            throw new Exception("Augeas tree node not specified for hosts, cannot retrive configuration");
        }

        String hostsTreeNode = augeasTreeNodeProperty.getStringValue();

        // Request data from augeas
        List<String> matches = augeas.match(hostsTreeNode);
        if (matches.size() == 0) {
            throw new Exception("Unable to load hosts data from augeas");
        }

        // Parse out the properties
        Configuration configuration = new Configuration();
        configuration.setNotes("Loaded from Augeas at " + new Date());

        PropertyList entriesList = new PropertyList("hostEntries");
        configuration.put(entriesList);

        for (String entryNode : matches) {
            String ip = augeas.get(entryNode + "/ipaddr");
            String canonical = augeas.get(entryNode + "/canonical");

            PropertyMap entry = new PropertyMap("hostEntry");
            entry.put(new PropertySimple("ip", ip));
            entry.put(new PropertySimple("canonical", canonical));

            entriesList.add(entry);
        }

        return configuration;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
    }
}
