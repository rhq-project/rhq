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