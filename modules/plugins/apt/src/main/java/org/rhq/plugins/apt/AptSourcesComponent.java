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
package org.rhq.plugins.apt;

import java.io.File;
import java.util.List;
import java.util.Date;
import net.augeas.Augeas;
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

/**
 * @author Jason Dobies
 */
public class AptSourcesComponent implements ResourceComponent, ConfigurationFacet {

    private ResourceContext resourceContext;
    private File aptSourcesFile;

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;

        Configuration pluginConfiguration = resourceContext.getPluginConfiguration();

        PropertySimple aptSourcesPathProperty = pluginConfiguration.getSimple("apt-sources-path");

        if (aptSourcesPathProperty == null) {
            throw new InvalidPluginConfigurationException("Apt sources not found in the plugin configuration, cannot start resource component");
        }

        String aptSourcesPath = aptSourcesPathProperty.getStringValue();

        aptSourcesFile = new File(aptSourcesPath);

        if (!aptSourcesFile.exists()) {
            throw new InvalidPluginConfigurationException("Apt sources file not found at specified location: " + aptSourcesPath);
        }
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        if (aptSourcesFile == null) {
            return AvailabilityType.DOWN;
        }

        return aptSourcesFile.exists() ? AvailabilityType.UP : AvailabilityType.DOWN;
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

        // Find out where to look for the apt sources tree
        PropertySimple augeasTreeNodeProperty = pluginConfiguration.getSimple("augeas-apt-sources-path");

        if (augeasTreeNodeProperty == null) {
            throw new Exception("Augeas tree node not specified for apt sources, cannot retrive configuration");
        }

        String sourcesTreeNode = augeasTreeNodeProperty.getStringValue();

        // Request data from augeas
        List<String> matches = augeas.match(sourcesTreeNode);
        if (matches.size() == 0) {
            throw new Exception("Unable to load apt sources data from augeas");
        }

        // Parse out the properties
        Configuration configuration = new Configuration();
        configuration.setNotes("Loaded from Augeas at " + new Date());

        PropertyList entriesList = new PropertyList("aptEntries");
        configuration.put(entriesList);

        for (String entryNode : matches) {
            String type = augeas.get(entryNode + "/type");
            String uri = augeas.get(entryNode + "/uri");
            String distribution = augeas.get(entryNode + "/distribution");

            PropertyMap entry = new PropertyMap("aptEntry");
            entry.put(new PropertySimple("type", type));
            entry.put(new PropertySimple("uri", uri));
            entry.put(new PropertySimple("distribution", distribution));

            entriesList.add(entry);
        }

        return configuration;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
    }
}
