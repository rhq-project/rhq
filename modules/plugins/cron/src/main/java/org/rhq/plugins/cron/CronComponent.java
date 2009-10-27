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
import java.util.Date;
import java.util.List;

import net.augeas.Augeas;
import net.augeas.AugeasException;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.plugins.augeas.AugeasConfigurationComponent;
import org.rhq.plugins.augeas.AugeasConfigurationDiscoveryComponent;
import org.rhq.plugins.augeas.helper.AugeasNode;
import org.rhq.plugins.augeas.helper.GlobFilter;
import org.rhq.plugins.platform.PlatformComponent;

/**
 * @author Lukas Krejci 
 */
public class CronComponent extends AugeasConfigurationComponent<PlatformComponent> implements CreateChildResourceFacet {

    private static final String CRONTAB_PROP = "..";
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
                String resourceKey = createCrontab(report.getResourceName(), report.getResourceType().getResourceConfigurationDefinition(), report.getResourceConfiguration());
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
    protected Property createPropertySimple(PropertyDefinitionSimple propDefSimple, Augeas augeas, AugeasNode node) {
        if (CRONTAB_PROP.equals(propDefSimple.getName())) {
            //we want the full path to the crontab file the entry is in..
            String crontabPath = getEntryCrontabPath(node);
            return new PropertySimple(CRONTAB_PROP, crontabPath);
        } else {
            return super.createPropertySimple(propDefSimple, augeas, node);
        }
    }

