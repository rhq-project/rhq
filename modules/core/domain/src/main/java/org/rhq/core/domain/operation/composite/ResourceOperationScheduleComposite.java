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

public class ResourceOperationScheduleComposite extends OperationScheduleComposite {
    private final int resourceId;
    private final String resourceName;
    private final String resourceTypeName;

    public ResourceOperationScheduleComposite(String jobName, String jobGroup, String operationName,
        long operationNextFireTime, int resourceId, String resourceName, String resourceTypeName) {
        super(new ScheduleJobId(jobName, jobGroup), operationName, operationNextFireTime);
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.resourceTypeName = resourceTypeName;
    }

    public int getResourceId() {
        return resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getResourceTypeName() {
        return resourceTypeName;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("ResourceOperationScheduleComposite: " + super.toString());
        str.append(", resource-id=[" + resourceId);
        str.append("], resource-name=[" + resourceName);
        str.append("], resource-type-name=[" + resourceTypeName);
        str.append("]");
        return str.toString();
    }
}