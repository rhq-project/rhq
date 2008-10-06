 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
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