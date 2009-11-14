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
    private final AbstractScheduleType scheduleType;
    private final Properties callbackData;

    public ScheduledJobDefinition(String jobId, boolean enabled, String methodName, AbstractScheduleType scheduleType,
        Properties callbackData) {

        this.jobId = jobId;
        this.enabled = enabled;
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

    public String getMethodName() {
        return this.methodName;
    }

    public AbstractScheduleType getScheduleType() {
        return this.scheduleType;
    }

    public Properties getCallbackData() {
        return this.callbackData;
    }
}
