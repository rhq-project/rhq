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
package org.rhq.plugins.cron;

import java.io.File;
import java.io.IOException;
import java.util.List;

import net.augeas.Augeas;
import net.augeas.AugeasException;

import org.rhq.augeas.util.GlobFilter;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.plugins.augeas.AugeasConfigurationComponent;
import org.rhq.plugins.augeas.AugeasConfigurationDiscoveryComponent;
import org.rhq.plugins.augeas.helper.AugeasNode;
import org.rhq.plugins.platform.PlatformComponent;

/**
 * Provides an overview of all entries in the crontabs split into hourly/daily/weekly/monthly entries.
 * Is able to create new cron tabs.
 * 
 * @author Lukas Krejci 
 */
public class CronComponent extends AugeasConfigurationComponent<PlatformComponent> implements CreateChildResourceFacet {

    private static final String HOURLY_JOBS_PROP = "hourlyJobs";
    private static final String HOURLY_JOBS_NODE = "entry[time/minute != \"*\" and time/hour = \"*\" and time/dayofmonth = \"*\" and time/month = \"*\" and time/dayofweek = \"*\"]";
    private static final String DAILY_JOBS_PROP = "dailyJobs";
    private static final String DAILY_JOBS_NODE = "entry[time/minute != \"*\" and time/hour != \"*\" and time/dayofmonth = \"*\" and time/month = \"*\" and time/dayofweek = \"*\"]";
    private static final String WEEKLY_JOBS_PROP = "weeklyJobs";
    private static final String WEEKLY_JOBS_NODE = "entry[time/minute != \"*\" and time/hour != \"*\" and time/dayofmonth = \"*\" and time/month = \"*\" and time/dayofweek != \"*\"]";
    private static final String MONTHLY_JOBS_PROP = "monthlyJobs";
    private static final String MONTHLY_JOBS_NODE = "entry[time/minute != \"*\" and time/hour != \"*\" and time/dayofmonth != \"*\" and time/month = \"*\" and time/dayofweek = \"*\"]";
    private static final String YEARLY_JOBS_PROP = "yearlyJobs";
    private static final String YEARLY_JOBS_NODE = "entry[time/minute != \"*\" and time/hour != \"*\" and time/dayofmonth != \"*\" and time/month != \"*\" and time/dayofweek = \"*\"]";
    private static final String CRONTAB_PROP = "crontab";
    private static final String CRONTAB_NODE = "..";
    private static final String AUGEAS_FILES_PREFIX = "/files";
    private static final int AUGEAS_FILES_PREFIX_LENGTH = AUGEAS_FILES_PREFIX.length();
    private static final String CRONTAB_RESOURCE_TYPE_NAME = "Cron Tab";

    @Override
    protected String getResourceConfigurationRootPath() {
        return "/files/"; //the trailing slash is intentional so that we can get the
                          // /files//blah paths...
    }

    public CreateResourceReport createResource(CreateResourceReport report) {
        if (CRONTAB_RESOURCE_TYPE_NAME.equals(report.getResourceType().getName())) {
            try {
                String resourceKey = createCrontab(report.getUserSpecifiedResourceName(), report.getResourceType()
                    .getResourceConfigurationDefinition(), report.getResourceConfiguration());
                report.setResourceName(resourceKey);
                report.setResourceKey(resourceKey);
                report.setStatus(CreateResourceStatus.SUCCESS);
            } catch (Exception e) {
                report.setException(e);
                report.setStatus(CreateResourceStatus.FAILURE);
            }
        } else {
            report.setErrorMessage("Don't know how to create resource of type " + report.getResourceType().getName());
            report.setStatus(CreateResourceStatus.FAILURE);
        }
        return report;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        report.setErrorMessage("This is a readonly view of cron tabs. Update isn't supported.");
        report.setStatus(ConfigurationUpdateStatus.FAILURE);
    }

    @Override
    protected String getAugeasPathRelativeToParent(PropertyDefinition propDef, AugeasNode parentNode, Augeas augeas) {
        String name = propDef.getName();
        if (CRONTAB_PROP.equals(name)) {
            return CRONTAB_NODE;
        } else if (HOURLY_JOBS_PROP.equals(name)) {
            return HOURLY_JOBS_NODE;
        } else if (DAILY_JOBS_PROP.equals(name)) {
            return DAILY_JOBS_NODE;
        } else if (WEEKLY_JOBS_PROP.equals(name)) {
            return WEEKLY_JOBS_NODE;
        } else if (MONTHLY_JOBS_PROP.equals(name)) {
            return MONTHLY_JOBS_NODE;
        } else if (YEARLY_JOBS_PROP.equals(name)) {
            return YEARLY_JOBS_NODE;
        } else {
            return super.getAugeasPathRelativeToParent(propDef, parentNode, augeas);
        }
    }

