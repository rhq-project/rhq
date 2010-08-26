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

import java.io.Serializable;
import java.util.Date;

import org.rhq.core.domain.operation.ScheduleJobId;

public abstract class OperationScheduleComposite implements Serializable {

    private static final long serialVersionUID = 1L;

    private ScheduleJobId operationJobId;
    private String operationName;
    private long operationNextFireTime;

    //no args constructor for serialization purposes not to be used.
    protected OperationScheduleComposite() {
        this.operationNextFireTime = -1;
        this.operationJobId = null;
        this.operationName = "(uninitialized)";
    }

    public OperationScheduleComposite(ScheduleJobId operationJobId, String operationName, long operationNextFireTime) {
        this.operationJobId = operationJobId;
        this.operationName = operationName;
        this.operationNextFireTime = operationNextFireTime;
    }

    public ScheduleJobId getOperationJobId() {
        return operationJobId;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        // Today, the OperationScheduleEntity does not contain the operation name and we can't query for it.
        // So we will allow operationName to have a setter so after the initial query that creates
        // this composite object is run, we'll allow someone else to come in and fill in the name of the
        // operation (which should be the operation's "display name" since this composite object
        // is used for displaying the information to the user).
        this.operationName = operationName;
    }

    public long getOperationNextFireTime() {
        return operationNextFireTime;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("operation-job-id=[" + operationJobId);
        str.append("], operation-name=[" + operationName);
        str.append("], operation-next-fire-time=[" + new Date(operationNextFireTime));
        str.append("]");
        return str.toString();
    }
}