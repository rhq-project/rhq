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

package org.rhq.enterprise.server.xmlschema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.rhq.core.clientapi.agent.metadata.ConfigurationMetadataParser;
import org.rhq.core.clientapi.agent.metadata.InvalidPluginDescriptorException;
import org.rhq.core.clientapi.descriptor.configuration.ConfigurationDescriptor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

/**
 * Provides methods to parse the server plugin descriptors.
 * 
 * @author John Mazzitelli
 */
public class ServerPluginDescriptorMetadataParser {

    // pre-defined property names for schedule definitions
    private static final String SCHEDULED_JOB_PROP_NAME_ENABLED = "enabled";
    private static final String SCHEDULED_JOB_PROP_NAME_CLASS = "class";
    private static final String SCHEDULED_JOB_PROP_NAME_METHOD_NAME = "methodName";
    private static final String SCHEDULED_JOB_PROP_NAME_CONCURRENT = "concurrent";
    private static final String SCHEDULED_JOB_PROP_NAME_SCHEDULE_TYPE = "scheduleType";
    private static final String SCHEDULED_JOB_PROP_NAME_SCHEDULE_TRIGGER = "scheduleTrigger";

    /**
     * Returns the global configuration definition for the plugin. This does not include any scheduled
     * job information - see {@link #getScheduledJobs(ServerPluginDescriptorType)} for that.
     * 
     * @param descriptor
     * 
     * @return the plugin configuration definition, or <code>null</code> if the descriptor did not define plugin config.
     *
     * @throws Exception if the plugin descriptor was invalid
     */
    public static ConfigurationDefinition getPluginConfigurationDefinition(ServerPluginDescriptorType descriptor)
        throws Exception {

        ConfigurationDefinition config = null;
        ConfigurationDescriptor configXml = descriptor.getPluginConfiguration();
        if (configXml != null) {
            config = ConfigurationMetadataParser.parse(descriptor.getName(), configXml);
        }
        return config;
    }

    /**
     * Given a descriptor, this will parse it and return any scheduled jobs that it finds.
     * This essentially gives you a list of the pre-defined jobs, prior to a user customizing
     * them with their own settings.
     * 
     * @param descriptor
     * 
     * @return list of jobs that are defined, will be empty if no jobs are defined.
     *
     * @throws Exception if the plugin descriptor was invalid
     */
    public static List<ScheduledJobDefinition> getScheduledJobs(ServerPluginDescriptorType descriptor) throws Exception {
        List<ScheduledJobDefinition> jobs = new ArrayList<ScheduledJobDefinition>();

        ConfigurationDescriptor configXml = descriptor.getScheduledJobs();
        if (configXml != null) {
            ConfigurationDefinition config = ConfigurationMetadataParser.parse(descriptor.getName(), configXml);
            for (PropertyDefinition propDef : config.getPropertyDefinitions().values()) {
                ScheduledJobDefinition jobDef = getScheduledJob(propDef);
                if (jobDef != null) {
                    jobs.add(jobDef);
                }
            }
        }

        return jobs;
    }

    private static ScheduledJobDefinition getScheduledJob(PropertyDefinition mapDef) throws Exception {
        ScheduledJobDefinition job = null;

        // if the definition is not a map, it can't be a schedule
        if (mapDef instanceof PropertyDefinitionMap) {
            PropertyDefinitionMap jobMapDef = (PropertyDefinitionMap) mapDef;
            ConfigurationTemplate defaultTemplate = jobMapDef.getConfigurationDefinition().getDefaultTemplate();
            PropertyMap defaults = defaultTemplate.getConfiguration().getMap(mapDef.getName());
            // prepare some defaults if the schedule didn't define some of these
            // we assume:
            //    the map name is the methodName that will be invoked
            //    the class name is null, which means its the stateful plugin component to be invoked
            //    the schedule is always enabled
            //    the schedule is never concurrent
            //    the schedule is a periodic schedule that triggers every 10 minutes
            //    the schedule has no callback data
            String methodName = defaults.getSimpleValue(SCHEDULED_JOB_PROP_NAME_METHOD_NAME, mapDef.getName());
            String className = defaults.getSimpleValue(SCHEDULED_JOB_PROP_NAME_CLASS, null);
            String enabledStr = defaults.getSimpleValue(SCHEDULED_JOB_PROP_NAME_ENABLED, "true");
            String concurrentStr = defaults.getSimpleValue(SCHEDULED_JOB_PROP_NAME_CONCURRENT, "false");
            String scheduleTypeStr = defaults.getSimpleValue(SCHEDULED_JOB_PROP_NAME_SCHEDULE_TYPE,
                PeriodicScheduleType.TYPE_NAME);
            String scheduleTriggerStr = defaults.getSimpleValue(SCHEDULED_JOB_PROP_NAME_SCHEDULE_TRIGGER, "600000");

            String jobId = jobMapDef.getName();
            boolean enabled = Boolean.parseBoolean(enabledStr);
            boolean concurrent = Boolean.parseBoolean(concurrentStr);

            AbstractScheduleType scheduleType;
            scheduleType = AbstractScheduleType.create(concurrent, scheduleTypeStr, scheduleTriggerStr);
            if (scheduleType == null) {
                throw new InvalidPluginDescriptorException("Invalid schedule type: " + scheduleTypeStr);
            }

            // the callback data will contain all simple properties in the schedule job map
            Properties callbackData = new Properties();
            Map<String, PropertyDefinition> allPropDefs = jobMapDef.getPropertyDefinitions();
            for (PropertyDefinition currentPropDef : allPropDefs.values()) {
                if (currentPropDef instanceof PropertyDefinitionSimple) {
                    String currentPropDefName = currentPropDef.getName();
                    callbackData.setProperty(currentPropDefName, defaults.getSimpleValue(currentPropDefName, null));
                }
            }

            job = new ScheduledJobDefinition(jobId, enabled, className, methodName, scheduleType, callbackData);
        }

        return job;
    }

