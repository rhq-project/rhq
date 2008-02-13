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
package org.rhq.core.pluginapi.operation;

import org.rhq.core.domain.configuration.Configuration;

/**
 * This interface is used by a plugin to communicate back into the plugin container for operations related tasks.
 *
 * @author Jason Dobies
 */
public interface OperationServices {
    /**
     * Synchronously invokes an operation on the resource. The resource against which the operation will be executed is
     * specified as part of the {@link OperationContext}. The name of the operation must correspond to an operation
     * defined in the plugin descriptor for resources of the associated resource's type.
     *
     * @param  context             passed into the {@link OperationFacet} at startup, this is used to identify the
     *                             resource against which the operation will run
     * @param  name                name of the operation being run; this must be the same name as an operation defined
     *                             in the plugin descriptor
     * @param  operationParameters any parameters necessary to invoke the operation; these parameters are defined in the
     *                             operation definition in the plugin descriptor
     * @param  timeout             time in seconds to wait before cancelling the operation; must be > 0.
     *
     * @return result object describing the results of invoking the operation; will not be <code>null</code>
     */
    OperationServicesResult invokeOperation(OperationContext context, String name, Configuration operationParameters,
        long timeout);
}