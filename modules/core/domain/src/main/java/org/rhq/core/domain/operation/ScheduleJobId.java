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
package org.rhq.core.domain.operation;

/**
 * This class is a mutable version of {@link JobId} and is used as the primary key to the
 * {@link OperationScheduleEntity} entities.
 *
 * @author John Mazzitelli
 */
public class ScheduleJobId extends JobId {
    private static final long serialVersionUID = 1L;

    public ScheduleJobId() {
        // this constructor is required due to the use of @IdClass
        // in the entities that use this class as a PK
        // let's just initialize with empty job name/group
        super("", "");
    }

    public ScheduleJobId(String jobName, String jobGroup) {
        super(jobName, jobGroup);
    }

    public ScheduleJobId(String jobIdString) {
        super(jobIdString);
    }

    public ScheduleJobId(String[] jobIdParts) {
        super(jobIdParts);
    }

    @Override
    public void setJobName(String jobName) {
        super.setJobName(jobName);
    }

    @Override
    public void setJobGroup(String jobGroup) {
        super.setJobGroup(jobGroup);
    }
}