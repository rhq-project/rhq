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
package org.rhq.core.clientapi.server.operation;

import org.rhq.core.communications.command.annotation.Asynchronous;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.util.exception.ExceptionPackage;

/**
 * The interface to a JON server's operation subsystem.
 */
public interface OperationServerService {
    /**
     * Called by the agent when a server-scheduled operation completes successfully.
     *
     * @param jobId          the server-assigned unique job id for this operation
     * @param result         the result of the operation
     * @param invocationTime the time at which the agent was asked to invoke the operation (epoch millis)
     * @param completionTime the time at which the operation completed (epoch millis)
     */
    @Asynchronous(guaranteedDelivery = true)
    void operationSucceeded(String jobId, Configuration result, long invocationTime, long completionTime);

    /**
     * Called by the agent when a server-scheduled operation fails.
     *
     * @param jobId          the server-assigned unique job id for this operation
     * @param result         a result object that can be used to capture any information available
     *                       up to the point of failure 
     * @param error          an exception describing why the operation failed
     * @param invocationTime the time at which the agent was asked to invoke the operation (epoch millis)
     * @param completionTime the time at which the operation completed (i.e. failed) (epoch millis)
     */
    @Asynchronous(guaranteedDelivery = true)
    void operationFailed(String jobId, Configuration result, ExceptionPackage error, long invocationTime,
        long completionTime);

    /**
     * Called by the agent when a server-scheduled operation times out.
     *
     * @param jobId          the server-assigned unique job id for this operation
     * @param invocationTime the time at which the agent was asked to invoke the operation (epoch millis)
     * @param timeoutTime    the time at which the operation timed out (epoch millis)
     */
    @Asynchronous(guaranteedDelivery = true)
    void operationTimedOut(String jobId, long invocationTime, long timeoutTime);
}