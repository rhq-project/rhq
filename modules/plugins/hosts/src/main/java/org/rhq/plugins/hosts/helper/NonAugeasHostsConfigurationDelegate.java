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
package org.rhq.plugins.hosts.helper;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.plugins.hosts.HostsComponent;

/**
 * @author Ian Springer
 */
public class NonAugeasHostsConfigurationDelegate implements ConfigurationFacet {
    private HostsComponent hostsComponent;

    public NonAugeasHostsConfigurationDelegate(HostsComponent hostsComponent) {
        this.hostsComponent = hostsComponent;
    }

    public Configuration loadResourceConfiguration() throws Exception {
        Configuration resourceConfig = new Configuration();
        List configurationFiles = this.hostsComponent.getConfigurationFiles();
        if (configurationFiles == null || configurationFiles.isEmpty()) {
            throw new Exception("Cannot find the hosts file on this machine");
        }
        File hostsFile = (File) configurationFiles.get(0);
        Hosts hosts = Hosts.load(hostsFile);
        resourceConfig.setNotes("Loaded at " + new Date());

        PropertyList entriesProp = new PropertyList(".");
        resourceConfig.put(entriesProp);

        for (HostsEntry entry : hosts.getEntries()) {
            PropertyMap entryProp = new PropertyMap("*[canonical]");
            entriesProp.add(entryProp);

            entryProp.put(new PropertySimple("ipaddr", entry.getIpAddress()));
            entryProp.put(new PropertySimple("canonical", entry.getCanonicalName()));
            StringBuilder aliasPropValue = new StringBuilder();
            for (String alias : entry.getAliases()) {
                aliasPropValue.append(alias).append("\n");
            }
            if (!entry.getAliases().isEmpty()) {
                // Chop the final newline char.
                aliasPropValue.deleteCharAt(aliasPropValue.length() - 1);
            }
            entryProp.put(new PropertySimple("alias", aliasPropValue));
        }

        return resourceConfig;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        Configuration resourceConfig = report.getConfiguration();
        Hosts newHosts = new Hosts();

        PropertyList entriesProp = resourceConfig.getList(".");
        for (Property entryProp : entriesProp.getList()) {
            PropertyMap entryPropMap = (PropertyMap) entryProp;
            String ipAddress = entryPropMap.getSimple("ipaddr").getStringValue();
            String canonicalName = entryPropMap.getSimple("canonical").getStringValue();
            String aliases = entryPropMap.getSimpleValue("alias", null);
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

        File hostsFile = (File) this.hostsComponent.getConfigurationFiles().get(0);
        try {
            Hosts.store(newHosts, hostsFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write hosts file [" + hostsFile + "].", e);
        }

        report.setStatus(ConfigurationUpdateStatus.SUCCESS);
        return;
    }
}
