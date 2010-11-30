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

package org.rhq.enterprise.server.plugin.pc;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.xmlschema.AbstractScheduleType;
import org.rhq.enterprise.server.xmlschema.ScheduledJobDefinition;
import org.rhq.enterprise.server.xmlschema.ServerPluginDescriptorMetadataParser;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

/**
 * The superclass to the concurrent and stateful job wrappers.
 * See the subclasses for more information. Plugin developers
 * do not use this class nor do they extend this class.
 *
 * @author John Mazzitelli
 */
abstract class AbstractJobWrapper implements Job {
    private final Log log = LogFactory.getLog(this.getClass());

    /**
     * All keys prefixed with this leader identify the data map values as our internal values.
     * Any data map key without this prefix is assumed to be part of the plugin callback data.
     */
    private static final String DATAMAP_LEADER = "__";

    /**
     *  Key to the job data map that indicates the plugin that scheduled the job.
     */
    public static final String DATAMAP_PLUGIN_NAME = DATAMAP_LEADER + "pluginName";

    /**
     *  Key to the job data map that indicates the type of plugin that scheduled the job.
     */
    public static final String DATAMAP_PLUGIN_TYPE = DATAMAP_LEADER + "pluginType";

    /**
     *  Key to the job data map that indicates the ID of the job.
     *   This ID is passed to the plugin container when the job was scheduled, see:
     *  {@link AbstractTypeServerPluginContainer#scheduleJob(ScheduledJobDefinition, String)}
     */
    public static final String DATAMAP_JOB_ID = DATAMAP_LEADER + "jobId";

    /**
     * Key to the job data map that indicates which plugin component class should be instantiated
     * in order to process the job. If not specified, the main plugin component instance is used.
     */
    public static final String DATAMAP_JOB_CLASS = DATAMAP_LEADER + "jobClass";

    /**
     * Key to the job data map that indicates which method on the job class should be invoked
     * to run the job.
     */
    public static final String DATAMAP_JOB_METHOD_NAME = DATAMAP_LEADER + "jobMethodName";

    /**
     * Key to the job data map that indicates what kind of schedule triggered the job
     */
    public static final String DATAMAP_SCHEDULE_TYPE = DATAMAP_LEADER + "scheduleType";

    /**
     * Key to the job data map that indicates how the schedule is triggered.
     * The value of the trigger datamap entry depends on the {@link #DATAMAP_SCHEDULE_TYPE type of schedule}.
     * For example, a cron schedule type has a schedule trigger that is a cron expression.
     * A periodic schedule type has a schedule trigger that is a time period, in milliseconds.
     */
    public static final String DATAMAP_SCHEDULE_TRIGGER = DATAMAP_LEADER + "scheduleTrigger";

    /**
     * Key to the job data map that indicates if the job is concurrent or not. The
     * value does not necessarily mean any currently executing job is concurrently running
     * with another; this just indicates if the job is allowed to run concurrently with another.
     */
    public static final String DATAMAP_IS_CONCURRENT = DATAMAP_LEADER + "isConcurrent";

    /**
     * Key to the job data map that indicates if the job is clustered or not. If this
     * job is clustered, the job may be executing on a machine that did not originally
     * schedule the job.
     */
    public static final String DATAMAP_IS_CLUSTERED = DATAMAP_LEADER + "isClustered";

    protected abstract ScheduledJobInvocationContext createContext(ScheduledJobDefinition jobDefinition,
        ServerPluginContext pluginContext, ServerPluginComponent serverPluginComponent, Map<String, String> jobData);

    /**
     * This is the method that quartz calls when the schedule has triggered. This method will
     * delegate to the plugin component that is responsible to do work for the plugin.
     * 
     * @see ScheduledJob#execute(String, Properties)
     */
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.debug("scheduled server plugin job triggered");

        JobDataMap dataMap = context.getJobDetail().getJobDataMap();

        // obtain our internal values that we need from the datamap 
        String pluginName = dataMap.getString(DATAMAP_PLUGIN_NAME);
        String pluginTypeString = dataMap.getString(DATAMAP_PLUGIN_TYPE);
        String jobId = dataMap.getString(DATAMAP_JOB_ID);
        String jobClass = dataMap.getString(DATAMAP_JOB_CLASS);
        String jobMethodName = dataMap.getString(DATAMAP_JOB_METHOD_NAME);
        boolean isConcurrent = Boolean.parseBoolean(dataMap.getString(DATAMAP_IS_CONCURRENT));
        boolean isClustered = Boolean.parseBoolean(dataMap.getString(DATAMAP_IS_CLUSTERED));
        String scheduleTypeStr = dataMap.getString(DATAMAP_SCHEDULE_TYPE);
        String scheduleTrigger = dataMap.getString(DATAMAP_SCHEDULE_TRIGGER);

        // verify the datamap has all the things we require
        if (pluginName == null) {
            throwJobExecutionException(pluginName, pluginTypeString, jobId, "Datamap missing plugin name", null);
        }
        if (pluginTypeString == null) {
            throwJobExecutionException(pluginName, pluginTypeString, jobId, "Datamap missing plugin type", null);
        }
        if (jobId == null) {
            throwJobExecutionException(pluginName, pluginTypeString, jobId, "Datamap missing job ID", null);
        }
        if (jobMethodName == null) {
            throwJobExecutionException(pluginName, pluginTypeString, jobId, "Datamap missing method name", null);
        }

        // obtain the plugin's own callback data from the datamap
        Properties callbackData = new Properties();
        for (String key : dataMap.getKeys()) {
            if (!key.startsWith(DATAMAP_LEADER)) {
                callbackData.setProperty(key, dataMap.getString(key));
            }
        }

