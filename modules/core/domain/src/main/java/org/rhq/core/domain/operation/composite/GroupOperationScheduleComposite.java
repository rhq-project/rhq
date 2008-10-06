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
package org.rhq.core.domain.operation.composite;

import org.rhq.core.domain.operation.ScheduleJobId;

/**
 * Provides some information on a compatible group's scheduled operation.
 *
 * @author John Mazzitelli
 */
public class GroupOperationScheduleComposite extends OperationScheduleComposite {
    private final int groupId;
    private final String groupName;
    private final String groupResourceTypeName;

    public GroupOperationScheduleComposite(String jobName, String jobGroup, String operationName,
        long operationNextFireTime, int groupId, String groupName, String groupResourceTypeName) {
        super(new ScheduleJobId(jobName, jobGroup), operationName, operationNextFireTime);
        this.groupId = groupId;
        this.groupName = groupName;
        this.groupResourceTypeName = groupResourceTypeName;
    }

    public int getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getGroupResourceTypeName() {
        return groupResourceTypeName;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("GroupOperationScheduleComposite: " + super.toString());
        str.append(", group-id=[" + groupId);
        str.append("], group-name=[" + groupName);
        str.append("], group-resource-type-name=[" + groupResourceTypeName);
        str.append("]");
        return str.toString();
    }
}