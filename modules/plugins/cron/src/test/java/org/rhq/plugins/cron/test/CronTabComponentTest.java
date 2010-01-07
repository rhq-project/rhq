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

package org.rhq.plugins.cron.test;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.plugins.augeas.AugeasConfigurationComponent;
import org.rhq.plugins.augeas.test.AbstractAugeasConfigurationComponentTest;

/**
 * 
 * 
 * @author Lukas Krejci
 */
public class CronTabComponentTest extends AbstractAugeasConfigurationComponentTest {

    @Override
    protected void tweakDefaultPluginConfig(Configuration defaultPluginConfig) {
        super.tweakDefaultPluginConfig(defaultPluginConfig);
        //the base cannot support the /etc/cron.d/* glob pattern because it reads stuff from the classloader resource stream
        //that doesn't support searching (or does it?)
        //so we have to update the include globs to include only concrete file paths
        defaultPluginConfig.put(new PropertySimple(AugeasConfigurationComponent.INCLUDE_GLOBS_PROP, CronComponentTest.INCLUDE_GLOBS));
    }

    @Override
    protected Set<Resource> getResources() {
        //let's pick the "another-crontab" file
        InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
        ResourceType resourceType = getResourceType();
        Set<Resource> resources = inventoryManager.getResourcesWithType(resourceType);
        
        for(Resource res : resources) {
            if ("/etc/cron.d/another-crontab".equals(res.getResourceKey())) {
                return Collections.singleton(res);
            }
        }
        return Collections.emptySet();
    }

    @Override
    protected Configuration getExpectedResourceConfig() {
        Configuration config = new Configuration();
        
        PropertyList envVars = new PropertyList("environmentSettings");
        config.put(envVars);
        
        PropertyList entries = new PropertyList("entries");
        config.put(entries);
        
        PropertyMap envVar = new PropertyMap("var");
        envVar.put(new PropertySimple("name", "SHELLVAR"));
        envVar.put(new PropertySimple("value", "value"));
        envVars.add(envVar);
        
        PropertyMap entry = new PropertyMap("entry");
        entry.put(new PropertySimple("time/minute", "1"));
        entry.put(new PropertySimple("time/hour", "0"));
        entry.put(new PropertySimple("time/dayofmonth", "*"));
        entry.put(new PropertySimple("time/month", "*"));
        entry.put(new PropertySimple("time/dayofweek", "*"));
        entry.put(new PropertySimple("user", "root"));
        entry.put(new PropertySimple(".", "echo \"tmp\""));
        entries.add(entry);

        return config;
    }

    @Override
    protected String getPluginName() {
        return "Cron";
    }

    @Override
    protected String getResourceTypeName() {
        return "Cron Tab";
    }

    @Override
    protected Configuration getUpdatedResourceConfig() {
        Configuration config = getExpectedResourceConfig();
        
        //update the envVar and add a new one
        PropertyList envVars = config.getList("environmentSettings");
        PropertyMap existingVar = (PropertyMap) envVars.getList().get(0);
        existingVar.put(new PropertySimple("name", "SHELLVARUPDATED"));
        existingVar.put(new PropertySimple("value", "updated_value"));
        
        PropertyMap newVar = new PropertyMap("var");
        newVar.put(new PropertySimple("name", "SHELLVARNEW"));
        newVar.put(new PropertySimple("value", "value"));
        envVars.add(newVar);
        
        
        //update existing and add a new entry
        PropertyList entries = config.getList("entries");
        PropertyMap existingEntry = (PropertyMap) entries.getList().get(0);
        existingEntry.put(new PropertySimple("time/minute", "2"));
        
        PropertyMap newEntry = new PropertyMap("entry");
        newEntry.put(new PropertySimple("time/minute", "1"));
        newEntry.put(new PropertySimple("time/hour", "0"));
        newEntry.put(new PropertySimple("time/dayofmonth", "*"));
        newEntry.put(new PropertySimple("time/month", "*"));
        newEntry.put(new PropertySimple("time/dayofweek", "*"));
        newEntry.put(new PropertySimple("user", "foo"));
        newEntry.put(new PropertySimple(".", "echo \"tmp\""));
        entries.add(newEntry);
        
        return config;
    }

}
