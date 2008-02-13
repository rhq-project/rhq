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
package org.rhq.core.pc.operation;

import org.rhq.core.pluginapi.operation.OperationContext;
import org.rhq.core.pluginapi.operation.OperationServices;

/**
 * Plugin container implementation of the {@link OperationContext}, holding on to an immutable copy of the resource ID
 * to which an instance of this class is associated.
 *
 * @author Jason Dobies
 */
public class OperationContextImpl implements OperationContext {
    private int resourceId;
    private OperationServices operationServices;

    public OperationContextImpl(int resourceId, OperationServices operationServices) {
        this.resourceId = resourceId;
        this.operationServices = operationServices;
    }

    public OperationServices getOperationServices() {
        return operationServices;
    }

    public int getResourceId() {
        return resourceId;
    }
}