    @Override
    protected Object toPropertyValue(PropertyDefinitionSimple propDefSimple, Augeas augeas, AugeasNode node) {
        if (CRONTAB_PROP.equals(propDefSimple.getName())) {
            return getEntryCrontabPath(node);
        } else {
            return super.toPropertyValue(propDefSimple, augeas, node);
        }
    }

    private String getEntryCrontabPath(AugeasNode crontabNode) {
        //the node's path is /files/blah/blah/entry/..
        //we want the /blah/blah part
        return crontabNode.getParent().getParent().getPath().substring(AUGEAS_FILES_PREFIX_LENGTH);
    }

    /**
     * Creates a new crontab resource based on the provided configuration.
     * 
     * @param resourceConfiguration
     * @return the resource key of the crontab
     */
    private String createCrontab(String resourceName, ConfigurationDefinition configurationDefinition,
        Configuration resourceConfiguration) {
        File crontabFile = new File(resourceName);

        try {
            if (!crontabFile.createNewFile()) {
                throw new RuntimeException("File " + resourceName
                    + " already exists. Creating the crontab would overwrite it.");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create the crontab file named: " + resourceName);
        }

        //check that the crontab's name passes the glob filters
        Configuration pluginConfiguration = getResourceContext().getPluginConfiguration();

        List<String> includeGlobs = AugeasConfigurationDiscoveryComponent.getGlobList(pluginConfiguration
            .getSimple(AugeasConfigurationComponent.INCLUDE_GLOBS_PROP));
        List<String> excludeGlobs = AugeasConfigurationDiscoveryComponent.getGlobList(pluginConfiguration
            .getSimple(AugeasConfigurationComponent.EXCLUDE_GLOBS_PROP));

        boolean isIncluded = false;

        for (String include : includeGlobs) {
            if (new GlobFilter(include).accept(crontabFile)) {
                isIncluded = true;
                break;
            }
        }

        String errorText = "Given Cron tab file name would be created outside of mapped filters. See the Cron resource connection properties for the inclusion and exclusion filters set up.";
        if (!isIncluded) {
            throw new IllegalArgumentException(errorText);
        }

        for (String exclude : excludeGlobs) {
            if (new GlobFilter(exclude).accept(crontabFile)) {
                throw new IllegalArgumentException(errorText);
            }
        }

        updateCrontab(resourceName, configurationDefinition, resourceConfiguration);

        return resourceName;
    }

    protected void updateCrontab(String crontabPath, ConfigurationDefinition crontabConfigurationDefinition,
        Configuration crontabConfiguration) {
        Augeas augeas = null;
        try {
            augeas = getAugeas();

            File crontabFile = new File(crontabPath);
            String basePath = AUGEAS_FILES_PREFIX + crontabFile.getAbsolutePath();
            AugeasNode baseNode = new AugeasNode(basePath);

            PropertyList entries = crontabConfiguration.getList(CronTabComponent.ENTRIES_PROP);
            PropertyDefinitionList entriesDef = crontabConfigurationDefinition
                .getPropertyDefinitionList(CronTabComponent.ENTRIES_PROP);
            setNodeFromPropertyList(entriesDef, entries, augeas, baseNode);

            PropertyList settings = crontabConfiguration.getList(CronTabComponent.ENVIRONMENT_SETTINGS_PROP);
            if (settings != null) {
                for (Property p : settings.getList()) {
                    PropertyMap setting = (PropertyMap) p;
                    String name = setting.getSimpleValue(CronTabComponent.NAME_PROP, null);
                    String value = setting.getSimpleValue(CronTabComponent.VALUE_PROP, "");
                    if (name != null) {
                        String settingPath = basePath + AugeasNode.SEPARATOR + name;
                        augeas.set(settingPath, value);
                    }
                }
            }

            augeas.save();
        } catch (AugeasException e) {
            if (augeas != null)
                throw new RuntimeException(summarizeAugeasError(augeas), e);
            else
                throw new RuntimeException(e);
        } finally {
            close();
        }
    }
}
