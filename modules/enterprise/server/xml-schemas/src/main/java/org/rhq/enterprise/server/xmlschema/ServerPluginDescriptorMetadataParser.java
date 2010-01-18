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
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginComponentType;
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
    private static final String SCHEDULED_JOB_PROP_NAME_CLUSTERED = "clustered";
    private static final String SCHEDULED_JOB_PROP_NAME_SCHEDULE_TYPE = "scheduleType";
    private static final String SCHEDULED_JOB_PROP_NAME_SCHEDULE_TRIGGER = "scheduleTrigger";

    /**
     * Returns the fully qualified class name of the plugin component.
     * If the descriptor did not define a plugin component, this will return <code>null</code>.
     * @param descriptor
     *
     * @return the name of the plugin component class, or <code>null</code> if not specified
     */
    public static String getPluginComponentClassName(ServerPluginDescriptorType descriptor) {

        ServerPluginComponentType componentXml = descriptor.getPluginComponent();
        if (componentXml == null) {
            return null;
        }

        String className = componentXml.getClazz();
        if (className == null) {
            // this should never happen, the xml schema validation should have caught this earlier
            throw new IllegalArgumentException("Missing plugin component classname for plugin " + descriptor.getName());
        }

        className = getFullyQualifiedClassName(descriptor, className);

        return className;
    }

    /**
     * Given a plugn descriptor that may or may not have defined a {@link ServerPluginDescriptorType#getPackage() package name},
     * this converts the given class name to a fully qualified class name.
     * 
     * If the descriptor does not define a package name, this method does nothing and returns <code>className</code> unchanged.
     * 
     * If <code>className</code> contains at least one "." character, it is assumed to be already fully qualified and so it
     * will be returned unchanged.
     * 
     * If <code>className</code> has no "." characters, and the descriptor defines a package name, that package name
     * will prefix the given class name and will form the fully qualified class name that is returned.
     * 
     * @param descriptor a plugin descriptor that may or may not define a package name
     * @param className a classname that may or may not be fully qualified
     * @return the fully qualified class name or <code>null</code> if <code>className</code> is <code>null</code>
     */
    public static String getFullyQualifiedClassName(ServerPluginDescriptorType descriptor, String className) {

        if (className != null) {
            String pkg = (descriptor != null) ? descriptor.getPackage() : null;
            if ((className.indexOf('.') == -1) && (pkg != null)) {
                className = pkg + '.' + className;
            }
        }
        return className;
    }

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
     * Returns the scheduled jobs configuration definition for the plugin.
     * Use {@link #getScheduledJobs(ServerPluginDescriptorType)} to return a list of a more
     * strongly typed jobs object, as opposed to a generic configuration definition.
     * 
     * @param descriptor
     * 
     * @return the scheduled jobs configuration definition, or <code>null</code> if the descriptor did not define plugin config.
     *
     * @throws Exception if the plugin descriptor was invalid
     */
    public static ConfigurationDefinition getScheduledJobsDefinition(ServerPluginDescriptorType descriptor)
        throws Exception {

        ConfigurationDefinition config = null;
        ConfigurationDescriptor configXml = descriptor.getScheduledJobs();
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
            String clusteredStr = defaults.getSimpleValue(SCHEDULED_JOB_PROP_NAME_CLUSTERED, "true");
            String scheduleTypeStr = defaults.getSimpleValue(SCHEDULED_JOB_PROP_NAME_SCHEDULE_TYPE,
                PeriodicScheduleType.TYPE_NAME);
            String scheduleTriggerStr = defaults.getSimpleValue(SCHEDULED_JOB_PROP_NAME_SCHEDULE_TRIGGER, "600000");

            String jobId = jobMapDef.getName();
            boolean enabled = Boolean.parseBoolean(enabledStr);
            boolean concurrent = Boolean.parseBoolean(concurrentStr);
            boolean clustered = Boolean.parseBoolean(clusteredStr);

            AbstractScheduleType scheduleType;
            scheduleType = AbstractScheduleType.create(concurrent, clustered, scheduleTypeStr, scheduleTriggerStr);
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
        } else if (!(mapDef instanceof PropertyDefinitionList)) {
            // mapDef isn't even a list (which would have indicated its a valid list-of-maps of jobs) - so assume its invalid 
            throw new Exception("Invalid scheduled job definition [" + mapDef.getName() + "]");
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
            String clusteredStr = jobMap.getSimpleValue(SCHEDULED_JOB_PROP_NAME_CLUSTERED, "true");
            String scheduleTypeStr = jobMap.getSimpleValue(SCHEDULED_JOB_PROP_NAME_SCHEDULE_TYPE,
                PeriodicScheduleType.TYPE_NAME);
            String scheduleTriggerStr = jobMap.getSimpleValue(SCHEDULED_JOB_PROP_NAME_SCHEDULE_TRIGGER, "600000");

            String jobId = jobMap.getName();
            boolean enabled = Boolean.parseBoolean(enabledStr);
            boolean concurrent = Boolean.parseBoolean(concurrentStr);
            boolean clustered = Boolean.parseBoolean(clusteredStr);

            AbstractScheduleType scheduleType;
            scheduleType = AbstractScheduleType.create(concurrent, clustered, scheduleTypeStr, scheduleTriggerStr);
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
