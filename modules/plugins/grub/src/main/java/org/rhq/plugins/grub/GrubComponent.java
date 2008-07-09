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
package org.rhq.plugins.grub;

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
public class GrubComponent implements ResourceComponent, ConfigurationFacet {

    private ResourceContext resourceContext;
    private File grubFile;

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;

        Configuration pluginConfiguration = resourceContext.getPluginConfiguration();

        PropertySimple grubPathProperty = pluginConfiguration.getSimple("grub-conf-path");

        if (grubPathProperty == null) {
            throw new InvalidPluginConfigurationException("GRUB configuration file path not found in the plugin configuration, cannot start resource component");
        }

        String grubPath = grubPathProperty.getStringValue();

        grubFile = new File(grubPath);

        if (!grubFile.exists()) {
            throw new InvalidPluginConfigurationException("GRUB configuration file not found at specified location: " + grubPath);
        }
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        if (grubFile == null) {
            return AvailabilityType.DOWN;
        }

        return grubFile.exists() ? AvailabilityType.UP : AvailabilityType.DOWN;
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

        // Find out where to look for the grub tree
        PropertySimple augeasTreeNodeProperty = pluginConfiguration.getSimple("augeas-grub-path");

        if (augeasTreeNodeProperty == null) {
            throw new Exception("Augeas tree node not specified for grub, cannot retrive configuration");
        }

        // Parse configuration properties
        Configuration configuration = new Configuration();
        configuration.setNotes("Loaded from Augeas at " + new Date());
        
        // Load default properties
        String grubTreeNode = augeasTreeNodeProperty.getStringValue();

        List<String> generalMatches = augeas.match(grubTreeNode);

        if (generalMatches.size() > 0) {
            PropertyMap generalProperties = new PropertyMap("generalProperties");
            configuration.put(generalProperties);

            for (String generalNode : generalMatches) {
                String name = generalNode.substring(generalNode.lastIndexOf("/") + 1);

                if (!name.startsWith("title")) {
                    String value = augeas.get(generalNode);
                    generalProperties.put(new PropertySimple(name, value));
                }
            }
        }

        // Load all kernels in menu
        String grubTitleNode = grubTreeNode.substring(0, grubTreeNode.length() - 1) + "title";

        // Request data from augeas
        List<String> kernelMatches = augeas.match(grubTitleNode);

        PropertyList entriesList = new PropertyList("kernelEntries");
        configuration.put(entriesList);

        for (String entryNode : kernelMatches) {
            String title = augeas.get(entryNode);
            String root = augeas.get(entryNode + "/root");
            String kernel = augeas.get(entryNode + "/kernel");
            String initrd = augeas.get(entryNode + "/initrd");

            PropertyMap entry = new PropertyMap("kernelEntry");
            entry.put(new PropertySimple("title", title));
            entry.put(new PropertySimple("root", root));
            entry.put(new PropertySimple("kernel", kernel));
            entry.put(new PropertySimple("initrd", initrd));

            entriesList.add(entry);
        }

        return configuration;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
    }
}
