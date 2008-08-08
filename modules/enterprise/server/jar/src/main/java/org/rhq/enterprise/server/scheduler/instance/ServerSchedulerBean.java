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
package org.rhq.enterprise.server.scheduler.instance;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.quartz.Job;
import org.quartz.SchedulerException;

import org.rhq.enterprise.server.cluster.instance.ServerManagerLocal;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;

/**
 * @author Joseph Marques
 */

@Stateless
public class ServerSchedulerBean implements ServerSchedulerLocal {

    @EJB
    ServerManagerLocal serverManager;

    @EJB
    SchedulerLocal scheduler;

    public void scheduleRepeatingJob(String jobName, String groupName, Class<? extends Job> jobClass,
        boolean rescheduleIfExists, boolean isVolatile, long initialDelay, long interval) throws SchedulerException {
        scheduler.scheduleRepeatingJob(jobName + "_" + serverManager.getIdentity(), groupName, null, jobClass,
            rescheduleIfExists, isVolatile, initialDelay, interval);
    }

    public void scheduleSimpleRepeatingJob(Class<? extends Job> jobClass, boolean rescheduleIfExists,
        boolean isVolatile, long initialDelay, long interval) throws SchedulerException {
        scheduler.scheduleRepeatingJob(jobClass.getName() + "_" + serverManager.getIdentity(), jobClass.getName(),
            null, jobClass, rescheduleIfExists, isVolatile, initialDelay, interval);
    }
}
