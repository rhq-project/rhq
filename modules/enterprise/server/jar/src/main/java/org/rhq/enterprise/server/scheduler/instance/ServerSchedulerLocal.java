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

import javax.ejb.Local;

import org.quartz.Job;
import org.quartz.SchedulerException;

/**
 * @author Joseph Marques
 */

@Local
public interface ServerSchedulerLocal {

    void scheduleRepeatingJob(String jobName, String groupName, Class<? extends Job> jobClass,
        boolean rescheduleIfExists, boolean isVolatile, long initialDelay, long interval) throws SchedulerException;

    void scheduleSimpleRepeatingJob(Class<? extends Job> jobClass, boolean rescheduleIfExists, boolean isVolatile,
        long initialDelay, long interval) throws SchedulerException;

}
