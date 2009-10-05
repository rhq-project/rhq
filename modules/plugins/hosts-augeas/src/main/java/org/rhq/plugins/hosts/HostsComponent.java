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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Date;
import java.util.Set;

import net.augeas.Augeas;

import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;
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
import org.rhq.plugins.hosts.util.AugeasUtility;

/**
 * @author Jason Dobies, Ian Springer 
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

        for (String entryPath : entryPaths) {
            String ip = augeas.get(entryPath + "/ipaddr");
            String canonical = augeas.get(entryPath + "/canonical");
            List<String> aliasPaths = augeas.match(entryPath + "/alias[*]");

            PropertyMap entry = new PropertyMap("entry");
            entry.put(new PropertySimple("ipAddress", ip));
            entry.put(new PropertySimple("canonicalName", canonical));
            if (!aliasPaths.isEmpty()) {
                StringBuilder aliasesPropValue = new StringBuilder();
                for (String aliasPath : aliasPaths) {
                    String alias = augeas.get(aliasPath);
                    aliasesPropValue.append(alias).append("\n");
                }
                // Chop the final newline char.
                aliasesPropValue.deleteCharAt(aliasesPropValue.length() - 1);
                entry.put(new PropertySimple("aliases", aliasesPropValue));
            }
            entriesList.add(entry);
        }

        return resourceConfig;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        Augeas augeas;
        try {
            augeas = createAugeas(pluginConfig);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to create Augeas object.", e);
        }

        PropertySimple augeasTreeNodeProperty = pluginConfig.getSimple("augeas-hosts-path");
        if (augeasTreeNodeProperty == null) {
            throw new IllegalStateException("Augeas tree node not specified for hosts, cannot retrieve configuration");
        }
        String hostsPath = augeasTreeNodeProperty.getStringValue();

        Set<String> originalEntryPaths = new HashSet<String>(augeas.match("/files/etc/hosts/*"));
        Set<String> updatedEntryPaths = new HashSet<String>();

        Configuration resourceConfig = report.getConfiguration();
        PropertyList entriesList = resourceConfig.getList("entries");
        for (Property entryProp : entriesList.getList()) {
            PropertyMap entry = (PropertyMap)entryProp;
            String ipAddress = entry.getSimple("ipAddress").getStringValue();
            String canonicalName = entry.getSimple("canonicalName").getStringValue();
            String aliases = entry.getSimpleValue("aliases", null);
            Set<String> aliasSet;
            if (aliases == null) {
                aliasSet = null;
            } else {
                String[] aliasArray = aliases.trim().split("\\s+");
                aliasSet = new LinkedHashSet<String>(aliasArray.length);
                for (String alias : aliasArray) {
                    aliasSet.add(alias);
                }
            }

            int newEntryIndex = 0;
            String newEntryPathPattern = hostsPath + "/0%d";

            String nameFilter = "/files/etc/hosts/*/canonical";
            List<String> namePaths = AugeasUtility.matchFilter(augeas, nameFilter, canonicalName);
            if (!namePaths.isEmpty()) {
                // overwrite existing entry

                // name
                String namePath = namePaths.get(0);
                augeas.set(namePath, canonicalName);

                String entryPath = AugeasUtility.getParentPath(namePath);
                updatedEntryPaths.add(entryPath);

                // IP
                String ipPath = entryPath + "/ipaddr";
                augeas.set(ipPath, canonicalName);

                // aliases
                // remove existing alias nodes first
                String aliasExpression = entryPath + "/alias[*]";
                List<String> aliasPaths = augeas.match(aliasExpression);
                for (String aliasPath : aliasPaths) {
                    augeas.remove(aliasPath);
                }
                // then add the new ones                
                String aliasPathPattern = entryPath + "/alias[%d]";
                int i = 1;
                for (String alias : aliasSet) {
                    String aliasPath = String.format(aliasPathPattern, i++);
                    augeas.set(aliasPath, alias);
                }
            } else {
                // no existing entry w/ this canonical name - add a new entry
                String newEntryPath = String.format(newEntryPathPattern, newEntryIndex++);
                augeas.set(newEntryPath + "/ipaddr", ipAddress);
                augeas.set(newEntryPath + "/canonical", canonicalName);
                String aliasPathPattern = newEntryPath + "/alias[%d]";
                int i = 1;
                for (String alias : aliasSet) {
                    String aliasPath = String.format(aliasPathPattern, i++);
                    augeas.set(aliasPath, alias);
                }
            }

        }

        // Remove any entries from the Augeas tree that are not present in the updated Configuration.
        for (String originalEntryPath : originalEntryPaths) {
            if (!updatedEntryPaths.contains(originalEntryPath)) {
                augeas.remove(originalEntryPath);
            }
        }

        // TODO: Backup the original file.

        // Write the updated Augeas tree out to the /etc/hosts file.
        augeas.save();

        // If we got this far, we've succeeded in out mission.
        report.setStatus(ConfigurationUpdateStatus.SUCCESS);

        return;
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

        Augeas augeas = new Augeas(rootPath, lensesPath, Augeas.NONE);
        return augeas;
    }

    private List<String> getEntryPaths(Configuration pluginConfiguration, Augeas augeas) throws Exception {
        // Find out where to look for the hosts tree
        PropertySimple augeasTreeNodeProperty = pluginConfiguration.getSimple("augeas-hosts-path");
        if (augeasTreeNodeProperty == null) {
            throw new Exception("Augeas tree node not specified for hosts, cannot retrieve configuration");
        }
        String hostsTreeNode = augeasTreeNodeProperty.getStringValue();

        // Request data from augeas
        List<String> matches = augeas.match(hostsTreeNode);
        if (matches.size() == 0) {
            throw new Exception("Unable to load hosts data from augeas");
        }
        return matches;
    }
}
