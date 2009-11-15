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

import java.util.Properties;

/**
 * Provides information about a server plugin scheduled job.
 * 
 * @author John Mazzitelli
 */
public class ScheduledJobDefinition {
    private final String jobId;
    private final boolean enabled;
    private final String methodName;
    private final String className;
    private final AbstractScheduleType scheduleType;
    private final Properties callbackData;

    public ScheduledJobDefinition(String jobId, boolean enabled, String methodName, AbstractScheduleType scheduleType,
        Properties callbackData) {

        this.jobId = jobId;
        this.enabled = enabled;
        this.className = null; // TODO: for future.. add class to schedule xml schema to support stateless job classes
        this.methodName = methodName;
        this.scheduleType = scheduleType;
        this.callbackData = callbackData;
    }

    public String getJobId() {
        return this.jobId;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * The plugin class name that that will perform the work for the job.
     * If <code>null</code>, the plugin component instance will be used
     * (i.e. if <code>null</code>, it means the plugin component instance that was used to initialize
     * the plugin is the same instance that will handle the job; a new instance of the plugin component
     * class is not created. This allows a plugin to have a stateful object be periodically
     * invoked for each job invocation, as opposed to having a new object instantiated for
     * each job invocation).
     *
     * @return the class name that will do the work of the job
     */
    public String getClassName() {
        // if null, then it means use the plugin component instance
        return this.className;
    }

    public String getMethodName() {
        return this.methodName;
    }

    public AbstractScheduleType getScheduleType() {
        return this.scheduleType;
    }

    public Properties getCallbackData() {
        return this.callbackData;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ": job-id=" + this.jobId;
    }
}