    /**
     * Given a configuration, this will return any scheduled jobs that are defined in it.
     * This essentially gives you a list of the jobs that may be different from the descriptor's
     * pre-defined jobs due to a user customizing them with their own settings.
     * 
     * @param jobsConfig the configuration with the scheduled job information
     * 
     * @return list of jobs that are defined, will be empty if no jobs are defined.
     *
     * @throws Exception if the configuration was invalid
     */
    public static List<ScheduledJobDefinition> getScheduledJobs(Configuration scheduledJobsConfig) throws Exception {

        List<ScheduledJobDefinition> jobs = new ArrayList<ScheduledJobDefinition>();

        if (scheduledJobsConfig != null) {
            for (Property prop : scheduledJobsConfig.getProperties()) {
                ScheduledJobDefinition jobDef = getScheduledJob(prop);
                if (jobDef != null) {
                    jobs.add(jobDef);
                } else {
                    // this might be a list-o-maps containing a list of user-define jobs
                    if (prop instanceof PropertyList) {
                        PropertyList listOfJobs = (PropertyList) prop;
                        for (Property listItem : listOfJobs.getList()) {
                            jobDef = getScheduledJob(listItem);
                            if (jobDef != null) {
                                jobs.add(jobDef);
                            }
                        }
                    }
                }
            }
        }

        return jobs;
    }

    private static ScheduledJobDefinition getScheduledJob(Property map) throws Exception {
        ScheduledJobDefinition job = null;

        // if the definition is not a map, it can't be a schedule
        if (map instanceof PropertyMap) {
            PropertyMap jobMap = (PropertyMap) map;
            // prepare some defaults if the schedule didn't define some of these
            // we assume:
            //    the map name is the methodName that will be invoked
            //    the class name is null, which means its the stateful plugin component to be invoked
            //    the schedule is always enabled
            //    the schedule is never concurrent
            //    the schedule is a periodic schedule that triggers every 10 minutes
            //    the schedule has no callback data
            String methodName = jobMap.getSimpleValue(SCHEDULED_JOB_PROP_NAME_METHOD_NAME, jobMap.getName());
            String className = jobMap.getSimpleValue(SCHEDULED_JOB_PROP_NAME_CLASS, null);
            String enabledStr = jobMap.getSimpleValue(SCHEDULED_JOB_PROP_NAME_ENABLED, "true");
            String concurrentStr = jobMap.getSimpleValue(SCHEDULED_JOB_PROP_NAME_CONCURRENT, "false");
            String scheduleTypeStr = jobMap.getSimpleValue(SCHEDULED_JOB_PROP_NAME_SCHEDULE_TYPE,
                PeriodicScheduleType.TYPE_NAME);
            String scheduleTriggerStr = jobMap.getSimpleValue(SCHEDULED_JOB_PROP_NAME_SCHEDULE_TRIGGER, "600000");

            String jobId = jobMap.getName();
            boolean enabled = Boolean.parseBoolean(enabledStr);
            boolean concurrent = Boolean.parseBoolean(concurrentStr);

            AbstractScheduleType scheduleType;
            scheduleType = AbstractScheduleType.create(concurrent, scheduleTypeStr, scheduleTriggerStr);
            if (scheduleType == null) {
                throw new InvalidPluginDescriptorException("Invalid schedule type: " + scheduleTypeStr);
            }

            // the callback data will contain all simple properties in the schedule job map
            Properties callbackData = new Properties();
            Map<String, Property> allProps = jobMap.getMap();
            for (Property currentProp : allProps.values()) {
                if (currentProp instanceof PropertySimple) {
                    String currentPropName = currentProp.getName();
                    callbackData.setProperty(currentPropName, jobMap.getSimpleValue(currentPropName, null));
                }
            }

            job = new ScheduledJobDefinition(jobId, enabled, className, methodName, scheduleType, callbackData);
        }

        return job;
    }
}
