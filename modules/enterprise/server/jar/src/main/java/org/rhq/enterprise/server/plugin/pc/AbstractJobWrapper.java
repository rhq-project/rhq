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

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * The superclass to the concurrent and stateful job wrappers.
 * See the subclasses for more information.
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
     *  {@link AbstractTypeServerPluginContainer#scheduleJob(Schedule, String, String, ScheduledJob, java.util.Properties)}
     */
    public static final String DATAMAP_JOB_ID = DATAMAP_LEADER + "jobId";

    /**
     * Key to the job data map that indicates which plugin component class should be instantiated
     * in order to process the job. This must implement {@link ScheduledJob}.
     */
    public static final String DATAMAP_SCHEDULED_JOB_CLASS = DATAMAP_LEADER + "scheduledJobClass";

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
        String scheduledJobClass = dataMap.getString(DATAMAP_SCHEDULED_JOB_CLASS);

        // obtain the plugin's own callback data from the datamap
        Properties callbackData = new Properties();
        for (String key : dataMap.getKeys()) {
            if (!key.startsWith(DATAMAP_LEADER)) {
                callbackData.setProperty(key, dataMap.getString(key));
            }
        }

        // determine what type of plugin is being triggered
        ServerPluginType pluginType;
        try {
            pluginType = new ServerPluginType(pluginTypeString);
        } catch (Throwable t) {
            // datamap has an invalid plugin type string - we need to unschedule this, do not refire
            log.error(logMsg(pluginName, pluginTypeString, jobId, "Datamap had invalid plugin type string", t));
            JobExecutionException jobException = new JobExecutionException(t, false);
            jobException.setUnscheduleFiringTrigger(true);
            throw jobException;
        }

        // determine which plugin component class will be invoked to perform the work of the job
        MasterServerPluginContainer mpc = LookupUtil.getServerPluginService().getMasterPluginContainer();
        AbstractTypeServerPluginContainer pc = mpc.getPluginContainerByPluginType(pluginType);

        ScheduledJob pluginJob;
        ServerPluginManager pluginManager = pc.getPluginManager();
        ServerPluginEnvironment pluginEnv = pluginManager.getPluginEnvironment(pluginName);

        if (scheduledJobClass == null) {
            try {
                pluginJob = (ScheduledJob) pluginManager.getServerPluginLifecycleListener(pluginName);
                if (pluginJob == null) {
                    log.error(logMsg(pluginName, pluginType, jobId, "no lifecycle listener to process job", null));
                    throw new UnsupportedOperationException("no lifecycle listener available to process the job");
                }
            } catch (Throwable t) {
                // no valid lifecycle listener that implements job interface - we need to unschedule this, do not refire
                log.error(logMsg(pluginName, pluginType, jobId, "invaild lifecycle listener", t));
                JobExecutionException jobException = new JobExecutionException(t, false);
                jobException.setUnscheduleFiringTrigger(true);
                throw jobException;
            }
        } else {
            try {
                pluginJob = (ScheduledJob) pluginManager.instantiatePluginClass(pluginEnv, scheduledJobClass);
            } catch (Throwable t) {
                // invalid class - we need to unschedule this, do not refire since it will never work
                log.error(logMsg(pluginName, pluginType, jobId, "invalid schedule job class", t));
                JobExecutionException jobException = new JobExecutionException(t, false);
                jobException.setUnscheduleFiringTrigger(true);
                throw jobException;
            }
        }

        ServerPluginContext pluginContext = pluginManager.createServerPluginContext(pluginEnv);

        // now actually tell the plugin its time to do the scheduled job
        try {
            pluginJob.execute(jobId, pluginContext, callbackData);
            log.info(logMsg(pluginName, pluginType, jobId, "scheduled job executed", null));
        } catch (Throwable t) {
            // any exception thrown out of the job will mean the job is to be unscheduled
            log.error(logMsg(pluginName, pluginType, jobId, "job threw exception, unscheduling it", t));
            JobExecutionException jobException = new JobExecutionException(t, false);
            jobException.setUnscheduleFiringTrigger(true);
            throw jobException;
        }

        return;
    }

    protected String logMsg(String pluginName, Object pluginType, String jobId, String msg, Throwable t) {
        String s = "Plugin [" + pluginName + "], type=[" + pluginType + "], jobId=[" + jobId + "]: " + msg;
        if (t != null) {
            s = s + ". Cause: " + ThrowableUtil.getAllMessages(t);
        }
        return s;
    }
}
