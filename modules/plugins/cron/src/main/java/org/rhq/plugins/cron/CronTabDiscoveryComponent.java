/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.plugins.cron;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.augeas.AugeasConfigurationComponent;
import org.rhq.plugins.augeas.AugeasConfigurationDiscoveryComponent;
import org.rhq.plugins.augeas.helper.Glob;

/**
 * Discovery for cron tabs.
 * 
 * @author Lukas Krejci
 */
public class CronTabDiscoveryComponent implements ResourceDiscoveryComponent<CronComponent> {

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<CronComponent> context)
        throws InvalidPluginConfigurationException, Exception {

        Configuration cronConfiguration = context.getParentResourceContext().getPluginConfiguration();

        List<String> includes = AugeasConfigurationDiscoveryComponent.getGlobList(
                cronConfiguration.getSimple(AugeasConfigurationComponent.INCLUDE_GLOBS_PROP));
        List<String> excludes = AugeasConfigurationDiscoveryComponent.getGlobList(
                cronConfiguration.getSimple(AugeasConfigurationComponent.EXCLUDE_GLOBS_PROP));
        
        String rootPath = cronConfiguration.getSimpleValue(AugeasConfigurationComponent.AUGEAS_ROOT_PATH_PROP, AugeasConfigurationComponent.DEFAULT_AUGEAS_ROOT_PATH);
        List<File> files = Glob.matchAll(new File(rootPath), includes);
        Glob.excludeAll(files, excludes);

        HashSet<DiscoveredResourceDetails> results = new HashSet<DiscoveredResourceDetails>();

        ResourceType resourceType = context.getResourceType();

        int pathUnderRootStartIdx = rootPath.endsWith(File.separator) ? rootPath.length() - 1 : rootPath.length();
        
        for (File f : files) {
            String resourceKey = f.getAbsolutePath().substring(pathUnderRootStartIdx);
            Configuration defaultConfiguration = context.getDefaultPluginConfiguration();
            defaultConfiguration.put(AugeasConfigurationDiscoveryComponent.getGlobList(
                AugeasConfigurationComponent.INCLUDE_GLOBS_PROP, Collections.singletonList(resourceKey)));
            defaultConfiguration.put(new PropertySimple(AugeasConfigurationComponent.AUGEAS_ROOT_PATH_PROP, rootPath));
            DiscoveredResourceDetails result = new DiscoveredResourceDetails(resourceType, resourceKey, resourceKey,
                null, null, defaultConfiguration, null);
            results.add(result);
        }
        return results;
    }
}
