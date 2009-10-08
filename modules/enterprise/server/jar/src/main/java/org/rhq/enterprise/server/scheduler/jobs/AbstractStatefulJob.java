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
package org.rhq.enterprise.server.scheduler.jobs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

/**
 * Provides a wrapper around Quartz stateful job.  Implementations of this
 * job will never trigger a misfire because the execute method will never
 * throw exceptions back to Quartz.
 * 
 * Jobs extending this class are assured by Quartz to never run concurrently within the cluster.
 * 
 * @author John Mazzitelli
 */
public abstract class AbstractStatefulJob implements StatefulJob {
    private final static Log LOG = LogFactory.getLog(AbstractStatefulJob.class);

    /**
     * Quartz will call this method when it needs to execute the job. This
     * is simply a wrapper around the subclass' implementation code, catching
     * and logging all exceptions (but not throwing those exceptions back to Quartz).
     * 
     * @see StatefulJob#execute(JobExecutionContext)
     */
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            executeJobCode(context);
        } catch (Throwable t) {
            LOG.error("Failed to execute job [" + this.getClass().getSimpleName() + "]", t);
        }
    }

    /**
     * Subclasses should implement this method to perform the actual job.
     * @param context
     * @throws JobExecutionException
     */
    abstract public void executeJobCode(JobExecutionContext context) throws JobExecutionException;
}
