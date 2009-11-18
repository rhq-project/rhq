package org.rhq.plugins.cron.test;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.plugins.augeas.AugeasConfigurationComponent;
import org.rhq.plugins.augeas.test.AbstractAugeasConfigurationComponentTest;

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

/**
 * Tests for cron component.
 * 
 * @author Lukas Krejci
 */
public class CronComponentTest extends AbstractAugeasConfigurationComponentTest {

    @Override
    protected void tweakDefaultPluginConfig(Configuration defaultPluginConfig) {
        super.tweakDefaultPluginConfig(defaultPluginConfig);
        //the base cannot support the /etc/cron.d/* glob pattern because it reads stuff from the classloader resource stream
        //that doesn't support searching (or does it?)
        //so we have to update the include globs to include only concrete file paths
        defaultPluginConfig.put(new PropertySimple(AugeasConfigurationComponent.INCLUDE_GLOBS_PROP, "/etc/crontab|/etc/cron.d/another-crontab"));
    }

    @Override
    protected Configuration getExpectedResourceConfig() {
        Configuration config = new Configuration();
        
        PropertyList hourlyJobs = new PropertyList("hourlyJobs");
        config.put(hourlyJobs);
        
        PropertyList dailyJobs = new PropertyList("dailyJobs");
        config.put(dailyJobs);
        
        PropertyList weeklyJobs = new PropertyList("weeklyJobs");
        config.put(weeklyJobs);
        
        PropertyList monthlyJobs = new PropertyList("monthlyJobs");
        config.put(monthlyJobs);
        
        PropertyList yearlyJobs = new PropertyList("yearlyJobs");
        config.put(yearlyJobs);
        
        PropertyMap yearlyJob = new PropertyMap(".");
        yearlyJob.put(new PropertySimple("time/minute", "0"));
        yearlyJob.put(new PropertySimple("time/hour", "0"));
        yearlyJob.put(new PropertySimple("time/dayofmonth", "1"));
        yearlyJob.put(new PropertySimple("time/month", "1"));
        yearlyJob.put(new PropertySimple("time/dayofweek", "*"));
        yearlyJob.put(new PropertySimple("user", "root"));
        yearlyJob.put(new PropertySimple(".", "echo \"tmp\""));
        yearlyJob.put(new PropertySimple("crontab", "/etc/crontab"));
        yearlyJobs.add(yearlyJob);
        
        PropertyMap dailyJob = new PropertyMap(".");
        dailyJob.put(new PropertySimple("time/minute", "1"));
        dailyJob.put(new PropertySimple("time/hour", "0"));
        dailyJob.put(new PropertySimple("time/dayofmonth", "*"));
        dailyJob.put(new PropertySimple("time/month", "*"));
        dailyJob.put(new PropertySimple("time/dayofweek", "*"));
        dailyJob.put(new PropertySimple("user", "root"));
        dailyJob.put(new PropertySimple(".", "echo \"tmp\""));
        dailyJob.put(new PropertySimple("crontab", "/etc/cron.d/another-crontab"));
        dailyJobs.add(dailyJob);
        
        return config;
    }

    @Override
    protected String getPluginName() {
        return "Cron";
    }

    @Override
    protected String getResourceTypeName() {
        return "Cron";
    }

    @Override
    protected Configuration getUpdatedResourceConfig() {
        //This component doesn't support updates.
        return getExpectedResourceConfig();
    }

    //TODO add createChildResource tests
}
