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

/**
 * Defines a type of schedule for a server plugin job.
 * 
 * This also has a static factory that allows you to build concrete forms
 * of schedule types.
 *  
 * @author John Mazzitelli
 */

public abstract class AbstractScheduleType {
    private final String typeName;
    private final boolean concurrent;
    private final boolean clustered;

    /**
     * Factory method that creates a concrete schedule type object based on the given parameters.
     * 
     * @param concurrent if true, multiple jobs can run concurrently.
     * @param clustered if true, the job may be executed by a server other than the one where the job was scheduled
     * @param scheduleType the name of the concrete schedule type
     * @param scheduleTrigger the string form of the schedule's trigger
     * 
     * @return a new instance of the desired schedule type
     */
    public static final AbstractScheduleType create(boolean concurrent, boolean clustered, String scheduleType,
        String scheduleTrigger) {

        AbstractScheduleType scheduleTypeObj = null;
        if (PeriodicScheduleType.TYPE_NAME.equalsIgnoreCase(scheduleType)) {
            scheduleTypeObj = new PeriodicScheduleType(concurrent, clustered, Long.parseLong(scheduleTrigger));
        } else if (CronScheduleType.TYPE_NAME.equalsIgnoreCase(scheduleType)) {
            scheduleTypeObj = new CronScheduleType(concurrent, clustered, scheduleTrigger);
        }
        return scheduleTypeObj;
    }

    /**
     * Builds the schedule type.
     * 
     * @param concurrent if true, multiple jobs can run concurrently. If false, only one
     *                   scheduled job will run at any one time across the RHQ Server cloud.
     * @param clustered if true, the job may be executed by a server other than the one where the job was scheduled
     * @param typeName the name of the concrete schedule type (subclasses must provide this)
     */
    public AbstractScheduleType(boolean concurrent, boolean clustered, String typeName) {
        this.typeName = typeName;
        this.concurrent = concurrent;
        this.clustered = clustered;
    }

    /**
     * The name that identifies this type of schedule.
     * 
     * @return type name string
     */
    public String getTypeName() {
        return this.typeName;
    }

    /**
     * If true, multiple jobs can execute at any one time. If false, only a single job will be allowed
     * to run at any one time (across all servers in the RHQ Server cloud). Even if the schedule
     * is triggered multiple times, if a job is still running, any future jobs that are triggered will
     * be delayed.
     * 
     * @return concurrent flag
     */
    public boolean isConcurrent() {
        return this.concurrent;
    }

    /**
     * If <code>true</code>, the job may be executed on a server other than the one where the job was scheduled.
     * If <code>false</code>, the job will always be executed on the server where it was scheduled (it will be
     * considered a non-clustered job).
     * If this is <code>false</code>, {@link #isConcurrent()} only affects the scheduler where the job is scheduled
     * and to be executed. This means a non-clustered job may run at the same time on different machines, even
     * if {@link #isConcurrent()} is <code>false</code>. When {@link #isConcurrent()} is <code>false</code>,
     * and {@link #isClustered()} is <code>false</code>, it means that job will not run concurrently on the box
     * where it was scheduled. But if the job was scheduled multiple times on different boxes, those multiple jobs
     * can run concurrently because they are on different boxes.
     * 
     * A job should not be clustered if it must run on all servers at the same regular schedule.
     * 
     * @return clustered flag
     */
    public boolean isClustered() {
        return this.clustered;
    }

    /**
     * Returns the string form of the trigger that causes the job to be invoked.
     * 
     * @return a string that describes the trigger of the schedule
     */
    public abstract String getScheduleTrigger();
}