        // determine what type of plugin is being triggered
        ServerPluginType pluginType = null;
        try {
            pluginType = new ServerPluginType(pluginTypeString);
        } catch (Throwable t) {
            // datamap has an invalid plugin type string - we need to unschedule this, do not refire
            throwJobExecutionException(pluginName, pluginTypeString, jobId, "Datamap had invalid plugin type string", t);
        }

        // do not execute the job if the master plugin container has been stopped
        ServerPluginServiceManagement serverPluginService = LookupUtil.getServerPluginService();
        if (!serverPluginService.isMasterPluginContainerStarted()) {
            String msg = "The master plugin container is shutdown, will not execute job here, will resubmit";
            log.error(logMsg(pluginName, pluginType, jobId, msg, null));
            JobExecutionException exception = new JobExecutionException();

            // we only refire immediately if we are in an HA environment- which means another server might
            // be able to take over - and that other server might have its master PC running for us to execute in.
            boolean isHA = LookupUtil.getCloudManager().getNormalServerCount() > 1;
            exception.setRefireImmediately(isHA);

            // abort this invocation now - we can't run without the plugin containers
            return;
        }

        // determine which plugin component class will be invoked to perform the work of the job
        MasterServerPluginContainer mpc = serverPluginService.getMasterPluginContainer();
        AbstractTypeServerPluginContainer pc = mpc.getPluginContainerByPluginType(pluginType);

        Object pluginJobObject = null;
        ServerPluginManager pluginManager = pc.getPluginManager();
        ServerPluginEnvironment pluginEnv = pluginManager.getPluginEnvironment(pluginName);

        if (pluginEnv == null) {
            throwJobExecutionException(pluginName, pluginType, jobId, "missing environment for plugin [" + pluginName
                + "]", null);
        }

        ServerPluginComponent pluginComponent = pluginManager.getServerPluginComponent(pluginName);

        if (jobClass == null) {
            // null classname means use the stateful plugin component as the target object
            pluginJobObject = pluginComponent;
            if (pluginJobObject == null) {
                throwJobExecutionException(pluginName, pluginType, jobId, "no plugin component to process job", null);
            }
        } else {
            try {
                ServerPluginDescriptorType descriptor = pluginEnv.getPluginDescriptor();
                jobClass = ServerPluginDescriptorMetadataParser.getFullyQualifiedClassName(descriptor, jobClass);
                pluginJobObject = pluginManager.instantiatePluginClass(pluginEnv, jobClass);
            } catch (Throwable t) {
                // invalid class - we need to unschedule this, do not refire since it will never work
                throwJobExecutionException(pluginName, pluginType, jobId, "bad job class [" + jobClass + "]", t);
            }
        }

        // try to find the method to invoke - first, see if the method exists with a parameter
        // that is the type of our job invocation context. Otherwise, rely on a no-arg method.
        Method jobMethod = null;
        Object[] params = null;

        try {
            jobMethod = pluginJobObject.getClass().getMethod(jobMethodName, ScheduledJobInvocationContext.class);
            params = new Object[1];

            AbstractScheduleType scheduleType = AbstractScheduleType.create(isConcurrent, isClustered, scheduleTypeStr,
                scheduleTrigger);
            if (scheduleType == null) {
                // how is this possible that we got bad schedule data in the datamap? this isn't fatal, just log it and leave it null
                log.warn(logMsg(pluginName, pluginType, jobId, "ignoring bad schedule type found in data map ["
                    + scheduleTypeStr + "]", null));
            }

            ScheduledJobDefinition jobDefinition = new ScheduledJobDefinition(jobId, true, jobClass, jobMethodName,
                scheduleType, callbackData);
            ServerPluginContext pluginContext = pluginManager.getServerPluginContext(pluginEnv);
            params[0] = createContext(jobDefinition, pluginContext, pluginComponent,dataMap);
        } catch (NoSuchMethodException e) {
            try {
                // see if there is a no-arg method of the given name
                jobMethod = pluginJobObject.getClass().getMethod(jobMethodName);
                params = null;
            } catch (Throwable t) {
                // invalid method - we need to unschedule this, do not refire since it will never work
                throwJobExecutionException(pluginName, pluginType, jobId, "bad schedule job method [" + jobMethodName
                    + "]", t);
            }
        }

        // now actually tell the plugin its time to do the scheduled job
        try {
            ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(pluginEnv.getPluginClassLoader());
                jobMethod.invoke(pluginJobObject, params);
            } finally {
                Thread.currentThread().setContextClassLoader(originalContextClassLoader);
            }
            log.info(logMsg(pluginName, pluginType, jobId, "scheduled job executed", null));
        } catch (Throwable t) {
            // any exception thrown out of the job will mean the job is to be unscheduled
            throwJobExecutionException(pluginName, pluginType, jobId, "job threw exception, unscheduling it", t);
        }

        return;
    }

    protected void throwJobExecutionException(String pluginName, Object pluginType, String jobId, String errorMsg,
        Throwable t) throws JobExecutionException {

        log.error(logMsg(pluginName, pluginType, jobId, errorMsg, t));

        JobExecutionException jobException;
        if (t != null) {
            jobException = new JobExecutionException(t, false);
        } else {
            jobException = new JobExecutionException(false);
        }
        jobException.setUnscheduleFiringTrigger(true);
        throw jobException;
    }

    protected String logMsg(String pluginName, Object pluginType, String jobId, String msg, Throwable t) {
        String s = "Plugin [" + pluginName + "], type=[" + pluginType + "], jobId=[" + jobId + "]: " + msg;
        if (t != null) {
            s = s + ". Cause: " + ThrowableUtil.getAllMessages(t);
        }
        return s;
    }
}
