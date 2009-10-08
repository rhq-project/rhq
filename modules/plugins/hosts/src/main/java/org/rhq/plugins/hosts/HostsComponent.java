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
import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
import org.rhq.plugins.hosts.helper.Hosts;
import org.rhq.plugins.hosts.helper.HostsComponentHelper;
import org.rhq.plugins.hosts.helper.HostsEntry;

/**
 * The ResourceComponent for the "Hosts File" ResourceType.
 *
 * @author Ian Springer
 */
public class HostsComponent implements ResourceComponent, ConfigurationFacet {
    public static final String PATH_PROP = "path";

    private final Log log = LogFactory.getLog(this.getClass());

    private ResourceContext resourceContext;
    private File hostsFile;

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;
        this.hostsFile = HostsComponentHelper.getHostsFile(this.resourceContext.getPluginConfiguration());
        HostsComponentHelper.validateHostFileExists(this.hostsFile);

        return;
    }

    public void stop() {
        return;
    }

    public AvailabilityType getAvailability() {
        try {
            HostsComponentHelper.validateHostFileExists(this.hostsFile);
            return AvailabilityType.UP;
        }
        catch (InvalidPluginConfigurationException e) {
            log.debug("Hosts file Resource is down: " + e.getLocalizedMessage());
            return AvailabilityType.DOWN;
        }
    }

    public Configuration loadResourceConfiguration() throws Exception {
        Configuration resourceConfig = new Configuration();
        Hosts hosts = Hosts.load(this.hostsFile);
        resourceConfig.setNotes("Loaded at " + new Date());

        PropertyList entriesProp = new PropertyList("entries");
        resourceConfig.put(entriesProp);

        Set<String> ipAddressesWithDuplicateEntries = hosts.getIpAddressesWithDuplicateEntries();
        if (!ipAddressesWithDuplicateEntries.isEmpty()) {
            log.debug("Hosts file [" + this.hostsFile + "] contains duplicate entries for the following IP addresses: "
                    + ipAddressesWithDuplicateEntries);
        }        
        Set<String> namesWithDuplicateEntries = hosts.getNamesWithDuplicateEntries();
        if (!namesWithDuplicateEntries.isEmpty()) {
            log.error("Hosts file [" + this.hostsFile + "] contains duplicate entries for the following names: "
                    + namesWithDuplicateEntries);
        }

        for (HostsEntry entry : hosts.getEntries()) {
            PropertyMap entryProp = new PropertyMap("entry");
            entriesProp.add(entryProp);

            PropertySimple ipAddressProp = new PropertySimple("ipAddress", entry.getIpAddress());
            entryProp.put(ipAddressProp);

            PropertySimple canonicalNameProp = new PropertySimple("canonicalName", entry.getCanonicalName());
            if (namesWithDuplicateEntries.contains(entry.getCanonicalName())) {
                canonicalNameProp.setErrorMessage("At the time of loading, there was more than one entry containing this name - please remove or fix duplicate entries before saving.");
            }
            entryProp.put(canonicalNameProp);

            StringBuilder aliasesPropValue = new StringBuilder();
            boolean foundDuplicateName = false;
            for (String alias : entry.getAliases()) {
                if (namesWithDuplicateEntries.contains(alias)) {
                    foundDuplicateName = true;
                }
                aliasesPropValue.append(alias).append("\n");
            }
            if (!entry.getAliases().isEmpty()) {
                // Chop the final newline char.
                aliasesPropValue.deleteCharAt(aliasesPropValue.length() - 1);
            }
            PropertySimple aliasesProp = new PropertySimple("aliases", aliasesPropValue);
            if (foundDuplicateName) {
                aliasesProp.setErrorMessage("At the time of loading, there was more than one entry containing this name - please remove or fix duplicate entries before saving.");
            }
            entryProp.put(aliasesProp);
        }

        return  resourceConfig;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        Configuration resourceConfig = report.getConfiguration();
        Hosts newHosts = new Hosts();

        PropertyList entriesProp = resourceConfig.getList("entries");
        for (Property entryProp: entriesProp.getList()) {
            PropertyMap entryPropMap = (PropertyMap) entryProp;
            String ipAddress = entryPropMap.getSimple("ipAddress").getStringValue();
            String canonicalName = entryPropMap.getSimple("canonicalName").getStringValue();
            String aliases = entryPropMap.getSimpleValue("aliases", null);
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
            HostsEntry entry = new HostsEntry(ipAddress, canonicalName, aliasSet);
            newHosts.addEntry(entry);
        }

        Set<String> namesWithDuplicateEntries = newHosts.getNamesWithDuplicateEntries();
        if (!namesWithDuplicateEntries.isEmpty()) {
            report.setErrorMessage("There are duplicate entries for the following names: "
                    + namesWithDuplicateEntries);
            return;
        }

        try {
            Hosts.store(newHosts, this.hostsFile);
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to write hosts file [" + this.hostsFile + "].", e);
        }

        report.setStatus(ConfigurationUpdateStatus.SUCCESS);
        return;
    }
}
