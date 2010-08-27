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

import org.rhq.core.domain.operation.OperationRequestStatus;

public class ResourceOperationLastCompletedComposite extends OperationLastCompletedComposite {

    private static final long serialVersionUID = 1L;

    private int resourceId;
    private String resourceName;
    private String resourceTypeName;

    //no args constructor. Not to be used. java bean/serialization requirement
    private ResourceOperationLastCompletedComposite() {
        super();
        this.resourceTypeName = "(unitialized type)";
        this.resourceName = "(unitialized)";
        this.resourceId = 0;
    }

    public ResourceOperationLastCompletedComposite(int operationId, String operationName, long operationStartTime,
        OperationRequestStatus operationStatus, int resourceId, String resourceName, String resourceTypeName) {
        super(operationId, operationName, operationStartTime, operationStatus);
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
}