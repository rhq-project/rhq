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

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * A component representing a single crontab file.
 * 
 * @author Lukas Krejci
 */
public class CronTabComponent implements ResourceComponent<CronComponent>, ConfigurationFacet {

    public static final String BASIC_SETTINGS_PROP = "basicSettings";
    public static final String ENTRIES_PROP = "entries";
    public static final String ADDITIONAL_SETTINGS_PROP = "additionalSettings";
    public static final String SETTING_PROP = "setting";
    public static final String NAME_PROP = "name";
    public static final String VALUE_PROP = "value";

    private ResourceContext<CronComponent> resourceContext;
    private File crontabFile;
    
    public Configuration loadResourceConfiguration() throws Exception {
        CronComponent parent = resourceContext.getParentResourceComponent();
        String resourceKey = resourceContext.getResourceKey();
        ConfigurationDefinition configDef = resourceContext.getResourceType().getResourceConfigurationDefinition(); 
        
        return parent.loadCronTab(resourceKey, configDef);
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        CronComponent parent = resourceContext.getParentResourceComponent();
        ConfigurationDefinition configDef = resourceContext.getResourceType().getResourceConfigurationDefinition();
        
        parent.updateCrontab(resourceContext.getResourceKey(), configDef, report.getConfiguration());
        
        report.setStatus(ConfigurationUpdateStatus.SUCCESS);
    }

    public void start(ResourceContext<CronComponent> context) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = context;
        crontabFile = new File(context.getResourceKey());
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        return crontabFile.exists() ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

}
