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

    /**
     * Factory method that creates a concrete schedule type object based on the given parameters.
     * 
     * @param concurrent if true, multiple jobs can run concurrently.
     * @param scheduleType the name of the concrete schedule type
     * @param scheduleTrigger the string form of the schedule's trigger
     * 
     * @return a new instance of the desired schedule type
     */
    public static final AbstractScheduleType create(boolean concurrent, String scheduleType, String scheduleTrigger) {
        AbstractScheduleType scheduleTypeObj = null;
        if (PeriodicScheduleType.TYPE_NAME.equalsIgnoreCase(scheduleType)) {
            scheduleTypeObj = new PeriodicScheduleType(concurrent, Long.parseLong(scheduleTrigger));
        } else if (CronScheduleType.TYPE_NAME.equalsIgnoreCase(scheduleType)) {
            scheduleTypeObj = new CronScheduleType(concurrent, scheduleTrigger);
        }
        return scheduleTypeObj;
    }

    /**
     * Builds the schedule type.
     * 
     * @param concurrent if true, multiple jobs can run concurrently. If false, only one
     *                   scheduled job will run at any one time across the RHQ Server cloud.
     * @param typeName the name of the concrete schedule type (subclasses must provide this)
     */
    public AbstractScheduleType(boolean concurrent, String typeName) {
        this.typeName = typeName;
        this.concurrent = concurrent;
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
     * Returns the string form of the trigger that causes the job to be invoked.
     * 
     * @return a string that describes the trigger of the schedule
     */
    public abstract String getScheduleTrigger();
}
