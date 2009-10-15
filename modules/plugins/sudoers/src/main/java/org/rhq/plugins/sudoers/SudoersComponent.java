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
package org.rhq.plugins.sudoers;

import java.io.File;
import java.util.Date;
import java.util.List;

import net.augeas.Augeas;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * @author Jason Dobies, Ian Springer 
 */
public class SudoersComponent implements ResourceComponent, ConfigurationFacet {

    private ResourceContext resourceContext;
    private File sudoersFile;

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;

        Configuration pluginConfiguration = resourceContext.getPluginConfiguration();

        PropertySimple sudoersPathProperty = pluginConfiguration.getSimple("sudoers-path");

        if (sudoersPathProperty == null) {
            throw new InvalidPluginConfigurationException(
                "sudoers path not found in the plugin configuration, cannot start resource component");
        }

        String sudoersPath = sudoersPathProperty.getStringValue();

        sudoersFile = new File(sudoersPath);

        if (!sudoersFile.exists()) {
            throw new InvalidPluginConfigurationException("Sudoers file not found at specified location: "
                + sudoersPath);
        }
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        if (sudoersFile == null) {
            return AvailabilityType.DOWN;
        }

        return sudoersFile.exists() ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    public Configuration loadResourceConfiguration() throws Exception {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        return loadResourceConfiguration(pluginConfig);
    }

    Configuration loadResourceConfiguration(Configuration pluginConfig) throws Exception {
        Augeas augeas = createAugeas(pluginConfig);
        List<String> entryPaths = getEntryPaths(pluginConfig, augeas);

        Configuration resourceConfig = new Configuration();
        resourceConfig.setNotes("Loaded from Augeas at " + new Date());

        PropertyList entriesList = new PropertyList("entries");
        resourceConfig.put(entriesList);

        //Sudoers format
        for (String entryPath : entryPaths) {
            String user = augeas.get(entryPath + "/user");
            String hostGroup = entryPath + "/host_group";
            String host = augeas.get(hostGroup + "/host");

            String commandPath = hostGroup + "/command";
            String command = augeas.get(commandPath);
            String runAsUser = augeas.get(commandPath + "/runas_user");
            boolean password = !"NOPASSWD".equals(augeas.get(commandPath + "/tag"));

            PropertyMap entry = new PropertyMap("entry");

            entry.put(new PropertySimple("user", user));
            entry.put(new PropertySimple("host", host));
            entry.put(new PropertySimple("command", command));
            entry.put(new PropertySimple("runAsUser", runAsUser));
            entry.put(new PropertySimple("password", password));
            entriesList.add(entry);
        }

        return resourceConfig;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
    }

    private Augeas createAugeas(Configuration pluginConfiguration) throws Exception {
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

        Augeas augeas = new Augeas(rootPath, "", Augeas.SAVE_NEWFILE);
        return augeas;
    }

    private List<String> getEntryPaths(Configuration pluginConfiguration, Augeas augeas) throws Exception {
        // Find out where to look for the sudoers tree
        PropertySimple augeasTreeNodeProperty = pluginConfiguration.getSimple("augeas-sudoers-path");
        if (augeasTreeNodeProperty == null) {
            throw new Exception("Augeas tree node not specified for sudoers, cannot retrieve configuration");
        }
        String sudoersTreeNode = augeasTreeNodeProperty.getStringValue();

        // Request data from augeas
        List<String> matches = augeas.match(sudoersTreeNode);
        if (matches.size() == 0) {
            throw new Exception("Unable to load sudoers data from augeas");
        }
        return matches;
    }
}
