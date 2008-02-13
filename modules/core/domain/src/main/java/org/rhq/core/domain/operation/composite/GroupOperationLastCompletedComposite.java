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

import org.rhq.core.domain.operation.OperationRequestStatus;

public class GroupOperationLastCompletedComposite extends OperationLastCompletedComposite {
    private final int groupId;
    private final String groupName;
    private final String groupResourceTypeName;

    public GroupOperationLastCompletedComposite(int operationId, String operationName, long operationStartTime,
        OperationRequestStatus operationStatus, int groupId, String groupName, String groupResourceTypeName) {
        super(operationId, operationName, operationStartTime, operationStatus);
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
}