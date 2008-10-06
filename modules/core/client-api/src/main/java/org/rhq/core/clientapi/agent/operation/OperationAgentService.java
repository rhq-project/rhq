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
package org.rhq.core.clientapi.agent.operation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.domain.configuration.Configuration;

/**
 * The interface to a JON agent's operation subsystem which allows the server to execute an operation on the agent.
 */
public interface OperationAgentService {
    /**
     * Invoke the operation with the specified name.
     *
     * @param  jobId         a unique job id for the invocation - when the agent sends back the result for the
     *                       invocation, it will include this job id so the server can associate the results with the
     *                       correct invocation
     * @param  resourceId    identifies the resource on which the operation should be invoked (either a physical
     *                       resource or a compatible group)
     * @param  operationName the name of the operation
     * @param  parameters    the parameters for the operation, or <code>null</code> if the operation has no parameters
     *
     * @throws PluginContainerException if failed to submit the request to the resource for invocation. Note that this
     *                                  is <i>not</i> an indication that the actual operation invocation failed. This
     *                                  method only submits the invocation request - the actual invocation happens
     *                                  asynchronously. The server will be notified via a separate mechanism that the
     *                                  operation invocation failed.
     */
    void invokeOperation(@NotNull
    String jobId, int resourceId, @NotNull
    String operationName, @Nullable
    Configuration parameters) throws PluginContainerException;

    /**
     * Asks that the operation invocation with the given <code>jobId</code> be canceled.
     *
     * @param  jobId identifies the job that is to be canceled
     *
     * @return the results of the cancelation
     */
    CancelResults cancelOperation(@NotNull
    String jobId);
}