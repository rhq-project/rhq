/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.core.pc.operation;

import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pluginapi.operation.OperationContext;
import org.rhq.core.pluginapi.operation.OperationServices;

/**
 * Plugin container implementation of the {@link OperationContext}, holding on to an immutable copy of the resource ID
 * to which an instance of this class is associated.
 *
 * @author Jason Dobies
 */
public class OperationContextImpl implements OperationContext {
    private final int resourceId;
    private final OperationManager operationManager;

    public OperationContextImpl(int resourceId, OperationManager operationManager) {
        this.resourceId = resourceId;
        this.operationManager = operationManager;
    }

    @Deprecated
    public OperationContextImpl(int resourceId) {
        this(resourceId, PluginContainer.getInstance().getOperationManager());
    }

    public OperationServices getOperationServices() {
        return new OperationServicesAdapter(operationManager);
    }

    public int getResourceId() {
        return resourceId;
    }
}