    @Override
    protected void setNodeFromPropertySimple(Augeas augeas, AugeasNode node, PropertyDefinitionSimple propDefSimple,
        PropertySimple propSimple) {
        
        if (CRONTAB_PROP.equals(propDefSimple.getName())) {
            String origPath = getEntryCrontabPath(node);
            if (!propSimple.getStringValue().equals(origPath)) {
                String newCrontab = AUGEAS_FILES_PREFIX + propSimple.getStringValue();
                //check that the newly specified crontab exists
                if (!augeas.exists(newCrontab)) {
                    throw new RuntimeException("The crontab '" + newCrontab + "' doesn't exist. You need to create it before you can move entries into it.");
                }
                augeas.move(node.getPath(), newCrontab);
            }
        } else {
            super.setNodeFromPropertySimple(augeas, node, propDefSimple, propSimple);
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
    private String createCrontab(String resourceName, ConfigurationDefinition configurationDefinition, Configuration resourceConfiguration) {
        File crontabFile = new File(resourceName);
        if (crontabFile.exists()) {
            throw new RuntimeException("A file with given name already exists. Creating the crontab would overwrite it.");
        }
        
        //check that the crontab's name passes the glob filters
        Configuration pluginConfiguration = getResourceContext().getPluginConfiguration();
        
        List<String> includeGlobs = AugeasConfigurationDiscoveryComponent.getGlobList(pluginConfiguration.getSimple(AugeasConfigurationComponent.INCLUDE_GLOBS_PROP));
        List<String> excludeGlobs = AugeasConfigurationDiscoveryComponent.getGlobList(pluginConfiguration.getSimple(AugeasConfigurationComponent.EXCLUDE_GLOBS_PROP));

        boolean isIncluded = false;
        
        for(String include : includeGlobs) {
            if (new GlobFilter(include).accept(crontabFile)) {
                isIncluded = true;
                break;
            }
        }
        
        String errorText = "Given Cron tab file name would be created outside of mapped filters. See the Cron resource connection properties for the inclusion and exclusion filters set up."; 
        if (!isIncluded) {
            throw new IllegalArgumentException(errorText);
        }
        
        for(String exclude : excludeGlobs) {
            if (new GlobFilter(exclude).accept(crontabFile)) {
                throw new IllegalArgumentException(errorText);
            }
        }
        
        updateCrontab(resourceName, configurationDefinition, resourceConfiguration);
        
        return resourceName;
    }
    
    protected void updateCrontab(String crontabPath, ConfigurationDefinition crontabConfigurationDefinition, Configuration crontabConfiguration) {
        try {
            Augeas augeas = getAugeas();
            augeas.load();
            
            File crontabFile = new File(crontabPath);
            String basePath = AUGEAS_FILES_PREFIX + crontabFile.getAbsolutePath();
            AugeasNode baseNode = new AugeasNode(basePath);
            
            PropertyMap basicSettings = crontabConfiguration.getMap(CronTabComponent.BASIC_SETTINGS_PROP);
            PropertyDefinitionMap basicSettingsDef = crontabConfigurationDefinition.getPropertyDefinitionMap(CronTabComponent.BASIC_SETTINGS_PROP);
            
            setNodeFromPropertyMap(basicSettingsDef, basicSettings, augeas, baseNode);
            
            PropertyList entries = crontabConfiguration.getList(CronTabComponent.ENTRIES_PROP);
            PropertyDefinitionList entriesDef = crontabConfigurationDefinition.getPropertyDefinitionList(CronTabComponent.ENTRIES_PROP);
            setNodeFromPropertyList(entriesDef, entries, augeas, baseNode);
            
            PropertyList additionalSettings = crontabConfiguration.getList(CronTabComponent.ADDITIONAL_SETTINGS_PROP);
            if (additionalSettings != null) {
                for(Property p : additionalSettings.getList()) {
                    PropertyMap setting = (PropertyMap)p;
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
            throw new RuntimeException(summarizeAugeasError(), e);
        }
    }
    
    /**
     * This method is only used by the {@link CronTabComponent}.
     * I placed the method in the parent component class, because here, we have all the information the
     * sub component need and we don't have to make them load stuff that is accessible right here and now.
     * 
     * @param crontabPath
     * @param crontabConfigurationDefinition
     * @return
     */
    protected Configuration loadCronTab(String crontabPath, ConfigurationDefinition crontabConfigurationDefinition) {
        try {
            Augeas augeas = getAugeas();
            augeas.load();
            
            File crontabFile = new File(crontabPath);
            String basePath = AUGEAS_FILES_PREFIX + crontabFile.getAbsolutePath();
            AugeasNode baseNode = new AugeasNode(basePath);
            
            Configuration ret = new Configuration();
            ret.setNotes("Loaded by Augeas at " + new Date());
            
            PropertyDefinitionMap basicSettingsDef = crontabConfigurationDefinition.getPropertyDefinitionMap(CronTabComponent.BASIC_SETTINGS_PROP);
            PropertyMap dummyParent = new PropertyMap(".");
            basicSettingsDef.setName(".");
            loadProperty(basicSettingsDef, dummyParent, augeas, baseNode);
            basicSettingsDef.setName(CronTabComponent.BASIC_SETTINGS_PROP);
            PropertyMap basicSettings = dummyParent.getMap(".");
            basicSettings.setName(CronTabComponent.BASIC_SETTINGS_PROP);
            basicSettings.setParentMap(null);
            ret.put(basicSettings);
                    
            PropertyDefinitionList entriesDef = crontabConfigurationDefinition.getPropertyDefinitionList(CronTabComponent.ENTRIES_PROP);
            entriesDef.setName(".");
            loadProperty(entriesDef, dummyParent, augeas, baseNode);
            entriesDef.setName(CronTabComponent.ENTRIES_PROP);
            PropertyList entries = dummyParent.getList(".");
            entries.setName(CronTabComponent.ENTRIES_PROP);
            entries.setParentMap(null);
            ret.put(entries);
            
    
            List<String> envSettings = augeas.match(basePath + "/*[label() != \"entry\" and label() != \"#comment\"]");
            
            PropertyList additionalSettings = new PropertyList(CronTabComponent.ADDITIONAL_SETTINGS_PROP);
            ret.put(additionalSettings);
            
            for(String env : envSettings) {
                String name = env.substring(env.lastIndexOf(AugeasNode.SEPARATOR_CHAR) + 1);
                if (!basicSettings.getMap().containsKey(name)) {
                    String value = augeas.get(env);
                    PropertyMap entry = new PropertyMap(CronTabComponent.SETTING_PROP);
                    additionalSettings.add(entry);
                    entry.put(new PropertySimple(CronTabComponent.NAME_PROP, name));
                    entry.put(new PropertySimple(CronTabComponent.VALUE_PROP, value));                
                }
                
            }
            
            return ret;
        } catch (AugeasException e) {
            throw new RuntimeException(summarizeAugeasError(), e);
        }
    }
}
